/*
========================== LLD DESIGN INTERVIEW SCRIPT (BOOKMYSHOW) ==========================

------------------------------------------------------------------------------------------
1) CLARIFYING REQUIREMENTS (what I ask first)
------------------------------------------------------------------------------------------
"Before designing BookMyShow, I want to confirm the scope and assumptions."

Questions I ask:
1. Core features needed for MVP?
   - Search movies by city/date/language/genre  ✅
   - View theatres + showtimes  ✅
   - View seat layout + availability  ✅
   - Select and lock seats for 5 minutes  ✅
   - Make payment + confirm booking  ✅
   - Download ticket  ✅

2. Out of scope for now:
   - Complex recommendation engine
   - Multi-city routing
   - Dynamic pricing
   - Food ordering inside theatre
   - Offline booking counter sync

3. Seat-locking assumptions:
   - Lock seats for 5 mins
   - If payment fails or lock expires → release seats

4. Payment assumptions:
   - One PaymentGateway interface
   - Only CreditCard/UPI implementation for demo

5. Theatre data:
   - Theatre has multiple screens
   - Screen has multiple screenings
   - Screening has seats + movie + showtime

6. Concurrency:
   - Multiple users selecting seats → avoid double-booking (seat status + lock)

Assumptions in design:
- In-memory storage (Maps)
- No authentication logic
- One city at a time (for simplicity)
- State pattern for BookingStatus + SeatStatus

------------------------------------------------------------------------------------------
2) FUNCTIONAL REQUIREMENTS
------------------------------------------------------------------------------------------
Users should be able to:
- Search & browse movies
- View theatres and available showtimes
- View seat layout with live availability
- Lock seats for a short time window
- Make payments for the booking
- Generate ticket after successful booking
- View booking details

Admins should be able to:
- Add movies
- Add theatres, screens, and screenings

------------------------------------------------------------------------------------------
3) NON-FUNCTIONAL REQUIREMENTS
------------------------------------------------------------------------------------------
- High availability (especially during blockbuster releases)
- Low latency seat selection
- Strong consistency for seat locking
- Thread-safe seat updates
- Extensible for multiple payment methods and loyalty points
- Maintainable service design

------------------------------------------------------------------------------------------
4) IDENTIFY ENTITIES (core classes)
------------------------------------------------------------------------------------------
User
Admin
Movie
Seat
Screen
Theatre
Screening (Movie + Screen + time + seats)
Booking
Ticket
PaymentRequest
PaymentGateway (interface) -> UpiPayment, CardPayment
BookMyShowService (service layer)

Enums:
SeatStatus (AVAILABLE, LOCKED, BOOKED)
BookingStatus (PENDING_PAYMENT, CONFIRMED, CANCELLED, EXPIRED)

------------------------------------------------------------------------------------------
5) RELATIONSHIPS
------------------------------------------------------------------------------------------
Theatre has List<Screen>
Screen has List<Screening>
Screening has:
   - Movie
   - List<Seat>
   - Showtime
   - Screen

Booking has:
   - User
   - Seats
   - Screening
   - Ticket

BookMyShowService has:
   - List<User>
   - List<Admin>
   - List<Theatre>
   - List<Booking>

PaymentGateway receives PaymentRequest and returns success/failure.

------------------------------------------------------------------------------------------
6) DESIGN CHOICES / PATTERNS
------------------------------------------------------------------------------------------
State Pattern:
- BookingStatus (Pending → Confirmed → Cancelled)
- SeatStatus   (Available → Locked → Booked)

Strategy Pattern:
- PaymentGateway (interface) with implementations (UPI, card, wallet)

Singleton Pattern:
- BookMyShowService as the orchestrator

Concurrency:
- Synchronized seat lock operations
- Screening-level locking for atomic selection

------------------------------------------------------------------------------------------
7) CORE APIs / METHOD SIGNATURES (high-level)
------------------------------------------------------------------------------------------
User-facing APIs:
- searchMovies(city, filters)
- getTheatres(movieId, city)
- getScreenings(movieId, theatreId)
- getSeatLayout(screeningId)
- lockSeats(screeningId, seatIds, userId)
- createBooking(userId, screeningId, seatIds)
- initiatePayment(bookingId, paymentMethod)
- confirmPayment(bookingId, success/failure)
- getBooking(bookingId)
- getTicket(ticketId)

Admin APIs:
- addMovie(movie)
- addTheatre(theatre)
- addScreen(theatreId, screen)
- addScreening(screenId, screening)

------------------------------------------------------------------------------------------
8) EDGE CASES DISCUSSED
------------------------------------------------------------------------------------------
- Two users select same seat → first lock wins, second fails
- Automatic unlock of seats after timeout
- Payment failure → release seats + cancel booking
- Partial seat locking not allowed → all-or-nothing
- Booking created but payment pending → status stays PENDING_PAYMENT
- If user closes app during payment → callback handles state

------------------------------------------------------------------------------------------
9) EXTENSIBILITY (future enhancements)
------------------------------------------------------------------------------------------
- Discounts, coupons, surge pricing
- Seat categories (Gold, Silver, Recliner)
- Food ordering
- Multiple cities, GPS-based theatre suggestion
- Webhooks for payment confirmation
- Distributed Locking (Redis) for real-world seat locks
- Kafka events for booking lifecycle

------------------------------------------------------------------------------------------
10) SAMPLE FLOW (walkthrough during interview)
------------------------------------------------------------------------------------------

User → searches movies
User → chooses theatre → showtime → seat layout
User → selects seats → system locks seats for 5 minutes
User → initiates payment
Payment success → seats become BOOKED → ticket generated
Payment failure → release seats → booking CANCELLED

==========================================================================================
(Implementation classes would go below this section if needed)
==========================================================================================

*/

