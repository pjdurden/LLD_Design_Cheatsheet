/*
======================== LLD DESIGN INTERVIEW SCRIPT (SPLITWISE) ========================

------------------------------------------------------------------------------------------
1) CLARIFYING REQUIREMENTS (what I ask first)
------------------------------------------------------------------------------------------
"Before designing, I want to clarify scope and assumptions."

Questions I ask:
1. Users:
   - Can users be uniquely identified by ID? ✅
   - Authentication in scope? ❌ (out of scope)
2. Groups:
   - Can expenses be split without a group? ✅
   - Can a group have multiple expenses? ✅
3. Splitting types:
   - Equal split? ✅
   - Exact amounts? ✅
   - Percentage-based? (optional, skipped here)
4. Settlement:
   - Do we auto-minimize cash flow like Splitwise? ✅
   - Should balances be tracked pairwise? ✅
5. Precision:
   - Floating-point acceptable? (double used)
6. Concurrency:
   - Multiple users adding expenses simultaneously? ✅
7. Scale:
   - In-memory solution acceptable for interview? ✅

Assumptions in this implementation:
- No authentication or persistence
- Users identified by userId
- Expense can be added with equal or exact split
- Balances are stored as: who owes whom and how much
- Uses in-memory data structures

------------------------------------------------------------------------------------------
2) REQUIREMENTS
------------------------------------------------------------------------------------------
Functional Requirements:
- Add users
- Add expenses (equal / exact)
- Track balances between users
- Show balances for a user
- Simplify balances (minimize transactions)

Non-Functional Requirements:
- Thread-safe expense addition
- Clean separation of responsibilities
- Extensible for new split types
- Accurate financial calculations

------------------------------------------------------------------------------------------
3) IDENTIFY ENTITIES (core classes)
------------------------------------------------------------------------------------------
User
Expense
Split
BalanceSheet
SplitwiseService
SplitType enum

------------------------------------------------------------------------------------------
4) RELATIONSHIPS (has-a / is-a)
------------------------------------------------------------------------------------------
SplitwiseService has Map<userId, User>
SplitwiseService has BalanceSheet
Expense has paidBy(User) + List<Split>
Split has User + amount
BalanceSheet has Map<User, Map<User, Double>>

------------------------------------------------------------------------------------------
5) DESIGN CHOICES / PATTERNS DISCUSSED
------------------------------------------------------------------------------------------
Command-style service:
- SplitwiseService exposes APIs
- Internal logic hidden from caller

Balance Sheet approach:
- balances[A][B] = amount A owes B
- balances[B][A] = -amount

Why not store Expense history only?
- Querying balances becomes expensive
- Precomputed balances give O(1) reads

Synchronization:
- addExpense() synchronized to avoid race conditions

(Extension idea)
Strategy Pattern:
- SplitStrategy:
  - EqualSplitStrategy
  - ExactSplitStrategy
  - PercentageSplitStrategy

------------------------------------------------------------------------------------------
6) CORE APIs (method signatures / entry points)
------------------------------------------------------------------------------------------
SplitwiseService.addUser(userId, name)
SplitwiseService.addExpense(paidBy, amount, participants, splitType, splits)
SplitwiseService.showBalance(userId)
SplitwiseService.showAllBalances()
SplitwiseService.simplifyDebts()

------------------------------------------------------------------------------------------
7) EDGE CASES I DISCUSS
------------------------------------------------------------------------------------------
- User not found ❌ rejected
- Split sum != total amount ❌ rejected
- Self-balance ignored
- Floating precision issues (can be solved with BigDecimal)
- Duplicate users in expense ❌ should be validated

------------------------------------------------------------------------------------------
8) EXTENSIBILITY (how to make it production-ready)
------------------------------------------------------------------------------------------
Possible upgrades:
- Persistent storage (DB)
- Percentage splits
- Expense history & audit trail
- Group support
- Currency support
- Decimal precision handling
- REST APIs
- Event-driven settlement

------------------------------------------------------------------------------------------
9) WALKTHROUGH (example flow)
------------------------------------------------------------------------------------------
Users: A, B, C
A pays 300 for A, B, C equally
Each owes 100
Balances:
B owes A 100
C owes A 100

==========================================================================================
CODE STARTS BELOW
==========================================================================================
*/

import java.util.*;

public class Splitwise_LLD {

    // ---------------- ENUMS ----------------
    enum SplitType {
        EQUAL,
        EXACT
    }

    // ---------------- ENTITIES ----------------
    static class User {
        String userId;
        String name;

        User(String userId, String name) {
            this.userId = userId;
            this.name = name;
        }
    }

    static class Split {
        User user;
        double amount;

        Split(User user, double amount) {
            this.user = user;
            this.amount = amount;
        }
    }

    static class Expense {
        User paidBy;
        double totalAmount;
        List<Split> splits;

        Expense(User paidBy, double totalAmount, List<Split> splits) {
            this.paidBy = paidBy;
            this.totalAmount = totalAmount;
            this.splits = splits;
        }
    }

