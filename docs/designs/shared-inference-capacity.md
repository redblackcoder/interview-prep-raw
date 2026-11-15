# Shared inference capacity

Share a fixed number of concurrent inference slots among latency-sensitive features.

## Worked design

Authenticate callers and assign a tier and weight. Keep deadline-bounded queues. On a free slot choose the highest nonempty tier, then choose a waiting feature proportionally to its weight. If one feature is idle its peers can use its share.

Count active grants globally or lease disjoint slot budgets to gateways. Reclaim only after the underlying call has ended or been safely fenced. Expire queued requests promptly and return a retry hint under sustained overload.
