/*
======================== LLD DESIGN INTERVIEW SCRIPT (E-COMMERCE / AMAZON) ========================

------------------------------------------------------------------------------------------
1) CLARIFYING REQUIREMENTS (what I ask first)
------------------------------------------------------------------------------------------
"Before designing, I want to confirm scope and assumptions."

Questions I ask:
1. Features required in MVP?
   - Browse/search products ✅
   - Add/remove/update cart ✅
   - Place order ✅
   - Track order status ✅
   - Payment support ✅
2. Do we need user login/authentication flows? (out of scope here, assume user exists)
3. Inventory rules:
   - Should inventory reduce at "add to cart" OR only at "place order"? (we reduce at place order)
4. Payment behavior:
   - Do we need multiple payment methods? ✅ (CreditCard implemented, can extend)
   - If payment fails, should we revert inventory? ✅
5. Order items:
   - Allow partial fulfillment (buy what’s available)? (current code does partial in placeOrder)
   - Or strict: either everything available or fail entire order? (can be changed)
6. Shipping/address/delivery tracking required? (out of scope for now)
7. Concurrency:
   - Two users placing orders at same time for same product → avoid overselling (we used synchronized + product-level sync)

Assumptions in this design:
- In-memory storage only (Maps)
- Product quantity is the inventory available
- Basic search by substring matching on product name
- Order status lifecycle simplified

------------------------------------------------------------------------------------------
2) REQUIREMENTS
------------------------------------------------------------------------------------------
Functional Requirements:
- User can register
- Product catalog can be added/managed
- User can search/browse products
- User can add/update/remove products in cart
- User can place order using cart
- Inventory is checked before confirming order
- Payment is processed; on success order moves to PROCESSING
- User can view order history and status

Non-Functional Requirements:
- Thread-safe inventory updates (avoid overselling)
- Extensible design (new payment methods, categories, cancellations, returns)
- Maintainable separation of concerns (Cart vs Order vs Product vs Service)

------------------------------------------------------------------------------------------
3) IDENTIFY ENTITIES (core classes)
------------------------------------------------------------------------------------------
User
Product
OrderItem
ShoppingCart
Order
AmazonService (service layer / orchestrator)

Payment interface
  -> CreditCardPayment (one implementation)

Enum:
OrderStatus

------------------------------------------------------------------------------------------
4) RELATIONSHIPS (has-a / is-a)
------------------------------------------------------------------------------------------
User has List<Order>
ShoppingCart has Map<ProductId, OrderItem>
Order has List<OrderItem>
OrderItem has Product + quantity

AmazonService has:
- users (userId -> User)
- products (productId -> Product)
- orders (orderId -> Order)

Payment is-a CreditCardPayment (implements interface)

------------------------------------------------------------------------------------------
5) DESIGN CHOICES / PATTERNS DISCUSSED
------------------------------------------------------------------------------------------
Singleton Pattern:
- AmazonService is a singleton representing the platform service layer

Strategy Pattern (via interface):
- Payment is an interface -> supports multiple payment methods easily
  (UPI, COD, NetBanking, Wallet etc.)

Service Layer:
- AmazonService orchestrates order placement and storage

Thread safety:
- ConcurrentHashMap for shared data
- synchronized placeOrder() for atomic order placement flow
- Product methods are synchronized for consistent inventory updates

------------------------------------------------------------------------------------------
6) CORE APIs (method signatures / entry points)
------------------------------------------------------------------------------------------
AmazonService.registerUser(User)
AmazonService.addProduct(Product)
AmazonService.searchProducts(keyword)
AmazonService.placeOrder(User, ShoppingCart, Payment) -> Order
AmazonService.getOrder(orderId)

Cart actions:
ShoppingCart.addItem(Product, qty)
ShoppingCart.removeItem(productId)
ShoppingCart.updateItemQuantity(productId, qty)
ShoppingCart.getItems()
ShoppingCart.clear()

Order:
Order.getStatus()
Order.setStatus(...)

------------------------------------------------------------------------------------------
7) EDGE CASES I DISCUSS (and partial handling here)
------------------------------------------------------------------------------------------
- Product out of stock -> item skipped in order (current code)
  (Alternative: fail entire order if any item unavailable)
- Cart empty or all unavailable -> throw IllegalStateException ✅
- Payment failure -> cancel order + revert inventory ✅
- Concurrent purchases -> needs locking (we have basic sync)
- Search keyword case sensitivity bug -> should normalize keyword (small improvement)

------------------------------------------------------------------------------------------
8) EXTENSIBILITY (how we can evolve this)
------------------------------------------------------------------------------------------
Easy additions:
- Categories, filters, sorting, price range
- Discounting/coupons
- Order cancellation / refunds
- Shipment tracking + address management
- Separate inventory service (distributed)
- Payment gateway integration
- Event-driven updates: order placed -> inventory reserved -> payment -> confirmed

------------------------------------------------------------------------------------------
9) WALKTHROUGH (example flow)
------------------------------------------------------------------------------------------
User registers -> products added -> user searches -> adds to cart -> placeOrder()
placeOrder():
- validate inventory
- reduce quantity
- create Order + store
- process payment
   - success -> PROCESSING
   - failure -> CANCELLED + revert quantity
User sees order history and status

==========================================================================================
CODE STARTS BELOW (Renamed class to ECommerce_LLD)
==========================================================================================
*/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ECommerce_LLD {
    // browse products
    // Shopping cart
    // order -> orderStatus
    // product categories
    // inventory management
    // multiple payment methods

    private interface Payment{
        boolean processPayment();
    }

    private static class CreditCardPayment implements Payment{
        @Override
        public boolean processPayment() {
            System.out.println("Doing credit card payment.");
            return true;
        }
    }

    private static class Product{
        private final String id;
        private final String name;
        private final String description;
        private final double price;
        private int quantity;

        public Product(String id, String name, String description, double price, int quantity) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.quantity = quantity;
        }

        public synchronized void updateQuantity(int quantity) {
            this.quantity += quantity;
        }

        public synchronized boolean isAvailable(int quantity) {
            return this.quantity >= quantity;
        }

        public String getId() { return id; }

        public String getName() { return name; }

        public String getDescription() { return description; }

        public double getPrice() { return price; }

        public int getQuantity() { return quantity; }
    }

    private static class OrderItem{
        private Product product;
        private int quantity;

        public OrderItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }

        public Product getProduct() { return product; }

        public void setProduct(Product product) { this.product = product; }

        public int getQuantity() { return quantity; }

        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    private enum OrderStatus{
        PENDING,
        PROCESSING,
        SHIPPED,
        DELIVERED,
        CANCELLED
    }

    private static class ShoppingCart{
        private Map<String, OrderItem> orderItems;

        public ShoppingCart() {
            this.orderItems = new HashMap<>();
        }

        public void addItem(Product product, int quantity){
            String productId = product.getId();
            if (orderItems.containsKey(productId)) {
                OrderItem item = orderItems.get(productId);
                quantity += item.getQuantity();
            }
            orderItems.put(productId, new OrderItem(product, quantity));
        }

        public void removeItem(String productId) {
            orderItems.remove(productId);
        }

        public void updateItemQuantity(String productId, int quantity) {
            OrderItem item = orderItems.get(productId);
            if (item != null) {
                orderItems.put(productId, new OrderItem(item.getProduct(), quantity));
            }
        }

        public List<OrderItem> getItems() {
            return new ArrayList<>(orderItems.values());
        }

        public void clear() {
            orderItems.clear();
        }
    }

    private static class Order{
        private final List<OrderItem> items;
        private final String id;
        private final User user;
        private OrderStatus orderStatus;
        private final double orderAmount;

        public Order(String id, User user, List<OrderItem> items) {
            this.id = id;
            this.user = user;
            this.items = items;
            this.orderAmount = calculateTotalAmount();
            this.orderStatus = OrderStatus.PENDING;
        }

        private double calculateTotalAmount(){
            return items.stream()
                    .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                    .sum();
        }

        public void setStatus(OrderStatus status) {
            this.orderStatus = status;
        }

        public String getId() { return id; }

        public User getUser() { return user; }

        public List<OrderItem> getItems() { return items; }

        public double getTotalAmount() { return orderAmount; }

        public OrderStatus getStatus() { return orderStatus; }
    }

    private static class User{
        private final String id;
        private final String name;
        private final String email;
        private final String password;
        private final List<Order> orders;

        public User(String id, String name, String email, String password) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.password = password;
            this.orders = new ArrayList<>();
        }

        public void addOrder(Order order) {
            orders.add(order);
        }

        public String getId() { return id; }

        public String getName() { return name; }

        public String getEmail() { return email; }

        public String getPassword() { return password; }

        public List<Order> getOrders() { return orders; }
    }

    private static class AmazonService{
        private static AmazonService instance = new AmazonService();
        private final Map<String, User> users;
        private final Map<String, Product> products;
        private final Map<String, Order> orders;

        private AmazonService() {
            users = new ConcurrentHashMap<>();
            products = new ConcurrentHashMap<>();
            orders = new ConcurrentHashMap<>();
        }

        public static synchronized AmazonService getInstance(){
            return instance;
        }

        public void registerUser(User user) {
            users.put(user.getId(), user);
        }

        public User getUser(String userId) {
            return users.get(userId);
        }

        public void addProduct(Product product) {
            products.put(product.getId(), product);
        }

        public Product getProduct(String productId) {
            return products.get(productId);
        }

        public List<Product> searchProducts(String keyWord) {
            String normalized = keyWord.toLowerCase(); // small improvement
            return products.values().stream()
                    .filter(product -> product.getName().toLowerCase().contains(normalized))
                    .collect(Collectors.toList());
        }

        public synchronized Order placeOrder(User user , ShoppingCart cart , Payment payment) {
            List<OrderItem> orderItems = new ArrayList<>();

            // Inventory validation + quantity reduction
            for (OrderItem item : cart.getItems()) {
                Product product = item.getProduct();
                int quantity = item.getQuantity();
                if (product.isAvailable(quantity)) {
                    product.updateQuantity(-quantity);
                    orderItems.add(item);
                }
            }

            if(orderItems.isEmpty()) {
                throw new IllegalStateException("No available products in the cart.");
            }

            String orderId = generateOrderId();
            Order order = new Order(orderId, user, orderItems);
            orders.put(orderId, order);
            user.addOrder(order);
            cart.clear();

            // Payment flow
            if (payment.processPayment()) {
                order.setStatus(OrderStatus.PROCESSING);
            } else {
                order.setStatus(OrderStatus.CANCELLED);
                // Revert the product quantities
                for (OrderItem item : orderItems) {
                    Product product = item.getProduct();
                    int quantity = item.getQuantity();
                    product.updateQuantity(quantity);
                }
            }

            return order;
        }

        public Order getOrder(String orderId) {
            return orders.get(orderId);
        }

        private String generateOrderId() {
            return "ORDER" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    public static void main(String[] args) {
        AmazonService shoppingService = AmazonService.getInstance();

        // Register users
        User user1 = new User("U001", "John Doe", "john@example.com", "password123");
        User user2 = new User("U002", "Jane Smith", "jane@example.com", "password456");
        shoppingService.registerUser(user1);
        shoppingService.registerUser(user2);

        // Add products
        Product product1 = new Product("P001", "Smartphone", "High-end smartphone", 999.99, 10);
        Product product2 = new Product("P002", "Laptop", "Powerful gaming laptop", 1999.99, 5);
        shoppingService.addProduct(product1);
        shoppingService.addProduct(product2);

        // User 1 adds products to cart and places an order
        ShoppingCart cart1 = new ShoppingCart();
        cart1.addItem(product1, 2);
        cart1.addItem(product2, 1);

        Payment payment1 = new CreditCardPayment();
        Order order1 = shoppingService.placeOrder(user1, cart1, payment1);
        System.out.println("Order placed: " + order1.getId());

        // User 2 searches for products and adds to cart
        List<Product> searchResults = shoppingService.searchProducts("laptop");
        System.out.println("Search Results:");
        for (Product product : searchResults) {
            System.out.println(product.getName());
        }

        ShoppingCart cart2 = new ShoppingCart();
        cart2.addItem(searchResults.get(0), 1);

        Payment payment2 = new CreditCardPayment();
        Order order2 = shoppingService.placeOrder(user2, cart2, payment2);
        System.out.println("Order placed: " + order2.getId());

        // User 1 views order history
        List<Order> userOrders = user1.getOrders();
        System.out.println("User 1 Order History:");
        for (Order order : userOrders) {
            System.out.println("Order ID: " + order.getId());
            System.out.println("Total Amount: $" + order.getTotalAmount());
            System.out.println("Status: " + order.getStatus());
        }
    }
}
