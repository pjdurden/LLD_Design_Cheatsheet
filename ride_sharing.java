import java.util.*;

public class ride_sharing {
    // Design a ride sharing application :
    // Driver can publish a ride
    // Rider can search a ride based on strategies such as shortest, fastest
    // Rider can select a ride
    // Bonus
    // show all rides taken and published by a user    

    // user can be both rider and publisher, ok
    // user - details , roletype , ridestaken , ridesgiven , ridesgoingon , vehicles
    // vehicle - owner name , vehicle name , vehicle number
    // rides , 
    // ride - src , dst , driver , list<riders>
    // bfs like multiple rides 
    // RideShare - list<Users> , list<Rides> , create_user(user details) , create_vehicle (User , vehicledetails) , 
    // offer_ride(driver , src , dst , seats , name , vehicle ) , select_ride , 

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
