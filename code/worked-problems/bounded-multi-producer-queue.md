# Bounded multi-producer queue

Coordinate many producers and consumers on a finite FIFO.

## Worked solution

Allocate capacity C greater than zero and maintain head, tail and size under the same mutex. Enqueue waits in a loop while size equals C, writes at tail, advances modulo C, increments size and signals a consumer. Dequeue waits while empty, reads head, advances it, decrements size and signals a producer.

Waiting atomically releases the mutex. Recheck the condition after wakeup because another thread may consume the resource first. FIFO item order alone gives no fairness guarantee to waiting consumers.
