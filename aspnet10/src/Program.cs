using System.Text.Json.Serialization;
using System.Text.Json.Serialization.Metadata;
using AspNet10.Health;
using AspNet10.Repository;
using AspNet10.Service;
using OpenTelemetry.Metrics;
using OpenTelemetry.Resources;
using OpenTelemetry.Trace;
using Npgsql;

var builder = WebApplication.CreateBuilder(new WebApplicationOptions
{
    Args = args,
    ContentRootPath = AppContext.BaseDirectory
});

builder.Services
    .AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull;
        var resolver = new DefaultJsonTypeInfoResolver();
        resolver.Modifiers.Add(typeInfo =>
        {
            foreach (var property in typeInfo.Properties.Where(
                         property => property.PropertyType == typeof(string)))
            {
                property.ShouldSerialize = (_, value) => value is string { Length: > 0 };
            }
        });
        options.JsonSerializerOptions.TypeInfoResolver = resolver;
    });

builder.Services.AddSingleton(serviceProvider =>
{
    var configuration = serviceProvider.GetRequiredService<IConfiguration>();
    var connectionString = configuration.GetConnectionString("Fruits")
        ?? throw new InvalidOperationException("ConnectionStrings:Fruits is required");

    return new NpgsqlDataSourceBuilder(connectionString).Build();
});
builder.Services.AddSingleton<IFruitRepository, FruitRepository>();
builder.Services.AddSingleton<FruitService>();
builder.Services.AddHealthChecks().AddCheck<DatabaseHealthCheck>("postgresql");

builder.Services
    .AddOpenTelemetry()
    .ConfigureResource(resource => resource.AddService("aspnet10"))
    .WithTracing(tracing => tracing
        .SetSampler(new TraceIdRatioBasedSampler(0.1))
        .AddSource(FruitService.ActivitySourceName)
        .AddAspNetCoreInstrumentation()
        .AddNpgsql()
        .AddOtlpExporter())
    .WithMetrics(metrics => metrics
        .AddAspNetCoreInstrumentation()
        .AddRuntimeInstrumentation()
        .AddPrometheusExporter()
        .AddOtlpExporter());

var app = builder.Build();

app.MapControllers();
app.MapHealthChecks("/health");
app.MapPrometheusScrapingEndpoint("/metrics");

app.Run();

public partial class Program;
