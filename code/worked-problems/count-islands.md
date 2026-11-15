# Count islands

Count four-connected land components in a rectangular grid.

## Worked solution

Scan row by row. On unvisited land increment the count, mark the cell and push it onto a stack. Pop cells and push each in-bounds unvisited land neighbor above, below, left or right, marking on push. Diagonal contact leaves two components. Every cell is processed at most once.

## Reference implementation

Reconstructed Python example.

```python
def islands(grid):
    seen = set()
    count = 0
    for row, cells in enumerate(grid):
        for col, cell in enumerate(cells):
            if cell != "1" or (row, col) in seen:
                continue
            count += 1
            pending = [(row, col)]
            seen.add((row, col))
            while pending:
                r, c = pending.pop()
                for x, y in ((r-1,c), (r+1,c), (r,c-1), (r,c+1)):
                    if 0 <= x < len(grid) and 0 <= y < len(grid[x]):
                        if grid[x][y] == "1" and (x,y) not in seen:
                            seen.add((x,y))
                            pending.append((x,y))
    return count


assert islands(["110", "000", "011"]) == 2
```
