
---

# High-Scale Notification System (Critical + Promotional)

A distributed, horizontally scalable notification system capable of processing **1M notifications/sec** with an **80/20 split** between **critical** (time-sensitive) and **promotional** (fanout-heavy) notifications.

Designed to support **email, push, and SMS** channels with strong delivery guarantees, idempotency, template management, and large-scale fanout.

---

# 1. Functional Requirements

###  Instant Notifications

Clients can send **critical notifications** that must be delivered immediately.

###  Promotional Notifications

Clients can send **promo notifications** that target 1 → 1M+ users, often scheduled.

###  Multi-Device Delivery

Support **Apple (APNs)**, **Android (FCM)**, **Email (SES)**, **SMS (Twilio)**.

---

# 2. Non-Functional Requirements

###  Ultra-Low Latency for Critical

Critical notifications must be **near real-time** (<50–100 ms end-to-end where possible).

###  At-Least-Once Delivery

Use queues + retries → ensures delivery even under system failures.

###  High Throughput Scaling

System must scale to:

* **800k/sec critical**
* **200k/sec promotional**
  (80/20 split)

---

# 3. Core Entities

| Entity           | Purpose                                               |
| ---------------- | ----------------------------------------------------- |
| **Client**       | Represents the business or app sending notifications. |
| **Notification** | A single notification event with intent & channel.    |
| **Template**     | Message templates stored in S3 + cached in Redis.     |
| **Job (Promo)**  | Represents promotional campaign requests.             |

---

# 4. API Endpoints

### `POST /notif`

Send immediate critical notifications.
(Uses idempotency key to prevent duplicate sends)

### `POST /template`

Upload or update a notification template.

### `POST /promo`

Create a promotional campaign (scheduled or immediate fanout).

---

# 5. High-Level Design (HLD)

This system separates **critical** and **promo** flows because their scalability patterns differ:

* **Critical = low fanout, extremely low latency**
* **Promo = massive fanout, scheduling + async fanout**

---

## 5.1 Functional Architecture

### Ingestion Layer

Receives API requests, validates, and pushes to the correct flow.

> Note: Ingestion must be stateless → scale horizontally.

### Workers

Separate workers ensure slow promotional traffic doesn’t block critical paths.

---

## 5.2 Non-Functional Design Choices

### Persistent Queues (Kafka)

Used for both flows to ensure durability and at-least-once delivery.

### Idempotency Store (Redis)

Prevents duplicate sending due to retries / client resubmissions.

---

# 6. End-to-End Flow

## 6.1 Critical Flow (Fast-Path)

1. Client → API Gateway
2. Ingestion Service publishes message to correct **Kafka shard**
3. Workers (Email/SMS/Push) consume
4. Load template from Redis or S3
5. Send to provider (SES/APN/FCM/Twilio)
6. Retry on failure → DLQ after max retries

**Why shards?**
To avoid a single partition bottleneck and allow parallel consumption at scale.

---

## 6.2 Promotional Flow (Slow-Path / Fanout)

1. Client creates a promo job (`POST /promo`)
2. Job stored in **JobDB**
3. Scheduler picks job using **lease locking**
4. Scheduler performs **fanout**:

   * expands segments
   * writes execution records to DB
5. Channel workers pick execution entries and deliver

**Fanout may produce millions of records**, so batching + async writes are critical.

---

# 7. Template Management

### Storage

* Templates stored in **S3** (large objects)
* Cached in **Redis** for low-latency access

> Most notifications spend more time waiting for template loading than sending, so caching is essential.

### Versioning

Use `template_id` + optional version to avoid breaking older jobs.

---

# 8. Detailed Architecture Diagram

```
                    +----------------+
                    |     Client     |
                    +-------+--------+
                            |
                        API Gateway
                            |
              +------------------------------+
              |       Ingestion Service      |
              +-------------+----------------+
                            |
            +---------------+----------------------+
            |                                      |
         Critical                               Promotional
            |                                      |
      +-----+--------+                       +-----+----------------+
      |   Kafka      |                       |    Promo Scheduler   |
      |   Shards     |                       +----------------------+
      +------+-------+                                |
             |                                        |
   +---------+------------+                 +----------+---------+
   | Channel Workers      |                 |     Promo Fanout   |
   | (email/sms/push)     |                 | (Segment → ExecDB) |
   +------+---+----+------+                 +----------+----------+
          |   |    |                                 |
       +--+ +--+  +--+                     +----------+------------+
       |SES| |APN| |Twilio|                |     Execution DB       |
       +----+----+------+                 +-----------+-------------+
                                                       |
                                         +-------------+-----------+
                                         | User DB & Segment DB    |
                                         +--------------------------+

             +-----------------------------------------------+
             | Template Service + S3 Storage (Redis Cache)    |
             +-----------------------------------------------+

                       +---------------------------+
                       | Idempotency Store (Redis) |
                       +---------------------------+
```

---

# 9. Deep Dives (Short & Practical)

## 9.1 Why Idempotency Matters

Network retries, client retries, and worker restarts may re-send the same notification.
**Idempotency keys ensure only one execution is processed.**

**Typical key:** `clientId:userId:channel:templateId:timestamp`

---

## 9.2 Scheduler Concurrency (Lease Locks)

Multiple schedulers may pick the same job.
Use **DynamoDB conditional updates** or **Redis SETNX with TTL** to ensure **only one scheduler owns the job** for a time window.

> A lease prevents double fanout and duplicate execution creation.

---

## 9.3 Promo Fanout Optimization

Promo can require writing **millions of execution records**.

**Optimizations:**

* Batch writes (25 items each for DynamoDB)
* Async fanout using internal queues
* Backpressure to avoid overloading ExecutionDB
* Horizontal sharding of execution tables with `campaign_id % shard`

---

## 9.4 Why Separate Channel Workers?

Email, SMS, Push all have different:

* Latencies
* Provider limits
* Retry strategies
* Costs

**Isolation prevents slow SMS queues from blocking Push or Email.**

---

## 9.5 Provider Reliability Strategy

Each provider integration must:

* Retry with exponential backoff
* Track bounce/invalid tokens
* Push failed messages to DLQ

> Providers are the most common point of failure—wrapping them with retry logic ensures reliability.

---

# 10. Capacity & Throughput Summary (800k Critical + 200k Promo/sec)

* **Ingestion Layer:** ~40–50 nodes needed (25k req/sec each) to handle 1M requests/sec.
* **Queues:** Critical requires **24 Kafka partitions**; promo requires **8 partitions** for safe throughput.
* **Workers:** Email (~3k/sec), Push (~5k/sec), SMS (~1k/sec) → **100–150 workers per channel** at peak.
* **Network Load:** System must sustain **3–5 Gbps** including retries and multi-channel traffic.
* **Promo Fanout:** 200k promo/sec can expand to **millions of execution records**, requiring batch writes + backpressure.

---

# 11. Additional Notes
* Use **Kafka partitions** = `num_workers * 3` as a starting point to avoid consumer lag.
* Use **structured logs** with traceId/userId for debugging high-volume flows.
* Use **dead letter queues** for both critical/promotional flows.
* Use **Redis pipelines** when resolving idempotency for high throughput.
* Use **CloudWatch/EventBridge** or **internal scheduler** depending on infra.
* Segment expansion should be offloaded to a **Segment Service** if complex.

---



