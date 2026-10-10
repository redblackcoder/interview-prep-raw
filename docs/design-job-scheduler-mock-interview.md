# Mock Design Interview — Distributed Job Scheduler

> **Source transcript** for the wiki. Full raw dialogue preserved (organized into phases; wording lightly cleaned).
> Format: **Senior Staff** systems-design round (~20 min). Roles: interviewer (Claude, "Distinguished Engineer at a big tech company") + candidate (me, presenting a pre-prepared design, then grilled). Captured 2026-09-26.
> Prompt: **Design a job scheduler for the company.** Multi-tenant; cron-scheduled jobs + ad-hoc "execute now"; history and live status (running / queued / delayed).
> Interviewer's closing verdict: **no hire at Sr. Staff (~5/10); lean hire Senior, borderline Staff** — architecture at bar (scheduler/dispatcher/driver layering, driver as sandbox trust boundary, lookahead + pre-dispatch delay); the claim operation was never given after two direct asks, exactly-once was over-claimed then conceded, the scale requirement was re-scoped mid-round, and mutation-after-materialization was skipped.

---

## Phase 1 — Requirements (candidate)

**Functional.**
- User can schedule jobs, driven by a **cron expression** (daily, weekly, every few hours, …).
- Per schedule, an **instance** of the job executes.
- **History**: when a job last ran, result, pass/fail.
- **Live status**: which jobs are running, queued, delayed.
- **Execute-now API**: ad-hoc / one-time job, no schedule.

**Non-functional.**
- Big scale: **~10,000 jobs executing every second**.
- **Exactly-once** execution — no duplicates.
- Start within **~1 second** of scheduled time (job may run longer; the *start* delay is bounded).
- Highly available — status must always be visible.
- History / telemetry of what happened to each job.
- Assumed **multi-tenant** — 10k/s is spread across many tenants.

---

## Phase 2 — High-level design (candidate, prepared)

**Sharding.** Data store sharded by **tenant ID**; each service deployment works on a subset of shards/tenants.

**Data model.**
1. **Job definition** — what the user creates: schedule, parameters, pointer to binary (binary lives in object store; table is metadata).
2. **Job execution** — one instance of a definition; one-to-many from definition. Has **status** (`queued → running → success | failed`) and an **attempt count** with a retry limit.

**Scheduler.** Polls the DB every couple of minutes for definitions due in the **next 5 minutes**; transactionally creates `queued` execution rows. Poll interval deliberately **shorter than the lookahead window** so a failed loop gets a retry before the jobs are due.

**Dispatcher.** Looks at executions due in the next ~5–10 s (configurable) and issues them to workers **ahead of time with a delay** — e.g. assign 5 s before midnight, worker waits 5 s, starts exactly at midnight. Gives slack for retries while hitting the start SLA. Polls every few seconds.

**Redis as dispatch cache.** Polling the relational DB every few seconds would be heavy I/O, so executions due in the next ~minute are also written to a **Redis cluster**. No distributed transaction — use a **transactional outbox**: the execution insert also writes an outbox row "needs to go to Redis"; mark it done once written; a **reconciliation job** re-pushes anything unsynced. Redis entries have a **TTL (~30 min)**; the DB remains the system of record.

**Placement / priority.** Dispatcher picks a target by heuristic (capacity), and can implement **priority** — separate Redis sets per priority class, high priority dispatched first.

**Exactly-once (as presented).** Dispatcher submits to worker → updates execution to `running` in DB → deletes from Redis. If the Redis delete fails, the next poll finds it, checks the DB status, sees `running`, skips. If the dispatcher submits but crashes before the DB write, the worker double-checks/flips status to `running` before starting.

**Driver (sandbox trust boundary).** Workers run arbitrary user binaries, so they must not have DB / object-store credentials. Introduce a **driver**: dispatcher → driver → worker. The driver is trusted control code — reports status, writes to DB — and hands the job to a sandboxed worker.

**Two-level resource management.** Dispatcher does coarse placement at the **pool** level (each driver owns a pool of workers); the driver does fine-grained worker selection. Reduces dispatcher metadata and speeds decisions.

**Heartbeats.** Driver heartbeats job progress to the DB; missing heartbeats ⇒ job considered dead and eligible for retry.

---

## Phase 3 — Interviewer's grilling questions

**Interviewer.** Clear walkthrough; layering is sensible. Pushing hardest on exactly-once and the 1-second SLA.

