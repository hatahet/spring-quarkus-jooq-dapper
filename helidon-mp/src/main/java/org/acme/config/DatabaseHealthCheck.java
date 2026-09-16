package org.acme.config;

import javax.sql.DataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {
  private final DataSource dataSource;

  @Inject
  public DatabaseHealthCheck(@Named("fruits") DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public HealthCheckResponse call() {
    try (var connection = dataSource.getConnection()) {
      return HealthCheckResponse.named("database").status(connection.isValid(2)).build();
    } catch (java.sql.SQLException e) {
      return HealthCheckResponse.down("database");
    }
  }
}
