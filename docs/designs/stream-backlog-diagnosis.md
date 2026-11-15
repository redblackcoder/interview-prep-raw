# Stream backlog diagnosis

Recover a partitioned event pipeline whose lag grows while consumers appear healthy.

## Worked design

Measure arrival rate, drain rate and oldest unprocessed event age by partition. Look for a hot key, repeated retries or slow downstream calls on the lagging partitions. Throttle an offending source if the contract allows.

Repartition by a finer key only after deciding which ordering guarantees may be relaxed. Replay from a checkpoint and compare processed counts and backlog age as the partition drains.
