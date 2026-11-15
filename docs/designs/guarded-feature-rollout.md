# Guarded feature rollout

Detect engagement regressions during frequent releases by independent teams.

## Worked design

Assign exposure consistently per user and record release version with each event. Compare canary and concurrent control over a sample large enough to distinguish noise. Monitor errors and latency alongside engagement.

Halt or revert a cohort when the guardrail crosses its threshold. Keep an exposure ledger so overlapping experiments can be isolated and a regression traced to its release.
