# Mask overlapping sensitive spans

Mask portions of each line identified by multiple detectors.

## Worked solution

Reconstructed completion. Run each detector on the original line so all offsets share one coordinate system. Validate bounds, discard empty spans and sort by start. Extend a current span while the next start is at or before its end.

Append untouched text up to each merged span and one replacement token, then append the final suffix. For abcdef with spans [1,4) and [3,5), the result is a[redacted]f. A line without spans passes through unchanged.

## Reference implementation

Reconstructed Python example.

```python
def mask(line, spans):
    pieces = []
    cursor = 0
    for start, end in sorted(spans):
        if not 0 <= start <= end <= len(line):
            raise ValueError("span outside line")
        if start == end or end <= cursor:
            continue
        if start > cursor or not pieces:
            pieces.extend([line[cursor:start], "[redacted]"])
        cursor = end
    return "".join(pieces) + line[cursor:]


assert mask("abcdef", [(1, 4), (3, 5)]) == "a[redacted]f"
assert mask("abc", [(0, 1), (1, 2)]) == "[redacted]c"
```
