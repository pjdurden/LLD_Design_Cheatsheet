import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
======================== LLD DESIGN INTERVIEW SCRIPT (VENDING MACHINE - STATE PATTERN) ========================

------------------------------------------------------------------------------------------
1) CLARIFYING REQUIREMENTS (what I ask first)
------------------------------------------------------------------------------------------
"Before I start coding, I’ll quickly confirm the vending machine workflow + edge cases."

Questions:
1. Flow:
   - User selects product ✅
   - User inserts money ✅
   - Machine dispenses product ✅
   - Machine returns change ✅
2. Payments supported:
   - Coins ✅ (PENNY/NICKEL/DIME/QUARTER)
   - Notes ✅ (ONE/FIVE/TEN/TWENTY)
3. Inventory handling:
   - Track product quantity ✅
   - Prevent selecting out-of-stock ✅
4. Change handling:
   - payment > price → return change ✅
   - payment = price → no change ✅
   - payment < price → block dispense ✅
5. Multiple products supported ✅
6. Single user at a time ✅ (no concurrency handling required for state transitions)

Assumptions:
- No cancel/refund in MVP (can be added later)
- Change returned as amount only (no coin-distribution logic)
- Inventory is thread-safe using ConcurrentHashMap

------------------------------------------------------------------------------------------
2) REQUIREMENTS
------------------------------------------------------------------------------------------
Functional:
- Add products into inventory with quantity
- Select a product
- Insert coins / notes
- Dispense only if payment is sufficient
- Return change (if any)
- Enforce valid actions depending on state

Non-functional:
- Maintainable design using State Pattern
- Extensible for new states (Maintenance / OutOfService)
- Extensible for new payment types (Card/UPI)
- Avoid currency precision bugs

------------------------------------------------------------------------------------------
3) ENTITIES (Core Classes)
------------------------------------------------------------------------------------------
- Product (id, name, priceCents)
- Inventory (productId -> item)
- InventoryItem (product + quantity)

- VendingMachine (orchestrator)
- VendingMachineState (state interface)

States:
- IdleState
- ReadyState
- DispenseState
- ReturnChangeState

Enums:
- Coin
- Note

Driver:
- VendingMachineLLD (main demo)

------------------------------------------------------------------------------------------
4) STATE TRANSITION FLOW
------------------------------------------------------------------------------------------
IdleState:
  - allows product selection
  - blocks payment/dispense/change

ReadyState:
  - accepts money
  - once enough money → transitions to DispenseState

DispenseState:
  - decrements inventory and dispenses product
  - transitions to ReturnChangeState

ReturnChangeState:
  - returns extra amount (if any)
  - resets transaction
  - transitions back to IdleState

------------------------------------------------------------------------------------------
5) DESIGN CHOICES / PATTERNS
------------------------------------------------------------------------------------------
✅ State Pattern:
- avoids huge if/else in VendingMachine
- each state controls allowed actions

✅ Singleton:
- one machine instance via getInstance()

✅ Correct money handling:
- use int cents instead of double to avoid floating precision issues

✅ Inventory correctness:
- use productId as key instead of Product object reference

------------------------------------------------------------------------------------------
6) EDGE CASES HANDLED
------------------------------------------------------------------------------------------
✅ selecting out of stock → blocked
✅ inserting money before selecting → blocked
✅ dispensing with insufficient payment → blocked
✅ dispensing when payment sufficient → allowed
✅ returning change after dispense → allowed
✅ transaction resets safely to Idle

------------------------------------------------------------------------------------------
7) FUTURE IMPROVEMENTS (nice extensions)
------------------------------------------------------------------------------------------
- Cancel transaction + refund state
- Return change as actual coins/notes distribution
- Maintenance/OutOfService mode
- PaymentStrategy for UPI/Card

==========================================================================================
CODE STARTS BELOW
==========================================================================================
*/

class Product {
    private final String productId;
    private final String name;
    private final int priceCents;

    public Product(String productId, String name, int priceCents) {
        this.productId = productId;
        this.name = name;
        this.priceCents = priceCents;
    }

    public String productId() { return productId; }
    public String name() { return name; }
    public int priceCents() { return priceCents; }
}

