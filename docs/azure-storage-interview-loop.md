# Azure Storage (Object Replication) — Interview Loop Transcript

> Redacted: interviewer and colleague names generalized to roles; company/product names retained. One session covering past-project experience and a live concurrent-programming exercise. **Candidate** = interviewee (software architect, Slack data team; ex-Salesforce Data Cloud). **Interviewer** = engineer on an Azure Storage object-replication team.

---

## Part 1 — Experience deep dive

**Interviewer:** [Intro] The team works on object replication — replication driven by customer-specified policy rules. Primary use case is Azure backup, which uses the replication engine to copy objects into a separate "vault" storage account. Adjacent teams work on geo-replication and tiering (cost-efficient storage by access pattern).

**Interviewer:** So tell me what you've been busy with lately, and the challenging stuff you're dealing with.

**Candidate:** I'm a software architect on the Slack data team, owning the governance and catalog layer for the Slack data ecosystem — search/discovery over data assets, connecting to and keeping those assets up to date, building lineage across systems to establish trust and to help the data-pipeline team locate failures/delays via lineage, and a metadata layer providing governance (data-quality metadata, and now AI-generated metadata for agents and users). I work with data science, product engineers, PMs, and business analysts. Joined this team a year ago.

Before that I was on Salesforce **Data Cloud** — a multi-tenant, petabyte-scale data warehouse for Salesforce customers — on the storage & metadata team. Worked on low-latency metadata access, replicating metadata across systems (Salesforce has its own metadata layer; we built an Iceberg lakehouse with an additional logical layer on top of Iceberg tables), and the **Zero-Copy** framework (analyze across ecosystems — e.g., Databricks on Azure — via data sharing instead of copying, because Iceberg tables are standardized). Improved latency on some of those projects. Before Data Cloud: Tableau data-management org (data catalog); started 10+ years ago in Microsoft Search on search-platform teams.

**Interviewer:** You mentioned performance/scaling work. What were the customer scenarios? How was the latency problem discovered, and what did it unblock?

**Candidate:** Two drivers. One: scale the platform to 100K tenants in our biggest region (US East), up from ~20K. Two: the metadata-replication pipeline was getting slow — customer escalations, especially on bulk operations.

The system: a Salesforce core Oracle DB is the system of truth for all metadata; customers define their data models there; it replicates to represent what the lakehouse looks like. This pipeline got delayed, especially on bulk operations — customers try out data modeling in a sandbox, then promote sandbox→production, creating lots of entities at once.

I led this across three scrum teams (one core-side, two lakehouse-side). Findings:
- The way we modeled objects on the core side made them **non-cacheable**. Salesforce's metadata ecosystem is old, many layers, with rules for what can be cached. Our model didn't satisfy them. Because we were the biggest user of the core metadata system, I worked with their metadata-platform architect to introduce new concepts so our objects could be cached. One piece was how the **ID for each object** is defined — IDs are shared across tenants, so the scheme has to support that many tenants; we extended the bit range reserved for tenants so our IDs fit. That improved read performance under the perf team's 100K-tenant tests (reads were degrading; caching fixed it).
- The second issue was **replication of the metadata**. Replication used Salesforce's core **eventing framework**, built to ensure fairness across partner teams. Via telemetry it tracks each partner team's CPU/event consumption; beyond a limit, your events get deprioritized and drained slower. Bulk metadata writes created lots of events, each taking a long time to process, tripping our limit. The crux fix: **batching** — reduce the number of events and the CPU cost (per-event work was duplicative; batching cut the reads from core roughly by the batch size). We also had to keep transactions open longer (previously point updates; now a whole batch), so we made write-side performance changes to support big batches.

**Interviewer:** Was this from internal benchmarking or observed customer change?

**Candidate:** Both. The customer-observed slowness was the bulk writes — on sandbox→production migration our queue drain rate dropped; we identified we were hitting the shared-platform threshold. After batching, those triggers stopped. The 100K-per-region read load we didn't see ourselves — the perf team emulated it.

**Interviewer:** With batching, multiple transactions batched into one — how do you ensure consistency if some fail but others succeed?

**Candidate:** We implemented a streaming system: one service opens a transaction on the other and streams events in the same order they occurred on the core side. These are **logical** transactions (not Oracle CDC) — we stamp each so we can order them. Ordering reduced errors (previously parallel independent processing meant an earlier change could apply later, fail, and retry). Even so a batch can fail, so we captured **dependencies** between logical entity classes (e.g., a pipeline object depends on its input object). We collected per-object errors based on the dependency graph and returned them to the client that initiated replication — so instead of retrying the whole batch, the client learns exactly what failed and needs retrying. Best-effort on the target side; return failures to the client.

**Interviewer:** What if the client retries the whole batch — would successful transactions execute again?

**Candidate:** They could, but it's idempotent, so no issue on our side. By contract it won't retry the whole thing unless it didn't get a response — and even then, idempotent.

**Interviewer:** How do you ensure idempotency?

**Candidate:** We replicate the **snapshot, not the delta**. On each change we send the whole object ("this object changed, replicate the whole object"), so a retry always applies the latest payload. Metadata operations aren't huge, so snapshots are fine.

**Interviewer:** Set frequency for snapshots — hourly, 15 min?

**Candidate:** Event-based. Anything changing on a table generates an event ("object ID 1 changed"); the replication side reads the whole object. (Snapshot is of the object, not the entire table.)

**Interviewer:** Any SLAs / latency requirements?

