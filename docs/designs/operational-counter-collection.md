# Operational counter collection

Aggregate application counter increments for time-window queries.

## Worked design

Accumulate increments by metric, dimensions and bucket in a local agent. Flush compact batches to collectors, then persist to a partitioned log. Reduce deltas into time-series buckets and roll up older data.

Assign a stable batch ID for collector retries to avoid counting a batch twice. Bound local buffers and expose dropped increments. Edge loss may suit operational telemetry but would change billing totals.
