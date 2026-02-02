/* 
------------------------------------------------------------------------------------------
1) PROBLEM UNDERSTANDING (I say this first)
------------------------------------------------------------------------------------------
"Design a Parking Lot system where vehicles enter, park, get a ticket, and then exit by paying a fee."

------------------------------------------------------------------------------------------
2) CLARIFYING QUESTIONS (1-2 mins)
------------------------------------------------------------------------------------------
I will ask:
1. Vehicle types allowed? (CAR/BIKE/TRUCK)
2. Multiple floors? (Yes)
3. Spot types fixed to vehicle type? (Bike goes to BikeSpot etc.)
4. Allocation rule? (Nearest available / lowest floor lowest number)
5. Fee calculation rule? (Hourly per vehicle type)
6. Is payment integration required? (No, just return fee)
7. Persistence required? (No, in-memory fine for interview)

Assumptions I'm making:
- One vehicle occupies one spot
- One ticket maps to exactly one parked vehicle
- We generate tickets sequentially
- Duration based on system time (millis)
- No reservation / pre-booking

------------------------------------------------------------------------------------------
3) REQUIREMENTS
------------------------------------------------------------------------------------------
Functional Requirements:
- Park a vehicle -> assign suitable available spot -> create ticket
- Unpark using ticket -> calculate fee -> free the spot
- Display status / availability of parking lot

Non-Functional Requirements:
- Extensible: add new vehicle types, new pricing rules, new allocation strategies
- Maintainable: clean separation of concerns
- Correctness: consistent mapping ticket -> spot
- (Optional) Concurrency safe (not implemented fully here but discussable)

------------------------------------------------------------------------------------------
4) IDENTIFY ENTITIES (NOUNS)
------------------------------------------------------------------------------------------
Vehicle (Car/Bike/Truck)
ParkingSpot (CarSpot/BikeSpot/TruckSpot)
Floor
ParkingLot
Ticket
Strategies:
- SlotAllocationStrategy
- FeeStrategy

------------------------------------------------------------------------------------------
5) RELATIONSHIPS
------------------------------------------------------------------------------------------
ParkingLot has Floors
Floor has ParkingSpots
ParkingSpot has Vehicle (when occupied)
Ticket has Vehicle + (floorNumber, spotNumber) + time info

Vehicle is-a Car/Bike/Truck (Inheritance)
ParkingSpot is-a CarSpot/BikeSpot/TruckSpot (Inheritance)

------------------------------------------------------------------------------------------
6) DESIGN PATTERNS USED (only where needed)
------------------------------------------------------------------------------------------
Strategy Pattern:
- SlotAllocationStrategy => can swap allocation rules without changing ParkingLot
- FeeStrategy => can swap pricing models (hourly/daily/surge)

Singleton Pattern:
- ParkingLot as singleton (single parking lot instance)
(Interview note: Singleton can be replaced by DI in production)

------------------------------------------------------------------------------------------
7) CORE APIs (what I'll expose)
------------------------------------------------------------------------------------------
ParkingLot.init(...)
ParkingLot.parkVehicle(vehicle) -> Ticket
ParkingLot.unparkVehicle(ticketId) -> fee
ParkingLot.displayStatus()
ParkingLot.displayAvailableSlots()

Strategies:
SlotAllocationStrategy.findSlot(floors, vehicle) -> ParkingSpot
FeeStrategy.calculateFee(durationMillis, vehicleType) -> double

------------------------------------------------------------------------------------------
8) EDGE CASES (say them explicitly)
------------------------------------------------------------------------------------------
- No available spot => return null / print message
- Invalid ticket during exit => return 0 / error
- Double unpark => invalid ticket after removal
- Concurrency: two vehicles competing for same spot (needs synchronization/locking)
- Vehicle type mismatch with spot type => prevented by canPark()

------------------------------------------------------------------------------------------
9) EXTENSIBILITY DISCUSSION (what can change tomorrow)
------------------------------------------------------------------------------------------
- Add new allocation strategies: NearestToEntry, Random, Handicap-first, EV-first
- Add pricing rules: daily cap, weekend pricing, dynamic surge, subscription passes
- Add more metadata: entry gate, exit gate, ticket QR, payment status

------------------------------------------------------------------------------------------
10) FLOW WALKTHROUGH (final validation)
------------------------------------------------------------------------------------------
Example:
Vehicle enters -> parkVehicle() -> allocationStrategy finds spot -> spot.park(vehicle) -> Ticket created
Vehicle exits -> unparkVehicle(ticketId) -> spot.unpark() -> calculate fee -> remove active ticket

==========================================================================================
CODE STARTS BELOW
==========================================================================================
*/

