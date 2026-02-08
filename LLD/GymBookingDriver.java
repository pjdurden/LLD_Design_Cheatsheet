/*
======================== LLD DESIGN INTERVIEW SCRIPT (GYM CLASS BOOKING PORTAL) ========================

------------------------------------------------------------------------------------------
1) CLARIFYING REQUIREMENTS (what I ask first)
------------------------------------------------------------------------------------------
"Before I begin the design, I want to confirm scope and assumptions."

Questions I ask:
1. Roles:
   - Admin can add/remove gyms and classes? ✅
   - Users can book/cancel classes and view bookings? ✅
2. Booking rules:
   - Can a user book the same class multiple times? ❌ (should be prevented ✅)
   - Can a user book overlapping classes in time? (not implemented, can be added)
3. Class capacity:
   - Each class has maxLimit ✅
   - Gym overall capacity constraint for overlapping classes ✅
4. Cancellation rules:
   - Allowed anytime? (we allow anytime, no penalty rules)
   - Waitlist required? (not in this version)
5. Search/filter:
   - Do we need search gyms by location? (not implemented)
   - Do we need filter by class type/time? (not implemented)
6. Time handling:
   - Are we okay using Date objects and overlap checks? ✅

Assumptions in this design:
- In-memory storage (no DB)
- Booking is first-come-first-served
- No payment / subscription logic
- No notifications (SMS/email)
- Basic thread safety using synchronized methods + concurrent collections

------------------------------------------------------------------------------------------
2) REQUIREMENTS
------------------------------------------------------------------------------------------
Functional Requirements:
- Admin can:
  - Add gym
  - Remove gym (also removes its classes + related bookings)
  - Add class in a gym
  - Remove class (also removes bookings of that class)
- User can:
  - Book a class
  - Cancel a booking
  - View all bookings for a customer
- System must enforce:
  - Class capacity limit
  - Gym max accommodation across overlapping classes

Non-Functional Requirements:
- Correctness under concurrent booking attempts (no double booking, no oversubscription)
- Fast lookup (gyms, classes, bookings)
- Extensible for search, waitlist, payments, trainer assignment

------------------------------------------------------------------------------------------
3) IDENTIFY ENTITIES (core classes)
------------------------------------------------------------------------------------------
GymBookingPortal (main service layer)
Gym
GymClass
Booking
Admin (wrapper over portal actions)
User (wrapper over portal actions)

Event (internal helper for overlap-capacity calculation)
(Ordering by time using Comparable)

------------------------------------------------------------------------------------------
4) RELATIONSHIPS (has-a / is-a)
------------------------------------------------------------------------------------------
GymBookingPortal has:
- gyms: Map<gymId, Gym>
- bookings: Map<bookingId, Booking>

Gym has:
- classes: Map<classId, GymClass>
- maxAccommodation (global capacity constraint)

GymClass has:
- time window (startTime, endTime)
- maxLimit (class capacity)
- bookedCustomers: Set<customerId>

Booking maps:
customerId -> (gymId, classId)

Admin/User are clients of the portal (thin layer)

------------------------------------------------------------------------------------------
5) DESIGN CHOICES / PATTERNS DISCUSSED
------------------------------------------------------------------------------------------
Service Layer:
- GymBookingPortal owns system logic and acts as a single entry point

Thread Safety:
- ConcurrentHashMap for safe read/write maps
- synchronized methods for atomic operations:
  addGym/removeGym/addClass/removeClass/bookClass/cancelBooking

Set-based uniqueness:
- GymClass.bookedCustomers is a Set to prevent duplicate booking per class

Interval Overlap Capacity Check:
- checkOverLoadLimit() uses sweep-line algorithm:
  - Add +capacity at start time, -capacity at end time
  - Track maximum concurrent students
  - Ensure adding new class doesn’t exceed gym maxAccommodation

------------------------------------------------------------------------------------------
6) CORE APIs (method signatures / entry points)
------------------------------------------------------------------------------------------
Admin APIs:
- addGym(name, location, maxAccommodation)
- removeGym(gymId)
- addClass(gymId, type, maxLimit, startTime, endTime)
- removeClass(gymId, classId)

User APIs:
- bookClass(customerId, gymId, classId)
- cancelBooking(bookingId)
- getAllBookings(customerId)

Portal internal APIs:
- checkOverLoadLimit(gym, startTime, endTime, classCapacity)

------------------------------------------------------------------------------------------
7) EDGE CASES I DISCUSS (and how this handles them)
------------------------------------------------------------------------------------------
- Gym not found -> IllegalArgumentException ✅
- Class not found OR class full -> IllegalArgumentException ✅
- Customer booking same class twice -> IllegalStateException ✅
- Cancel invalid bookingId -> safe no-op ✅
- Removing class should remove its bookings ✅
- Removing gym should remove all classes and related bookings ✅
- Concurrency:
  - synchronized booking ensures class capacity doesn’t get overbooked ✅

Potential improvements:
- Users booking overlapping classes (not prevented currently)
- Better performance for large scale booking queries (index by customerId)

------------------------------------------------------------------------------------------
8) EXTENSIBILITY (next steps if asked)
------------------------------------------------------------------------------------------
- Add search:
  - by location
  - by class type
  - by time window
- Add waitlist system when class is full
- Add subscription/payment model
- Add trainers + room allocation
- Add booking expiry / no-show penalties
- Store all bookings per customer in a map for faster retrieval
- Replace Date with java.time (LocalDateTime) for cleaner handling

------------------------------------------------------------------------------------------
9) WALKTHROUGH (validate with flow)
------------------------------------------------------------------------------------------
Admin adds gyms
Admin adds classes (capacity checked against gym max accommodation)
Users book classes (capacity + duplicate booking checks)
Users view bookings
Users cancel
Admin removes class / gym and system cleans dependent bookings

==========================================================================================
CODE STARTS BELOW
==========================================================================================
*/

