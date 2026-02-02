 Treasury as a Service

                              Acquirers
                                  │
                                  ▼
                         ┌───────────────┐
                         │  API Gateway  │
                         │  (Auth, Rate  │
                         │   Limiting)   │
                         └───────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        TaaS Core Platform                                │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐ │
│  │ Validation  │──▶│  Holdings   │──▶│  FX Rate    │──▶│   Ledger    │ │
│  │  Service    │   │  Service    │   │  Service    │   │   Service   │ │
│  └─────────────┘   └─────────────┘   └─────────────┘   └─────────────┘ │
│                                                               │         │
│                                                               ▼         │
│                                                      ┌─────────────┐    │
│                                                      │    Bank     │    │
│                                                      │ Integrator  │    │
│                                                      │  Service    │    │
│                                                      └─────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
           ┌───────────────┐           ┌───────────────┐
           │ JPMC Singapore│           │Barclays London│
           │   (SGD, USD,  │           │  (GBP, EUR,   │
           │    EUR, etc.) │           │   USD, etc.)  │
           └───────────────┘           └───────────────┘



1) Architecture & Flow

Q: Why a separate Validation Service?
A: Centralizes schema/business rules, reduces downstream load, enables independent rule rollout.
Q: Why a saga-based Funding Orchestrator?
A: Explicit steps with compensations/retries, better failure isolation and observability.
Q: Why bank adapters for JPMC SG and Barclays UK?
A: Port/adapter isolates formats/signing/transport; partner-specific logic behind a common interface.
Q: Why keep Ledger as a distinct service?
A: Strong consistency and audit guarantees; independent scaling and guarded change surface.
Q: Why Angular for ops tooling?
A: Team proficiency and existing components; faster delivery for operations workflows.
Q: Why AOP in Spring Boot?
A: Uniform idempotency/audit/validation without duplicating boilerplate across services.

2) Data & Ledger Correctness

Q: Why IBM DB2 over PostgreSQL for ledger?
A: Mature ACID + RR/RS isolation and HADR; fits enterprise ops and performance SLAs.
Q: How is monetary precision handled?
A: Store minor units as integers; avoid float/double; FX rates as integer ratios.
Q: How is double-entry integrity enforced?
A: Transactional writes + invariant check sum(amount*side)=0 per txn; daily trial-balance.
Q: How are FX rate locks applied?
A: TTL-locked snapshot with slippage guard; reject/re-quote beyond tolerance; fully audited.
Q: How is idempotency guaranteed end-to-end?
A: Idempotency key per acquirer + request_id; dedupe in cache/DB; carry key to bank ext_ref.
Q: How is multi-currency holdings managed?
A: Per-currency balances with pre-reservation; settle on bank ACK; release on failure/timeout.

3) Messaging, Concurrency & Caching

Q: Why RabbitMQ over Kafka?
A: Strong command/work-queue semantics and routing keys; replay not primary need for instruction flow.
Q: How is DB→MQ consistency ensured?
A: Outbox on commit + CDC publisher; consumers idempotent by txn_id/ext_ref.
Q: How are retries/backoff handled?
A: Classify transient vs permanent; exponential backoff + jitter; DLQ for manual resolution.
Q: Why Hazelcast (and when Redis)?
A: Hazelcast for distributed data grid and near-cache; Redis for counters/locks when needed.
Q: How is rate limiting/backpressure applied?
A: Gateway limits per tenant/partner; RabbitMQ prefetch and consumer concurrency tuning.
Q: How is ordering guaranteed?
A: Keyed queues per account/txn; ledger as single source of truth; idempotent downstream effects.

4) Bank Integration & Statements

Q: How are transports secured to banks?
A: mTLS for APIs; SFTP with key rotation/IP allowlists; HSM-backed signing and payload hashing.
Q: Why ISO 20022/MT normalizer first?
A: Unified schema enables deterministic matching and simpler exception handling.
Q: How are cutoffs (SGT/UKT) handled?
A: UTC storage with partner-local cutoff scheduling; cutoff-aware backlog and D+1 routing.
Q: How is statement matching done?
A: ext_ref + amount/currency + value_date; fuzzy windows for fees/time drift; unmatched to exceptions.
Q: How are partner differences abstracted?
A: Adapter maps pain.001/MT payloads, signatures, and ACK/NACK parsing behind a common interface.
Q: Why Trace Transformer for SWIFT files?
A: Reliable mapping/validation pipeline with auditable transforms for bank formats.

5) Reliability, DR & Observability

Q: How did you achieve 99.98% uptime?
A: Circuit breakers, isolated failures via saga, health checks, blue/green deploys, rapid rollback.
Q: What’s the DR strategy?
A: Active/standby regions with HADR; RPO < 60s, RTO < 15m; documented runbook and quarterly drills.
Q: What metrics/SLOs are tracked?
A: P95 funding latency, bank ACK/NACK rates, statement match rate, DLQ drain time.
Q: Why Jenkins + Splunk + Grafana?
A: Existing CI/CD and log/metrics stack; quick integration and mature alerting/observability.
Q: How is tracing done?
A: Propagated trace-id across services; RED metrics; bank_code tags for adapter visibility.
Q: How are incidents handled?
A: Playbooks for adapter outages/cutoffs; throttled backlog drain; customer ETA updates.

6) Performance & Scaling

Q: How did you scale to 2000+ TPS?
A: Horizontal scale, connection pooling, async I/O in adapters, DB2 indexing/partitioning, batching where safe.
Q: How is WebSphere tuned?
A: Thread pools, JDBC pool sizing, timeouts, GC tuning; per-connector concurrency caps.
Q: How are validations kept fast?
A: Hot caches (Hazelcast), precompiled rules, early reject, lightweight schema checks at gateway.
Q: Why Maven over Gradle?
A: Standard corporate pipelines and reproducible builds; consistent dependency management.
Q: How is throughput protected near cutoffs?
A: Cutoff-aware pacing, dedicated queues, priority routing for high-value transactions.
Q: What optimization prevented duplicates?
A: First-write-wins idempotency cache + unique DB constraints; adapter-level dedupe via ext_ref.


