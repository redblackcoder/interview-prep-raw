# Whole-string pattern matching

Match a string against literals, dot and the zero-or-more star operator.

## Worked solution

For a valid pattern let match(i,j) mean that text[i:] matches pattern[j:]. When j equals the pattern length, return whether i equals the text length. Set first when i is in bounds and the current atom matches text[i]. If the next pattern character is a star, return match(i,j+2) or (first and match(i+1,j)). Otherwise return first and match(i+1,j+1).

Cache each pair before returning. Short-circuiting avoids advancing beyond the text. Empty text matches a*b*, and aab matches c*a*b. There are O((n+1)(m+1)) possible pairs.

## Reference implementation

Reconstructed Python example.

```python
from functools import lru_cache


def matches(text, pattern):
    @lru_cache(None)
    def visit(i, j):
        if j == len(pattern):
            return i == len(text)
        first = i < len(text) and pattern[j] in (".", text[i])
        if pattern[j + 1:j + 2] == "*":
            return visit(i, j + 2) or (first and visit(i + 1, j))
        return first and visit(i + 1, j + 1)
    return visit(0, 0)


assert matches("aab", "c*a*b")
assert matches("", "a*b*")
assert not matches("abc", "ab")
```
