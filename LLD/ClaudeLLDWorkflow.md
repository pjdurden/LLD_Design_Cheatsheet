Here is the converted Markdown file. I've cleaned up the formatting to ensure it's high-contrast and easy to copy-paste as a template for your Low-Level Design (LLD) practice.

---

# LLD Interview Script Workflow

Use this template to generate LLD interview scripts for any topic. Fill in each section during your preparation or live interview.

```markdown
/*
======================== LLD DESIGN INTERVIEW SCRIPT ({SYSTEM_NAME}) ========================

------------------------------------------------------------------------------------------
1) CLARIFYING REQUIREMENTS (What I ask first)
------------------------------------------------------------------------------------------
"Before I start designing, I want to clarify scope and assumptions."

Questions I ask:
1. Core features required in MVP?
   - {Feature 1} ✅
   - {Feature 2} ✅
   - {Feature 3} ✅
2. {Domain-specific question}?
   - {Sub-question with answer}
3. {Edge case / rule question}?
   - {Sub-question with answer}
4. Concurrency:
   - {Concurrent access scenario}? ✅
5. Persistence:
   - In-memory acceptable for interview? ✅

Assumptions in this implementation:
- In-memory storage (Maps/Lists)
- {Assumption 1}
- {Assumption 2}
- {Assumption 3}

------------------------------------------------------------------------------------------
2) REQUIREMENTS
------------------------------------------------------------------------------------------
Functional Requirements:
- {Actor} can {action 1}
- {Actor} can {action 2}
- {Actor} can {action 3}
- System must enforce {business rule}

Non-Functional Requirements:
- Thread-safe handling for {critical operations}
- Extensible for {future features}
- Maintainable separation of concerns
- {Additional NFR if applicable}

------------------------------------------------------------------------------------------
3) IDENTIFY ENTITIES (Core Classes)
------------------------------------------------------------------------------------------
- {Entity1}
- {Entity2}
- {Entity3}
- {ServiceLayer} (orchestrator / manager)

Enums:
- {Enum1}
- {Enum2}

Interfaces:
- {Interface1} (if applicable)

------------------------------------------------------------------------------------------
4) RELATIONSHIPS (Has-a / Is-a)
------------------------------------------------------------------------------------------
{Entity1} has:
- {field1}
- {field2}
- List<{Entity2}>

{Entity2} has:
- {field1}
- belongs to {Entity1}

{ServiceLayer} has:
- Map<id, {Entity1}>
- Map<id, {Entity2}>

{Entity3} is-a {BaseClass} (inheritance if applicable)

------------------------------------------------------------------------------------------
5) DESIGN PATTERNS USED
------------------------------------------------------------------------------------------
{Pattern 1}:
- {Why and where used}

{Pattern 2}:
- {Why and where used}

Thread Safety:
- {Approach: synchronized / ConcurrentHashMap / locks}

(Extension ideas):
- {Pattern that could be added later}

------------------------------------------------------------------------------------------
6) CORE APIs (Method Signatures / Entry Points)
------------------------------------------------------------------------------------------
- {ServiceLayer}.{method1}({params}) -> {return}
- {ServiceLayer}.{method2}({params}) -> {return}
- {ServiceLayer}.{method3}({params}) -> {return}

------------------------------------------------------------------------------------------
7) KEY IMPLEMENTATION NOTES (Optional, for complex logic)
------------------------------------------------------------------------------------------
- {Note 1: e.g., state transitions, validation rules}
- {Note 2: e.g., concurrency handling approach}
- {Note 3: e.g., edge case handling}

*/

// ======================== CODE STARTS BELOW ========================

```

---

## How to Use

1. **Pick a topic:** (e.g., "Library Management", "Hotel Booking", "Chess Game").
2. **Section 1:** List 4–6 clarifying questions you'd ask the interviewer, with assumptions.
3. **Section 2:** Write functional + non-functional requirements.
4. **Section 3:** List core entities (nouns from requirements).
5. **Section 4:** Define relationships (has-a, is-a, maps).
6. **Section 5:** Mention design patterns used (Strategy, State, Singleton, Factory, Observer, etc.).
7. **Section 6:** Define public APIs exposed by the service layer.
8. **Section 7:** Add implementation notes for tricky logic.
9. **Code:** Implement classes below the comment block.
