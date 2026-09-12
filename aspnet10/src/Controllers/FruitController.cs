using AspNet10.Dto;
using AspNet10.Service;
using Microsoft.AspNetCore.Mvc;

namespace AspNet10.Controllers;

[ApiController]
[Route("fruits")]
public sealed class FruitController(FruitService fruitService) : ControllerBase
{
    [HttpGet]
    [ProducesResponseType<IReadOnlyList<FruitDto>>(StatusCodes.Status200OK)]
    public async Task<ActionResult<IReadOnlyList<FruitDto>>> GetAll(
        CancellationToken cancellationToken)
    {
        return Ok(await fruitService.GetAllFruitsAsync(cancellationToken));
    }

    [HttpGet("{name}")]
    [ProducesResponseType<FruitDto>(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<FruitDto>> GetFruit(
        string name,
        CancellationToken cancellationToken)
    {
        var fruit = await fruitService.GetFruitByNameAsync(name, cancellationToken);
        return fruit is null ? NotFound() : Ok(fruit);
    }

    [HttpPost]
    [ProducesResponseType<FruitDto>(StatusCodes.Status200OK)]
    public async Task<ActionResult<FruitDto>> AddFruit(
        [FromBody] FruitDto fruit,
        CancellationToken cancellationToken)
    {
        return Ok(await fruitService.CreateFruitAsync(fruit, cancellationToken));
    }
}