/*
========================== LLD DESIGN INTERVIEW SCRIPT (BOOKMYSHOW) ==========================

(Your full explanation section remains unchanged here…)

==========================================================================================
(Implementation classes start below)
==========================================================================================
*/

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/* ---------------- ENUMS ---------------- */
enum BookingStatus { PENDING, CONFIRMED, CANCELLED }
enum SeatStatus    { AVAILABLE, LOCKED, BOOKED }
enum Role          { USER, ADMIN }

/* ---------------- ENTITY: MOVIE ---------------- */
class Movie {
    public String id;
    public String title;

    Movie(String id, String title) {
        this.id = id;
        this.title = title;
    }
}

/* ---------------- ENTITY: SEAT ---------------- */
class Seat {
    public String id;
    public SeatStatus status;

    Seat(String id) {
        this.id = id;
        this.status = SeatStatus.AVAILABLE;
    }

    public synchronized boolean lock() {
        if (status == SeatStatus.AVAILABLE) {
            status = SeatStatus.LOCKED;
            return true;
        }
        return false;
    }

    public synchronized void unlock() {
        if (status == SeatStatus.LOCKED) status = SeatStatus.AVAILABLE;
    }

    public synchronized void book() {
        status = SeatStatus.BOOKED;
    }
}

/* ---------------- ENTITY: SCREEN ---------------- */
class Screen {
    public String id;
    public List<Screening> screenings = new ArrayList<>();
    Screen(String id){ this.id = id; }
}

/* ---------------- ENTITY: THEATRE ---------------- */
class Theatre {
    public String id;
    public String name;
    public List<Screen> screens = new ArrayList<>();

    Theatre(String id, String name){
        this.id = id;
        this.name = name;
    }
}

/* ---------------- ENTITY: SCREENING ---------------- */
class Screening {
    public String id;
    public Movie movie;
    public Date time;
    public List<Seat> seats;

    Screening(String id, Movie movie, Date time, int count){
        this.id = id;
        this.movie = movie;
        this.time = time;

        seats = new ArrayList<>();
        for(int i=1; i<=count; i++){
            seats.add(new Seat("S"+i));
        }
    }
}

/* ---------------- ENTITY: USER ---------------- */
class User {
    public String id;
    public String name;
    public Role role;
    public List<Booking> bookings = new ArrayList<>();

    User(String id, String name, Role role){
        this.id = id;
        this.name = name;
        this.role = role;
    }
}

/* ---------------- ENTITY: BOOKING ---------------- */
class Booking {
    public String id;
    public User user;
    public Screening screening;
    public List<Seat> seats;
    public BookingStatus status = BookingStatus.PENDING;
    public Ticket ticket;

    Booking(String id, User user, Screening sc, List<Seat> seats){
        this.id = id;
        this.user = user;
        this.screening = sc;
        this.seats = seats;
    }
}

/* ---------------- ENTITY: TICKET ---------------- */
class Ticket {
    public String id;
    public Booking booking;

    Ticket(String id, Booking b){
        this.id = id;
        this.booking = b;
    }
}

/* ---------------- PAYMENT STRATEGY ---------------- */
interface PaymentGatewayInterface {
    boolean processPayment();
}

class CreditCardPayment implements PaymentGatewayInterface {
    public boolean processPayment() {
        System.out.println("Processing payment...");
        return true;
    }
}

/* ================================================================================
                                SERVICE LAYER
=================================================================================== */

class BookMyShowService {

    private static final BookMyShowService instance = new BookMyShowService();
    public static BookMyShowService getInstance(){ return instance; }
    private BookMyShowService(){}

    public Map<String, User> users       = new ConcurrentHashMap<>();
    public Map<String, Movie> movies     = new ConcurrentHashMap<>();
    public Map<String, Theatre> theatres = new ConcurrentHashMap<>();
    public Map<String, Booking> bookings = new ConcurrentHashMap<>();

    PaymentGatewayInterface paymentGateway = new CreditCardPayment();

    /* ---------------- SEARCH ---------------- */
    public List<Movie> searchMovies(String keyword){
        List<Movie> res = new ArrayList<>();
        for(Movie m : movies.values()){
            if(m.title.toLowerCase().contains(keyword.toLowerCase()))
                res.add(m);
        }
        return res;
    }

