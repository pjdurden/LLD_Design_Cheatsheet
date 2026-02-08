/*
======================== LLD DESIGN INTERVIEW SCRIPT (RIDE SHARING SYSTEM) ========================

------------------------------------------------------------------------------------------
1) CLARIFYING REQUIREMENTS (what I ask first)
------------------------------------------------------------------------------------------
"Before I start designing, I’ll clarify scope and rules to avoid wrong assumptions."

Questions I ask:
1. Core features required?
   - User onboarding ✅ (users exist in this code)
   - Add vehicle ✅
   - Offer ride ✅
   - Search rides ✅
   - Book seats ✅
2. Ride search:
   - Only direct rides OR transit rides also? ✅ (both exist here)
   - Sorting preference needed? ✅ (earliest ending / lowest duration)
3. Booking rules:
   - Can passenger book multiple rides? ✅
   - Can passenger cancel booking? ❌ (not implemented)
   - Can driver cancel ride? ❌
4. Seats & capacity:
   - Should booking reduce available seats immediately? ✅
   - Prevent overbooking under concurrency? (not handled fully)
5. Pricing & payments required? ❌ (out of scope)
6. Matching / location:
   - Exact match origin/destination strings? ✅ (simple matching)
   - Or geo-based near-by pickup/drop? ❌ (out of scope)

Assumptions in this implementation:
- In-memory system (lists)
- Origins/destinations are exact string match
- Ride booking is seat-based only (no payment confirmation)
- No ride status states (REQUESTED/CONFIRMED/COMPLETED)
- No concurrency handling for booking

------------------------------------------------------------------------------------------
2) REQUIREMENTS
------------------------------------------------------------------------------------------
Functional Requirements:
- User can register (pre-created here)
- User can add vehicles
- Driver can offer rides with seats + time + duration
- Passenger can search rides from origin to destination
- Passenger can book seats if available
- System supports searching by preference:
  - earliest_ending_ride (implemented as startTime sort)
  - lowest_duration_ride (duration sort)
- Support transit rides (multi-hop) using DFS

Non-Functional Requirements:
- Extensible (new preferences, cancellation, ride statuses, payments)
- Maintainable separation of concerns:
  - VehicleService manages vehicles
  - RideService manages rides and search
- Correctness for seat availability (needs locking in real world)

------------------------------------------------------------------------------------------
3) IDENTIFY ENTITIES (core classes)
------------------------------------------------------------------------------------------
User
Vehicle
Ride
RideRequest
RideService
VehicleService

(Helper concepts)
Preference string (could become Enum)
Transit search implemented using DFS

------------------------------------------------------------------------------------------
4) RELATIONSHIPS (has-a / is-a)
------------------------------------------------------------------------------------------
User has:
- List<Vehicle> vehicles
- List<Ride> offeredRides
- List<RideRequest> takenRides

Vehicle has:
- owner(User)
- name + registrationNumber

Ride has:
- driver(User)
- vehicle(Vehicle)
- origin/destination
- startTime + duration
- availableSeats

RideRequest has:
- passenger(User)
- origin/destination
- seatsRequired + preference

RideService has:
- List<Ride> rides (in-memory store)

VehicleService has:
- List<Vehicle> vehicles

------------------------------------------------------------------------------------------
5) DESIGN CHOICES / PATTERNS DISCUSSED
------------------------------------------------------------------------------------------
Service Layer design:
- RideService encapsulates offerRide + search logic
- VehicleService encapsulates vehicle creation and association to user

Preference-based sorting:
- Strategy Pattern can be introduced later:
  - RideSelectionStrategy (earliest ending / shortest duration / cheapest / best rating)
Current implementation uses simple if-else checks.

Transit rides:
- DFS approach finds all possible paths from origin -> destination using existing rides
(Works for small data; may explode for large graphs)

------------------------------------------------------------------------------------------
6) CORE APIs (entry points)
------------------------------------------------------------------------------------------
VehicleService.addVehicle(owner, name, registrationNumber) -> Vehicle

RideService.offerRide(driver, vehicle, origin, destination, startTime, duration, availableSeats) -> Ride
RideService.searchRides(origin, destination, preference) -> List<Ride>
RideService.findTransitRides(origin, destination) -> List<List<Ride>>

Ride.bookSeats(seatsRequired) -> boolean

User.offerRide(ride)
User.takeRide(rideRequest)

------------------------------------------------------------------------------------------
7) EDGE CASES I DISCUSS (and what’s missing currently)
------------------------------------------------------------------------------------------
- Booking more seats than available -> returns false ✅
- Multiple users booking simultaneously -> race condition ❌
  (availableSeats updates should be synchronized/atomic)
- Search when no matching ride exists -> returns empty list ✅
- Preference string invalid -> no sorting applied ✅
- Transit ride cycles -> prevented using visited set ✅
- Cancel booking / refund seats -> not implemented ❌
- Ride states (ACTIVE/CANCELLED/COMPLETED) -> not implemented ❌

------------------------------------------------------------------------------------------
8) EXTENSIBILITY (future improvements)
------------------------------------------------------------------------------------------
- Add RideStatus enum: ACTIVE, FULL, COMPLETED, CANCELLED
- Add booking entity with status:
  - REQUESTED -> CONFIRMED -> CANCELLED -> COMPLETED
- Add concurrency safety for booking:
  - synchronized bookSeats() or AtomicInteger for seats
- Introduce Strategy pattern for ride selection preferences
- Replace string origin/destination with location coordinates (geo search)
- Add rating system and driver matching logic
- Add pricing + payments + wallet

------------------------------------------------------------------------------------------
9) WALKTHROUGH (validate the flow)
------------------------------------------------------------------------------------------
Driver offers ride Bangalore -> Mysore with 3 seats
Passenger searches rides Bangalore -> Mysore sorted by preference
Passenger selects ride and books 2 seats
System reduces available seats and stores taken ride request under passenger

==========================================================================================
CODE STARTS BELOW
==========================================================================================
*/

