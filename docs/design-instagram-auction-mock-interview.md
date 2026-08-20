# Mock Design Interview — Instagram Auctions

> **Source transcript** for the wiki. Full raw dialogue preserved (organized into phases; wording lightly cleaned).
> Format: Google **Senior Staff (L7)** systems-design screen (~40 min). Roles: interviewer (Claude, "Distinguished Engineer at Google") + candidate (me). Captured 2026-08-20.
> Prompt: **Design an auction feature for Instagram.** A user posts an item for auction; other users bid; the auction has a fixed end time; at close the highest bidder wins. Assume feed/distribution is solved (another team). No payments, no shipping — scope is *post → bid → clock runs out → declare highest bidder*.
> Interviewer's closing verdict: **lean hire, staff-level moments** — standout on the log+aggregation crux and clock-skew handling; gap to senior-staff is *self-critique cadence* (over-claimed linearizability; bolted-on failover reintroduced split-brain).

---

## Phase 1 — Requirements & scoping

**Candidate.** Start with functional requirements.

*Create an auction.* Like a normal post (photos + description) but with structured data attached: an **end time**, an optional **start time** (creator may want to schedule — post is up but bidding opens later), and a **minimum bid** so nobody low-balls at $0.01.

*Place a bid.* A user views an auction and bids. They can see: the current **highest bid amount**, **time remaining**, and an indicator of **whether they are currently the top bidder** or have been outbid. Integrates with the **notification service** — creator hears about new bids; bidders hear "you've been outbid" and "auction ending soon."

*Declare a winner.* At close, one winner is decided and shown on the post; winner is notified. (Raised **second-price / Vickrey** as a possible variant — should we consider it?)

**Non-functional.** The novel part isn't posting/serving at scale — Instagram already does that for likes/comments. The interesting part is **serialization of bidding**: globally consistent, linearizable bids that feel fair — when you see the top bid and beat it, your bid must not be lost or overwritten. **Correctness is paramount.** Plus high availability, and **low latency** — a new bid should show up in ~100–200 ms; this is live bidding.

**Interviewer.** Good separation, not rushing to boxes. Scoping calls:
- Scheduled start → lightweight state machine (`SCHEDULED → ACTIVE → ENDED`), not a core concern.
- **Vickrey: scope out.** Nice that you know it (signals mechanism-design awareness), but do a plain English/first-price auction. Just don't *preclude* second-price — if you keep full bid history you get it nearly free.
- Push: do we show *who* the top bidder is, or only the amount? Think about behavior/leakage.
- Held you to a higher bar on scale: "scales like likes/comments" is a hand-wave and it's wrong in an important way. Two asks: (1) **real numbers**, esp. *peak bid rate on a single hot auction in the last 10s*; (2) of {linearizability, low latency, availability}, **which do you bend first?**

---

## Phase 2 — Scale estimate

**Candidate.**
- 100M DAU; 0.1% create an auction → **100k auctions/day**.
- Avg auction open ~7 days → **~700k live auctions** at any moment.
- Bidders: ~1000/item → 700k × 1000 = 700M... *that doesn't fit in 100M DAU.* Cap at **~100M active bidders** (same person bids on many items).
- ~1 bid/bidder/day → 100M bids/day ÷ 86.4k s ≈ **~1000 bids/s average**; peak ~100× → **~100k bids/s**.
- Conceded these are **heavily skewed onto a few hot items**, not spread evenly.

On display: agreed to show **only the top amount, not the bidder's identity** — keeps bidders anonymous and protects the seller from bidder collusion.

**Interviewer.** Credited the **self-correction** on the 700M nonsense (numerical honesty) and the anonymity/collusion instinct (staff-level second-order thinking). Then the key reframe:

> 100k bids/s *aggregate* is trivial — spread across 700k auctions it's a fraction of a bid each; you could do it on a laptop. That's **not** the workload. The workload is **~10k+ bids/s against a single object**, every one serialized against the current max. You can shard away *aggregate* load; you **cannot shard away contention on one auction**. That's a **hot-key write-contention** problem, and it's why "linearizable + low-latency + available" is in tension.

Asked for the **write path for a single bid**: store, concurrency-control primitive, and what happens when $100 and $101 arrive within a millisecond.

---

## Phase 3 — Consistency stance (CAP / PACELC)

**Candidate.** For a single item we're up against CAP. Partitions are unavoidable; under one we must choose. A single auction lives in one place (replicated). **Drop availability, keep consistency** — a *wrong* auction is worse than an *unavailable* one; users won't tolerate incorrect results. Consequence: during a partition, the item may **go dark in Europe** while continuing in the Americas.

