# Distributed ID generation

Allocate unique identifiers from multiple machines without a central write for every ID.

## Worked design

For range allocation, atomically advance a durable high-water mark by B and return the old to new interval to one worker. The worker hands out IDs locally until exhausted. Never recycle a partially consumed range after a crash.

Larger ranges amortize coordination at the cost of gaps. For time-sortable IDs, assign nonoverlapping worker epochs, cap the per-tick sequence, and wait on clock rollback or sequence exhaustion. Neither approach implies gap-free global creation order.