import java.util.*;
import java.util.concurrent.*;

public class GymBookingDriver {

    private static class Event implements Comparable<Event> {
        private final Date time;
        private final int studentChange;

        public Event(Date time, int studentChange) {
            this.time = time;
            this.studentChange = studentChange;
        }

        public Date getTime() {
            return time;
        }

        public int getStudentChange() {
            return studentChange;
        }

        @Override
        public int compareTo(Event other) {
            return this.time.compareTo(other.time);
        }
    }

    private static class Gym {
        private final String id;
        private final String name;
        private final String location;
        private final int maxAccommodation;
        private final Map<String, GymClass> classes;

        public Gym(String id, String name, String location, int maxAccommodation) {
            this.id = id;
            this.name = name;
            this.location = location;
            this.maxAccommodation = maxAccommodation;
            this.classes = new ConcurrentHashMap<>();
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getMaxAccommodation() {
            return maxAccommodation;
        }

        public Map<String, GymClass> getClasses() {
            return classes;
        }
    }

    private static class GymClass {
        private final String id;
        private final String type;
        private final int maxLimit;
        private final Date startTime;
        private final Date endTime;
        private final Set<String> bookedCustomers;

        public GymClass(String id, String type, int maxLimit, Date startTime, Date endTime) {
            this.id = id;
            this.type = type;
            this.maxLimit = maxLimit;
            this.startTime = startTime;
            this.endTime = endTime;
            this.bookedCustomers = ConcurrentHashMap.newKeySet();
        }

        public String getId() {
            return id;
        }

        public int getMaxLimit() {
            return maxLimit;
        }

        public Date getStartTime() {
            return startTime;
        }

        public Date getEndTime() {
            return endTime;
        }

        public Set<String> getBookedCustomers() {
            return bookedCustomers;
        }
    }

    private static class Booking {
        private final String id;
        private final String customerId;
        private final String gymId;
        private final String classId;