import java.util.*;

// ----------------- ENUMS -----------------
enum VehicleType { CAR, BIKE, TRUCK }
enum SpotType { CAR, BIKE, TRUCK }

// ----------------- VEHICLE -----------------
abstract class Vehicle {
    String vehicleNumber;
    VehicleType type;

    public Vehicle(String vehicleNumber, VehicleType type) {
        this.vehicleNumber = vehicleNumber;
        this.type = type;
    }
}

class Car extends Vehicle {
    public Car(String number) { super(number, VehicleType.CAR); }
}
class Bike extends Vehicle {
    public Bike(String number) { super(number, VehicleType.BIKE); }
}
class Truck extends Vehicle {
    public Truck(String number) { super(number, VehicleType.TRUCK); }
}

// ----------------- PARKING SPOT -----------------
abstract class ParkingSpot {
    int spotNumber;
    Vehicle vehicle;

    public ParkingSpot(int spotNumber) {
        this.spotNumber = spotNumber;
    }

    abstract boolean canPark(Vehicle vehicle);

    boolean isOccupied() { return vehicle != null; }

    void park(Vehicle v) { this.vehicle = v; }

    void unpark() { this.vehicle = null; }
}

class CarSpot extends ParkingSpot {
    public CarSpot(int number) { super(number); }
    boolean canPark(Vehicle v) { return v.type == VehicleType.CAR; }
}

class BikeSpot extends ParkingSpot {
    public BikeSpot(int number) { super(number); }
    boolean canPark(Vehicle v) { return v.type == VehicleType.BIKE; }
}

class TruckSpot extends ParkingSpot {
    public TruckSpot(int number) { super(number); }
    boolean canPark(Vehicle v) { return v.type == VehicleType.TRUCK; }
}

// ----------------- TICKET -----------------
class Ticket {
    final int ticketId;
    final long startTime;
    long endTime;
    final Vehicle vehicle;
    final int floorNumber;
    final int spotNumber;

    public Ticket(int id, Vehicle v, int floor, int spot) {
        this.ticketId = id;
        this.vehicle = v;
        this.floorNumber = floor;
        this.spotNumber = spot;
        this.startTime = System.currentTimeMillis();
    }
}

// ----------------- FEE STRATEGY -----------------
interface FeeStrategy {
    double calculateFee(long durationMillis, VehicleType type);
}

class HourlyFeeStrategy implements FeeStrategy {
    private final Map<VehicleType, Integer> rates;

    public HourlyFeeStrategy(Map<VehicleType, Integer> rates) {
        this.rates = rates;
    }

    @Override
    public double calculateFee(long duration, VehicleType type) {
        long hours = Math.max(1, duration / (1000 * 60 * 60));
        return hours * rates.get(type);
    }
}

// ----------------- FLOOR -----------------
class Floor {
    int floorNumber;
    List<ParkingSpot> spots;

    public Floor(int number, Map<SpotType, Integer> distribution) {
        this.floorNumber = number;
        this.spots = new ArrayList<>();
        int spotCounter = 0;

        for (Map.Entry<SpotType, Integer> entry : distribution.entrySet()) {
            SpotType type = entry.getKey();
            int count = entry.getValue();
            for (int i = 0; i < count; i++) {
                switch (type) {
                    case CAR -> spots.add(new CarSpot(spotCounter++));
                    case BIKE -> spots.add(new BikeSpot(spotCounter++));
                    case TRUCK -> spots.add(new TruckSpot(spotCounter++));
                }
            }
        }
    }
}

// ----------------- SLOT ALLOCATION STRATEGY -----------------
interface SlotAllocationStrategy {
    ParkingSpot findSlot(List<Floor> floors, Vehicle vehicle);
}

class LFLNStrategy implements SlotAllocationStrategy {
    @Override
    public ParkingSpot findSlot(List<Floor> floors, Vehicle vehicle) {
        for (Floor floor : floors) {
            for (ParkingSpot spot : floor.spots) {
                if (!spot.isOccupied() && spot.canPark(vehicle)) {
                    return spot;
                }
            }
        }
        return null;
    }
}

