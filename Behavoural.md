# eBay EM – Reusable STAR Story Bank (6 stories cover most questions)

> Goal: Use a small set of stories that can be reused across many behavioral questions.

---

## ✅ Story 1: Leading a team + delivering at scale (Leadership / Ownership)

**Use for:**
- “Tell me about a time you led”
- “Ownership example”
- “Deliver with impact”
- “Cross-functional leadership”

**S:** At Visa, our real-time payments platform had a monolith + multiple microservices, and we needed to scale delivery while keeping reliability strong.  
**T:** I had to lead execution across an 8-member engineering team and ensure the system could handle high traffic reliably.  
**A:** I broke work into service-level ownership, aligned the team on clear deliverables, built a predictable release and on-call rhythm, and worked closely with Ops/partner teams when changes affected production. I also ensured we had the right monitoring in place so delivery didn’t compromise stability.  
**R:** We supported **2000+ TPS**, integrated new capabilities without major disruption, and maintained **99.98% uptime**, while improving team execution and ownership clarity.

---

## ✅ Story 2: Production incident handling + reliability improvements (Execution under pressure)

**Use for:**
- “A time you handled a critical incident”
- “Worked under pressure”
- “Act with urgency”
- “How you debug production issues”

**S:** We had production incidents impacting cross-border payment flows, often caused by partner variability and time-sensitive failures.  
**T:** I needed to restore service quickly and prevent repeat incidents.  
**A:** I drove the triage using logs/monitoring, identified root causes by correlating failures with downstream dependencies, fixed the immediate issue, and then improved long-term reliability by adding better error classification, safe retry behavior, and tighter observability dashboards.  
**R:** We resolved **30+ incidents**, reduced repeat failures, and kept the platform at **99.98% uptime**.

---

## ✅ Story 3: Simplifying hardcoded logic into a reusable framework (Design / Simplify)

**Use for:**
- “A time you simplified a system”
- “Designed something extensible”
- “Reduced tech debt”
- “Handling evolving requirements”

**S:** Validations for payer/beneficiary identity checks were hardcoded across many SWIFT flows, making changes slow, risky, and inconsistent.  
**T:** I needed to make validations configurable without sacrificing performance.  
**A:** I designed a configurable validation framework by separating rule definition from execution. Instead of embedding rules in code across flows, we centralized validation behavior and made it extensible for new rules and message types. Performance and reliability were maintained using efficient lookups and caching where needed.  
**R:** We removed hardcoded validations across **100+ SWIFT message types**, supported **1000+ TPS**, and made future rule updates faster and safer.

---

## ✅ Story 4: Partner API integration at scale (Ambiguity + External dependencies)

**Use for:**
- “Working with external partners/dependencies”
- “Handling ambiguity”
- “Influencing stakeholders”
- “Customer-first decisions”

**S:** Visa’s cross-border network required integrating multiple partner bank APIs, each with different behaviors, payload formats, and reliability characteristics.  
**T:** My job was to integrate quickly but safely, without breaking production.  
**A:** I standardized integration patterns (validation, timeouts, retries, fallbacks), ensured observability was in place before rollout, and partnered with teams to manage edge cases like inconsistent responses and transient outages. I also ensured failures were visible and actionable for ops teams.  
**R:** We integrated **10+ partner APIs**, improved stability during rollouts, and reduced integration surprises in production.

---

## ✅ Story 5: Observability + CI/CD improvements (Engineering excellence)

**Use for:**
- “How did you improve engineering productivity?”
- “Improving processes”
- “Reducing MTTR / faster debugging”
- “Driving quality and consistency”

**S:** Debugging production issues was slow because monitoring wasn’t consistent and deployments were too manual across services.  
**T:** Improve speed and reliability of releases and troubleshooting.  
**A:** I automated CI/CD pipelines using Jenkins, strengthened monitoring using Splunk dashboards, and exposed Hazelcast cluster metrics into Grafana for real-time system health and performance visibility.  
**R:** Releases became more predictable, debugging time dropped, and the team detected performance issues earlier instead of reacting after failures.

---

## ✅ Story 6: Founder story (Veri5) — end-to-end ownership (0→1 building)

**Use for:**
- “Tell me something you built from scratch”
- “Entrepreneurial mindset”
- “Innovation under uncertainty”
- “Ownership beyond assigned scope”

**S:** I noticed a gap in real-time identity/account verification for EU cross-border payments, especially for banks and PSPs needing fast and reliable verification before transfers.  
**T:** I built an MVP end-to-end that proved the concept with production-grade engineering practices.  
**A:** I designed the system as an API platform with a high-performance distributed caching layer using Redis + Hazelcast, added observability using Prometheus + Grafana, and integrated the MVP with sandbox/mock APIs from EU open banking providers to validate the flow and performance.  
**R:** I shipped a production-ready MVP supporting **1000+ req/sec**, onboarded early testers, and demonstrated a scalable approach to verification infrastructure.

---

## ✅ Quick mapping (how to pick a story fast)
- **Leadership / Ownership** → Story 1  
- **Execution / Pressure** → Story 2  
- **Design / Simplify / Extensible** → Story 3  
- **Ambiguity / External dependency** → Story 4  
- **Process / Engineering excellence** → Story 5  
- **Vision / 0→1 / Entrepreneurship** → Story 6  


## EM Team Fit – Top 6 Questions

1) **Why eBay? Why eBay Live specifically?**
   - Motivation + understanding of live commerce + trust/community angle.

2) **Tell me about your leadership style. How do you run a team day-to-day?**
   - Ownership, execution rhythm, quality bar, communication.

3) **How do you work with Product Managers and handle disagreements on scope/timelines?**
   - Tradeoffs, alignment, decision-making.

4) **Tell me about a time you handled a high-severity incident or production outage.**
   - Calm under pressure, debugging approach, prevention.

5) **How do you ensure quality and reliability while moving fast?**
   - Guardrails: monitoring, feature flags, canaries, testing strategy.

6) **How do you grow engineers (mentoring) and handle underperformance?**
   - Coaching, feedback loops, expectations, improvement plan.
