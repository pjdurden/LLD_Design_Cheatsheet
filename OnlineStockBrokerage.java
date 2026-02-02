/*
======================== LLD DESIGN INTERVIEW SCRIPT (ONLINE STOCK BROKERAGE) ========================

------------------------------------------------------------------------------------------
1) CLARIFYING REQUIREMENTS (what I ask first)
------------------------------------------------------------------------------------------
"Before I jump into design, I want to clarify requirements so I don’t overbuild."

Questions I ask:
1. Order types needed? (Market order vs Limit order?)
2. Stocks universe? (Predefined stock list or dynamic add/remove?)
3. Do we need a real matching engine (bid/ask order book) OR instant execution at price?
4. Is partial fill allowed? (example: buy 10 shares but only 6 available)
5. Do we need settlement delay (T+1 / T+2) or instant settlement?
6. Is portfolio updated immediately after execution?
7. Do we track transaction history? (not implemented fully here but mention)
8. Multi-threading requirement? (multiple orders coming at same time)
9. Do we need authentication / roles? (out of scope for LLD here)

Assumptions I make in this implementation:
- We support Buy and Sell orders
- Orders execute immediately at given price (no real exchange matching engine)
- No partial fills (either executed or rejected)
- Settlement is instant (balance/portfolio updated immediately)
- Everything is in-memory (no DB)
- Concurrency safety is basic (ConcurrentHashMap + synchronized on Account/Portfolio)

------------------------------------------------------------------------------------------
2) REQUIREMENTS
------------------------------------------------------------------------------------------
Functional Requirements:
- User can create brokerage account with balance
- Broker maintains stock list and their latest price
- User can place Buy / Sell orders
- Validate balance before buy
- Validate holdings before sell
- Execute order and update Account + Portfolio
- Maintain order status (PENDING, EXECUTED, REJECTED)

Non-Functional Requirements:
- Thread-safe order placement and account updates
- Extensible: new order types (LimitOrder), new execution logic, trade history, settlement engine
- Maintainable separation of concerns: Account vs Portfolio vs Orders vs Broker

------------------------------------------------------------------------------------------
3) IDENTIFY ENTITIES (core classes)
------------------------------------------------------------------------------------------
User
Account
Portfolio
Stock
Order (abstract)
  -> BuyOrder
  -> SellOrder
StockBroker (central service / orchestrator)
Exceptions:
- InsufficientFundsException
- InsufficientStockException

------------------------------------------------------------------------------------------
4) RELATIONSHIPS (has-a / is-a)
------------------------------------------------------------------------------------------
User has Account
Account has Portfolio
Portfolio maintains holdings (symbol -> quantity)

StockBroker has:
- accounts map (accountId -> Account)
- stocks map (symbol -> Stock)
- orderQueue (to process orders sequentially)

Order has:
- account, stock, quantity, price, status

Order is-a BuyOrder/SellOrder

------------------------------------------------------------------------------------------
5) DESIGN PATTERNS (what I intentionally used)
------------------------------------------------------------------------------------------
Singleton Pattern:
- StockBroker is a singleton (one brokerage instance)

Command Pattern (implicit):
- Order is a command object encapsulating an action (execute())

Queue-based processing:
- Orders are placed into orderQueue and processed in FIFO order
(Note: in real world, async workers handle this)

------------------------------------------------------------------------------------------
6) APIs (what my system exposes)
------------------------------------------------------------------------------------------
StockBroker.createAccount(user, initialBalance)
StockBroker.getAccount(accountId)

StockBroker.addStock(stock)
StockBroker.getStock(symbol)

StockBroker.placeOrder(order)
StockBroker.processOrders()

Order.execute()  // polymorphic execution

Account.deposit(amount)
Account.withdraw(amount)
Portfolio.addStock(stock, qty)
Portfolio.removeStock(stock, qty)

------------------------------------------------------------------------------------------
7) EDGE CASES I DISCUSS (and handle)
------------------------------------------------------------------------------------------
- Buy with insufficient balance -> reject + exception
- Sell with insufficient holdings -> reject + exception
- Stock not found (not handled fully, can be added)
- Negative quantities/prices (should validate)
- Concurrency:
  - Account.withdraw/deposit are synchronized
  - Portfolio add/remove are synchronized
  - Maps are ConcurrentHashMap for thread-safe access
  - Queue is ConcurrentLinkedQueue for safe order enqueuing
(Real-world improvement: locks per account + async processing thread pool)

------------------------------------------------------------------------------------------
8) EXTENSIBILITY (how this design scales for new asks)
------------------------------------------------------------------------------------------
Possible improvements tomorrow:
- Add LimitOrder / MarketOrder
- Add OrderBook + MatchingEngine (bid/ask queues)
- Add Transaction/TradeHistory storage
- Add OrderStatus lifecycle: NEW -> VALIDATED -> EXECUTED -> SETTLED
- Add fee calculation: brokerage fee, GST, exchange fee
- Add cancel order support
- Introduce settlement system (T+1) and reserved funds/holdings

------------------------------------------------------------------------------------------
9) WALKTHROUGH (example flow to validate)
------------------------------------------------------------------------------------------
User creates account with $10,000
Broker adds stock symbols AAPL, GOOGL
User places BuyOrder(AAPL 10 @ 150)
 -> validate funds -> withdraw money -> update portfolio -> EXECUTED
User places SellOrder(AAPL 5 @ 160)
 -> validate holdings -> deposit money -> update portfolio -> EXECUTED

==========================================================================================
CODE STARTS BELOW
==========================================================================================
*/

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class OnlineStockBrokerage{
    // users - buy and sell stock , view portfolio and tran history
    // real time stock quotes , and market data
    // order placement, execution and settlement process
    // checking account balance and stock availability before buying

    // Stock
    // User -> buy stock, sell stock , Portfolio , tran history -> this just adds
    // Order - buy sell
    //

    private static class User{
        private final String userId;
        private final String name;
        private final String email;

        public User(String userId, String name, String email) {
            this.userId = userId;
            this.name = name;
            this.email = email;
        }
    }

    private static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String message) {
            super(message);
        }
    }

    private static class Portfolio{
        private final Account account;
        private Map<String, Integer> holdings;

        public Portfolio(Account account) {
            this.account = account;
            this.holdings = new ConcurrentHashMap<>();
        }

        public synchronized void addStock(Stock stock, int quantity) {
            holdings.put(stock.getSymbol(), holdings.getOrDefault(stock.getSymbol(), 0) + quantity);
        }

        public synchronized void removeStock(Stock stock, int quantity) {
            String symbol = stock.getSymbol();
            if (holdings.containsKey(symbol)) {
                int currentQuantity = holdings.get(symbol);
                if (currentQuantity > quantity) {
                    holdings.put(symbol, currentQuantity - quantity);
                } else if (currentQuantity == quantity) {
                    holdings.remove(symbol);
                } else {
                    throw new InsufficientStockException("Insufficient stock quantity in the portfolio.");
                }
            } else {
                throw new InsufficientStockException("Stock not found in the portfolio.");
            }
        }

        public Map<String, Integer> getHoldings() {
            return holdings;
        }
    }

    private static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String message) {
            super(message);
        }
    }

    private static class Account{
        private final String accountId;
        private final User user;
        private double balance;
        private final Portfolio portfolio;

        public Account(String accountId, User user, double initialBalance) {
            this.accountId = accountId;
            this.user = user;
            this.balance = initialBalance;
            this.portfolio = new Portfolio(this);
        }

        public synchronized void deposit(double amount) {
            balance += amount;
        }

        public synchronized void withdraw(double amount) {
            if (balance >= amount) {
                balance -= amount;
            } else {
                throw new InsufficientFundsException("Insufficient funds in the account.");
            }
        }

        public String getAccountId() {
            return accountId;
        }

        public User getUser() {
            return user;
        }

        public double getBalance() {
            return balance;
        }

        public Portfolio getPortfolio() {
            return portfolio;
        }

        public int getStockQuantity(String symbol)
        {
            if(portfolio.getHoldings()==null)
                return 0;
            return portfolio.getHoldings().get(symbol);
        }
    }

    private static class Stock {
        private final String symbol;
        private final String name;
        private double price;

        public Stock(String symbol, String name, double price) {
            this.symbol = symbol;
            this.name = name;
            this.price = price;
        }

        public synchronized void updatePrice(double newPrice) {
            price = newPrice;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getName() {
            return name;
        }

        public double getPrice() {
            return price;
        }
    }

    public enum OrderStatus{
        PENDING,
        EXECUTED,
        REJECTED
    }

    private static abstract class Order{
        protected final String orderId;
        protected final Account account;
        protected final Stock stock;
        protected final int quantity;
        protected final double price;
        protected OrderStatus status;

        public Order(String orderId, Account account, Stock stock,
                int quantity, double price) {
            this.orderId = orderId;
            this.account = account;
            this.stock = stock;
            this.quantity = quantity;
            this.price = price;
            this.status = OrderStatus.PENDING;
        }
        public abstract void execute();
    }

    private static class BuyOrder extends Order{

        public BuyOrder(String orderId, Account account, Stock stock, int quantity, double price) {
            super(orderId, account, stock, quantity, price);
        }

        @Override
        public void execute() {
            double totalCost = quantity * price;
            if (account.getBalance() >= totalCost) {
                account.withdraw(totalCost);
                // Update portfolio and perform necessary actions
                status = OrderStatus.EXECUTED;
            } else {
                status = OrderStatus.REJECTED;
                throw new InsufficientFundsException("Insufficient funds to execute the buy order.");
            }
        }
    }

    private static class SellOrder extends Order{
        public SellOrder(String orderId, Account account, Stock stock, int quantity, double price) {
            super(orderId, account, stock, quantity, price);
        }

        @Override
        public void execute() {
            double totalAmount = quantity * price;
            if (account.getStockQuantity(stock.getSymbol()) >= quantity) {
                account.deposit(totalAmount);
                // Update portfolio and perform necessary actions
                status = OrderStatus.EXECUTED;
            } else {
                status = OrderStatus.REJECTED;
                throw new InsufficientStockException("Insufficient Stocks to execute the sell order.");
            }
        }
    }

    // buy , sell
    private static class StockBroker{
        private static StockBroker instance = new StockBroker();
        private final Map<String, Account> accounts;
        private final Map<String, Stock> stocks;
        private final Queue<Order> orderQueue;
        private final AtomicInteger accountIdCounter;

        private StockBroker() {
            accounts = new ConcurrentHashMap<>();
            stocks = new ConcurrentHashMap<>();
            orderQueue = new ConcurrentLinkedQueue<>();
            accountIdCounter = new AtomicInteger(0);
        }

        public static StockBroker getInstance(){
            return instance;
        }

        public void createAccount(User user, double initialBalance) {
            String accountId = generateAccountId();
            Account account = new Account(accountId, user, initialBalance);
            accounts.put(accountId, account);
        }

        public Account getAccount(String accountId) {
            return accounts.get(accountId);
        }

        public void addStock(Stock stock)
        {
            stocks.put(stock.getSymbol(), stock);
        }

        public Stock getStock(String symbol)
        {
            return stocks.get(symbol);
        }

        public void placeOrder(Order order)
        {
            orderQueue.add(order);
            processOrders();
        }

        public void processOrders(){
            while(!orderQueue.isEmpty())
            {
                Order currentOrder = orderQueue.poll();
                try {
                    currentOrder.execute();
                } catch (InsufficientFundsException | InsufficientStockException e) {
                    // Handle exception and notify user
                    System.out.println("Order failed: " + e.getMessage());
                }
            }
        }

        private String generateAccountId() {
            int accountId = accountIdCounter.getAndIncrement();
            return "A" + String.format("%03d", accountId);
        }
    }

    public static void main(String[] args) {
        StockBroker stockBroker = StockBroker.getInstance();

        // Create user and account
        User user = new User("U001", "John Doe", "john@example.com");
        stockBroker.createAccount(user, 10000.0);
        Account account = stockBroker.getAccount("A000");

        // Add stocks to the stock broker
        Stock stock1 = new Stock("AAPL", "Apple Inc.", 150.0);
        Stock stock2 = new Stock("GOOGL", "Alphabet Inc.", 2000.0);
        stockBroker.addStock(stock1);
        stockBroker.addStock(stock2);

        // Place buy orders
        Order buyOrder1 = new BuyOrder("O001", account, stock1, 10, 150.0);
        Order buyOrder2 = new BuyOrder("O002", account, stock2, 5, 2000.0);
        stockBroker.placeOrder(buyOrder1);
        stockBroker.placeOrder(buyOrder2);

        // Place sell orders
        Order sellOrder1 = new SellOrder("O003", account, stock1, 5, 160.0);
        stockBroker.placeOrder(sellOrder1);

        // Print account balance and portfolio
        System.out.println("Account Balance: $" + account.getBalance());
        System.out.println("Portfolio: " + account.getPortfolio().getHoldings());
    }
}
