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