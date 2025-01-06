// Parking Lot
// the class will have 
// parking spot -> level -> ParkingLot
// Vehicle - Bike ,Car , Truck

import java.util.ArrayList;
import java.util.List;

public class Parking_Lot{

    private enum VehicleType{
        Bike, Car, Truck
    } 

    private static abstract class Vehicle{
        private VehicleType vehicleType;
        private String vehicleNumber;
        public Vehicle(VehicleType vehicleType , String vehicleNumber){
            this.vehicleType = vehicleType;
            this.vehicleNumber = vehicleNumber;
        }
        public VehicleType getVehicleType() {
            return vehicleType;
        }
        public void setVehicleType(VehicleType vehicleType) {
            this.vehicleType = vehicleType;
        }
        public String getVehicleNumber() {
            return vehicleNumber;
        }
        public void setVehicleNumber(String vehicleNumber) {
            this.vehicleNumber = vehicleNumber;
        }
        

    }
    
    private static class Bike extends Vehicle{
        public Bike(String vehicleNumber)
        {
            super(VehicleType.Bike,vehicleNumber);
        }
    }

    private static class Car extends Vehicle{
        public Car(String vehicleNumber)
        {
            super(VehicleType.Car,vehicleNumber);
        } 
    }

    private static class Truck extends Vehicle{
        public Truck(String vehicleNumber)
        {
            super(VehicleType.Truck,vehicleNumber);
        }   
    }

    private static class ParkingSpot{
        private final int spotNumber;
        private Vehicle vehicle;
        private final VehicleType vehicleType;
        public VehicleType getVehicleType() {
            return vehicleType;
        }
        public int getSpotNumber() {
            return spotNumber;
        }
        public Vehicle getVehicle() {
            return vehicle;
        }
        private ParkingSpot(int spotNumber, VehicleType vehicleType)
        {
            this.spotNumber = spotNumber;
            this.vehicle = null;
            this.vehicleType = vehicleType;
        }
        public synchronized boolean isAvailable(){
            return vehicle == null;
        }
        public synchronized void park(Vehicle vehicle)
        {
            this.vehicle = vehicle; 
        }
        public synchronized void unpark()
        {
            vehicle = null;
        }
    }

    private static class Level{
        private final int floor;
        private final List<ParkingSpot> parkingSpots;
        public Level(int floor , int numSpots)
        {
            this.floor=floor;
            parkingSpots = new ArrayList<>();
            
             // Assign spots in ration of 50:40:10 for bikes, cars and trucks
            double spotsForBikes = 0.30;
            double spotsForCars = 0.30;

            int numBikes = (int) (numSpots * spotsForBikes);
            int numCars = (int) (numSpots * spotsForCars);

            for (int i = 1; i <= numBikes; i++) {
                parkingSpots.add(new ParkingSpot(i,VehicleType.Bike));
            }
            for (int i = numBikes + 1; i <= numBikes + numCars; i++) {
                parkingSpots.add(new ParkingSpot(i,VehicleType.Car));
            }
            for (int i = numBikes + numCars + 1; i <= numSpots; i++) {
                parkingSpots.add(new ParkingSpot(i,VehicleType.Truck));
            }

        }
        public synchronized boolean parkVehicle(Vehicle vehicle)
        {
            for(ParkingSpot parkingSpot : parkingSpots)
            {
                if(parkingSpot.isAvailable() && parkingSpot.getVehicleType() == vehicle.getVehicleType())
                {
                    parkingSpot.park(vehicle);
                    return true;
                }
            }
            return false;
        }
        public synchronized boolean unparkVehicle(Vehicle vehicle)
        {
            for(ParkingSpot parkingSpot : parkingSpots)
            {
                if(!parkingSpot.isAvailable() && parkingSpot.getVehicle().equals(vehicle))
                {
                    parkingSpot.unpark();
                    return true;
                }
            }
            return false;
        }
        public void DisplayAvailability(){
            System.out.println("Level " + floor + " Availability:");
            for (ParkingSpot spot : parkingSpots) {
                System.out.println("Spot " + spot.getSpotNumber() + ": " + (spot.isAvailable() ? "Available For"  : "Occupied By ")+" "+spot.getVehicleType());
            }
        }

    }

    private static class ParkingLot{
        private static ParkingLot instance;
        private final List<Level> levels;
        private ParkingLot()
        {
            levels = new ArrayList<>();
        }

        public static ParkingLot getInstance(){
                    if(instance==null)
                    {
                        instance = new ParkingLot();
                    }
                    return instance;
                }
                public void addLevel(Level level)
                {
                    levels.add(level);
                }
        
                public boolean parkVehicle(Vehicle vehicle){
                    for(Level level: levels )
                    {
                        if(level.parkVehicle(vehicle))
                        {
                            System.out.println("Vehicle parked successfully.");
                            return true;
                        }
                    }
                    return false;
        
                }
                public boolean unparkVehicle(Vehicle vehicle)
                {
                    for(Level level: levels )
                    {
                        if(level.unparkVehicle(vehicle))
                        {
                            return true;
                        }
                    }
                    return false;
                }
                public void DisplayAvailability(){
                    for (Level level : levels) {
                        level.DisplayAvailability();
                    }
                }
        
        
            }

    public static void main(String[] args) {
        ParkingLot parkingLot = ParkingLot.getInstance();
        parkingLot.addLevel(new Level(1, 100));
        parkingLot.addLevel(new Level(2, 80));

        Vehicle car = new Car("ABC123");
        Vehicle truck = new Truck("XYZ789");
        Vehicle motorcycle = new Bike("M1234");


         // Park vehicles
         parkingLot.parkVehicle(car);
         parkingLot.parkVehicle(truck);
         parkingLot.parkVehicle(motorcycle);
 
         // Display availability
         parkingLot.DisplayAvailability();
 
         // Unpark vehicle
         parkingLot.unparkVehicle(motorcycle);
 
         // Display updated availability
         parkingLot.DisplayAvailability();
        
        
        
    }
}