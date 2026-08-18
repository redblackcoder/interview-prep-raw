# Mock Design Interview — Distributed Unique ID Generator

> **Source transcript** for the wiki concept page [[system-design-concepts/distributed-id-generation]].
> Format: Rippling L8 / Staff systems-design screen (60 min, virtual whiteboard). Roles: interviewer (Claude) + candidate (me). Captured 2026-08-17.
> Prompt: **"Design a distributed unique ID generator at Claude for a cloud-scale service."**

---

## Phase 1 — Clarify the problem (~0–12 min)

**Candidate.** Let me clarify what we're building. A unique identifier here is a *globally unique* ID used to identify resources across a big system — no collisions, relied on by many services. So this is central, internal infrastructure: high throughput, highly available, effectively a P0.

Questions / assumptions on the **shape**:
- Thinking of a ~16-char GUID, grouped, alphanumeric.
- **Lowercase only / case-insensitive** so it works across systems that don't distinguish case.
- Not a tiny-URL-style short code — this is for internal service consumption, associating resources to a unique ID.

**Non-functional:**
- **Availability:** dependents may want 3 nines; so start the generator at ~4 nines. Margin is thin, but services get there via **retries** on top.
- **Latency:** retries only work if it's low-latency; assume it's on the **hot path** whenever a resource is created. Target **p99 < 5 ms**.

**Functional:**
- Globally unique.
- Possible **structure** in the ID (embed tenant / region / service?) — depends on scope.
- **Expiration/cleanup?** Space is huge, we won't run out → **no cleanup**; an issued ID is valid forever.
- **Persistence:** the service must persist which IDs are in use (← flagged later as an implementation artifact, not a requirement).