**Q1 — Dispatch race.**
- Multiple dispatcher instances per shard. A and B both read execution X from Redis in the same cycle; both see `queued`. What stops both submitting?
- A submits X to a driver then crashes before the DB write. Next dispatcher sees `queued` and re-submits. The worker "double-checks" — checks what, writes what? Exact state transition and owner.
- *Wants the specific DB operation, not "we check the status."*

Queued:
2. **What does exactly-once mean?** Driver partitioned 60 s, job still running; heartbeats stop, retry elsewhere ⇒ two copies. Prevent it, or what's the customer contract? Is exactly-once even promisable for arbitrary binaries (charge a card, then crash)?
3. **Capacity with numbers.** 10k starts/s average, cron isn't average — peak at 00:00 UTC on the 1st? DB writes/s per execution lifecycle (insert, dispatch, running, heartbeats, terminal)? 10-min average job ⇒ how many concurrent, heartbeat cost?
4. **Midnight thundering herd vs 1 s SLA.** 1M jobs due at 00:00:00 — what happens to job #900,000? What gives: SLA, priority, admission control?
5. **Redis durability.** Failover loses last second of async-replicated writes; outbox already says "synced." Who notices before start time is missed?
6. **Mutation after materialization.** User edits/deletes a definition or pauses a tenant after the next 5 minutes are already in DB + Redis.
7. **Execute-now.** Skips lookahead — how does it hit 1 s; can a tenant flood it and starve scheduled work?
8. **Hot tenant.** One tenant is 30% of a shard; shard key is tenant ID.

---

## Phase 4 — Candidate's answers (round 1)

**Q1.** The dispatcher already sent the job but couldn't write the DB or delete from Redis; a retry/another dispatcher will try again. With the driver, the **driver** writes `running` to the DB (if not already running) *before* starting the job on the worker, then retries the worker submit until the job starts.
"I see the problem now — we moved the issue from the dispatcher to the driver." If the worker starts but the ack to the driver is lost, the driver starts it on a different worker ⇒ two copies. **Exactly-once is really hard** because jobs have arbitrary side effects and we can't impose idempotence; **at-least-once** is the guarantee we can provide.

**Q2.** Same thing — heartbeat lost ⇒ marked dead ⇒ retried. Could add a driver-side DB within a pool to record running status, but across pools drivers would need to reconcile. **Go with at-least-once.**

**Q3 — capacity.** "10,000 starting per second is the peak I was considering… let's consider this as the number of **concurrent** jobs running — 10k running at any instant."
- Status updates `queued → running → done`: "10k running means 10k ending and 10k starting every moment" ⇒ ~10k QPS writes + ~10k reads for status checks.
- Heartbeat every 30 s ⇒ 10k / 30 ≈ ~400 QPS.
- ≈ 21k QPS status.
- Scheduler writes creating executions at "a 1:10 ratio" ⇒ +2k ⇒ ~23k.
- +30% buffer for reconciliation and retries ⇒ ~7k ⇒ **~30k QPS**.
- ~5k IOPS per DB ⇒ **6-way shard**.
- Frequent updates on the same row are bad for Postgres (MVCC bloat, vacuum load) ⇒ move status out. Redis vs DynamoDB: Redis can lose recent writes (snapshotting; AOF `always` fsync too slow) ⇒ **DynamoDB for status, heartbeats, lifecycle events, telemetry**; reconcile with the relational DB on the read side.

**Q4 — thundering herd.** A 1 s SLA with everyone at midnight isn't reasonable. Option 1: **we control the exact time** — user gives "daily, in this range," system spreads it. Option 2: **priority** — only a quota (e.g. 10%) of jobs get the 1 s guarantee; the rest can take longer.

**Q5 — Redis durability.** Keep a **watermark** of the last schedule Redis has seen; if the dispatcher sees a gap between now and Redis's latest, fall back to the DB to fetch missing ones and refill Redis.

**Q7 — execute-now.** Skips the scheduler: immediately creates the Redis entry and nudges the dispatcher. Stricter per-tenant rate limit; return 4xx ("4-9s", meant 429) if over rate or over capacity.

**Q6 — mutation.** "Not in the initial requirements — skipping for now given time."

---

## Phase 5 — Interviewer pushback

