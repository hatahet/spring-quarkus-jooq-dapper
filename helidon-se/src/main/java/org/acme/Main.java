package org.acme;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.config.Config;
import io.helidon.health.HealthCheckResponse;
import io.helidon.health.HealthCheckType;
import io.helidon.http.media.jackson.JacksonSupport;
import io.helidon.tracing.providers.opentelemetry.HelidonOpenTelemetry;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.observe.ObserveFeature;
import io.helidon.webserver.observe.health.HealthObserver;
import io.helidon.webserver.observe.metrics.MetricsObserver;
import io.helidon.webserver.observe.tracing.TracingObserver;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.acme.config.Database;
import org.acme.repository.FruitRepository;
import org.acme.rest.FruitController;
import org.acme.service.FruitService;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

public final class Main {
  private Main() {}

  public static void main(String[] args) {
    Config config = Config.create();
    var telemetry = AutoConfiguredOpenTelemetrySdk.builder()
        .addPropertiesSupplier(() -> config.get("otel").asMap().orElse(java.util.Map.of()))
        .setResultAsGlobal().build().getOpenTelemetrySdk();
    var tracer = HelidonOpenTelemetry.create(telemetry, telemetry.getTracer("helidon-se"), java.util.Map.of());
    var database = new Database(config.get("datasource"), telemetry);
    var validators = Validation.byDefaultProvider().configure()
        .messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory();
    var service = new FruitService(new FruitRepository(database.dsl()), telemetry.getTracer("org.acme"));
    try {
      var server = serverBuilder(config, service, validators.getValidator())
          .addFeature(ObserveFeature.builder()
              .addObserver(HealthObserver.builder()
                  .addCheck(() -> HealthCheckResponse.builder().status(database.healthy()).build(),
                      HealthCheckType.READINESS, "database").build())
              .addObserver(MetricsObserver.create())
              .addObserver(TracingObserver.create(tracer)).build())
          .build().start();
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try {
          server.stop();
        } finally {
          validators.close();
          database.close();
          telemetry.close();
        }
      }));
      System.out.println("Helidon SE started on port " + server.port());
    } catch (RuntimeException e) {
      validators.close();
      database.close();
      telemetry.close();
      throw e;
    }
  }

  public static WebServerConfig.Builder serverBuilder(Config config, FruitService service, Validator validator) {
    var mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    return WebServer.builder()
        .config(config.get("server"))
        .mediaContext(context -> context.addMediaSupport(JacksonSupport.create(mapper)))
        .routing(routing -> routing.register("/fruits", new FruitController(service, validator)));
  }
}