class InventoryItem {
    private final Product product;
    private int quantity;

    public InventoryItem(Product product, int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative");
        this.product = product;
        this.quantity = quantity;
    }

    public Product product() { return product; }
    public int quantity() { return quantity; }

    public void decrementOrThrow() {
        if (quantity <= 0) throw new IllegalStateException("Out of stock");
        quantity--;
    }
}

class Inventory {
    private final Map<String, InventoryItem> items = new ConcurrentHashMap<>();

    public void addProduct(Product product, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        items.put(product.productId(), new InventoryItem(product, quantity));
    }

    public boolean isAvailable(String productId) {
        InventoryItem item = items.get(productId);
        return item != null && item.quantity() > 0;
    }

    public InventoryItem getItemOrThrow(String productId) {
        InventoryItem item = items.get(productId);
        if (item == null) throw new IllegalStateException("Product not found");
        return item;
    }

    public void decrementStockOrThrow(String productId) {
        getItemOrThrow(productId).decrementOrThrow();
    }
}

interface VendingMachineState {
    void selectProduct(String productId);
    void insertCoin(Coin coin);
    void insertNote(Note note);
    void dispenseProduct();
    void returnChange();
}

enum Coin {
    PENNY(1),
    NICKEL(5),
    DIME(10),
    QUARTER(25);

    private final int cents;
    Coin(int cents) { this.cents = cents; }
    public int cents() { return cents; }
}

enum Note {
    ONE(100),
    FIVE(500),
    TEN(1000),
    TWENTY(2000);

    private final int cents;
    Note(int cents) { this.cents = cents; }
    public int cents() { return cents; }
}

class IdleState implements VendingMachineState {
    private final VendingMachine vm;

    public IdleState(VendingMachine vm) {
        this.vm = vm;
    }

    @Override
    public void selectProduct(String productId) {
        if (!vm.inventory.isAvailable(productId)) {
            System.out.println("Product not available / out of stock.");
            return;
        }

        vm.selectedProductId = productId;
        vm.state = vm.readyState;

        Product p = vm.inventory.getItemOrThrow(productId).product();
        System.out.println("Product selected: " + p.name() + " | Price: $" + (p.priceCents() / 100.0));
    }

    @Override public void insertCoin(Coin coin) { System.out.println("Select product first."); }
    @Override public void insertNote(Note note) { System.out.println("Select product first."); }
    @Override public void dispenseProduct() { System.out.println("Select product and pay first."); }
    @Override public void returnChange() { System.out.println("No change to return."); }
}

class ReadyState implements VendingMachineState {
    private final VendingMachine vm;

    public ReadyState(VendingMachine vm) {
        this.vm = vm;
    }

    @Override
    public void selectProduct(String productId) {
        System.out.println("Already selected. Insert money or dispense if paid.");
    }

    @Override
    public void insertCoin(Coin coin) {
        vm.totalPaymentCents += coin.cents();
        System.out.println("Inserted coin: " + coin + " | Total: $" + vm.totalPaymentCents / 100.0);
        moveToDispenseIfPaid();
    }

    @Override
    public void insertNote(Note note) {
        vm.totalPaymentCents += note.cents();
        System.out.println("Inserted note: " + note + " | Total: $" + vm.totalPaymentCents / 100.0);
        moveToDispenseIfPaid();
    }

    private void moveToDispenseIfPaid() {
        int price = vm.selectedProduct().priceCents();
        if (vm.totalPaymentCents >= price) {
            vm.state = vm.dispenseState;
            System.out.println("Payment sufficient ✅ Ready to dispense.");
        } else {
            System.out.println("Remaining: $" + (price - vm.totalPaymentCents) / 100.0);
        }
    }

    @Override
    public void dispenseProduct() {
        int price = vm.selectedProduct().priceCents();
        if (vm.totalPaymentCents >= price) {
            vm.state = vm.dispenseState;
            vm.dispenseProduct();
        } else {
            System.out.println("Insufficient payment. Add more money.");
        }
    }

    @Override
    public void returnChange() {
        System.out.println("Cannot return change before dispensing.");
    }
}

class DispenseState implements VendingMachineState {
    private final VendingMachine vm;

