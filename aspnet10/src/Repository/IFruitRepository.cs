using AspNet10.Domain;

namespace AspNet10.Repository;

public interface IFruitRepository
{
    Task<IReadOnlyList<Fruit>> FindAllAsync(CancellationToken cancellationToken = default);
    Task<Fruit?> FindByNameAsync(string name, CancellationToken cancellationToken = default);
    Task<Fruit> SaveAsync(Fruit fruit, CancellationToken cancellationToken = default);
}