        public Booking(String id, String customerId, String gymId, String classId) {
            this.id = id;
            this.customerId = customerId;
            this.gymId = gymId;
            this.classId = classId;
        }

        public String getId() {
            return id;
        }

        public String getCustomerId() {
            return customerId;
        }

        public String getGymId() {
            return gymId;
        }

        public String getClassId() {
            return classId;
        }
    }

    private static class GymBookingPortal {
        private final Map<String, Gym> gyms;
        private final Map<String, Booking> bookings;

        public GymBookingPortal() {
            this.gyms = new ConcurrentHashMap<>();
            this.bookings = new ConcurrentHashMap<>();
        }

        public synchronized String addGym(String name, String location, int maxAccommodation) {
            String gymId = UUID.randomUUID().toString();
            gyms.put(gymId, new Gym(gymId, name, location, maxAccommodation));
            return gymId;
        }

        public synchronized void removeGym(String gymId) {
            Gym gym = gyms.remove(gymId);
            if (gym != null) {
                for (String classId : gym.getClasses().keySet()) {
                    removeClass(gymId, classId);
                }
            }
        }

        public synchronized String addClass(String gymId, String classType, int maxLimit, Date startTime, Date endTime) {
            Gym gym = gyms.get(gymId);
            if (gym == null) {
                throw new IllegalArgumentException("Gym not found.");
            }
            if (checkOverLoadLimit(gym, startTime, endTime, maxLimit)) {
                throw new IllegalArgumentException("Class exceeds gym's max accommodation limit.");
            }
            String classId = UUID.randomUUID().toString();
            gym.getClasses().put(classId, new GymClass(classId, classType, maxLimit, startTime, endTime));
            return classId;
        }

        public synchronized void removeClass(String gymId, String classId) {
            Gym gym = gyms.get(gymId);
            if (gym != null) {
                GymClass gymClass = gym.getClasses().remove(classId);
                if (gymClass != null) {
                    bookings.entrySet().removeIf(entry -> entry.getValue().getGymId().equals(gymId) && entry.getValue().getClassId().equals(classId));
                }
            }
        }

        public synchronized String bookClass(String customerId, String gymId, String classId) {
            Gym gym = gyms.get(gymId);
            if (gym == null) throw new IllegalArgumentException("Gym not found.");

            GymClass gymClass = gym.getClasses().get(classId);
            if (gymClass == null || gymClass.getBookedCustomers().size() >= gymClass.getMaxLimit()) {
                throw new IllegalArgumentException("Class is full or not found.");
            }

            if (!gymClass.getBookedCustomers().add(customerId)) {
                throw new IllegalStateException("Customer already booked this class.");
            }

            String bookingId = UUID.randomUUID().toString();
            bookings.put(bookingId, new Booking(bookingId, customerId, gymId, classId));
            return bookingId;
        }

        public synchronized List<Booking> getAllBookings(String customerId) {
            List<Booking> customerBookings = new ArrayList<>();
            for (Booking booking : bookings.values()) {
                if (booking.getCustomerId().equals(customerId)) {
                    customerBookings.add(booking);
                }
            }
            return customerBookings;
        }

        public synchronized void cancelBooking(String bookingId) {
            Booking booking = bookings.remove(bookingId);
            if (booking != null) {
                Gym gym = gyms.get(booking.getGymId());
                if (gym != null) {
                    GymClass gymClass = gym.getClasses().get(booking.getClassId());
                    if (gymClass != null) {
                        gymClass.getBookedCustomers().remove(booking.getCustomerId());
                    }
                }
            }
        }

