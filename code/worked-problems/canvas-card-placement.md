# Canvas card placement

Build an ordered list of cards with canvas-specific ranking, eligibility and mandatory positions.

## Worked solution

For the recency canvas, sort eligible news, sports and weather by descending time. Sort ads separately and consume the first two for slots 3 and 8. For the score canvas, rank by descending score, reserve slot 2 for the highest-ranked weather card and slot 5 for an ad, then use news for the open slots. Discard surplus weather.

Allocate an output array of the requested length. Convert each one-based slot to an index once. Fail the build if any mandatory slot is out of range or its pool is empty. Walk the empty slots in order, consuming each remaining eligible card at most once.

## Reference implementation

Reconstructed Python example.

```python
def build_canvas(cards, size, canvas):
    policies = {
        "A": ("time", {"news", "sports", "weather"}, [(3, "ad"), (8, "ad")]),
        "B": ("score", {"news"}, [(2, "weather"), (5, "ad")]),
    }
    rank, flowing, slots = policies[canvas]
    ranked = sorted(cards, key=lambda card: card[rank], reverse=True)
    result = [None] * size
    for position, kind in slots:
        if position > size:
            raise ValueError("layout too short for required slot")
        index = next((i for i, card in enumerate(ranked) if card["kind"] == kind), None)
        if index is None:
            raise ValueError("missing required " + kind)
        result[position - 1] = ranked.pop(index)
    candidates = iter(card for card in ranked if card["kind"] in flowing)
    for index, card in enumerate(result):
        if card is None:
            result[index] = next(candidates, None)
            if result[index] is None:
                if any(item is not None for item in result[index + 1:]):
                    raise ValueError("insufficient content before required slot")
                return result[:index]
    return result


cards = [dict(kind=k, score=i) for i, k in enumerate(["ad", "news", "weather", "news", "news"])]
assert [c["kind"] for c in build_canvas(cards, 5, "B")] == ["news", "weather", "news", "news", "ad"]
```
