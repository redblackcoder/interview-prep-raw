# Board winner check

Detect a winning row, column or diagonal on a square game board.

## Worked solution

For a snapshot, inspect each row, each column and both diagonals. A line wins if every position holds the same nonempty marker. This costs O(n²).

For an incremental board reject occupied cells, then increment counters for the player's row and column and for each diagonal containing the move. A counter reaching n signals victory. The center of an odd board can affect both diagonals.