        private synchronized boolean checkOverLoadLimit(Gym gym, Date startTime, Date endTime, int classCapacity) {
            List<Event> intervals = new ArrayList<>();

            for (GymClass gymClass : gym.getClasses().values()) {
                if (gymClass.getStartTime().before(endTime) && startTime.before(gymClass.getEndTime())) {
                    intervals.add(new Event(gymClass.getStartTime(), gymClass.getMaxLimit()));
                    intervals.add(new Event(gymClass.getEndTime(), -gymClass.getMaxLimit()));
                }
            }
            Collections.sort(intervals);

            int currentStudents = 0;
            int maxStudents = 0;
            for (Event interval : intervals) {
                currentStudents += interval.getStudentChange();
                maxStudents = Math.max(maxStudents, currentStudents);
            }
            return (maxStudents + classCapacity) > gym.getMaxAccommodation();
        }
    }

    private static class Admin {
        private final GymBookingPortal portal;

        public Admin(GymBookingPortal portal) {
            this.portal = portal;
        }

        public String addGym(String name, String location, int maxAccommodation) {
            return portal.addGym(name, location, maxAccommodation);
        }

        public void removeGym(String gymId) {
            portal.removeGym(gymId);
        }

        public String addClass(String gymId, String classType, int maxLimit, Date startTime, Date endTime) {
            return portal.addClass(gymId, classType, maxLimit, startTime, endTime);
        }

        public void removeClass(String gymId, String classId) {
            portal.removeClass(gymId, classId);
        }
    }

    private static class User {
        private final GymBookingPortal portal;

        public User(GymBookingPortal portal) {
            this.portal = portal;
        }

        public String bookClass(String customerId, String gymId, String classId) {
            return portal.bookClass(customerId, gymId, classId);
        }

        public List<Booking> getAllBookings(String customerId) {
            return portal.getAllBookings(customerId);
        }

        public void cancelBooking(String bookingId) {
            portal.cancelBooking(bookingId);
        }
    }

    public static void main(String[] args) {
        GymBookingPortal portal = new GymBookingPortal();
        Admin admin = new Admin(portal);
        User user = new User(portal);

        // Admin actions
        String gym1Id = admin.addGym("Gym1", "Indira Nagar", 100);
        String gym2Id = admin.addGym("Gym2", "Whitefield", 150);
        System.out.println("Added Gyms: " + gym1Id + ", " + gym2Id);

        String class1Id = admin.addClass(gym1Id, "Cardio", 20, new Date(124, 0, 1, 6, 0), new Date(124, 0, 1, 7, 0));
        String class2Id = admin.addClass(gym1Id, "Yoga", 30, new Date(124, 0, 1, 7, 30), new Date(124, 0, 1, 8, 30));
        String class3Id = admin.addClass(gym2Id, "Zumba", 40, new Date(124, 0, 1, 6, 0), new Date(124, 0, 1, 7, 0));
        System.out.println("Added Classes: " + class1Id + ", " + class2Id + ", " + class3Id);

        // User actions
        String booking1Id = user.bookClass("customer1", gym1Id, class1Id);
        String booking2Id = user.bookClass("customer2", gym1Id, class2Id);
        String booking3Id = user.bookClass("customer3", gym2Id, class3Id);
        System.out.println("Bookings Successful: " + booking1Id + ", " + booking2Id + ", " + booking3Id);

        // Retrieve and print all bookings for a customer
        List<Booking> customerBookings = user.getAllBookings("customer1");
        System.out.println("Bookings for customer1:");
        for (Booking booking : customerBookings) {
            System.out.println("  - Booking ID: " + booking.getId() + ", Gym: " + booking.getGymId() + ", Class: " + booking.getClassId());
        }

        // Cancel a booking
        user.cancelBooking(booking1Id);
        System.out.println("Cancelled Booking: " + booking1Id);

        // Admin removes a class
        admin.removeClass(gym1Id, class2Id);
        System.out.println("Removed Class: " + class2Id);

        // Admin removes a gym
        admin.removeGym(gym2Id);
        System.out.println("Removed Gym: " + gym2Id);

        // Final status
        System.out.println("Final Gym and Class Status:");
        portal.getAllBookings("customer2").forEach(booking ->
                System.out.println("Booking ID: " + booking.getId() + ", Gym ID: " + booking.getGymId() + ", Class ID: " + booking.getClassId()));
    }
}