**Interviewer (two pushes).**
1. **Everyday case, not the partition case.** Partitions are rare (minutes/year); contention on the hot item happens *always*, healthy network or not. CAP is almost a distraction — the real question is **what serializes the concurrent writers**. The framework is **PACELC**: *else, latency vs. consistency* — that "else" branch is where this system lives 99.9% of the time.
2. **Give the mechanism, not the philosophy.** A box, a store, the exact operation adjudicating a bid; trace $100 vs $101 and say what the loser sees.

---

## Phase 4 — The design (log + deferred aggregation)

**Candidate.**

*High level.* Stateless **auction service** behind an LB (any instance handles any request). **Auction detail** (metadata) in a **relational DB + cache**: cache for high-read/low-latency, RDB as system of truth for writes. Bids **rejected if outside the active window** (cheap early filter). A **daemon** runs an event loop over auctions that ended in the last *x* seconds and asynchronously runs end-of-life (winner) workflows.

*Bid path — the core.* Do **not** compare-and-set on the write. **Append every bid to a log partitioned by `auction_id`** → an ordered ledger per item in one partition (Kafka-like: high-throughput, low-latency appends). Auction service **batches heavily** for throughput. Bids carry **both client-side and server-side timestamps**. Stream the full ledger to a warehouse for **audit**.

*Deriving the max.* A **consumer reads the partition in batches**, computes the **max over the batch**, and **compare-and-sets the current highest bid into Redis** (`key = auction_id/item_id → highest bid`). At 10k bids/s a batch is scanned in-memory "in a blink." Read a **trailing 1-second window** (buffer off the current second). Auction service serves the live top bid from Redis, fresh to ~100s of ms.

*Scaling past one partition.* Because **max is aggregatable**, split a hot auction across multiple partitions (partition key includes an auction-server id → a group of servers writes one partition). Each consumer finds a **local max**; all CAS into the **single Redis key**. Batch at every layer; keep the final aggregator (Redis) at a sane rate.

*Clock skew.* Clients read the top value from Redis, so skew doesn't affect display. Skew bites at the **boundary**: did a bid land before close? Server clocks disagree, so **accept bids for a small grace window (~50 ms) after local end**; the recorded timestamps then adjudicate. The daemon triggers the close workflow ~50 ms after end so the ± skew window is considered. (Didn't detail the exact skew-reconciliation algorithm.)

*Failover (added late).* Run the auction in **two regions in parallel** — bids to both, both compute, active serves, passive takes over on outage; near-instant failover via Anycast.

**Interviewer.** Called the **core genuinely strong**: append-log + deferred, layered max, **exploiting that `max` is commutative/associative** to aggregate across partitions; and the clock-skew-at-boundary handling. Then two hard challenges.

---

## Phase 5 — Challenge 1: you abandoned linearizability without noticing

**Interviewer.** Fifteen minutes ago you *demanded* linearizability and were willing to trade away availability for it. But this design is **eventually consistent and optimistic**: append → batch → max computed a second later downstream. So when I tap "bid $101," what do I get **synchronously**? You can't say "you're now top bidder" — you only appended. You find out later, via push. Pick a lane: (a) is that async/optimistic UX actually acceptable? (b) if so, what *was* the linearizability talk about — what genuinely needs it?

**Candidate.** Retract the linearizability claim. If the top is $100 and A and B see it and bid $101 and $102, then **regardless of ordering, B wins** — so we don't need linearizability; we need the **max in a window**. The $101 bidder learns quickly they've been outbid — the post shows $102 and a boolean "not you" (identity hidden).

**Interviewer.** That's the reconciliation, done right. Pocket version:

> "I don't need linearizability. `max` is commutative, associative, **idempotent** → the winner is invariant under bid ordering. What I need is **durability of every accepted bid** and **agreement on the cutoff** (which bids are in-window). Ordering is irrelevant; **membership** is authoritative."

Bonus: idempotent max makes Kafka's **at-least-once / duplicate delivery a non-issue**. Strongest 30 seconds of the interview.

---

## Phase 6 — Challenge 2: the failover reintroduces split-brain