**Candidate:** Internally ~5 minutes at p99. Bulk metadata copies were tied to an interactive use case — Salesforce trial/training orgs. Users drop a training packet to initialize a setup; often internal, and everyone comes at the end when training is due, creating a huge payload of changes. If we hit queue issues they'd be blocked on the training for hours. Even within 5 minutes it's fine — they get a "your org is ready" email. Customers moving their setup over also get "we're working on your org, we'll notify you." 5 minutes made sense across these scenarios.

**Interviewer:** Bottlenecks staying SLA-compliant on bulk movement?

**Candidate:** Yes — the training scenario (many people doing bulk writes at the end). Even with batching we couldn't always keep up under the 5-min SLA when many orgs were created at once. So we did a further optimization: since the training-org metadata shape is known in advance, instead of streaming metadata changes we **pre-created a package in S3**; on that event we just notify the off-core side to create the whole metadata from the package, sidestepping the pipeline.

**Interviewer:** How do you keep one tenant's huge payload from impacting others in the same environment?

**Candidate:** The eventing framework is tenant-aware as well as partner-team-aware — a tenant generating lots of events gets slowed down. Tenant operations are driven only from the core side, so on the off-core side we didn't have to build a similar fairness mechanism for metadata (query-side is different — queries hit the data directly).

**Interviewer:** How do customers use the replicated (secondary) copy — read-only, or do they write to it?

**Candidate:** When an object's schema changes it's an update on the off-core side (our secondary metadata repository). E.g., start with 10 columns, add columns; the new columns must reach the other side before the ingestion pipeline can ingest them. The ingestion pipeline is itself represented by an object; it changes to say "ingest these two new fields." That's the dependency — the pipeline metadata replication only succeeds once the input object's metadata replication has. Updates weren't very frequent; mostly create-then-read (customers then query/ingest into the created objects).

**Interviewer:** After a schema update on primary, can secondary serve stale metadata? What about conflicts if they write to secondary while schema updates on primary?

**Candidate:** The escalations we got: a customer updates their schema (they see the core DB source of truth showing the added columns), then writes a SQL query reading those columns; if metadata replication hasn't happened, the query fails. We'd check our operational tools (per-tenant, per-object replication delay) and use special hooks to force-sync a tenant's/object's metadata outside the regular sync. We also had cases where the sync dropped things — event creation and object update were asynchronous, not in the same transaction, so a failed event creation silently lost some replication. But because we always snapshot, a later update catches it — though customers sometimes still reported not seeing an updated object.

**Interviewer:** Any built-in mechanism to detect dropped events / failed sync?

**Candidate:** Yes — a periodic delta check comparing a tenant's objects on both sides, force-syncing anything missing. But it was expensive (all tenants, both sides). There were plans to improve it that we didn't get to. It was a fail-safe.

---

## Part 2 — Live coding: KeyedTaskExecutor

**Problem (HackerRank-style):** implement a task executor.
- `submit(key, task)`.
- Must behave correctly under overlapping calls (synchronize by key).
- Tasks with the same key execute **serially, in accepted order**.
- At most **4** tasks execute concurrently.
- At most **1000** queued-or-running tasks accepted globally.
- No task accepted when full or after shutdown begins.
- `submit` reports whether the task was accepted for execution.
- A failing task must not prevent later tasks from running.
- `shutdown(timeout)`: stop accepting, do accepted work up to the timeout.

**Candidate's approach (Java; ran out of time, incomplete):**
- `ConcurrentHashMap<key, queue-of-tasks>` — per key a queue of tasks; concurrency ≤4 across all keys, serial per key.
- `AtomicInteger acceptedTasks`, `AtomicBoolean shutdown`, `MAX_TASKS = 1000`.
- On submit: check shutdown and count. Recognized the two atomics together aren't atomic → the real admission gate is a `getAndIncrement`-with-bound (only increment if `< MAX_TASKS`), so only one thread wins the last slot. Agreed it's fine to fail (return false) when full rather than block/spin.
- Add task to the key's queue (create queue if absent).
- `shutdown`: set the boolean (idempotent), plus a timeout.
- Execution: "at most four executors"; considered tracking running tasks and which keys are running (need more than a count — need to know which key is running to keep it serial). Sketched a dispatcher loop that scans the map every ~100ms, and per-key a map from key→running task so only one per key is scheduled; on completion, clean up the map; on timeout, kill running ones.

**Gaps (interviewer wrapped at time):** no working ≤4 concurrency mechanism, never decremented the accepted counter on completion, no `try/catch` around the task, per-key map cleanup/leak + teardown race unaddressed, and `sleep(timeout)`-style shutdown rather than a real drain. Interviewer: "not possible to implement everything in the short time; I got the approach."

---

## Part 3 — Candidate's closing questions

- Does the same team own customer-facing (non-backup) replication (data available across regions via policy)? → Yes; available both standalone (customer creates policies between their accounts) and as part of Azure backup (backup team owns config/lifetime; this team owns the replication engine + pipeline).
- Prioritization across use cases (interactive replication vs backup)? → Currently treated the same for scheduling; some backup-specific monitoring for data integrity, but no scheduling differentiation.
- These services seem mature — where are the hard problems? → Object replication is ~5 years old; policy-based/backup is newer (last couple of years). Current challenges: moving from **active-passive to active-active** (consistency, avoiding overwriting newer writes with delayed writes from another endpoint, clock skew across regions/accounts); scaling the replication engine to new higher-TPS/higher-ingress storage-account capabilities; for geo, making the (currently fixed) secondary region configurable.
</content>
