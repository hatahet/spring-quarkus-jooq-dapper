package org.acme.config;

import javax.sql.DataSource;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@ApplicationScoped
public class JooqConfiguration {
  @Produces
  @Singleton
  DSLContext dslContext(@Named("fruits") DataSource dataSource, OpenTelemetry telemetry) {
    return DSL.using(JdbcTelemetry.create(telemetry).wrap(dataSource), SQLDialect.POSTGRES);
  }
}
