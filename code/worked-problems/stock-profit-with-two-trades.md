# Stock profit with two trades

Maximize profit with at most two non-overlapping transactions.

## Worked solution

Reconstructed completion. Build left[i] as the best one-trade profit through day i by carrying forward the previous best and tracking the prefix minimum. Scan backward with the suffix maximum to build right[i], the best profit starting on or after i. Maximize left[i] + right[i+1] across splits, including the single-trade case.

For prices [1,5,2,7], splitting after 5 yields 4 + 5 = 9. Both scans are linear.

## Reference implementation

Reconstructed Python example.

```python
def max_profit(prices):
    suffix = [0] * (len(prices) + 1)
    peak = float("-inf")
    for i in reversed(range(len(prices))):
        peak = max(peak, prices[i])
        suffix[i] = max(suffix[i + 1], peak - prices[i])
    floor = float("inf")
    first = best = 0
    for i, price in enumerate(prices):
        floor = min(floor, price)
        first = max(first, price - floor)
        best = max(best, first + suffix[i + 1])
    return best


assert max_profit([1, 5, 2, 7]) == 9
assert max_profit([]) == 0
```