    // ---------------- BALANCE SHEET ----------------
    static class BalanceSheet {

        // balances[A][B] = amount A owes B
        Map<User, Map<User, Double>> balances = new HashMap<>();

        void addBalance(User from, User to, double amount) {
            balances.putIfAbsent(from, new HashMap<>());
            balances.get(from).put(
                    to,
                    balances.get(from).getOrDefault(to, 0.0) + amount
            );
        }

        void showBalance(User user) {
            if (!balances.containsKey(user)) return;

            for (Map.Entry<User, Double> entry : balances.get(user).entrySet()) {
                if (entry.getValue() > 0) {
                    System.out.println(
                            user.name + " owes " +
                            entry.getKey().name + " : " +
                            entry.getValue()
                    );
                }
            }
        }

        void showAllBalances() {
            for (User user : balances.keySet()) {
                showBalance(user);
            }
        }

        // ---------------- SIMPLIFY DEBTS ----------------
        void simplifyDebts() {

            Map<User, Double> net = new HashMap<>();

            // Step 1: Calculate net balance per user
            for (User from : balances.keySet()) {
                for (Map.Entry<User, Double> entry : balances.get(from).entrySet()) {
                    User to = entry.getKey();
                    double amount = entry.getValue();

                    net.put(from, net.getOrDefault(from, 0.0) - amount);
                    net.put(to, net.getOrDefault(to, 0.0) + amount);
                }
            }

            Queue<Map.Entry<User, Double>> debtors = new LinkedList<>();
            Queue<Map.Entry<User, Double>> creditors = new LinkedList<>();

            // Step 2: Separate debtors and creditors
            for (Map.Entry<User, Double> entry : net.entrySet()) {
                if (entry.getValue() < 0) {
                    debtors.add(entry);
                } else if (entry.getValue() > 0) {
                    creditors.add(entry);
                }
            }

            System.out.println("\n---- Simplified Balances ----");

            // Step 3: Greedy settlement
            while (!debtors.isEmpty() && !creditors.isEmpty()) {
                Map.Entry<User, Double> debtor = debtors.poll();
                Map.Entry<User, Double> creditor = creditors.poll();

                double settleAmount = Math.min(
                        -debtor.getValue(),
                        creditor.getValue()
                );

                System.out.println(
                        debtor.getKey().name + " pays " +
                        creditor.getKey().name + " : " +
                        settleAmount
                );

                debtor.setValue(debtor.getValue() + settleAmount);
                creditor.setValue(creditor.getValue() - settleAmount);

                if (debtor.getValue() < 0) debtors.add(debtor);
                if (creditor.getValue() > 0) creditors.add(creditor);
            }
        }
    }

    // ---------------- SERVICE ----------------
    static class SplitwiseService {

        Map<String, User> users = new HashMap<>();
        BalanceSheet balanceSheet = new BalanceSheet();

        public void addUser(String userId, String name) {
            users.put(userId, new User(userId, name));
        }

        public synchronized void addExpense(
                String paidById,
                double amount,
                List<String> participantIds,
                SplitType splitType,
                List<Double> exactAmounts
        ) {
            User paidBy = users.get(paidById);
            List<User> participants = new ArrayList<>();

            for (String id : participantIds) {
                participants.add(users.get(id));
            }

            List<Split> splits = new ArrayList<>();

            if (splitType == SplitType.EQUAL) {
                double share = amount / participants.size();
                for (User user : participants) {
                    splits.add(new Split(user, share));
                }
            } else if (splitType == SplitType.EXACT) {
                double sum = 0;
                for (double val : exactAmounts) sum += val;
                if (sum != amount) throw new IllegalArgumentException("Invalid split");

                for (int i = 0; i < participants.size(); i++) {
                    splits.add(new Split(participants.get(i), exactAmounts.get(i)));
                }
            }

            Expense expense = new Expense(paidBy, amount, splits);
            updateBalances(expense);
        }

        private void updateBalances(Expense expense) {
            for (Split split : expense.splits) {
                if (split.user != expense.paidBy) {
                    balanceSheet.addBalance(
                            split.user,
                            expense.paidBy,
                            split.amount
                    );
                }
            }
        }

        public void showBalance(String userId) {
            balanceSheet.showBalance(users.get(userId));
        }

        public void showAllBalances() {
            balanceSheet.showAllBalances();
        }

        public void simplifyDebts() {
            balanceSheet.simplifyDebts();
        }
    }

     public static void main(String[] args) {
        SplitwiseService service = new SplitwiseService();

        service.addUser("u1", "Alice");
        service.addUser("u2", "Bob");
        service.addUser("u3", "Charlie");

        service.addExpense(
                "u1",
                300,
                Arrays.asList("u1", "u2", "u3"),
                SplitType.EQUAL,
                null
        );
        service.simplifyDebts();
        service.showAllBalances();
    }
}