- **Q1 still unanswered.** Crash-after-submit explained; two-dispatchers-same-cycle not. "Read status, then act" isn't atomic. What statement, who runs it, *before* anything reaches a driver?
- **At-least-once: right answer, reached reactively.** Should be stated up front. What *is* guaranteed — stable ID usable as idempotency key? At-most-once option? Does an orphaned driver self-kill, and when?
- **Capacity: requirement changed and math doesn't reconcile.** 10k/s executing ≠ 10k concurrent (~600×). With 10-min jobs, does 10k running mean 10k starting/s? Scheduler's execution insert *is* the same insert — why the 1:10 add-on? Redo with 10k starts/s, 10-min average: concurrency, heartbeat writes/s at 30 s, transition writes/s, which dominates.
- **DynamoDB for status breaks Q1.** Moving heartbeats/telemetry off Postgres is right; moving the *state machine* out of the source of truth means the claim spans two stores; reconcile-on-read is where bugs live. Which data needs transactional semantics vs high-volume noise?
- **Herd: good instinct, incomplete.** Spreading (Jenkins `H`) is legit but changes the contract — what about the 00:00 financial close? Cron load is **known in advance** (scheduler materialized next 5 min) — what can you do with that? SLA = every job or p99?
- **Redis durability too vague.** Watermark catches tail loss, not a hole in the middle. Simplest DB query for "due soon, still queued, not claimed"?
- **Execute-now skipped the source of truth.** Redis failover loses a job the user got a 200 for.
- **Mutation: pushing back on skipping.** Delete-then-it-runs-anyway is a correctness bug and a day-one ticket; the fix is ~30 s.
- **Hot tenant: unanswered.**
- *Q1 (exact claim op) and the capacity redo decide the round.*

---

## Phase 6 — Candidate (round 2: hot tenant only)

A hot tenant loads both scheduler (many crons to materialize) and dispatcher (many Redis entries). Make scheduler/dispatcher multi-threaded or run multiple instances over **pages** of schedules/executions; further shard the scheduler by **job ID**. Or change the shard key to **job ID** (embedding tenant ID + UUID) so jobs distribute across shards and there's no page-ownership coordination.

---

## Phase 7 — Interviewer pushback (hot tenant) + time call

- Job-ID partitioning *within* a shard is a fine work-distribution tactic; changing the **shard key** trades one problem for several:
  - Tenant-scoped reads ("my running/queued/delayed jobs") become **scatter-gather** — availability impact on a must-always-work API.
  - Tenant operations (quotas, rate limits, pause, delete) become cross-shard coordination.
  - **Lost isolation** — one tenant's spike now smeared across every shard.
  - CPU isn't the real hot-tenant problem — **fairness over driver/worker capacity** is. Where is a tenant's share enforced?
  - Middle ground: keep tenant sharding, split only the hot few — what's needed?
- ~15 min mark. Two gaps that dominate the write-up, still asked: (1) the claim statement and its placement relative to submit; (2) the capacity redo in one line each.

**Candidate.** "We are timed out. Wrap up now and give the judgement."

---

## Phase 8 — Interviewer's closing verdict (summary)

**No hire at Sr. Staff, ~5/10** (lean hire Senior, borderline Staff).
- **Landed:** scheduler/dispatcher/driver layering; driver as control-plane/sandbox trust boundary; two-level (pool → worker) placement; lookahead with poll < window; pre-dispatch with start delay; transactional outbox to Redis; honest pivot to at-least-once.
- **Hurt:** claim operation never given (should be `UPDATE executions SET state='CLAIMED', owner=:d, fence=fence+1, lease_until=now()+30s WHERE id=:x AND state='QUEUED'` — claim *before* submit; fenced transitions; lease renewal = heartbeat; self-kill on lost lease); exactly-once over-claimed; scale requirement re-scoped; Little's-law error (correct: 10k/s × 600 s = 6M concurrent ⇒ 200k heartbeat writes/s at 30 s; ~40k transition writes/s; batch heartbeats per driver); state machine split into DynamoDB; watermark instead of a DB sweeper; execute-now bypassing the DB; mutation skipped (fix: `definition_version` checked in the claim CAS); hot tenant answered with shard-key change instead of per-tenant fair share + hot-tenant split.
- **Craft:** answer the asked question first; never take on a guarantee you'll give back; don't re-scope scary numbers — follow them; "not in requirements" is never an answer to a correctness question; shorter turns.
