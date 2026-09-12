using System.Diagnostics;
using AspNet10.Dto;
using AspNet10.Mapping;
using AspNet10.Repository;

namespace AspNet10.Service;

public sealed class FruitService(IFruitRepository fruitRepository)
{
    public const string ActivitySourceName = "AspNet10.FruitService";
    private static readonly ActivitySource ActivitySource = new(ActivitySourceName);

    public async Task<IReadOnlyList<FruitDto>> GetAllFruitsAsync(
        CancellationToken cancellationToken = default)
    {
        using var activity = ActivitySource.StartActivity("FruitService.getAllFruits");
        var fruits = await fruitRepository.FindAllAsync(cancellationToken);
        return fruits.Select(FruitMapper.ToDto).ToArray();
    }

    public async Task<FruitDto?> GetFruitByNameAsync(
        string name,
        CancellationToken cancellationToken = default)
    {
        using var activity = ActivitySource.StartActivity("FruitService.getFruitByName");
        activity?.SetTag("arg.name", name);
        var fruit = await fruitRepository.FindByNameAsync(name, cancellationToken);
        return fruit is null ? null : FruitMapper.ToDto(fruit);
    }

    public async Task<FruitDto> CreateFruitAsync(
        FruitDto fruitDto,
        CancellationToken cancellationToken = default)
    {
        using var activity = ActivitySource.StartActivity("FruitService.createFruit");
        activity?.SetTag("arg.fruit", fruitDto.ToString());
        var fruit = await fruitRepository.SaveAsync(FruitMapper.ToDomain(fruitDto), cancellationToken);
        return FruitMapper.ToDto(fruit);
    }
}
