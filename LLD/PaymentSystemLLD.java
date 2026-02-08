/*
======================== LLD DESIGN INTERVIEW SCRIPT (PAYMENT SYSTEM) ========================

------------------------------------------------------------------------------------------
1) CLARIFYING REQUIREMENTS (what I ask first)
------------------------------------------------------------------------------------------
"Before jumping into design, I want to clarify the scope and assumptions."

Questions I ask:
1. Payment flow:
   - Can users initiate payments? ✅
   - Are payments one-time only? ✅ (no subscriptions)
   - Do we support refunds? ❌ (out of scope for now)
2. Payment method:
   - Card / UPI / Wallet? (abstracted as PaymentMethod)
   - External PSP involved? ✅ (simulated here)
3. Idempotency:
   - Can duplicate requests happen? ✅
   - Should retries be safe? ✅ (idempotency key required)
4. State model:
   - Possible states? INITIATED, PROCESSING, SUCCESS, FAILED
   - Terminal states? SUCCESS, FAILED
5. Concurrency:
   - Same payment retried concurrently? ✅
   - Thread safety required? ✅
6. Scale:
   - LLD-level scale → in-memory is acceptable
7. Guarantees:
   - At-most-once charge? ✅ (critical)
8. Authentication:
   - Out of scope (User object passed directly)

Assumptions:
- Single currency
- External PSP simulated
- No refunds or chargebacks
- In-memory storage
- Exactly-once charge enforced via idempotency key

------------------------------------------------------------------------------------------
2) REQUIREMENTS
------------------------------------------------------------------------------------------
Functional Requirements:
- Initiate a payment
- Process payment asynchronously
- Query payment status
- Prevent duplicate charges using idempotency

Non-Functional Requirements:
- Thread safety
- Idempotency
- Consistency of payment state
- Extensible for refunds, webhooks, multiple PSPs

------------------------------------------------------------------------------------------
3) IDENTIFY ENTITIES (core classes)
------------------------------------------------------------------------------------------
User
Payment
PaymentRequest
PaymentService
PaymentGateway (interface)
MockPaymentGateway (implementation)

Data Structures:
payments: paymentId -> Payment
idempotencyMap: idempotencyKey -> paymentId

------------------------------------------------------------------------------------------
4) RELATIONSHIPS
------------------------------------------------------------------------------------------
PaymentService:
- owns Map<paymentId, Payment>
- owns Map<idempotencyKey, paymentId>

Payment:
- belongs to a User
- has PaymentStatus
- uses PaymentMethod

PaymentGateway:
- external dependency abstraction

------------------------------------------------------------------------------------------
5) DESIGN CHOICES / PATTERNS
------------------------------------------------------------------------------------------
State Pattern (implicit):
- Payment moves through fixed lifecycle states

Idempotency Pattern:
- Idempotency key mapped to paymentId
- Duplicate requests return same payment

Thread Safety:
- ConcurrentHashMap
- Atomic state transitions using synchronized blocks

Abstraction:
- PaymentGateway interface allows PSP swap

------------------------------------------------------------------------------------------
6) CORE APIs (entry points)
------------------------------------------------------------------------------------------
PaymentService.initiatePayment(request) -> paymentId
PaymentService.getPaymentStatus(paymentId) -> PaymentStatus

Internal:
Payment.process()
Payment.transitionState()

------------------------------------------------------------------------------------------
7) EDGE CASES HANDLED
------------------------------------------------------------------------------------------
- Duplicate payment request → same payment returned
- Concurrent processing attempts → synchronized
- PSP failure → payment marked FAILED
- Invalid paymentId → exception

------------------------------------------------------------------------------------------
8) EXTENSIBILITY
------------------------------------------------------------------------------------------
- Add refunds
- Add multiple PSPs
- Add async processing via queue
- Add persistence (DB)
- Add webhook callbacks
- Add currency conversion

------------------------------------------------------------------------------------------
9) WALKTHROUGH
------------------------------------------------------------------------------------------
User sends payment request with idempotency key
System checks idempotency map
If new → create payment
Payment moves INITIATED → PROCESSING → SUCCESS/FAILED
Subsequent retries return same payment result

==========================================================================================
CODE STARTS BELOW
==========================================================================================
*/

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// -------- ENUMS --------
enum PaymentStatus {
    INITIATED,
    PROCESSING,
    SUCCESS,
    FAILED
}

// -------- USER --------
class User {
    private final String userId;

    public User(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}

// -------- PAYMENT --------
class Payment {
    private final String paymentId;
    private final User user;
    private final double amount;
    private PaymentStatus status;

    public Payment(String paymentId, User user, double amount) {
        this.paymentId = paymentId;
        this.user = user;
        this.amount = amount;
        this.status = PaymentStatus.INITIATED;
    }

    public synchronized void process(PaymentGateway gateway) {
        if (status != PaymentStatus.INITIATED) return;

        status = PaymentStatus.PROCESSING;
        boolean success = gateway.charge(amount);

        status = success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
    }

    public PaymentStatus getStatus() {
        // volatile can be used if we want to avoid synchronization for reads, but here we keep it simple
        // volatile is used when we have multiple threads reading/writing a variable without synchronization. In this case, since we are synchronizing the process method, we can ensure visibility of changes to the status variable across threads. However, if we want to allow concurrent reads of the status without synchronization, we could declare it as volatile to ensure that all threads see the most up-to-date value.
        return status;
    }

    public String getPaymentId() {
        return paymentId;
    }
}

// -------- PAYMENT REQUEST --------
class PaymentRequest {
    public final User user;
    public final double amount;
    public final String idempotencyKey;

    public PaymentRequest(User user, double amount, String idempotencyKey) {
        this.user = user;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
    }
}

// -------- PAYMENT GATEWAY --------
interface PaymentGateway {
    boolean charge(double amount);
}

// Mock PSP
class MockPaymentGateway implements PaymentGateway {
    public boolean charge(double amount) {
        return amount <= 1000; // simulate failures for large amounts
    }
}

// -------- PAYMENT SERVICE --------
class PaymentService {
    private final Map<String, Payment> payments = new ConcurrentHashMap<>();
    private final Map<String, String> idempotencyMap = new ConcurrentHashMap<>();
    private final AtomicLong paymentIdCounter = new AtomicLong(0);
    private final PaymentGateway gateway = new MockPaymentGateway();

    public String initiatePayment(PaymentRequest request) {
        if (idempotencyMap.containsKey(request.idempotencyKey)) {
            return idempotencyMap.get(request.idempotencyKey);
        }

        synchronized (this) {
            if (idempotencyMap.containsKey(request.idempotencyKey)) {
                return idempotencyMap.get(request.idempotencyKey);
            }

            String paymentId = "pay_" + paymentIdCounter.incrementAndGet();
            Payment payment = new Payment(paymentId, request.user, request.amount);

            payments.put(paymentId, payment);
            idempotencyMap.put(request.idempotencyKey, paymentId);

            payment.process(gateway);
            return paymentId;
        }
    }

    public PaymentStatus getPaymentStatus(String paymentId) {
        Payment payment = payments.get(paymentId);
        if (payment == null) throw new IllegalArgumentException("Payment not found");
        return payment.getStatus();
    }
}

// -------- MAIN --------
public class PaymentSystemLLD {
    public static void main(String[] args) {
        PaymentService service = new PaymentService();
        User user = new User("user-1");

        PaymentRequest req = new PaymentRequest(user, 500, "idem-123");
        String paymentId = service.initiatePayment(req);

        System.out.println("Payment ID: " + paymentId);
        System.out.println("Status: " + service.getPaymentStatus(paymentId));
    }
}
