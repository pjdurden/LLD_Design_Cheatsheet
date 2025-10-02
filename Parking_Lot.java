/*
🚗 Parking Lot Problem Statement

You are required to design and implement a Parking Lot system.

Functional Requirements

The parking lot should have multiple floors, each with a fixed number of parking slots.

Each slot can be of one type: Car, Bike, Truck (or extensible to more vehicle types).

A vehicle can enter the parking lot and should be assigned the nearest available slot based on some strategy (e.g., lowest floor, lowest slot number).

A vehicle can exit the parking lot, which frees up the slot.

The system should generate a ticket when a vehicle enters and assign it to a slot.

On exit, the system should compute the parking fee based on duration and vehicle type.

Support APIs like:

parkVehicle(vehicle) → returns Ticket

unparkVehicle(ticketId) → returns Fee and frees slot

getAvailableSlots(vehicleType) → returns available slot count per floor

displayStatus() → prints current occupancy

Constraints

Parking lot size is configurable at the start (e.g., N floors, M slots per floor).

Different vehicle types may have different slot requirements (Truck needs a bigger slot than a Bike).

Assume basic time tracking for fee calculation (don’t need actual clock API, can mock).

The system should be extensible – e.g., tomorrow if you add ElectricCar needing charging slots, it should be possible without rewriting everything.

Example
Initialize ParkingLot with 2 floors, 6 slots each.
Slot distribution: [Truck, Bike, Car, Car, Bike, Car] on each floor.

parkVehicle(Car KA-01-1234)
-> Ticket issued: Floor 1, Slot 3

parkVehicle(Bike KA-05-4321)
-> Ticket issued: Floor 1, Slot 2

unparkVehicle(ticketId=1)
-> Vehicle KA-01-1234 exited. Fee = Rs. 20


👉 Evaluation Criteria

OOP design (classes like ParkingLot, Floor, Slot, Vehicle, Ticket, FeeCalculator).

Clean code and extensibility (new vehicle types, new fee strategies).

Efficient slot allocation strategy.

Use of design patterns (Factory for vehicle, Strategy for fee, Singleton for parking lot if required).

Parking Lot -> n * Floors ( Singleton )
Floor -> n * parking spots
Parking spot -> only 1 type vehicle , Ticket(TimeStamp ) , FeeCalculator ( created at startup )
LFLN -> lowest floor lowest number (Strategy)
FeeType -> per hour , per day , vehicle type ( Static method , just gives a number back)
Vehicle -> VehicleType (Enum) , VehicleNumber ( Multiple objects made during runtime)

parkVehicle -> requires vehicle object -> finds Parking spot -> park vehicle(spot occupied) -> return Ticket
unparkVehicle -> requires ticket object -> finds Parking spot -> unpark vehicle(spot free) -> return Fee
displayAvailableSlots -> iterate through all floors -> iterate through all parking spots -> count free spots of that vehicle type
displayStatus -> iterate through all floors -> iterate through all parking spots -> print status of each spot

PARKING LOT DESIGN  

VehicleType -> Enum ( Car , Bike , Truck , ElectricCar )
    -> Car ( class )
    -> Bike ( class )
    -> Truck ( class )
    -> ElectricCar ( class )

-> Vehicle ( abstract class ) , VehicleNumber ( String ) , VehicleType ( Enum )

ParkingSpotType -> Enum ( Car , Bike , Truck , ElectricCar )
    -> CarSpot ( class )
    -> BikeSpot ( class )
    -> TruckSpot ( class )
    -> ElectricCarSpot ( class )

-> ParkingSpot ( abstract class ) , SpotNumber ( int ) , VehicleType ( Enum ) Ticket ( class ) , TicketId ( int ) , VehicleNumber ( String ) , isOccupied ( boolean ) , parkVehicle() , unparkVehicle()

-> Floor ( class ) , FloorNumber ( int ) , List<ParkingSpot> ( List of parking spots ) , parkVehicle() , unparkVehicle() , getAvailableSlots() , displayStatus()

-> ParkingLot ( class ) , List<Floor> ( List of floors ) , parkVehicle() , unparkVehicle() , displayAvailableSlots() , displayStatus()

-> FeeCalculator ( class ) , calculateFee() ( static method ) , FeeType ( Enum ) , VehicleType ( Enum ) , RateCard ( Map<VehicleType, Rate> )

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
