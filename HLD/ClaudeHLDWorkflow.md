# System Design: {Topic Name}

---

## 1. Functional Requirements

* **{Feature 1}:** {Description}
* **{Feature 2}:** {Description}
* **{Feature 3}:** {Description}

**Out of Scope:** {List features explicitly excluded}

---

## 2. Non-Functional Requirements

* **Latency:** {Target, e.g., <50ms p99}
* **Scalability:** {Target, e.g., 1M DAU, 10K TPS}
* **Availability:** {Target, e.g., 99.99%}
* **Consistency:** {AP vs CP, eventual vs strong}
* **Security/Durability:** {If applicable}

---

## 3. Core Entities

| Entity       | Purpose                          |
|--------------|----------------------------------|
| **{Entity1}** | {Description}                   |
| **{Entity2}** | {Description}                   |
| **{Entity3}** | {Description}                   |

**Relationships:** {1:N, N:M if relevant}

---

## 4. API Design

### Endpoints

* `POST /{resource}` — {Description}
* `GET /{resource}/{id}` — {Description}
* `PUT/DELETE /{resource}/{id}` — {Description}

**Auth:** {JWT / API Key / OAuth}

---

## 5. High-Level Design (HLD)

### 5.1 {Subsystem 1: e.g., Write Path}

* **Bad:** {Naive approach and why it fails}
* **Good:** {Better approach}
* **Best:** {Optimal approach with rationale}

### 5.2 {Subsystem 2: e.g., Read Path}

* **Bad:** {Naive approach}
* **Good:** {Better approach}
* **Best:** {Optimal approach}

### 5.3 {Subsystem 3: e.g., Sync/Notification/Scheduling}

* {Describe flow, components, trade-offs}

---

## 6. System Architecture (ASCII/Diagram)
{ASCII diagram or reference to image}


---

## 7. Deep Dives

### 7.1 {Critical Aspect 1: e.g., Scalability/Hot Keys/Sharding}

* **Problem:** {Describe challenge}
* **Solution:** {Describe approach, e.g., consistent hashing, partitioning}
* **Trade-offs:** {What you gain/lose}

### 7.2 {Critical Aspect 2: e.g., Fault Tolerance/Replication}

* **Problem:** {Describe challenge}
* **Solution:** {Describe approach, e.g., leader-follower, quorum writes}
* **Trade-offs:** {What you gain/lose}

### 7.3 {Critical Aspect 3: e.g., Security/Idempotency/Durability}

* **Problem:** {Describe challenge}
* **Solution:** {Describe approach, e.g., CDC, idempotency keys, signed requests}
* **Trade-offs:** {What you gain/lose}

---

## 8. Capacity Estimation (Optional but Recommended)

| Metric             | Value                  |
|--------------------|------------------------|
| Storage per record | {e.g., 500 bytes}      |
| Total storage      | {e.g., 500 GB for 1B}  |
| Write TPS          | {e.g., 1K/sec}         |
| Read TPS           | {e.g., 100K/sec}       |
| Nodes required     | {e.g., 10 shards}      |

---

## 9. Final Architecture Summary

* {Component 1}
* {Component 2}
* {Component 3}
* {Key trade-offs and design choices}

---

## 10. Bonus (Optional)

* {Mention advanced topics: CDC, webhooks, bloom filters, circuit breakers, etc.}

## How to Use
* Pick a topic (e.g., "Distributed Message Queue", "Ride Sharing", "Video Streaming CDN").
* Fill in each section using the template above.
* Use Bad/Good/Best for design choices to show trade-off thinking.
* Include 2–4 Deep Dives on the most critical aspects (scalability, consistency, security, hot keys, etc.).
* Add capacity estimation to show you understand scale.