// ----------------- PARKING LOT -----------------
class ParkingLot {
    private static ParkingLot instance;
    List<Floor> floors;
    SlotAllocationStrategy allocationStrategy;
    FeeStrategy feeStrategy;
    int ticketCounter = 0;
    Map<Integer, Ticket> activeTickets = new HashMap<>();

    private ParkingLot() {}

    public static ParkingLot getInstance() {
        if (instance == null) {
            instance = new ParkingLot();
        }
        return instance;
    }

    void init(int numFloors, Map<SpotType, Integer> distribution,
              SlotAllocationStrategy allocationStrategy,
              FeeStrategy feeStrategy) {
        this.floors = new ArrayList<>();
        this.allocationStrategy = allocationStrategy;
        this.feeStrategy = feeStrategy;
        for (int i = 0; i < numFloors; i++) {
            floors.add(new Floor(i, distribution));
        }
    }

    Ticket parkVehicle(Vehicle vehicle) {
        ParkingSpot spot = allocationStrategy.findSlot(floors, vehicle);
        if (spot == null) {
            System.out.println("No spot available for vehicle: " + vehicle.vehicleNumber);
            return null;
        }
        spot.park(vehicle);
        Ticket ticket = new Ticket(++ticketCounter, vehicle,
                                   getFloorOfSpot(spot), spot.spotNumber);
        activeTickets.put(ticket.ticketId, ticket);
        return ticket;
    }

    double unparkVehicle(int ticketId) {
        Ticket ticket = activeTickets.get(ticketId);
        if (ticket == null) {
            System.out.println("Invalid Ticket!");
            return 0;
        }
        Floor floor = floors.get(ticket.floorNumber);
        ParkingSpot spot = floor.spots.get(ticket.spotNumber);
        spot.unpark();
        ticket.endTime = System.currentTimeMillis();

        long duration = ticket.endTime - ticket.startTime;
        double fee = feeStrategy.calculateFee(duration, ticket.vehicle.type);

        activeTickets.remove(ticketId);
        return fee;
    }

    void displayAvailableSlots() {
        for (Floor floor : floors) {
            long free = floor.spots.stream().filter(s -> !s.isOccupied()).count();
            System.out.println("Floor " + floor.floorNumber + " has " + free + " free spots");
        }
    }

    void displayStatus() {
        for (Floor floor : floors) {
            System.out.println("Floor " + floor.floorNumber);
            for (ParkingSpot spot : floor.spots) {
                if (spot.isOccupied()) {
                    System.out.println("Spot " + spot.spotNumber + " -> " + spot.vehicle.vehicleNumber);
                } else {
                    System.out.println("Spot " + spot.spotNumber + " is free");
                }
            }
        }
    }

    private int getFloorOfSpot(ParkingSpot spot) {
        for (Floor floor : floors) {
            if (floor.spots.contains(spot)) {
                return floor.floorNumber;
            }
        }
        return -1;
    }
}

// ----------------- MAIN -----------------
public class Parking_Lot {
    public static void main(String[] args) throws InterruptedException {
        // Define rate card
        Map<VehicleType, Integer> rates = Map.of(
                VehicleType.CAR, 20,
                VehicleType.BIKE, 10,
                VehicleType.TRUCK, 30
        );

        // Define distribution: 2 Car, 2 Bike, 2 Truck per floor
        Map<SpotType, Integer> distribution = new HashMap<>();
        distribution.put(SpotType.CAR, 2);
        distribution.put(SpotType.BIKE, 2);
        distribution.put(SpotType.TRUCK, 2);

        ParkingLot lot = ParkingLot.getInstance();
        lot.init(2, distribution, new LFLNStrategy(), new HourlyFeeStrategy(rates));

        Vehicle v1 = new Car("KA-01-1234");
        Ticket t1 = lot.parkVehicle(v1);
        System.out.println("Ticket " + t1.ticketId + " issued at floor " + t1.floorNumber);

        Vehicle v2 = new Bike("KA-05-4321");
        Ticket t2 = lot.parkVehicle(v2);
        System.out.println("Ticket " + t2.ticketId + " issued at floor " + t2.floorNumber);

        lot.displayStatus();

        Thread.sleep(2000); // mock some time

        double fee = lot.unparkVehicle(t1.ticketId);
        System.out.println("Vehicle " + v1.vehicleNumber + " exited. Fee = Rs." + fee);

        lot.displayAvailableSlots();
        lot.displayStatus();
    }
}
