# Mock Design Interview — Design ChatGPT

> **Source transcript** for the wiki. Full raw dialogue preserved.
> Format: Meta Staff (E6/E7) systems-design screen (45 min). Roles: interviewer (Claude) + candidate (me). Captured 2026-08-19.
> Prompt: **"Design ChatGPT"** — a conversational AI product where a user types a message and gets back an LLM-generated response, with multi-turn conversations.
> Interviewer's closing grade: **6/10** (hireable-borderline for a senior IC round). Core weakness: reasons well qualitatively, shakily quantitatively; thin on the inference tier where this problem is hardest.

---

## Phase 1 — Scoping & requirements

**Candidate.** Designing ChatGPT fully in 45 minutes isn't justifiable, so we focus on a slice. The crux is the **conversation** — the user takes turns communicating back and forth with an LLM behind the scenes, and conversations are multi-turn (later turns can refer to earlier ones).

Auxiliary (acknowledged, then parked): user profile, conversation history, settings (model selection, "think longer vs respond fast"). Important as features, but metadata — not the distinguishing design problem.

**Functional requirements**
- Multi-turn conversation: chatbox → natural-language message → system → generated response → back.
- Conversations persist and can be referred to across turns.

**Non-functional requirements**
- **Scale:** ~100M DAU, ~10 conversations/day → ~1B conversations/day. ~20 turns/conversation → ~20B messages/day in, ~20B out. Noted the **read/write symmetry** — unlike a traditional web app where reads dominate.
- **Availability:** highly available, resilient to failures; a session started on browser must be resumable on mobile — nothing lost, conversations durable once landed.
- **Latency:** interactive — response in a few seconds, not "forever."
- **Security/privacy:** opt out of training/retention; GDPR-style scrub; data export; PII sanitization (SSN/phone) before storage; encryption at rest.

**Interviewer.** Good scoping — narrowing to the conversation loop is the right instinct, and the read/write symmetry observation is sharp (every user turn is a write *and* triggers an expensive generation). Feedback:
1. **Numbers:** you said "20,000 in and out" but meant 20B/day each way — convert to QPS and tell me why peak ≠ average.
2. **What makes this different?** Your NFR list could describe WhatsApp. What is fundamentally different about serving a *generated* response vs. delivering a message between two humans?
3. **Park privacy** — real, but not where the design pressure is. Keep only what forces an architectural decision.

---

## Phase 2 — Scale & the crux

**Candidate.** 20B/day ÷ ~100k sec/day ≈ 20,000 QPS average [*sic* — arithmetic slip]. Peak ~10× off-hours/events → 200,000 QPS. This isn't a regular messaging service pushing dumb messages — it's **generative**: each message is compute-expensive, an AI churns through it to produce a response. 200K QPS is modest vs. Discord/WhatsApp (tens of millions msg/s); the middleware isn't the critical part. **The critical part is the fleet of GPU servers running the model and generating the response.**

**Interviewer (correction).** Watch the math: 20B ÷ 100k = **200,000 QPS *average***, so your 10× peak is **~2M QPS**. Don't hand-wave a factor of 10 — those errors compound when sizing fleets. But the shape is right, and you nailed the crux. Now internalize *why* it's different, beyond "expensive":
- A single generation **occupies a GPU for seconds**, not ms.
- The response **streams token by token** — the connection is held open the whole time.
- GPUs are **scarce and expensive** — you can't autoscale to 2M concurrent like web servers. You're capacity-constrained on the most expensive hardware in the building.

That combination — long-held, streaming, on a constrained resource you can't cheaply overprovision — is the whole ballgame.

---

## Phase 3 — High-level design (single turn)

**Candidate.** Client sends the message via a streaming HTTP connection (the request carries the conversation; a streaming response returns tokens one by one). The connection from client → middleware stays open ~5s (avg generation duration).

