# Live auctions

Accept bids until a fixed close and determine one winner under heavy contention.

## Worked design

Partition the bid log by auction ID. Acknowledge a bid only after the log persists it. Reduce batches to the largest bid and publish an approximate current leader. Resolve equal amounts using a documented tie-breaker such as earliest accepted offset.

Put a close marker in the same ordering domain as admission. Once consumers have processed the prefix through that marker, persist the winner exactly once. A delayed live-display update cannot change the closed result.