import java.util.*;

public class ride_sharing {
    private static class User {
        private int id;
        private String name;
        private String gender;
        private int age;
        private List<Vehicle> vehicles;
        private List<Ride> offeredRides;
        private List<RideRequest> takenRides;

        public User(int id, String name, String gender, int age) {
            this.id = id;
            this.name = name;
            this.gender = gender;
            this.age = age;
            this.vehicles = new ArrayList<>();
            this.offeredRides = new ArrayList<>();
            this.takenRides = new ArrayList<>();
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getGender() { return gender; }
        public int getAge() { return age; }
        public List<Vehicle> getVehicles() { return vehicles; }
        public List<Ride> getOfferedRides() { return offeredRides; }
        public List<RideRequest> getTakenRides() { return takenRides; }

        public void addVehicle(Vehicle vehicle) {
            this.vehicles.add(vehicle);
        }

        public void offerRide(Ride ride) {
            this.offeredRides.add(ride);
        }

        public void takeRide(RideRequest rideRequest) {
            this.takenRides.add(rideRequest);
        }
    }

    private static class Vehicle {
        private int id;
        private User owner;
        private String name;
        private String registrationNumber;

        public Vehicle(int id, User owner, String name, String registrationNumber) {
            this.id = id;
            this.owner = owner;
            this.name = name;
            this.registrationNumber = registrationNumber;
        }

        public int getId() { return id; }
        public User getOwner() { return owner; }
        public String getName() { return name; }
        public String getRegistrationNumber() { return registrationNumber; }
    }

    private static class RideRequest {
        private int id;
        private User passenger;
        private String origin;
        private String destination;
        private int seatsRequired;
        private String preference;

        public RideRequest(int id, User passenger, String origin, String destination, int seatsRequired, String preference) {
            this.id = id;
            this.passenger = passenger;
            this.origin = origin;
            this.destination = destination;
            this.seatsRequired = seatsRequired;
            this.preference = preference;
        }

        public int getId() { return id; }
        public User getPassenger() { return passenger; }
        public String getOrigin() { return origin; }
        public String getDestination() { return destination; }
        public int getSeatsRequired() { return seatsRequired; }
        public String getPreference() { return preference; }
    }

    private static class Ride {
        private int id;
        private User driver;
        private Vehicle vehicle;
        private String origin;
        private String destination;
        private Date startTime;
        private long duration; // Duration in milliseconds
        private int availableSeats;

        public Ride(int id, User driver, Vehicle vehicle, String origin, String destination, Date startTime, long duration, int availableSeats) {
            this.id = id;
            this.driver = driver;
            this.vehicle = vehicle;
            this.origin = origin;
            this.destination = destination;
            this.startTime = startTime;
            this.duration = duration;
            this.availableSeats = availableSeats;
        }

        public int getId() { return id; }
        public User getDriver() { return driver; }
        public Vehicle getVehicle() { return vehicle; }
        public String getOrigin() { return origin; }
        public String getDestination() { return destination; }
        public Date getStartTime() { return startTime; }
        public long getDuration() { return duration; }
        public int getAvailableSeats() { return availableSeats; }

        public boolean bookSeats(int seatsRequired) {
            if (seatsRequired <= availableSeats) {
                availableSeats -= seatsRequired;
                return true;
            }
            return false;
        }
    }

    private static class RideService {
        private List<Ride> rides;
        private int rideCounter;

        public RideService() {
            this.rides = new ArrayList<>();
            this.rideCounter = 0;
        }

        public Ride offerRide(User driver, Vehicle vehicle, String origin, String destination, Date startTime, long duration, int availableSeats) {
            rideCounter++;
            Ride ride = new Ride(rideCounter, driver, vehicle, origin, destination, startTime, duration, availableSeats);
            driver.offerRide(ride);
            rides.add(ride);
            return ride;
        }