**Interviewer.** Two regions both taking bids won't see identical streams (internet reorders/loses): A sees {100,101,103}, B sees {100,102,103} → **two divergent logs, two truths = dual-leader**. You recreated the split-brain you chose CP to avoid. How do you survive a **region** loss without two masters? What owns the authoritative log, how is it replicated, and **what's your RPO** — do you lose the last few hundred ms on a hard region failure? (Hint: what does Kafka's in-region replication already buy you, and which failure is actually left?)

**Candidate.** Agreed it was bolted on. **Active region is the source of truth.** Don't fork at the request layer — make the **Kafka log the source of truth and replicate it cross-region** (~1s delay). Recovery idea: the **client tracks its acknowledged bids from the last ~1s**; on detecting a region outage, it **replays** them, which reconciles into the (new) log.

**Interviewer.** Single-leader + async cross-region (RPO ~1s) is the right backbone, and client-replay is creative — but two things:
1. **Don't trust the client for bid *values*.** Item-value/money-adjacent: a malicious client could fabricate/inflate bids during failover chaos, when defenses are weakest. Client replay is a **best-effort hint**, not a source of truth — the authoritative amount must be a **server-signed record**.
2. Sharper move: **don't pay for zero-RPO globally — pay only where it matters.** ~1s RPO on a rare region failure is usually fine; for the **last 30 s of a hot auction**, flip *that* auction to **synchronous cross-region / quorum write** and eat the latency only there, only then. **Consistency is a dial you turn up locally**, not a global constant. Also: **Kafka in-region replication (RF=3) already survives an AZ loss for free** — the only thing left to solve is **region** loss; the AZ case was over-built.

---

## Phase 7 — Debrief & grade

**Strong (hire signals).**
- **Requirements & scoping** crisp; **self-corrected a bad number** without defending it (intellectual honesty).
- **Core architecture genuinely good**: log → deferred, layered max aggregation, exploiting commutativity to shard a hot key. Real insight, not a memorized pattern.
- **Clock-skew at the boundary** reached unprompted — most never get there.

**Dinged (gap to senior-staff).**
- **Asserted a hard requirement (linearizability) that wasn't needed** and didn't catch the contradiction until forced. At staff+, you interrogate your *own* requirements first — derive "what consistency do I need?" from the **math of the operation** (`max` → order-free), proactively.
- **Region failover bolted on in haste** → reintroduced split-brain. Even under time pressure, run the 10-second check: "does this new box violate anything I already committed to?"

**Net.** Ceiling is staff (the aggregation insight + boundary handling). Gap to senior-staff is **self-critique cadence** — catching your own over-claims/inconsistencies before the interviewer does. Instincts are there; the discipline of continuously reconciling the design against your own stated constraints is what turns *lean hire* into *strong hire*.

**Threads left open (not designed):** winner-declaration daemon exactly-once/idempotent close; read-path fan-out to millions of watchers of the live price; bid admission control / anti-spam (best-effort reject-below-max at the edge using stale Redis, authoritative decision at close).

---

## Concepts to extract (candidate notes)

1. **Hot-key write contention** — aggregate load shards trivially; contention on a *single* object does not. Identifying "10k writes/s to one key" vs "10k writes/s across many keys" as different problems is the whole crux. *(new — system-design-concepts)*
2. **Commutative/associative/idempotent aggregation beats linearizability** — for `max`/`sum`/`set-union`, the result is order-invariant, so you need **durability + an agreed cutoff (membership)**, not ordering. Idempotence makes at-least-once delivery safe. CRDT-adjacent. *(new — system-design-concepts)*
3. **Log-structured writes to absorb single-key contention** — append-only ledger + **deferred, batched, layered aggregation** instead of read-modify-write/CAS on the hot row. Ties to the-log-abstraction / table-log-duality / Kafka. *(extend existing)*
4. **PACELC over CAP for everyday reasoning** — partitions are rare; the *else* branch (**latency vs. consistency** under normal operation) is where contended systems actually live. *(new theory page, or extend consistency-models)*
5. **Deadline correctness under clock skew** — grace window at close + recorded event timestamps to adjudicate membership; server-clock disagreement is the real hazard, not display. Ties to event-time-vs-processing-time. *(extend existing)*
6. **Consistency as a local dial** — turn sync/quorum replication *up* only for the high-stakes window (last 30 s), not globally. AZ loss ≠ region loss (in-region RF handles AZ; only region loss needs cross-region). Ties to durability-rpo-rto. *(extend existing)*
7. **Don't trust the client as a source of truth** — recovery/replay from clients is a best-effort hint; authoritative records must be server-signed (integrity during failover). *(new or fold into a security concept)*
8. **Optimistic/async write acknowledgment** — decoupling via a log/queue means "bid received" ≠ "bid adjudicated"; feedback ("you're top" / "outbid") returns asynchronously. Name the return path. Ties to async-response-routing. *(extend existing)*