    /* ---------------- THEATRES ---------------- */
    public List<Theatre> getTheatres(String movieId){
        List<Theatre> res = new ArrayList<>();
        for(Theatre t : theatres.values()){
            for(Screen s : t.screens){
                for(Screening sc : s.screenings){
                    if(sc.movie.id.equals(movieId)){
                        res.add(t);
                    }
                }
            }
        }
        return res;
    }

    /* ---------------- SCREENINGS ---------------- */
    public List<Screening> getScreenings(String movieId, String theatreId){
        List<Screening> res = new ArrayList<>();
        Theatre t = theatres.get(theatreId);
        if(t == null) return res;

        for(Screen s : t.screens){
            for(Screening sc : s.screenings){
                if(sc.movie.id.equals(movieId))
                    res.add(sc);
            }
        }
        return res;
    }

    /* ---------------- SEATS ---------------- */
    public List<Seat> getSeatLayout(String scId){
        Screening sc = findScreening(scId);
        return sc == null ? new ArrayList<>() : sc.seats;
    }

    /* ---------------- LOCK SEATS ---------------- */
    public synchronized boolean lockSeats(String scId, List<String> ids){
        Screening sc = findScreening(scId);
        if(sc == null) return false;

        for(String sid : ids){
            Seat st = getSeat(sc, sid);
            if(st == null || st.status != SeatStatus.AVAILABLE)
                return false;
        }
        for(String sid : ids){
            getSeat(sc, sid).lock();
        }
        return true;
    }

    /* ---------------- CREATE BOOKING ---------------- */
    public Booking createBooking(String userId, String scId, List<String> ids){
        User user = users.get(userId);
        Screening sc = findScreening(scId);

        List<Seat> sel = new ArrayList<>();
        for(String sid : ids) sel.add(getSeat(sc, sid));

        Booking b = new Booking(UUID.randomUUID().toString(), user, sc, sel);
        bookings.put(b.id, b);
        user.bookings.add(b);
        return b;
    }

    /* ---------------- PAYMENT ---------------- */
    public boolean initiatePayment(String bid){
        return paymentGateway.processPayment();
    }

    public synchronized void confirmPayment(String bid, boolean success){
        Booking b = bookings.get(bid);
        if(b == null) return;

        if(success){
            for(Seat s : b.seats) s.book();
            b.status = BookingStatus.CONFIRMED;
            b.ticket = new Ticket("T-" + bid, b);
        } else {
            for(Seat s : b.seats) s.unlock();
            b.status = BookingStatus.CANCELLED;
        }
    }

    /* ---------------- ADMIN ---------------- */
    public void addMovie(Movie m){ movies.put(m.id, m); }
    public void addTheatre(Theatre t){ theatres.put(t.id, t); }
    public void addScreen(String thId, Screen s){ theatres.get(thId).screens.add(s); }
    public void addScreening(String thId, String scrId, Screening sc){
        for(Screen s : theatres.get(thId).screens){
            if(s.id.equals(scrId)) s.screenings.add(sc);
        }
    }

    /* ---------------- UTIL ---------------- */
    private Screening findScreening(String id){
        for(Theatre t : theatres.values())
            for(Screen s : t.screens)
                for(Screening sc : s.screenings)
                    if(sc.id.equals(id)) return sc;
        return null;
    }

    private Seat getSeat(Screening sc, String id){
        for(Seat s : sc.seats)
            if(s.id.equals(id)) return s;
        return null;
    }
}

/* ==================================================================================
                                  DRIVER MAIN
=================================================================================== */

public class BookMyShow {
    public static void main(String[] args) {

        BookMyShowService service = BookMyShowService.getInstance();

        User admin = new User("A1", "Admin", Role.ADMIN);
        User user  = new User("U1", "Prajjwal", Role.USER);

        service.users.put(admin.id, admin);
        service.users.put(user.id, user);

        Movie m = new Movie("M1", "Inception");
        service.addMovie(m);

        Theatre t = new Theatre("T1", "PVR Cinemas");
        service.addTheatre(t);

        Screen screen1 = new Screen("SCR1");
        service.addScreen("T1", screen1);

        Screening sc = new Screening("SC1", m, new Date(), 10);
        service.addScreening("T1", "SCR1", sc);

        System.out.println(service.searchMovies("inception"));
        System.out.println(service.getTheatres("M1"));
        System.out.println(service.getScreenings("M1", "T1"));

        List<Seat> seats = service.getSeatLayout("SC1");
        System.out.println("Seat layout:");
        for(Seat s : seats) System.out.println(s.id + " - " + s.status);

        List<String> seatIds = Arrays.asList("S1", "S2");
        System.out.println("Locking seats: " + service.lockSeats("SC1", seatIds));

        Booking booking = service.createBooking("U1", "SC1", seatIds);
        System.out.println("Booking created: " + booking.id);

        boolean paid = service.initiatePayment(booking.id);
        service.confirmPayment(booking.id, paid);

        System.out.println("Final booking status: " + booking.status);
        if(booking.ticket != null)
            System.out.println("Ticket ID: " + booking.ticket.id);
    }
}
