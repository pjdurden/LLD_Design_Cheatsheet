/*
======================== LLD DESIGN INTERVIEW SCRIPT (VENDING MACHINE - STATE PATTERN) ========================

------------------------------------------------------------------------------------------
1) CLARIFYING REQUIREMENTS (what I ask first)
------------------------------------------------------------------------------------------
"Before implementing, I’ll confirm the vending machine requirements and edge cases."

Questions I ask:
1. Product selection flow:
   - User selects product first ✅
   - Then inserts money ✅
   - Then product dispenses ✅
   - Then change is returned ✅
2. Payment types supported:
   - Coins ✅ (PENNY/NICKEL/DIME/QUARTER)
   - Notes ✅ (ONE/FIVE/TEN/TWENTY)
3. Inventory:
   - Must track product quantity ✅
   - Should prevent selecting out-of-stock products ✅
4. Change handling:
   - If payment > price -> return change ✅
   - If exact payment -> no change ✅
   - If insufficient -> do not dispense ✅
5. Multi-product:
   - Multiple products supported ✅
6. System behavior:
   - One user at a time? ✅ (assumed)
   - Concurrency required? ❌ (not required here)

Assumptions in this implementation:
- Single vending machine instance (Singleton)
- State-based flow (Idle -> Ready -> Dispense -> ReturnChange -> Idle)
- Inventory just tracks quantities, no SKU IDs
- Change returned as a numeric amount (not actual coin distribution)
- No cancel transaction feature (can be added)

------------------------------------------------------------------------------------------
2) REQUIREMENTS
------------------------------------------------------------------------------------------
Functional Requirements:
- Add products to inventory
- Select a product
- Insert coins and notes
- Dispense product when sufficient payment is made
- Return remaining change (if any)
- Enforce flow restrictions based on machine state

Non-Functional Requirements:
- Maintainable design via State Pattern
- Extensible for new states (OutOfService / Maintenance)
- Extensible for new payment types (Card / UPI)
- Thread safety for inventory updates (basic via ConcurrentHashMap)

------------------------------------------------------------------------------------------
3) IDENTIFY ENTITIES (core classes)
------------------------------------------------------------------------------------------
Product
Inventory

VendingMachine (core orchestrator)

State interface:
VendingMachineState

State implementations:
IdleState
ReadyState
DispenseState
ReturnChangeState

Enums for payments:
Coin
Note

Driver/Main:
VendingMachineLLD

------------------------------------------------------------------------------------------
4) RELATIONSHIPS (has-a / is-a)
------------------------------------------------------------------------------------------
VendingMachine has:
- Inventory inventory
- selectedProduct
- totalPayment
- currentState

VendingMachine has multiple states:
- idleState
- readyState
- dispenseState
- returnChangeState

All state classes implement VendingMachineState and hold reference to VendingMachine

Inventory has Map<Product, Integer> productQuantities

------------------------------------------------------------------------------------------
5) DESIGN CHOICES / PATTERNS DISCUSSED
------------------------------------------------------------------------------------------
✅ State Pattern (main concept):
- Each machine behavior depends on current state
- Prevents huge if-else inside VendingMachine

✅ Singleton Pattern:
- One vending machine instance using getInstance()

✅ Encapsulation of rules:
- IdleState enforces "select product first"
- ReadyState accepts money and checks if enough amount is paid
- DispenseState dispenses product and updates inventory
- ReturnChangeState calculates and returns change, resets transaction

Thread safety notes:
- Inventory uses ConcurrentHashMap for product quantities ✅
- But Product used as key without equals/hashCode override ⚠️
  (works only if same Product object instance is used everywhere)

------------------------------------------------------------------------------------------
6) CORE APIs (entry points)
------------------------------------------------------------------------------------------
Inventory:
- addProduct(product, qty)
- updateQuantity(product, qty)
- isAvailable(product)
- getQuantity(product)

VendingMachine:
- selectProduct(product)
- insertCoin(coin)
- insertNote(note)
- dispenseProduct()
- returnChange()

State interface:
VendingMachineState:
- selectProduct()
- insertCoin()
- insertNote()
- dispenseProduct()
- returnChange()

------------------------------------------------------------------------------------------
7) EDGE CASES I DISCUSS (and what this handles)
------------------------------------------------------------------------------------------
✅ handled:
- Selecting unavailable product -> prints not available
- Inserting payment without selecting product -> blocked in IdleState
- Dispense without payment -> blocked in ReadyState
- Extra payment -> change returned in ReturnChangeState
- Reset to Idle after returning change ✅

⚠️ gaps / improvements:
- DispenseState sets state to ReadyState first (unnecessary) ❌
  (it immediately moves to ReturnChangeState anyway)
- No “cancel transaction + refund” feature ❌
- No coin-change distribution (like return 2 quarters, 1 dime etc.) ❌
- Inventory can go negative if updateQuantity called incorrectly (should validate)
- Product should implement equals/hashCode (or use productId string as key)

------------------------------------------------------------------------------------------
8) EXTENSIBILITY (future upgrades)
------------------------------------------------------------------------------------------
- Add CancelState / Refund feature:
  - user cancels and system returns full inserted amount
- Add PaymentStrategy:
  - CardPayment, UPI, Wallet
- Add MaintenanceState / OutOfServiceState
- Add display layer:
  - show products, prices, balance remaining
- Add exact-change mode:
  - prevent purchase if machine can’t return change
- Add Product ID and store inventory by productId

------------------------------------------------------------------------------------------
9) WALKTHROUGH (validate the flow)
------------------------------------------------------------------------------------------
User selects Coke ($1.5)
Machine transitions: Idle -> Ready
User inserts coins/notes until >= 1.5
Machine transitions: Ready -> Dispense
User dispenses product (inventory--)
Machine transitions: Dispense -> ReturnChange
Machine returns change and resets transaction
Machine transitions: ReturnChange -> Idle

==========================================================================================
CODE STARTS BELOW
==========================================================================================
*/

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class Product{
    private final String name;
    private final double price;

    public Product(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }
}

