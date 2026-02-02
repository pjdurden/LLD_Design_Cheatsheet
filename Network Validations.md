Network Validations

Clients/Acquirers        Ops/Admin UI (Angular)
      |                           |
      v                           v
+-------------+           +------------------+
| API Gateway |<--------->| Rules Console    |
| (Auth, RL)  |           | (RBAC, version)  |
+------+------+           +---------+--------+
       |                              |
       v                              v
         +----------------------------------------------+
         | Validation Service (Spring Boot)             |
         | - Execution Engine (compiled AST)            |
         | - Schema Validator (JSON/ISO/SWIFT)          |
         | - AOP: audit, metrics, idempotency           |
         +----------+-----------+-----------+-----------+
                    |           |           |
            Hazelcast Cache     |     RabbitMQ (cmd/replies)
          (read-through, TTL)   |     validate.req/.res, violations
                                v
                    +---------------------------+
                    | DB2 (Config & Decisions) |
                    | rules, conditions,        |
                    | versions, tenants, audit  |
                    +---------------------------+
                                |
                                v
                       Splunk/Grafana (obs)

Scope & Rationale

Q: What did it replace?
A: Hardcoded Java rule engine; moved to DB2-backed Rule table + Conditions table to change business logic without a Java release.
Q: What flows use it?
A: Wallets, normal payments, treasury payments via one shared service and policy sets.
Q: Why centralize validations?
A: Consistent checks, faster rule rollout, fewer regressions across products.

Rule & Config Model

Q: How are rules stored?
A: Rule table (rule_id, product, priority, effect, version, effective_from/to); Conditions table (rule_id, field, op, value, join).
Q: How do updates go live?
A: Admin UI publishes new version → validate config → hot-reload via config.changed event → gradual enable by tenant.
Q: How is safety ensured?
A: Staged rollout (shadow/canary), immutable versions, instant rollback to last-good, full audit of changes.
Q: Multi-tenancy?
A: tenant_id scoped rules with defaults and overrides; per-tenant caches.

Execution & Performance

Q: How are rules executed fast?
A: Compile conditions to AST; short-circuit evaluation; vectorize simple checks; pre-validate schema to skip invalid payloads early.
Q: External lookups?
A: Side-effect-free providers (KYC/AML, account status) with timeouts, circuit breakers, and cached results.
Q: Idempotency/path?
A: Request hash (tenant+external_id); first-write-wins; store decision for retries.
Q: Throughput targets?
A: 1000+ TPS with Hazelcast read-through cache, bounded pools, and async provider calls.

Messaging, APIs & Integrations

Q: Why RabbitMQ here (vs Kafka)?
A: Command/work-queue semantics, routing by tenant/flow, simpler ops; replay not required for inline validations.
Q: API modes?
A: Sync REST for inline decisions; Async via validate.req/.res for bulk/slow provider checks.
Q: Schema handling?
A: OpenAPI + JSON Schema; ISO/SWIFT normalized to canonical model before rule evaluation.

Reliability, Observability & DR

Q: How prevent bad rule pushes?
A: Lint + dry-run on historical samples, shadow mode, canary by tenant, kill-switch per rule set.
Q: DR strategy?
A: DB2 HADR + warm standby; config snapshots; RPO<60s, RTO<15m.
Q: What’s instrumented?
A: P50/P95 latency, cache hit rate, provider timeout/error %, rule-eval depth, deny/allow rates by tenant/product.
Q: Auditing?
A: Append-only decision log with inputs hash, rule_version, outcome, actor; WORM storage for compliance.

Tech Choices & Trade-offs

Q: Why Spring Boot?
A: Mature integrations (validation, schedulers, AOP), team familiarity, quick delivery.
Q: Why DB2 for configs/decisions?
A: Existing HADR/ops, strong ACID; fits enterprise SLAs.
Q: Why Hazelcast over Redis?
A: Java data grid, near-cache, easy POJO caching; Redis used only for counters/locks if needed.
Q: Why DSL-in-DB over Drools?
A: Deterministic, lighter footprint, versioned in tables, easy canary/rollback; avoids Rete complexity.
Q: Fail-open or fail-closed?
A: Fail-closed on provider errors with bounded fallbacks to cached allowlists/denylists; clear ops override.