        public List<Ride> searchRides(String origin, String destination, String preference) {
            List<Ride> matchingRides = new ArrayList<>();
            for (Ride ride : rides) {
                if (ride.getOrigin().equals(origin) && ride.getDestination().equals(destination)) {
                    matchingRides.add(ride);
                }
            }

            if (preference.equals("earliest_ending_ride")) {
                matchingRides.sort(Comparator.comparing(Ride::getStartTime));
            } else if (preference.equals("lowest_duration_ride")) {
                matchingRides.sort(Comparator.comparing(Ride::getDuration));
            }

            return matchingRides;
        }

        public List<List<Ride>> findTransitRides(String origin, String destination) {
            List<List<Ride>> result = new ArrayList<>();
            Stack<Ride> stack = new Stack<>();
            Set<Ride> visited = new HashSet<>();
            dfsTransitRides(origin, destination, stack, visited, result);
            return result;
        }

        private void dfsTransitRides(String current, String destination, Stack<Ride> stack, Set<Ride> visited, List<List<Ride>> result) {
            if (current.equals(destination)) {
                result.add(new ArrayList<>(stack));
                return;
            }

            for (Ride ride : rides) {
                if (!visited.contains(ride) && ride.getOrigin().equals(current)) {
                    visited.add(ride);
                    stack.push(ride);
                    dfsTransitRides(ride.getDestination(), destination, stack, visited, result);
                    stack.pop();
                    visited.remove(ride);
                }
            }
        }
    }

    private static class VehicleService {
        private List<Vehicle> vehicles;
        private int vehicleCounter;

        public VehicleService() {
            this.vehicles = new ArrayList<>();
            this.vehicleCounter = 0;
        }

        public Vehicle addVehicle(User owner, String name, String registrationNumber) {
            vehicleCounter++;
            Vehicle vehicle = new Vehicle(vehicleCounter, owner, name, registrationNumber);
            owner.addVehicle(vehicle);
            vehicles.add(vehicle);
            return vehicle;
        }

        public List<Vehicle> getVehicles() {
            return vehicles;
        }
    }

    public static void main(String[] args) {
        // Create users
        User john = new User(1, "John", "M", 26);
        User smith = new User(2, "Smith", "M", 30);
        User alice = new User(3, "Alice", "F", 24);

        // Initialize services
        VehicleService vehicleService = new VehicleService();
        RideService rideService = new RideService();

        // Add vehicles
        Vehicle swift = vehicleService.addVehicle(john, "Swift", "KA-09-32321");
        Vehicle sedan = vehicleService.addVehicle(smith, "Sedan", "KA-05-56789");

        // Offer rides
        rideService.offerRide(john, swift, "Bangalore", "Mysore", new Date(), 3 * 60 * 60 * 1000, 3);
        rideService.offerRide(smith, sedan, "Bangalore", "Mysore", new Date(System.currentTimeMillis() + 3600000), 4 * 60 * 60 * 1000, 2);

        // Search and book rides for Alice
        List<Ride> ridesForAlice = rideService.searchRides("Bangalore", "Mysore", "earliest_ending_ride");
        System.out.println("Available Rides for Alice:");
        for (Ride r : ridesForAlice) {
            System.out.println("Ride ID: " + r.getId() + ", Driver: " + r.getDriver().getName() + ", Seats Available: " + r.getAvailableSeats());
        }

        if (!ridesForAlice.isEmpty()) {
            Ride selectedRide = ridesForAlice.get(0);
            boolean bookingSuccess = selectedRide.bookSeats(2);
            if (bookingSuccess) {
                alice.takeRide(new RideRequest(1, alice, "Bangalore", "Mysore", 2, "earliest_ending_ride"));
                System.out.println("Alice booked Ride ID: " + selectedRide.getId() + " with Driver: " + selectedRide.getDriver().getName());
            } else {
                System.out.println("Booking failed for Alice.");
            }
        }

        // Search and book rides for Smith
        List<Ride> ridesForSmith = rideService.searchRides("Bangalore", "Mysore", "lowest_duration_ride");
        System.out.println("\nAvailable Rides for Smith:");
        for (Ride r : ridesForSmith) {
            System.out.println("Ride ID: " + r.getId() + ", Driver: " + r.getDriver().getName() + ", Seats Available: " + r.getAvailableSeats());
        }

        if (!ridesForSmith.isEmpty()) {
            Ride selectedRide = ridesForSmith.get(0);
            boolean bookingSuccess = selectedRide.bookSeats(1);
            if (bookingSuccess) {
                smith.takeRide(new RideRequest(2, smith, "Bangalore", "Mysore", 1, "lowest_duration_ride"));
                System.out.println("Smith booked Ride ID: " + selectedRide.getId() + " with Driver: " + selectedRide.getDriver().getName());
            } else {
                System.out.println("Booking failed for Smith.");
            }
        }
    }
}