class Inventory{
    private final Map<Product, Integer> products;

    public Inventory() {
        products = new ConcurrentHashMap<>();
    }

    public void addProduct(Product product, int quantity) {
        products.put(product, quantity);
    }

    public void removeProduct(Product product) {
        products.remove(product);
    }

    public void updateQuantity(Product product, int quantity) {
        products.put(product, quantity);
    }

    public int getQuantity(Product product) {
        return products.getOrDefault(product, 0);
    }

    public boolean isAvailable(Product product) {
        return products.containsKey(product) && products.get(product) > 0;
    }
}

interface VendingMachineState {
    void selectProduct(Product product);
    void insertCoin(Coin coin);
    void insertNote(Note note);
    void dispenseProduct();
    void returnChange();
}

enum Coin{
    PENNY(0.01),
    NICKEL(0.05),
    DIME(0.1),
    QUARTER(0.25);

    private final double value;

    Coin(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }
}

enum Note{
    ONE(1),
    FIVE(5),
    TEN(10),
    TWENTY(20);

    private final int value;

    Note(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

class IdleState implements VendingMachineState{
    private final VendingMachine vendingMachine;

    public IdleState(VendingMachine vendingMachine) {
        this.vendingMachine = vendingMachine;
    }

    @Override
    public void selectProduct(Product product) {
        if (vendingMachine.inventory.isAvailable(product)) {
            vendingMachine.setSelectedProduct(product);
            vendingMachine.setState(vendingMachine.getReadyState());
            System.out.println("Product selected: " + product.getName());
        } else {
            System.out.println("Product not available: " + product.getName());
        }
    }

    @Override
    public void insertCoin(Coin coin) {
        System.out.println("Please select a product first.");
    }

    @Override
    public void insertNote(Note note) {
        System.out.println("Please select a product first.");
    }

    @Override
    public void dispenseProduct() {
        System.out.println("Please select a product and make payment.");
    }

    @Override
    public void returnChange() {
        System.out.println("No change to return.");
    }
}

class ReadyState implements VendingMachineState{
    private final VendingMachine vendingMachine;

    public ReadyState(VendingMachine vendingMachine) {
        this.vendingMachine = vendingMachine;
    }

    @Override
    public void selectProduct(Product product) {
        System.out.println("Product already selected. Please make payment.");
    }

    @Override
    public void insertCoin(Coin coin) {
        vendingMachine.addCoin(coin);
        System.out.println("Coin inserted: " + coin);
        checkPaymentStatus();
    }

    @Override
    public void insertNote(Note note) {
        vendingMachine.addNote(note);
        System.out.println("Note inserted: " + note);
        checkPaymentStatus();
    }

    private void checkPaymentStatus() {
        if (vendingMachine.getTotalPayment() >= vendingMachine.getSelectedProduct().getPrice()) {
            vendingMachine.setState(vendingMachine.getDispenseState());
        }
    }

    @Override
    public void dispenseProduct() {
        System.out.println("Please make payment first.");
    }

    @Override
    public void returnChange() {
        System.out.println("Please make payment first.");
    }
}

class DispenseState implements VendingMachineState{
    private final VendingMachine vendingMachine;

    public DispenseState(VendingMachine vendingMachine) {
        this.vendingMachine = vendingMachine;
    }

    @Override
    public void selectProduct(Product product) {
        System.out.println("Product already selected. Please collect the dispensed product.");
    }

    @Override
    public void insertCoin(Coin coin) {
        System.out.println("Payment already made. Please collect the dispensed product.");
    }

    @Override
    public void insertNote(Note note) {
        System.out.println("Payment already made. Please collect the dispensed product.");
    }

    @Override
    public void dispenseProduct() {
        // (Minor improvement: no need to set ReadyState here first)
        Product product = vendingMachine.getSelectedProduct();
        vendingMachine.inventory.updateQuantity(product, vendingMachine.inventory.getQuantity(product) - 1);
        System.out.println("Product dispensed: " + product.getName());
        vendingMachine.setState(vendingMachine.getReturnChangeState());
    }

    @Override
    public void returnChange() {
        System.out.println("Please collect the dispensed product first.");
    }
}

class ReturnChangeState implements VendingMachineState{
    private final VendingMachine vendingMachine;

    public ReturnChangeState(VendingMachine vendingMachine) {
        this.vendingMachine = vendingMachine;
    }

    @Override
    public void selectProduct(Product product) {
        System.out.println("Please collect the change first.");
    }

    @Override
    public void insertCoin(Coin coin) {
        System.out.println("Please collect the change first.");
    }

    @Override
    public void insertNote(Note note) {
        System.out.println("Please collect the change first.");
    }

    @Override
    public void dispenseProduct() {
        System.out.println("Product already dispensed. Please collect the change.");
    }

    @Override
    public void returnChange() {
        double change = vendingMachine.getTotalPayment() - vendingMachine.getSelectedProduct().getPrice();
        if (change > 0) {
            System.out.println("Change returned: $" + change);
            vendingMachine.resetPayment();
        } else {
            System.out.println("No change to return.");
        }
        vendingMachine.resetSelectedProduct();
        vendingMachine.setState(vendingMachine.getIdleState());
    }
}

class VendingMachine{
    public static VendingMachine vendingMachine;
    private Product selectedProduct;
    Inventory inventory;

    private final VendingMachineState idleState;
    private final VendingMachineState readyState;
    private final VendingMachineState dispenseState;
    private final VendingMachineState returnChangeState;

    private VendingMachineState currentState;
    private double totalPayment;

    private VendingMachine() {
        inventory = new Inventory();
        idleState = new IdleState(this);
        readyState = new ReadyState(this);
        dispenseState = new DispenseState(this);
        returnChangeState = new ReturnChangeState(this);
        currentState = idleState;
        selectedProduct = null;
        totalPayment = 0.0;
    }

    public static synchronized VendingMachine getInstance(){
        if(vendingMachine== null)
        {
            vendingMachine = new VendingMachine();
        }
        return vendingMachine;
    }

    public Product getSelectedProduct() {
        return selectedProduct;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public VendingMachineState getIdleState() {
        return idleState;
    }

    public VendingMachineState getReadyState() {
        return readyState;
    }

    public VendingMachineState getDispenseState() {
        return dispenseState;
    }

    public VendingMachineState getReturnChangeState() {
        return returnChangeState;
    }

    public VendingMachineState getCurrentState() {
        return currentState;
    }

    public double getTotalPayment() {
        return totalPayment;
    }

    public void setSelectedProduct(Product product)
    {
        selectedProduct = product;
    }

    public void setState(VendingMachineState state)
    {
        currentState = state;
    }

    public void addCoin(Coin coin)
    {
        totalPayment+=coin.getValue();
    }

    public void addNote(Note note)
    {
        totalPayment+=note.getValue();
    }

    public void resetPayment(){
        totalPayment = 0.0;
    }

    public void resetSelectedProduct(){
        selectedProduct = null;
    }

    public void selectProduct(Product product){
        currentState.selectProduct(product);
    }

    public void insertCoin(Coin coin)
    {
        currentState.insertCoin(coin);
    }

    public void insertNote(Note note){
        currentState.insertNote(note);
    }

    public void dispenseProduct(){
        currentState.dispenseProduct();
    }

    public void returnChange(){
        currentState.returnChange();
    }
}

public class VendingMachineLLD {
    // vending machine
    // state - Idle ready dispensing Return change
    // product , inventory
    // coin , note

    public static void main(String[] args) {
        VendingMachine vendingMachine = VendingMachine.getInstance();

        // Add products to the inventory
        Product coke = new Product("Coke", 1.5);
        Product pepsi = new Product("Pepsi", 1.5);
        Product water = new Product("Water", 1.0);

        vendingMachine.inventory.addProduct(coke, 5);
        vendingMachine.inventory.addProduct(pepsi, 3);
        vendingMachine.inventory.addProduct(water, 2);

        // Select a product
        vendingMachine.selectProduct(coke);

        // Insert coins
        vendingMachine.insertCoin(Coin.QUARTER);
        vendingMachine.insertCoin(Coin.QUARTER);
        vendingMachine.insertCoin(Coin.QUARTER);
        vendingMachine.insertCoin(Coin.QUARTER);

        // Insert a note
        vendingMachine.insertNote(Note.FIVE);

        // Dispense the product
        vendingMachine.dispenseProduct();

        // Return change
        vendingMachine.returnChange();

        // Select another product
        vendingMachine.selectProduct(pepsi);

        // Insert insufficient payment
        vendingMachine.insertCoin(Coin.QUARTER);

        // Try to dispense the product
        vendingMachine.dispenseProduct();

        // Insert more coins
        vendingMachine.insertCoin(Coin.QUARTER);
        vendingMachine.insertCoin(Coin.QUARTER);
        vendingMachine.insertCoin(Coin.QUARTER);
        vendingMachine.insertCoin(Coin.QUARTER);

        // Dispense the product
        vendingMachine.dispenseProduct();

        vendingMachine.insertCoin(Coin.QUARTER);

        vendingMachine.dispenseProduct();

        // Return change
        vendingMachine.returnChange();
    }
}