    public DispenseState(VendingMachine vm) {
        this.vm = vm;
    }

    @Override public void selectProduct(String productId) { System.out.println("Dispensing in progress..."); }
    @Override public void insertCoin(Coin coin) { System.out.println("Already paid. Dispensing in progress..."); }
    @Override public void insertNote(Note note) { System.out.println("Already paid. Dispensing in progress..."); }

    @Override
    public void dispenseProduct() {
        Product p = vm.selectedProduct();

        try {
            vm.inventory.decrementStockOrThrow(p.productId());
            System.out.println("✅ Dispensed: " + p.name());
        } catch (Exception e) {
            System.out.println("Dispense failed: " + e.getMessage());
        }

        vm.state = vm.returnChangeState;
    }

    @Override
    public void returnChange() {
        System.out.println("Collect product first.");
    }
}

class ReturnChangeState implements VendingMachineState {
    private final VendingMachine vm;

    public ReturnChangeState(VendingMachine vm) {
        this.vm = vm;
    }

    @Override public void selectProduct(String productId) { System.out.println("Collect change first."); }
    @Override public void insertCoin(Coin coin) { System.out.println("Collect change first."); }
    @Override public void insertNote(Note note) { System.out.println("Collect change first."); }
    @Override public void dispenseProduct() { System.out.println("Already dispensed. Collect change."); }

    @Override
    public void returnChange() {
        Product p = vm.selectedProduct();

        int change = vm.totalPaymentCents - p.priceCents();
        if (change > 0) {
            System.out.println("✅ Change returned: $" + (change / 100.0));
        } else {
            System.out.println("No change to return.");
        }

        // reset transaction
        vm.totalPaymentCents = 0;
        vm.selectedProductId = null;
        vm.state = vm.idleState;
    }
}

class VendingMachine {
    private static volatile VendingMachine instance;

    final Inventory inventory = new Inventory();

    final VendingMachineState idleState = new IdleState(this);
    final VendingMachineState readyState = new ReadyState(this);
    final VendingMachineState dispenseState = new DispenseState(this);
    final VendingMachineState returnChangeState = new ReturnChangeState(this);

    VendingMachineState state = idleState;

    String selectedProductId = null;
    int totalPaymentCents = 0;

    private VendingMachine() {}

    public static VendingMachine getInstance() {
        if (instance == null) {
            synchronized (VendingMachine.class) {
                if (instance == null) instance = new VendingMachine();
            }
        }
        return instance;
    }

    Product selectedProduct() {
        if (selectedProductId == null) throw new IllegalStateException("No product selected");
        return inventory.getItemOrThrow(selectedProductId).product();
    }

    // External APIs
    public void selectProduct(String productId) { state.selectProduct(productId); }
    public void insertCoin(Coin coin) { state.insertCoin(coin); }
    public void insertNote(Note note) { state.insertNote(note); }
    public void dispenseProduct() { state.dispenseProduct(); }
    public void returnChange() { state.returnChange(); }
}

public class VendingMachineLLD {
    public static void main(String[] args) {
        VendingMachine vm = VendingMachine.getInstance();

        Product coke = new Product("P1", "Coke", 150);
        Product pepsi = new Product("P2", "Pepsi", 150);
        Product water = new Product("P3", "Water", 100);

        vm.inventory.addProduct(coke, 2);
        vm.inventory.addProduct(pepsi, 1);
        vm.inventory.addProduct(water, 1);

        // Buy Coke
        vm.selectProduct("P1");
        vm.insertCoin(Coin.QUARTER);
        vm.insertCoin(Coin.QUARTER);
        vm.insertCoin(Coin.QUARTER);
        vm.insertCoin(Coin.QUARTER);
        vm.insertNote(Note.FIVE);
        vm.dispenseProduct();
        vm.returnChange();

        System.out.println("--------------------------------------------------");

        // Buy Pepsi with insufficient payment first
        vm.selectProduct("P2");
        vm.insertCoin(Coin.QUARTER);
        vm.dispenseProduct(); // blocked

        vm.insertNote(Note.ONE);
        vm.insertCoin(Coin.QUARTER);
        vm.dispenseProduct();
        vm.returnChange();
    }
}
