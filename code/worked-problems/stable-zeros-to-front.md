# Stable zeros to front

Move zeros to the beginning of an array while keeping the order of nonzeros.

## Worked solution

Start write at the last index. Scan read backwards. Whenever the value is nonzero, copy it to write and decrement write. Set indices zero through write to zero. Moving backwards preserves unread entries. [2,0,3,0,1] becomes [0,0,2,3,1] in linear time and constant extra space.

## Reference implementation

Reconstructed Python example.

```python
def move_zeros(values):
    write = len(values) - 1
    for read in reversed(range(len(values))):
        if values[read] != 0:
            values[write] = values[read]
            write -= 1
    for index in range(write + 1):
        values[index] = 0


values = [0, 4, 0, 2, 0]
move_zeros(values)
assert values == [0, 0, 0, 4, 2]
```
