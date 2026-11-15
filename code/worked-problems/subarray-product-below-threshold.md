# Subarray product below threshold

Count contiguous subarrays of positive integers with product strictly below k.

## Worked solution

For k at most one return zero. Multiply the running product by each new value. While the product is at least k divide out the leftmost value and advance left. Add right−left+1, the number of valid suffixes. For [10,5,2,6] and k=100 the increments are 1,2,2,3, totaling 8.

## Reference implementation

Reconstructed Python example.

```python
def count_products(values, k):
    if k <= 1:
        return 0
    product, left, count = 1, 0, 0
    for right, value in enumerate(values):
        product *= value
        while product >= k:
            product //= values[left]
            left += 1
        count += right - left + 1
    return count


assert count_products([10, 5, 2, 6], 100) == 8
```
