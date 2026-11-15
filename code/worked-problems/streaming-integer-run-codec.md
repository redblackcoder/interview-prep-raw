# Streaming integer run codec

Encode and lazily decode integers in repeated-value runs or groups of eight literals.

## Worked solution

In repeat mode keep a value and count. On a mismatch after at least eight repeats, emit the run and start a fresh candidate with the incoming value. On an earlier mismatch, copy the repeats and the triggering value into a literal buffer. Emit that buffer when it reaches eight and reset. At end of stream emit the pending run, even if short.

Decode with a run index and an offset. Yield the repeated value or the indexed literal, increment the offset and reset it to zero when advancing to the next run. Eight identical values followed by a singleton decode as eight copies and one final value.

## Reference implementation

Reconstructed Python example.

```python
def encode(values):
    count = 0
    literals = []
    for value in values:
        if literals:
            literals.append(value)
        elif count == 0:
            repeated, count = value, 1
        elif value == repeated:
            count += 1
        elif count >= 8:
            yield ("repeat", repeated, count)
            repeated, count = value, 1
        else:
            literals = [repeated] * count + [value]
            count = 0
        if len(literals) == 8:
            yield ("literal", literals)
            literals = []
    if literals:
        yield ("literal", literals)
    elif count:
        yield ("repeat", repeated, count)


def decode(runs):
    for run in runs:
        if run[0] == "repeat":
            for _ in range(run[2]):
                yield run[1]
        else:
            yield from run[1]


for data in ([], [1], [2]*7+[3], [2]*8+[3], list(range(17))):
    assert list(decode(encode(data))) == data
```
