package org.acme;

import io.helidon.microprofile.server.Server;

public final class Main {
  private Main() {}

  public static void main(String[] args) {
    var server = Server.create().start();
    System.out.println("Helidon MP started on port " + server.port());
  }
}
