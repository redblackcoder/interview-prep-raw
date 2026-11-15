# Extensible campaign metrics

Add dashboard metrics and dimensions without rewriting a batch pipeline for each request.

## Worked design

Ingest structured campaign events with stable event IDs. Validate required fields and deduplicate before aggregating into time buckets at campaign grain. Join dimension values valid for the event time.

Define each metric's numerator, denominator and permitted dimensions in configuration. Compute ratios from summed components. When a definition changes, version it and backfill so a dashboard cannot silently mix incompatible definitions.
