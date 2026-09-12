package org.acme.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import io.agroal.api.AgroalDataSource;

@ApplicationScoped
public class JooqConfiguration {
  @Produces
  @Singleton
  DSLContext dslContext(AgroalDataSource dataSource) {
    return DSL.using(dataSource, SQLDialect.POSTGRES);
  }
}
