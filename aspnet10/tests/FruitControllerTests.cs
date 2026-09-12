using System.Net;
using System.Net.Http.Json;
using AspNet10.Domain;
using AspNet10.Dto;
using AspNet10.Repository;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using NSubstitute;

namespace AspNet10.Tests;

public sealed class FruitControllerTests : IClassFixture<FruitApplicationFactory>
{
    private readonly FruitApplicationFactory _factory;
    private readonly IFruitRepository _repository;

    public FruitControllerTests(FruitApplicationFactory factory)
    {
        _factory = factory;
        _repository = factory.Repository;
        _repository.ClearReceivedCalls();
    }

    [Fact]
    public async Task GetAllReturnsMappedFruit()
    {
        var fruit = CreateFruit();
        _repository.FindAllAsync(Arg.Any<CancellationToken>())
            .Returns(Task.FromResult<IReadOnlyList<Fruit>>([fruit]));

        using var client = _factory.CreateClient();
        var response = await client.GetAsync("/fruits");

        response.EnsureSuccessStatusCode();
        var fruits = await response.Content.ReadFromJsonAsync<IReadOnlyList<FruitDto>>();
        var dto = Assert.Single(Assert.IsAssignableFrom<IReadOnlyList<FruitDto>>(fruits));
        Assert.Equal(1, dto.Id);
        Assert.Equal("Apple", dto.Name);
        Assert.Equal("Hearty fruit", dto.Description);
        Assert.Equal(1.29F, Assert.Single(dto.StorePrices!).Price);
        await _repository.Received(1).FindAllAsync(Arg.Any<CancellationToken>());
    }

    [Fact]
    public async Task GetUnknownFruitReturnsNotFound()
    {
        _repository.FindByNameAsync("Dragonfruit", Arg.Any<CancellationToken>())
            .Returns(Task.FromResult<Fruit?>(null));

        using var client = _factory.CreateClient();
        var response = await client.GetAsync("/fruits/Dragonfruit");

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
        await _repository.Received(1)
            .FindByNameAsync("Dragonfruit", Arg.Any<CancellationToken>());
    }

    [Fact]
    public async Task GetExistingFruitReturnsMappedFruit()
    {
        var fruit = CreateFruit();
        _repository.FindByNameAsync("Apple", Arg.Any<CancellationToken>())
            .Returns(Task.FromResult<Fruit?>(fruit));

        using var client = _factory.CreateClient();
        var response = await client.GetAsync("/fruits/Apple");

        response.EnsureSuccessStatusCode();
        var dto = await response.Content.ReadFromJsonAsync<FruitDto>();
        Assert.Equal(1, dto!.Id);
        Assert.Equal("Apple", dto.Name);
        Assert.Equal("Store 1", Assert.Single(dto.StorePrices!).Store.Name);
        await _repository.Received(1)
            .FindByNameAsync("Apple", Arg.Any<CancellationToken>());
    }

    [Fact]
    public async Task GetFruitOmitsEmptyOptionalValues()
    {
        var fruit = new Fruit { Id = 10, Name = "Kiwi", Description = "" };
        _repository.FindByNameAsync("Kiwi", Arg.Any<CancellationToken>())
            .Returns(Task.FromResult<Fruit?>(fruit));

        using var client = _factory.CreateClient();
        var response = await client.GetAsync("/fruits/Kiwi");

        response.EnsureSuccessStatusCode();
        var json = await response.Content.ReadAsStringAsync();
        Assert.DoesNotContain("\"description\"", json);
        Assert.DoesNotContain("\"storePrices\"", json);
    }

    [Fact]
    public async Task PostCreatesFruit()
    {
        _repository.SaveAsync(Arg.Any<Fruit>(), Arg.Any<CancellationToken>())
            .Returns(callInfo =>
            {
                var fruit = callInfo.Arg<Fruit>();
                fruit.Id = 11;
                return Task.FromResult(fruit);
            });

        using var client = _factory.CreateClient();
        var response = await client.PostAsJsonAsync(
            "/fruits",
            new FruitDto(null, "Grapefruit", "Summer fruit", null));

        response.EnsureSuccessStatusCode();
        var fruit = await response.Content.ReadFromJsonAsync<FruitDto>();
        Assert.Equal(11, fruit!.Id);
        Assert.Equal("Grapefruit", fruit.Name);
        await _repository.Received(1).SaveAsync(
            Arg.Is<Fruit>(candidate => candidate.Name == "Grapefruit"),
            Arg.Any<CancellationToken>());
    }

    private static Fruit CreateFruit()
    {
        var fruit = new Fruit { Id = 1, Name = "Apple", Description = "Hearty fruit" };
        var store = new Store
        {
            Id = 1,
            Name = "Store 1",
            Currency = "USD",
            Address = new Address("123 Main St", "Anytown", "USA")
        };
        fruit.StorePrices.Add(new StoreFruitPrice
        {
            Id = new StoreFruitPriceId(store.Id, fruit.Id.Value),
            Store = store,
            Fruit = fruit,
            Price = 1.29M
        });
        return fruit;
    }
}

public sealed class FruitApplicationFactory : WebApplicationFactory<Program>
{
    public IFruitRepository Repository { get; } = Substitute.For<IFruitRepository>();

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.UseEnvironment("Testing");
        builder.ConfigureServices(services =>
        {
            services.RemoveAll<IFruitRepository>();
            services.AddSingleton(Repository);
        });
    }
}