**Scale estimate (candidate's numbers):**
- Assume ~100 GUIDs/min per service ≈ ~1.6/s; round to ~100/s per service.
- ~100 services → **~10,000 QPS**.
- Storage: 16 B/GUID → trivial per year; the driver is **QPS ≈ 10K**, not storage.
- Should be **globally available** — services in many regions reach it; global uniqueness ⇒ some cross-region coordination concern.

**Interviewer (challenges):**
1. *Push on scale.* "Cloud-scale" — one request touches many services, each stamping IDs on rows/spans/logs/retries/idempotency keys. Does 10K QPS hold? Pick a number and defend it.
2. *Push hard on persistence.* Why must the generator remember every ID it ever issued? Storing+checking every issue fights the latency/throughput goals. Is persistence fundamental or an assumed design?
3. *Encoding vs generation.* You picked an *encoding* (16 lowercase chars) before a *generation scheme* — backwards; the bits you can pack depend on generation, not printing.
4. Good hook: cross-region coordination is the crux; coordination is where availability dies.

---

## Phase 2 — Refine requirements under pushback (~12–16 min)

**Candidate.**
1. **Scale — scope it down.** 10K QPS stands *because I'm scoping this service to long-lived, globally-unique business IDs* (e.g. tweet IDs, user IDs). **Trace/log/event IDs are locally-scoped and short-lived** — they don't need a central global generator; the emitting services own them. Push that class of load out of scope.
2. **Persistence — retracted.** It was an implementation artifact, not a requirement. The requirement is "globally unique." If generation *guarantees* no two IDs collide, we don't store anything. Dropped.
3. **Encoding — defended.** A known human-readable encoding matters for how engineers *use* the ID: log search, dashboards, `WHERE` clauses, DB keys. Not a raw byte blob. Keep the encoding *goal* but treat generation and presentation as separate layers.

**Interviewer.** Accepts scope-down (with a "keep headroom honest" caveat), credits the persistence retraction ("uniqueness by construction, not by remembering"), grants the encoding rebuttal but insists generation-scheme and print-encoding stay two layers.

---

## Phase 3 — High-level generation scheme (~16–32 min)

**Candidate (first pass, with math stumbles).**
- Tried alphanumeric char math: 36 symbols, confused 4-bit/char → "only 16 options," restricting the space. (Tangled bits vs encoding.)
- Reasoned toward **ever-increasing IDs** or a **time component** to avoid storage.
- Correctly argued **no central cross-region coordination** on the hot path: US↔Asia ~150 ms round trip breaks a 5 ms SLA → **generate locally per region**.
- Proposed layout: `version | region | time | sequence`. Tried µs/ns time, panicked on bit budget (10^13 → "130 bits"), and **dropped time in favor of an incrementing counter + buckets** (1024 buckets, ~34 bits counter).

**Interviewer (two corrections + the crux question).**
- **Correction 1 — decouple layers.** Payload = fixed *bits* (e.g. 64). Presentation = how you print them. Lowercase-alnum case-insensitive = **base32 = 5 bits/char**, so 16 chars = 80 bits. Print format must not shrink uniqueness space.
- **Correction 2 — time was a math panic, not a constraint.** Use **milliseconds**: `41 bits ms ≈ 69 years`. Pair coarse ms with a per-generator sequence for sub-ms disambiguation. Put time back — it buys k-sortability. Snowflake shape: `[ timestamp ~41b | worker/region ~10b | sequence ~12b ]`.
- **Crux question.** Who owns the counter, physically? Many processes in one region+bucket want IDs concurrently. Shared central counter ⇒ bottleneck+hop; independent local counters ⇒ collision. How do two processes in the same region mint concurrently, no central counter, no collision?

---

## Phase 4a — The Redis attempt (~32–40 min)

**Candidate.** Combine counter+timestamp: `version+region ~10b | timestamp ~41b | sequence`. Pre-generate via a **leader "generator"** writing one key **per millisecond** into **Redis**; callers `INCR`+`GET` the current-ms key to claim a sequence number, then assemble `version | region | time | sequence`. Bound Redis with a few-seconds **TTL** on ms-keys (only ~2–3K live keys). Multi-AZ Redis; ~3 GUID handlers across AZs; single active generator + passive standby via K8s.

**Interviewer (three challenges).**
1. **You reintroduced the hot-path network hop + a stateful P0 dependency.** `INCR` to Redis is a round trip per ID; if Redis is down, can *any* service in the region mint an ID? Walk through a 5s Redis failover.
2. **Single-leader = SPOF with a correctness cliff.** During failover, what stops a new generator re-issuing ms-keys the old one issued while an in-flight caller still increments the old one? Split-brain on the sacred invariant.
3. **The deeper one.** What does Redis give you that a `worker_id` doesn't? If each process leases a unique `worker_id` and IDs are `[timestamp | region | worker_id | local_sequence]` with an in-memory sequence, generation is purely local, no Redis, uniqueness by construction, coordination shrinks to "lease a worker_id once at startup." Defend Redis or switch.

---

## Phase 4b — Switch to worker-id model (~40–48 min)

**Candidate.** Concede the latency point (in-DC ~200µs, cross-AZ ~1ms — critical vs 5ms SLA) and the complexity point. But first, **defend the Redis design's correctness**:
1. Generator uses **create-if-absent** on ms-keys, so a standby needs no state from the dead leader → **no split-brain**.
2. The bad cross-region round trip (~150ms) is out; the Redis hop is in-region but still ~1ms, which matters.
3. Redis-failover *availability* concern is real → that's the reason to drop it.

**Switch to:** a fleet of local generators, ID = `version | region | worker | timestamp_ms | sequence`.
- Regions independent (own region-space); workers independent (own worker-space); time+sequence unique per worker.
- **Worker id** from etcd/ZooKeeper at startup (HA); *off* the hot path, so a central dep is fine. Once up, generation is fully local.
- **API:** `generateGuid(N) -> N guids`. Smart client caches in memory and prefetches when low → hides even the sidecar hop.

**Interviewer credits:** defended correctness *and still switched* ("correct ≠ best"); batch API is the right latency optimization.

---

## Phase 4c — Failure-mode deep dive (~48–52 min)

**Interviewer.** Two failure modes; #1 is a fail-the-candidate-if-handwaved item.

**Risk 1 — clock goes backward.** ID = `[time_ms | worker | seq]` assumes time only moves forward. NTP step, VM live-migration, leap seconds move wall clocks back by ms. Worker at t=1000 → NTP yanks to t=995 → keeps generating → **re-issues** 995–1000 → duplicate PK weeks later. What's the *policy at the moment the clock goes backward*?

**Candidate.** Worker keeps **last-generated in memory and persisted on disk**, refuses to go back in time, survives restarts without losing sequence.

*(Interviewer's follow-up standard: state the policy — spin/wait on small regressions vs refuse+alert on large; persist per-batch not per-ID; on crash resume from `last_persisted + batch_size`, never `last_issued`, or you replay a gap.)*

**Risk 2 — worker-id lifecycle / fencing.** How many bits; survive autoscaling churn; and the split-brain: worker leases `42`, GC-pauses/partitions 30s, lease TTL expires, etcd reassigns `42`, paused worker wakes still believing it's `42` and mints locally → two live workers, same id.

**Candidate.** Register worker with zk/etcd, **heartbeat** to keep the lease fresh; missed heartbeat → marked unhealthy → **removed from LB** so it gets no requests; a new worker takes the now-expired id. Bounded workers ⇒ bounded ids ⇒ fit the bits.

*(Interviewer's reservation: this handles the cooperative case but does NOT fence the pause/partition case — generation is local, so LB eviction isn't a fence. Needed: worker self-checks lease and stops minting **fail-closed** the instant it can't confirm the lease; reclaim only after `lease_TTL + max_clock_skew + safety_margin`.)*

---

## Follow-up deep dive — clock-backward without disk persistence (post-interview discussion)

**Candidate's question.** How to solve Risk 1 *without* disk persistence? An SSD flush is 2–3 ms — unacceptable on the generation hot path.

**Reframe: it's two sub-problems, not one. The flush pain only appears if you conflate them.**

**Sub-problem A — regression while the process is alive** (NTP slew, leap-second smear, small step). **Zero disk.**
- Seed from the wall clock **once at startup**, then drive the timestamp field from a **monotonic clock** (`CLOCK_MONOTONIC` / `System.nanoTime`) — never regresses by definition, so NTP moving the wall clock underneath can't move the ids.
- In-memory `last_issued` watermark: `==` bump sequence / spin on overflow; small `<` regression spin-wait; large `<` refuse+alert. Fully covers A, no flush.

**Sub-problem B — watermark lost across crash/restart.** The only part that ever wanted durability, and local disk is the wrong tool.
- **Insight:** you already have a durable, consistent, off-hot-path store — the **etcd/ZK worker-id lease**. Push restart-durability into the startup coordination you already do. Don't add a second durable store on the hot path.
- **The reclaim delay from Risk 2 solves B for free.** Assume `max_clock_skew` bounded (NTP-monitored; over-skew host self-ejects). Then:
  ```
  A's lease expires at E (per etcd); A self-fences ⇒ A's max timestamp ≤ E + max_skew.
  Id 42 reclaimed only after lease_TTL past E ⇒ when B gets 42, B's clock ≥ E + lease_TTL − max_skew.
  lease_TTL > 2 × max_skew  ⇒  B's clock > A's max timestamp, BY CONSTRUCTION, before B mints anything.
  ```
- Size **`lease_TTL ≥ 2·max_skew + margin`** → cross-restart clock hazard gone, no disk, no startup wait.
- **Fast-restart edge:** a pod bouncing in 500 ms must not resurrect its old id inside the cooldown. Rule: **every process start is a cold claimant** — fresh lease / new fencing token, never fast-path re-acquisition of a still-cooling id.

**If you genuinely want an explicit persisted watermark (belt + suspenders): reserve-ahead, never record-behind.**
- **Record-behind** (persist `last_issued` *after* minting) → crash resumes from stale state → **replay**. Unsafe (the trap).
- **Reserve-ahead** (persist a ceiling *before* minting) → crash resumes *above* it → **gap, never replay**. Safe by construction.
- One etcd write: "worker 42 may issue up to `now + Δ`" (Δ ≈ 2 s); mint below the ceiling with zero per-id I/O; async task bumps it ahead. Cost = **one write per Δ, off hot path**; gaps free (no contiguity requirement).

**Bottom line.** In-life regression = a *clock-source* problem (monotonic). Cross-restart regression = a *coordination* problem already paid for via the lease. The 2–3 ms flush never enters the picture.

**Residual risk to pressure-test next:** what if `max_clock_skew` is *violated* (broken NTP host that doesn't self-eject in time)? — needs a defense-in-depth answer.

---

## Phase 5 — Candidate wrap-up (~52 min)

**Summary.** 10K QPS, globally distributed, HA GUID generation for long-lived IDs. Schema `version | region | worker | timestamp | sequence`. Regions independent (region-space); workers independent (worker-space); time+sequence unique per worker. Workers = a per-region fleet, rely on zk/etcd for registration + lease + heartbeat, behind an LB that mirrors zk/etcd leased-worker state.

**Error scenarios covered:**
1. **Worker dies mid-request** → LB auto-retries on another worker (service mesh, e.g. Istio) → caller keeps HA + low latency.
2. **etcd/zk failover** → only impacts spawning new workers + lease-timeout duration; available as long as recovery < lease timeout.
3. **Region overload / backlog** → **bulk-generate** on the worker (one disk flush per batch, not per ID) and hand out per request → avoids excessive flushes under queueing.
4. **Region failover** → route to another region. Fine if there's no hard requirement that a region's GUIDs carry its exact region id; higher latency until recovery. If region bits must be preserved, the request can carry the region and the handler stamps that instead of its own — needs coordination so two regions don't mint duplicates from the same region id.

---

## Interviewer scorecard (summary)

**Recommendation: Hire (Staff / L8), one reservation.**

**Strengths:**
- Clarified before designing; **scoped load *out*** (trace/log ids) with a defensible reason.
- **Retracted the persistence assumption** cleanly under pushback → "uniqueness by construction."
- **Defended Redis's correctness and still abandoned it** — "correct ≠ best" judgment; strong breadth.
- Final architecture correct & complete; batch API + client prefetch is the right latency move.
- Covered ops/failure modes unprompted (LB retry, etcd blast radius, backpressure, region failover).

**Gaps (the reservation):**
1. **Risk 2 fencing not closed** — detected the split-brain but LB eviction isn't a fence; needed worker self-fencing fail-closed + `TTL+skew+margin` reclaim delay. Fencing should be a reflex at Staff.
2. **Risk 1 under-specified** — right instinct (watermark), missing the explicit policy (spin-small vs fail-loud-large) and the persist-per-batch / resume-forward detail.
3. **Early arithmetic** (bits/base32 confusion; µs→"130 bits"→drop-time) cost ~10 min and nudged toward Redis by accident. Keep `41b ms ≈ 69 yrs` and `5 bits/base32 char` as muscle memory.
4. **Security thread dropped** — flagged external visibility early, never closed it; sortable+external = enumerable (leaks volume/time). State internal-only, or give external surfaces an opaque/random-tail variant.

**Dimension ratings:** Clarify & frame — Strong · Requirements — Strong · High-level design — Strong · Risk deep-dive — Mixed · Tradeoffs/breadth — Very Strong · Rollout/ops — Good · Communication under pushback — Strong.

**Would've asked with +10 min:** rolling out a new `version` across the fleet (no flag day); on-call dashboard + first alert that uniqueness is at risk (clock-regression events/s, worker-id churn); region-failover coordination to prevent duplicate region ids.
