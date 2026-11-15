# Merge sorted arrays in place

Merge two sorted arrays into the first array using its spare trailing capacity.

## Worked solution

Let i and j be the final occupied indices of the two inputs and k the last destination index. While j is nonnegative, take the first array's tail if i is valid and that tail is larger, otherwise take the second array's tail. Write at k and decrement the selected cursor and k. Once the second input is exhausted the remaining prefix is already correct.

## Reference implementation

Reconstructed Python example.

```python
def merge(first, occupied, second):
    if len(first) != occupied + len(second) or occupied < 0:
        raise ValueError("wrong destination capacity")
    left, right = occupied - 1, len(second) - 1
    target = len(first) - 1
    while right >= 0:
        if left >= 0 and first[left] > second[right]:
            first[target] = first[left]
            left -= 1
        else:
            first[target] = second[right]
            right -= 1
        target -= 1


values = [2, 4, None, None]
merge(values, 2, [1, 3])
assert values == [1, 2, 3, 4]
```