Veri5

Clients (Banks/PSPs)          Ops/Admin UI
        |                           |
        v                           v
+-----------------+        +-------------------+
| API Gateway     |<------>| Config Console    |
| (Auth, RL, mTLS)|        | (RBAC, flags)     |
+--------+--------+        +---------+---------+
         |                            |
         v                            v
            +-------------------------------------------+
            | Verification Orchestrator (Spring Boot)   |
            | - Identity & Account checks (PSD2/OpenBank)|
            | - Idempotency, retries, circuit breakers   |
            | - Webhooks/events for async callbacks      |
            +-----------+--------------+-----------------+
                        |              |
             (near-cache)             | (persist/audit)
            +-----------+             v
            | Hazelcast |      +------------------+
            +-----------+      | PostgreSQL       |
                        |      | requests, tokens |
                        v      | results, audits  |
                  +-----------+ +-----------------+
                  |   Redis   |
                  | counters, |
                  | locks, TTL|
                  +-----------+
                        |
                        v
             +----------------------+
             | Providers Integrations|
             | (OAuth2/mTLS to PSD2) |
             +----------------------+
                        |
                        v
             +-----------------------+
             | Observability Stack   |
             | Prometheus/Grafana    |
             | OpenTelemetry + Logs  |
             +-----------------------+

             Architecture & Flow

Architecture & Flow
          
Q: Why an API Gateway?
A: Centralized auth, rate limits, mTLS termination, request shaping.
Q: Why a dedicated orchestrator?
A: Encapsulates verification steps, retries, and provider routing; isolates failures.
Q: Sync vs async callbacks?
A: Sync for fast providers; async via webhooks when banks respond later; correlation by external_id.
Q: How are providers integrated?
A: OAuth2 client creds/authorization code; mTLS; per-provider adapters with uniform interface.

Data & Models

Q: Why PostgreSQL over MongoDB?
A: Strong ACID, relational joins for identities/accounts, transactional audits, simpler GDPR deletes.
Q: Core entities?
A: VerificationRequest, IdentityRecord, AccountStatus, ProviderToken, VerificationResult, AuditEvent.
Q: Idempotency strategy?
A: Unique (tenant_id, external_id); return first completed result; dedupe at adapter with ext_ref.

Performance & Scaling

Q: How reach 1000+ req/sec?
A: Hazelcast near-cache for hot identities, Redis for counters/locks, connection pooling, async I/O, bounded thread pools.
Q: Cache policies?
A: TTL by data class, stampede protection (locks), negative caching for transient “not found”.
Q: Backpressure & rate limits?
A: Per-tenant/endpoint limits at gateway; orchestrator applies concurrency caps per provider.

Security & Compliance

Q: Provider security?
A: OAuth2/OIDC, mTLS, signed requests; secrets in Vault/KMS with rotation.
Q: PII handling (GDPR)?
A: Minimize stored fields, encrypt at rest, purpose-bound access, data retention + deletion APIs.
Q: Audit & non-repudiation?
A: Append-only audit log with payload hashes, actor, decision; WORM storage for critical trails.
Reliability & Observability
Q: Failure handling?
A: Circuit breakers per provider, exponential backoff+jitter, fallback providers when available.
Q: DR/SLOs?
A: Multi-AZ, automated backups, target P95 latency < X ms, availability ≥ 99.9%.
Q: What’s instrumented?
A: Request latency, provider error/timeout rates, cache hit ratio, webhook success, idempotency dedupe rate.

Tech Choices & Trade-offs

Q: Why Spring Boot vs Node.js?
A: Mature integrations, robust validation/AOP, predictable performance for JVM; team familiarity.
Q: Why Redis and Hazelcast together?
A: Redis for fast atomic counters/locks/TTL; Hazelcast for distributed near-cache of rich objects.
Q: Why no Kafka initially?
A: Verification is request/response; replay not needed; added lightweight webhooks/events only.
Q: Why Docker + AWS?
A: Portable deployments, rapid scaling, managed networking for secure provider egress.
Q: Why OpenTelemetry + Prometheus/Grafana?
A: Vendor-neutral tracing, reliable metrics/alerts, quick dashboards for ops readiness.