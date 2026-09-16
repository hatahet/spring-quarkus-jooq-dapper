package org.acme.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.helidon.config.Config;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

public final class Database implements AutoCloseable {
  private final HikariDataSource pool;
  private final DSLContext dsl;

  public Database(Config config, OpenTelemetry telemetry) {
    var hikari = new HikariConfig();
    hikari.setJdbcUrl(config.get("url").asString().get());
    hikari.setUsername(config.get("username").asString().get());
    hikari.setPassword(config.get("password").asString().get());
    hikari.setMaximumPoolSize(config.get("maximum-pool-size").asInt().orElse(50));
    pool = new HikariDataSource(hikari);
    dsl = DSL.using(JdbcTelemetry.create(telemetry).wrap(pool), SQLDialect.POSTGRES);
  }

  public DSLContext dsl() {
    return dsl;
  }

  public boolean healthy() {
    try (var connection = pool.getConnection()) {
      return connection.isValid(2);
    } catch (java.sql.SQLException e) {
      return false;
    }
  }

  @Override
  public void close() {
    pool.close();
  }
}
