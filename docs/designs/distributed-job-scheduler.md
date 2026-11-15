# Distributed job scheduler

Run scheduled and ad hoc jobs for many tenants with bounded start delay and retry support.

## Worked design

Materialize a stable execution ID for each scheduled occurrence. Dispatchers scan due runs and conditionally change queued to claimed, assigning a lease and an increasing fence. A worker renews while executing and writes a terminal result only if its fence still matches.

On expiry, another worker may claim a new attempt. Use the stable execution ID for idempotent external effects. Batch lease renewals and stagger scans to contain heartbeat traffic and synchronized starts.
