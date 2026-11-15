# Remove overlapping intervals

Delete the fewest intervals so the survivors do not overlap.

## Worked solution

Sort positive-length intervals by start. For interval i binary-search the first later index j with start at least end[i]. Define keep[n]=0 and compute keep[i]=max(keep[i+1],1+keep[j]) backwards. Return n−keep[0]. Counting retained intervals automatically accounts for every skipped overlap. Touching endpoints are compatible. Total time is O(n log n).

## Reference implementation

Reconstructed Python example.

```python
from bisect import bisect_left


def minimum_removals(intervals):
    intervals = sorted(intervals)
    starts = [start for start, end in intervals]
    keep = [0] * (len(intervals) + 1)
    for i in reversed(range(len(intervals))):
        successor = bisect_left(starts, intervals[i][1], lo=i + 1)
        keep[i] = max(keep[i + 1], 1 + keep[successor])
    return len(intervals) - keep[0]


assert minimum_removals([(1, 4), (1, 2), (2, 3), (3, 4)]) == 1
```