Concurrency (Little's Law): 2M req/s × ~5s = **~10M concurrent connections**. Supporting 100k connections/box is doable for a well-scaled service → **~100 edge boxes** at peak. Elastic fleet scales up/down.

These edge/middleware boxes forward requests to the **GPU fleet**, which caches context (prior conversation + system prompts) and generates tokens. The GPU→middleware connection also stays open and streams tokens back, which the middleware relays to the client. (Candidate flagged: hasn't worked with GPU internals, unsure how that tier scales.)

**Interviewer.** Connection math is clean (Little's Law → 10M concurrent → ~100 boxes). I don't need CUDA from you — reason about the GPU tier as a **constrained resource**. Pointed question: at 2M req/s, you don't have 2M GPUs. A GPU instance keeps maybe a few dozen generations in flight via batching, and the fleet is large-but-finite. **What happens to an incoming request when all GPUs are saturated?** In your diagram the edge box just blocks on a synchronous call. Is that okay? What do you introduce to handle the arrival-vs-throughput mismatch?

---

## Phase 4 — The queue, and the streaming return path

**Candidate.** Treat the generator fleet as a black box that handles, say, ~10,000 concurrent. Huge mismatch: 10M in vs 10K served. If requests just park in the kernel TCP buffer, boxes OOM and crash. Introduce a **persisted queue (Kafka)**: requests enqueue; the fleet consumes at its capacity (~10K/s). During peak, user response time degrades but the system doesn't fall over.

**Streaming in this model:** edge boxes hold the client connections (the ~100 boxes); threads immediately read the request, push to the queue, and wait. A **consumer** pulls the message, hands it to the generator, and as tokens are produced pushes them back over a (reused) connection between consumer and the edge box holding that client. The edge box relays token-by-token to the client.

**Bonus properties:**
- Kafka = durable storage for conversations → nothing lost. Other consumers (e.g., conversation-history writer) read the same topic.
- If the client disconnects, the middle tier saves the completed response to a per-user **inbox**; on reconnect (even different device) it's relayed. WhatsApp/Discord-style online/offline handling, but with an LLM always on the other side.

**Ops / production (candidate, racing the clock):**
- Metrics: connection count, queue depth, Kafka partition lag, consumer rate, GPU utilization/CPU/mem → auto-balance or human intervention.
- Client reconnection/resume helped by durable queue.
- **Idempotency** via `conversation_id + turn_id` to dedupe retries.
- Service crashes: edge tier tracks where users connect via **consistent hashing**; migrate clients to new box on failure. Kafka consumer-group offsets enable retry on generation errors.

**Interviewer (closing challenges).**
1. **Streaming return path (under-specified).** When a worker pulls a job off Kafka, it has *no idea which of the 100 edge boxes holds that user's socket* — Kafka deliberately decoupled them. You need a **session registry / back-channel**: pub/sub keyed by `session_id`, edge box subscribes to `stream:{session_id}`, worker publishes tokens there. And you'd **not** push the token stream through Kafka — enqueue the *request* durably, stream tokens over a lighter real-time channel.
2. **Correction (the important one).** You leaned on the queue to absorb peak. But 10M in vs 10K served is a **1000× gap** — a queue smooths *bursts*, it cannot rescue a *sustained* capacity shortfall (backlog → hours, everyone times out). Real levers: **admission control / load-shedding** ("at capacity"), **priority** (paid users first), **degradation** (smaller/faster model). "Add Kafka" ≠ provisioning GPUs near peak or deciding what to drop.
3. **Never touched (would raise the level):** **KV-cache / context management** — every turn resends the growing context; context windows are finite; **batching** many sequences per GPU is what actually sets capacity and cost.

---

## Phase 5 — Debrief & grade

**Overall: 6/10** — hireable-borderline for senior IC. Pattern: strong qualitative reasoning, weak quantitative; thin on the inference tier (the crux).

| Dimension | Score | Notes |
|---|---|---|
| Requirements & scoping | 8/10 | Cut to the conversation loop fast; parked auxiliary deliberately. Strongest muscle. |
| Estimation / quantitative | 4/10 | Dropped a 10× on core QPS; believed a queue could absorb a 1000× deficit. Numbers decorated decisions instead of driving them. |
| High-level design | 6/10 | Good decoupling instinct + correct Little's Law; hand-waved the token-return routing. |
| Depth on the crux | 3/10 | Identified the GPU fleet as bottleneck, then treated it as a black box. |
| Trade-offs / prod maturity | 7/10 | Idempotency keys, consistent hashing, consumer-offset retries, log reuse, sensible metrics. |
| Communication | 7/10 | Clear, thought aloud, took correction well, self-managed time. Prone to breadth-as-comfort-zone. |

**Improvement areas (prioritized):**
1. Learn **when a queue helps vs. can't** — arrival > service on average ⇒ only add capacity / shed / degrade.
2. Make **numbers drive decisions** — after each estimate, ask "what does this force me to do?" A 1000× gap should trigger load-shedding immediately.
3. **Open the inference black box** — KV cache, continuous batching, memory-bandwidth roofline.
4. **Specify paths to the last hop** — "how does the response find its way home?" (session registry + pub/sub).
5. **Separate durable path from real-time path** — don't run live tokens and durable history through one Kafka mental model.

---

## Concepts to extract (candidate notes)
- Constrained-resource serving (GPU fleet) — queue vs. capacity vs. load-shedding.
- Little's Law for connection/fleet sizing.
- Streaming return path: session registry + pub/sub back-channel.
- Durable log vs. real-time channel separation.
- KV cache & continuous batching (knowledge gap — see reading list).
- Load-shedding / admission control / graceful degradation under sustained overload.
