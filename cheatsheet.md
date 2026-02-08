# eBay EM – Design Fundamentals Cheat Sheet (LLD + HLD)

---

## LLD THEORY (Principles + Patterns)

### OOP (1–2 lines each)
- **Encapsulation**: Keep state private, expose behavior via methods.
- **Abstraction**: Hide implementation details behind interfaces/contracts.
- **Inheritance**: “is-a” relationship; use sparingly to avoid rigid hierarchies.
- **Composition**: “has-a” relationship; preferred for flexibility and reuse.
- **Polymorphism**: Same interface, different implementations at runtime.

---

### SOLID (1–2 lines each)
- **S — Single Responsibility**: One class/module should have one reason to change.
- **O — Open/Closed**: Extend behavior via new code, avoid modifying stable code.
- **L — Liskov Substitution**: Child classes must behave like the parent contract promises.
- **I — Interface Segregation**: Prefer small, focused interfaces over one large one.
- **D — Dependency Inversion**: Depend on abstractions, not concrete implementations.

---

### Core Design Principles
- **DRY**: Avoid duplicating logic; centralize shared behavior.
- **KISS**: Prefer simplest design that meets requirements.
- **YAGNI**: Don’t build future features until needed.
- **High Cohesion**: Keep related responsibilities together in a class/module.
- **Low Coupling**: Minimize dependency between modules to improve change safety.
- **Separation of Concerns**: Split business logic, persistence, communication cleanly.
- **Law of Demeter**: Avoid chaining calls; talk only to direct collaborators.

---

## Design Patterns (What + When)

### Creational
- **Factory**: Create objects based on input type while hiding instantiation logic.
- **Abstract Factory**: Create families of related objects without specifying classes.
- **Builder**: Construct complex objects step-by-step (many optional fields).
- **Singleton**: Single instance with global access; use cautiously (testing/threading).

### Structural
- **Adapter**: Wrap incompatible interfaces to work together.
- **Decorator**: Add features dynamically without changing core class.
- **Facade**: Provide a simplified API over complex subsystems.

### Behavioral (Most Interview-Useful)
- **Strategy**: Swap algorithms/rules (pricing, ranking, payment) without modifying clients.
- **State**: Object behavior changes based on state (order lifecycle, vending machine).
- **Observer (Pub/Sub)**: Notify subscribers on events (bid updates, notifications).
- **Command**: Encapsulate requests as objects (undo, queueing, auditing).
- **Chain of Responsibility**: Pass request through handlers (validation, fraud rules).

---

## LLD Common Concerns (Quick Reminders)
- **Validation**: Centralize checks; avoid scattered validations everywhere.
- **Extensibility**: Add new behavior via interfaces/strategies, not conditionals.
- **Testability**: Inject dependencies; avoid hard-coded globals/time/network calls.
- **Concurrency basics**: Avoid race conditions for bid/seat/inventory updates.
- **Idempotency**: Same request repeated → should not duplicate orders/payments.

---

# HLD FUNDAMENTALS (Keep Full)

---

## API Design
- **REST basics**: Use correct HTTP verbs, resource-oriented URIs.
- **Pagination**: Cursor-based preferred for large datasets. - 
- **Filtering/Sorting**: Query params; validate allowed fields.
- **Versioning**: `/v1/...` or headers to support backward compatibility.
- **Idempotency keys**: Required for payment/order creation endpoints.

---

## Storage & Data Modeling
- **SQL**: Strong consistency, relational constraints (orders, payments, user identity).
- **NoSQL**: High scale, flexible schema (chat messages, event logs, feeds).
- **Indexing**: Add indexes for frequent queries; watch write amplification.
- **Sharding/Partitioning**: Distribute data by key (userId, sellerId, eventId).
- **Denormalization**: Used for read performance; tradeoff is update complexity.

---

## Caching
- **Cache-aside**: App reads cache → falls back to DB → writes cache (most common).
- **TTL**: Expire stale data; choose TTL based on freshness requirement.
- **Cache invalidation**: Hard part; use events or versioned keys where possible.
- **What to cache**: Product details, seller profile, configs, feature flags.

---

## Messaging / Async Processing
- **Queue usage**: Offload slow tasks and absorb traffic spikes.
- **Event streaming**: Great for audit trails, analytics, fanout updates.
- **At-least-once delivery**: Expect duplicates → dedupe via idempotency.
- **Outbox pattern**: Persist event + DB changes atomically to avoid missing events.

---

## Reliability & Fault Tolerance
- **Timeouts**: Every network call must have a timeout.
- **Retries**: Use exponential backoff + jitter; retry only safe operations.
- **Circuit breaker**: Prevent cascading failures when dependencies are down.
- **Bulkheads**: Isolate failures by limiting resource sharing between components.
- **Graceful degradation**: Fallback experiences when services fail (read-only, cached).

---

## Consistency & Concurrency (Distributed Systems)
- **Strong consistency**: Needed for payments, inventory decrement, bidding winner.
- **Eventual consistency**: OK for analytics, activity feeds, counters.
- **Optimistic locking**: Version field to detect concurrent writes.
- **Pessimistic locking**: DB row lock for high-contention updates.
- **Distributed locks**: Redis-based locks for cross-node critical sections (use carefully).

---

## Idempotency (Must-Know)
- **Why**: Prevent duplicate charges/orders during retries.
- **How**: Store `(idempotencyKey -> response/result)` with expiry + status.
- **Where**: Payment capture, checkout/order creation, bid placement.

---

## Scaling Concepts
- **Horizontal scaling**: Add instances; keep services stateless.
- **Load balancing**: Spread traffic; health checks + autoscaling.
- **Read replicas**: Scale reads; beware replication lag.
- **CQRS (basic idea)**: Separate read model from write model when needed.

---

## Observability
- **Metrics**: Latency p95/p99, error rate, throughput, queue lag.
- **Logs**: Structured logs with correlation IDs.
- **Tracing**: Distributed tracing to find bottlenecks across services.
- **Alerting**: Actionable alerts tied to SLO breaches.

---

## Security Basics
- **AuthN vs AuthZ**: Identity vs permissions.
- **OAuth/JWT**: Common session/token patterns.
- **Rate limiting**: Protect APIs from abuse.
- **PII protection**: Encrypt at rest + in transit; least privilege access.
- **Audit logs**: Track critical actions (payment, refunds, seller changes).

---

## eBay Live / Auctions (Domain-Aware HLD Notes)
- **Real-time updates**: WebSocket or streaming for bids/chat updates.
- **Fanout**: Publish bid events → clients subscribe (Observer/PubSub conceptually).
- **Inventory reservation**: Lock stock during checkout window; release on timeout.
- **Fraud/spam controls**: Rate limit chat/bids + moderation pipelines.
- **Checkout reliability**: Idempotent payment capture + retry-safe order creation.
