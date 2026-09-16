#!/usr/bin/env python3
"""Check packaged Helidon apps using isolated PostgreSQL and an OTLP receiver.

Build both modules first, then run from any directory:
    python3 scripts/tests/helidon-smoke-test.py [helidon-se | helidon-mp]
Requires Java 21+, Docker, and Python 3. Logs are saved in each module's target/.
"""
import argparse
import concurrent.futures
import gzip
import http.server
import json
import pathlib
import shutil
import socket
import subprocess
import threading
import time
import urllib.error
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[2]
JAVA = shutil.which('java') or 'java'
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('modules', nargs='*', help='helidon-se and/or helidon-mp; defaults to both')
modules = parser.parse_args().modules or ['helidon-se', 'helidon-mp']
for module in modules:
    if module not in ('helidon-se', 'helidon-mp'):
        parser.error(f'Unknown module: {module}')
    if not (ROOT / module / 'target' / f'{module}.jar').is_file():
        parser.error(f'Build {module} first: ./mvnw -f {module}/pom.xml package')
payloads = []
class Collector(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        body = self.rfile.read(int(self.headers['Content-Length']))
        if self.headers.get('Content-Encoding') == 'gzip':
            body = gzip.decompress(body)
        payloads.append(body)
        self.send_response(200)
        self.send_header('Content-Type', 'application/x-protobuf')
        self.end_headers()
    def log_message(self, *args):
        pass
collector = http.server.ThreadingHTTPServer(('127.0.0.1', 0), Collector)
threading.Thread(target=collector.serve_forever, daemon=True).start()
def docker(*args, **kwargs):
    return subprocess.check_output(['docker', *args], text=True, **kwargs).strip()
def request(port, path, body=None, accept='application/json'):
    data = None if body is None else body.encode()
    req = urllib.request.Request(f'http://127.0.0.1:{port}{path}', data=data,
        headers={'Content-Type': 'application/json', 'Accept': accept})
    try:
        with urllib.request.urlopen(req, timeout=5) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as error:
        return error.code, error.read()
container = docker('run', '--rm', '-d', '-p', '127.0.0.1::5432', '-e', 'POSTGRES_USER=fruits',
                   '-e', 'POSTGRES_PASSWORD=fruits', '-e', 'POSTGRES_DB=fruits', 'postgres:17')
try:
    dbport = docker('port', container, '5432/tcp').rsplit(':', 1)[1]
    for _ in range(60):
        result = subprocess.run(['docker', 'exec', container, 'pg_isready', '-h', '127.0.0.1', '-U', 'fruits', '-d', 'fruits'], capture_output=True)
        if result.returncode == 0:
            break
        time.sleep(.25)
    else:
        raise AssertionError('PostgreSQL did not become ready')
    with (ROOT / 'scripts/dbdata/db.sql').open() as sql:
        subprocess.run(['docker', 'exec', '-i', container, 'psql', '-U', 'fruits', '-v', 'ON_ERROR_STOP=1'], stdin=sql, check=True, stdout=subprocess.DEVNULL)
    for module in modules:
        payloads.clear()
        with socket.socket() as sock:
            sock.bind(('127.0.0.1', 0))
            port = sock.getsockname()[1]
        collector_url = f'http://127.0.0.1:{collector.server_port}'
        args = [JAVA, f'-Dserver.port={port}']
        jdbc = f'jdbc:postgresql://127.0.0.1:{dbport}/fruits'
        if module == 'helidon-se':
            args += [f'-Ddatasource.url={jdbc}', f'-Dotel.exporter.otlp.endpoint={collector_url}', '-Dotel.traces.sampler=always_on']
            health, metrics = '/observe/health/ready', '/observe/metrics'
        else:
            args += [f'-Djavax.sql.DataSource.fruits.dataSource.url={jdbc}',
                     f'-Dotel.exporter.otlp.endpoint={collector_url}', '-Dotel.traces.sampler=always_on']
            health, metrics = '/health/ready', '/metrics'
        args += ['-jar', str(ROOT / module / 'target' / f'{module}.jar')]
        log_path = ROOT / module / 'target' / 'smoke-test.log'
        with log_path.open('w') as log:
            process = subprocess.Popen(args, stdout=log, stderr=subprocess.STDOUT)
            try:
                for _ in range(100):
                    if process.poll() is not None:
                        raise AssertionError(f'{module} exited {process.returncode}; see {log_path}')
                    try:
                        status, body = request(port, '/fruits')
                        if status == 200:
                            break
                    except (OSError, urllib.error.URLError):
                        pass
                    time.sleep(.2)
                else:
                    raise AssertionError(f'{module} did not start; see {log_path}')
                fruits = json.loads(body)
                assert len([f for f in fruits if f['id'] <= 10]) == 10
                assert sum(len(f.get('storePrices', [])) for f in fruits if f['id'] <= 10) == 34
                assert fruits[0]['storePrices'][0]['price'] == 1.29
                assert request(port, '/fruits/Unknown')[0] == 404
                assert request(port, '/fruits', '{"name":"   "}')[0] == 400
                name = 'Smoke-' + module
                status, body = request(port, '/fruits', json.dumps({'name': name}))
                created = json.loads(body)
                assert status == 200 and created['id'] > 10
                assert 'description' not in created and 'storePrices' not in created, created
                assert json.loads(request(port, '/fruits/' + name)[1])['id'] == created['id']
                with concurrent.futures.ThreadPoolExecutor(max_workers=16) as executor:
                    statuses = list(executor.map(lambda _: request(port, '/fruits')[0], range(100)))
                assert set(statuses) == {200}
                status, body = request(port, health)
                assert status in (200, 204), (status, body)
                if module == 'helidon-se':
                    assert request(port, health + '/database')[0] in (200, 204)
                else:
                    assert b'UP' in body and b'database' in body
                status, body = request(port, metrics, accept='text/plain')
                assert status == 200 and b'# TYPE' in body, (status, body[:200])
                connections = int(docker('exec', container, 'psql', '-U', 'fruits', '-Atc',
                    "select count(*) from pg_stat_activity where usename='fruits' and backend_type='client backend' and pid <> pg_backend_pid()"))
                assert 0 < connections <= 50, connections
                for _ in range(50):
                    payload = b''.join(payloads)
                    if b'FruitService.getAllFruits' in payload and b'SELECT' in payload.upper():
                        break
                    time.sleep(.2)
                assert b'FruitService.getAllFruits' in payload, f'{module}: missing service spans'
                assert b'SELECT' in payload.upper(), f'{module}: missing JDBC spans'
                print(f'{module}: API, validation, concurrent reads, health, metrics, OTLP service/JDBC spans passed; {connections} pooled connections', flush=True)
            finally:
                process.terminate()
                try:
                    process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()
            connections = int(docker('exec', container, 'psql', '-U', 'fruits', '-Atc',
                "select count(*) from pg_stat_activity where usename='fruits' and backend_type='client backend' and pid <> pg_backend_pid()"))
            assert connections == 0, f'{module}: {connections} connections remain after shutdown'
            print(f'{module}: all pooled connections closed on shutdown', flush=True)
finally:
    docker('stop', container)
    collector.shutdown()
