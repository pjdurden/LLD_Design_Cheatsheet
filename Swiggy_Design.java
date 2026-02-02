/*
======================== LLD DESIGN INTERVIEW SCRIPT (SWIGGY / FOOD DELIVERY SYSTEM) ========================

------------------------------------------------------------------------------------------
1) CLARIFYING REQUIREMENTS (what I ask first)
------------------------------------------------------------------------------------------
"Before I start designing, I’ll clarify scope and assumptions."

Questions I ask:
1. Core features required in MVP?
   - Register customers ✅
   - Register restaurants ✅
   - Browse restaurants ✅
   - View menu ✅
   - Place order ✅
   - Update order status ✅
   - Assign delivery agent ✅
   - Cancel order ✅
2. Payments:
   - Do we need payment methods (UPI/COD/Card)? ❌ (not implemented here)
   - Payment before confirmation OR after delivery? (out of scope)
3. Menu availability:
   - Should menu items have availability flag? ✅ (exists)
   - Should system prevent ordering unavailable items? ❌ (not implemented)
4. Order lifecycle:
   - Should restaurant explicitly accept/reject? (not modeled)
   - What statuses do we support? ✅ (PENDING → CONFIRMED → PREPARING → OUT_FOR_DELIVERY → DELIVERED / CANCELLED)
5. Delivery assignment:
   - Auto-assign first available agent? ✅ (current)
   - Should it pick nearest agent by location? ❌ (out of scope)
6. Concurrency:
   - Multiple orders at same time?
   - Agent assignment should be race-free? (currently weak, can improve)

Assumptions in this implementation:
- In-memory storage for users/restaurants/orders
- No location-based search
- Delivery agent assigned after order confirmed
- Basic notification hooks exist but not implemented
- No restaurant capacity limits and no delivery ETA

------------------------------------------------------------------------------------------
2) REQUIREMENTS
------------------------------------------------------------------------------------------
Functional Requirements:
- Customer can:
  - Browse available restaurants
  - View restaurant menu
  - Place order with list of items
  - Track order status
  - Cancel order
- Restaurant can:
  - Receive order notification
  - Update order status (simulated through service)
- Delivery agent can:
  - Get assigned to order
  - Update availability (busy/free)

Non-Functional Requirements:
- Thread-safe handling for orders and agent assignment (basic)
- Extensible to multiple payment methods
- Maintainability: separate entities and orchestration logic
- Scalability: avoid scanning all agents for assignment (can improve)

------------------------------------------------------------------------------------------
3) IDENTIFY ENTITIES (core classes)
------------------------------------------------------------------------------------------
Customer
Restaurant
MenuItem
OrderItem
Order
DeliveryAgent
FoodDeliveryService (system/service layer)
OrderStatus enum

------------------------------------------------------------------------------------------
4) RELATIONSHIPS (has-a / is-a)
------------------------------------------------------------------------------------------
Restaurant has List<MenuItem> menu
Order has:
- customer
- restaurant
- List<OrderItem>
- status
- assigned delivery agent

OrderItem has:
- MenuItem
- quantity

FoodDeliveryService has:
- customers map (customerId -> Customer)
- restaurants map (restaurantId -> Restaurant)
- orders map (orderId -> Order)
- deliveryAgents map (agentId -> DeliveryAgent)

------------------------------------------------------------------------------------------
5) DESIGN CHOICES / PATTERNS DISCUSSED
------------------------------------------------------------------------------------------
Singleton Pattern:
- FoodDeliveryService is singleton (central orchestrator)

Service Layer orchestration:
- placeOrder() creates and stores order, then notifies restaurant
- updateOrderStatus() updates order state, notifies customer
- assignDeliveryAgent() assigns first available agent

Thread safety:
- customers/orders/deliveryAgents use ConcurrentHashMap ✅
- restaurants uses HashMap ❌ (should be ConcurrentHashMap for consistency)
- agent availability updates are not atomic across threads ❌

Notification hooks:
- notifyCustomer/notifyRestaurant/notifyDeliveryAgent exist as placeholders

------------------------------------------------------------------------------------------
6) CORE APIs (entry points)
------------------------------------------------------------------------------------------
FoodDeliveryService:
- registerCustomer(customer)
- registerRestaurant(restaurant)
- registerDeliveryAgent(agent)

- getAvailableRestaurants()
- getRestaurantMenu(restaurantId)

- placeOrder(customerId, restaurantId, items) -> Order
- updateOrderStatus(orderId, status)
- cancelOrder(orderId)

Order:
- getStatus()
- setStatus(status)
- assignDeliveryAgent(agent)

------------------------------------------------------------------------------------------
7) EDGE CASES I DISCUSS (and gaps in this code)
------------------------------------------------------------------------------------------
✅ handled:
- Invalid customer/restaurant -> placeOrder returns null
- Assign delivery agent only after CONFIRMED status
- Cancel order sets CANCELLED

⚠️ gaps / improvements:
- MenuItem availability is not checked before ordering ❌
- Agent assignment race condition if multiple orders confirm simultaneously ❌
- Delivery agent never becomes available again after delivery ❌
- Cancel after CONFIRMED should free agent and revert availability ❌
- No order amount calculation / taxes / delivery fee ❌
- No order history per customer ❌
- No ETA / tracking milestones ❌

------------------------------------------------------------------------------------------
8) EXTENSIBILITY (future improvements)
------------------------------------------------------------------------------------------
- Add PaymentStrategy interface:
  - CardPayment, UPIPayment, CODPayment
- Add OrderPricing:
  - item total + packaging fee + delivery fee + taxes
- Add Restaurant acceptance/rejection flow
- Add inventory / item-level stock checks
- Add location-aware matching:
  - nearest restaurant and nearest agent
- Add delivery agent pool management:
  - availableAgents queue
  - agent assignment by distance + load
- Add order history and tracking timeline events
- Add cancellation rules and refund logic

------------------------------------------------------------------------------------------
9) WALKTHROUGH (validate the flow)
------------------------------------------------------------------------------------------
Customer browses restaurants -> selects restaurant -> views menu
Customer places order -> system stores order and notifies restaurant
Restaurant confirms -> system assigns delivery agent
Order moves through PREPARING -> OUT_FOR_DELIVERY -> DELIVERED
Customer can cancel before delivery (simplified here)

==========================================================================================
CODE STARTS BELOW
==========================================================================================
*/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Swiggy_Design {

    // Customer
    // Menu
    // Place Order
    // Restaurant - Menu( prices ) , Availability
    // Delivery agent , accept fullfill order
    // System - order tracking and status updates , support multiple payment options

    private static class Customer{
        private final String customerId;
        private String customerDetails;

        public Customer(String customerId, String customerDetails) {
            this.customerId = customerId;
            this.customerDetails = customerDetails;
        }

        public String getId() {
            return customerId;
        }

        public String getCustomerDetails() {
            return customerDetails;
        }
    }

    private static class DeliveryAgent{
        private final String deliveryAgentId;
        private String deliveriyAgentDetails;
        private boolean isAvailable;

        public DeliveryAgent(String deliveryAgentId, String customerDetails) {
            this.deliveryAgentId = deliveryAgentId;
            this.deliveriyAgentDetails = customerDetails;
            this.isAvailable = true;
        }

        public String getDeliveryAgentId() {
            return deliveryAgentId;
        }

        public String getDeliveriyAgentDetails() {
            return deliveriyAgentDetails;
        }

        public boolean isAvailable() {
            return isAvailable;
        }

        public void setAvailable(boolean isAvailable) {
            this.isAvailable = isAvailable;
        }
    }

    private static class MenuItem{
        private final String id;
        private final String name;
        private final String description;
        private final double price;
        private boolean available;

        public MenuItem(String id, String name, String description, double price) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.available = true;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }
    }

    private static class Restaurant{
        private final String restaurantId;
        private final String restaurantDetails;
        private final List<MenuItem> menu;

        public Restaurant(String restaurantId, String restaurantDetails, List<MenuItem> menuItems) {
            this.restaurantId = restaurantId;
            this.restaurantDetails = restaurantDetails;
            this.menu = menuItems;
        }

        public void addMenuItem(MenuItem menuItem) {
            menu.add(menuItem);
        }

        public String getId() {
            return restaurantId;
        }

        public String getRestaurantDetails() {
            return restaurantDetails;
        }

        public List<MenuItem> getMenu() {
            return menu;
        }
    }

    private enum OrderStatus{
        PENDING,
        CONFIRMED,
        PREPARING,
        OUT_FOR_DELIVERY,
        DELIVERED,
        CANCELLED
    }

    private static class OrderItem{
        private final MenuItem menuItem;
        private final int quantity;

        public OrderItem(MenuItem menuItem, int quantity) {
            this.menuItem = menuItem;
            this.quantity = quantity;
        }
    }

    private static class Order{
        private final String id;
        private final Customer customer;
        private final Restaurant restaurant;
        private final List<OrderItem> items;
        private OrderStatus status;
        private DeliveryAgent deliveryAgent;

        public Order(String id, Customer customer, Restaurant restaurant,
                List<OrderItem> items) {
            this.id = id;
            this.customer = customer;
            this.restaurant = restaurant;
            this.items = items;
            this.status = OrderStatus.PENDING;
            this.deliveryAgent = null;
        }

        public void addItem(OrderItem orderItem) {
            items.add(orderItem);
        }

        public void removeItem(OrderItem item) {
            items.remove(item);
        }

        public void setStatus(OrderStatus orderStatus) {
            this.status = orderStatus;
        }

        public void assignDeliveryAgent(DeliveryAgent agent) {
            this.deliveryAgent = agent;
        }

        public String getId() {
            return id;
        }

        public OrderStatus getStatus() {
            return status;
        }
    }

    private static class FoodDeliveryService{
        private static FoodDeliveryService foodDeliveryService = new FoodDeliveryService();
        private final ConcurrentHashMap< String , Customer > customers;
        private final ConcurrentHashMap< String , DeliveryAgent> deliveryAgents;
        private final Map<String , Restaurant> restaurants;
        private final ConcurrentHashMap< String , Order> orders;

        private FoodDeliveryService() {
            this.customers = new ConcurrentHashMap<>();
            this.deliveryAgents = new ConcurrentHashMap<>();
            restaurants = new HashMap<>();
            orders = new ConcurrentHashMap<>();
        }

        public static FoodDeliveryService getInstance(){
            return foodDeliveryService;
        }

        public void registerCustomer(Customer customer) {
            customers.put(customer.getId(), customer);
        }

        public void registerDeliveryAgent(DeliveryAgent deliveryAgent) {
            deliveryAgents.put(deliveryAgent.getDeliveryAgentId(),deliveryAgent);
        }

        public void registerRestaurant(Restaurant restaurant) {
            restaurants.put(restaurant.getId(), restaurant);
        }

        public List<Restaurant> getAvailableRestaurants() {
            return new ArrayList<>(restaurants.values());
        }

        public List<MenuItem> getRestaurantMenu(String restaurantId) {
            Restaurant restaurant = restaurants.get(restaurantId);
            if(restaurant!=null) {
                return restaurant.getMenu();
            }
            return new ArrayList<>();
        }

        public Order placeOrder(String customerId , String restaurantId, List<OrderItem> items) {
            Customer customer = customers.get(customerId);
            Restaurant restaurant = restaurants.get(restaurantId);

            if(customer!=null && restaurant!=null) {
                Order order = new Order(generateOrderId(), customer, restaurant, items);
                orders.put(order.getId(), order);
                notifyRestaurant(order);
                System.out.println("Order placed: " + order.getId());
                return order;
            }
            return null;
        }

        public void updateOrderStatus(String orderId, OrderStatus status) {
            Order order = orders.get(orderId);
            if(order!=null) {
                order.setStatus(status);
                notifyCustomer(order);
                if (status == OrderStatus.CONFIRMED) {
                    assignDeliveryAgent(order);
                }
            }
        }

        public void cancelOrder(String orderId){
            Order order = orders.get(orderId);
            if(order!=null) {
                order.setStatus(OrderStatus.CANCELLED);
            }
        }

        private void assignDeliveryAgent(Order order){
            for(DeliveryAgent deliveryAgent: deliveryAgents.values())
            {
                if(deliveryAgent.isAvailable())
                {
                    deliveryAgent.setAvailable(false);
                    order.assignDeliveryAgent(deliveryAgent);
                    notifyDeliveryAgent(order);
                    break;
                }
            }
        }

        private void notifyCustomer(Order order) {
            // Send notification to the customer about the order status update
        }

        private void notifyRestaurant(Order order) {
            // Send notification to the restaurant about the new order or order status update
        }

        private void notifyDeliveryAgent(Order order) {
            // Send notification to the delivery agent about the assigned order
        }

        private String generateOrderId() {
            return "ORD" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    public static void main(String[] args) {
        FoodDeliveryService deliveryService = FoodDeliveryService.getInstance();

        // Register customers
        Customer customer1 = new Customer("C001", "John Doe");
        Customer customer2 = new Customer("C002", "Jane Smith");
        deliveryService.registerCustomer(customer1);
        deliveryService.registerCustomer(customer2);

        // Register restaurants
        List<MenuItem> restaurant1Menu = new ArrayList<>();
        restaurant1Menu.add(new MenuItem("M001", "Burger", "Delicious burger", 9.99));
        restaurant1Menu.add(new MenuItem("M002", "Pizza", "Cheesy pizza", 12.99));
        Restaurant restaurant1 = new Restaurant("R001", "Restaurant 1", restaurant1Menu);
        deliveryService.registerRestaurant(restaurant1);

        List<MenuItem> restaurant2Menu = new ArrayList<>();
        restaurant2Menu.add(new MenuItem("M003", "Sushi", "Fresh sushi", 15.99));
        restaurant2Menu.add(new MenuItem("M004", "Ramen", "Delicious ramen", 10.99));
        Restaurant restaurant2 = new Restaurant("R002", "Restaurant 2", restaurant2Menu);
        deliveryService.registerRestaurant(restaurant2);

        // Register delivery agents
        DeliveryAgent agent1 = new DeliveryAgent("D001", "Agent 1");
        DeliveryAgent agent2 = new DeliveryAgent("D002", "Agent 2");
        deliveryService.registerDeliveryAgent(agent1);
        deliveryService.registerDeliveryAgent(agent2);

        // Place an order
        List<OrderItem> orderItems = new ArrayList<>();
        orderItems.add(new OrderItem(restaurant1Menu.get(0), 2));
        orderItems.add(new OrderItem(restaurant1Menu.get(1), 1));
        Order order = deliveryService.placeOrder(customer1.getId(), restaurant1.getId(), orderItems);

        // Update order status
        deliveryService.updateOrderStatus(order.getId(), OrderStatus.CONFIRMED);
        System.out.println("Order status updated: " + order.getStatus());

        // Cancel an order
        Order order2 = deliveryService.placeOrder(customer2.getId(), restaurant2.getId(),
                List.of(new OrderItem(restaurant2Menu.get(0), 1)));
        deliveryService.cancelOrder(order2.getId());
    }
}
