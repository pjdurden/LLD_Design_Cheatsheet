/*
======================== LLD DESIGN INTERVIEW SCRIPT (ELEVATOR SYSTEM) ========================

------------------------------------------------------------------------------------------
1) CLARIFYING REQUIREMENTS (what I ask first)
------------------------------------------------------------------------------------------
"Before designing, I want to confirm scope and assumptions."

Questions I ask:
1. Building size? (how many floors)
2. Number of elevators? fixed or dynamic?
3. Requests type:
   - External call button (UP/DOWN) at a floor?
   - Internal cabin request (destination selection)?
   (Current design treats request as startFloor -> dstFloor)
4. Scheduling:
   - Should we optimize for minimum waiting time OR nearest elevator?
   - Should direction be respected? (elevator going UP shouldn’t pick DOWN calls usually)
5. Capacity constraints:
   - Are we modeling passenger count + max load? (here capacity exists but not enforced)
6. Edge cases:
   - If elevator is busy, can we queue requests? ✅
   - Should requests be ordered/optimized? (SCAN/LOOK algorithm) (not in current code)
7. Concurrency:
   - Multiple people requesting at the same time ✅
   - Thread-per-elevator model acceptable for interview ✅

Assumptions in this implementation:
- Requests are just (sourceFloor, destinationFloor)
- Elevator starts at floor 0
- Each elevator processes requests sequentially (FIFO)
- Optimal elevator selection = closest current floor (greedy)
- Movement is simulated using Thread.sleep()

------------------------------------------------------------------------------------------
2) REQUIREMENTS
------------------------------------------------------------------------------------------
Functional Requirements:
- Controller manages multiple elevators
- User can request elevator: source -> destination
- Controller assigns request to an elevator
- Elevator moves floor-by-floor and reaches destination
- Elevator processes multiple requests continuously

Non-Functional Requirements:
- Thread-safe handling of concurrent requests
- Scalable to multiple elevators
- Extensible scheduling strategy (nearest / direction-based / SCAN)
- Maintainable separation: Controller decides assignment, Elevator executes

------------------------------------------------------------------------------------------
3) IDENTIFY ENTITIES (core classes)
------------------------------------------------------------------------------------------
Request (source + destination)
Elevator (state + queue + worker thread)
ElevatorController (dispatcher / scheduler)
Direction enum (UP/DOWN)

------------------------------------------------------------------------------------------
4) RELATIONSHIPS (has-a / is-a)
------------------------------------------------------------------------------------------
ElevatorController has List<Elevator>
Elevator has List<Request> queue
Request has startFloor + dstFloor
Elevator has state: currentFloor + direction + capacity + id

------------------------------------------------------------------------------------------
5) DESIGN CHOICES / PATTERNS DISCUSSED
------------------------------------------------------------------------------------------
Producer-Consumer pattern (basic):
- Controller produces requests and pushes into elevator queue
- Elevator consumes requests in a dedicated thread

Thread-per-elevator model:
- Each elevator runs independently and handles its own queue

Synchronization:
- addRequest() is synchronized + notify()
- processRequests() waits when queue empty
- findOptimalElevator() synchronized to avoid race during selection

(Extension idea)
Strategy Pattern:
- ElevatorSelectionStrategy can be introduced:
  - NearestElevatorStrategy (current)
  - DirectionAwareStrategy
  - LeastLoadedStrategy

------------------------------------------------------------------------------------------
6) CORE APIs (method signatures / entry points)
------------------------------------------------------------------------------------------
ElevatorController.requestElevator(source, destination)
ElevatorController.findOptimalElevator(source, destination)

Elevator.addRequest(request)
Elevator.processRequests()
Elevator.processRequest(request)

------------------------------------------------------------------------------------------
7) EDGE CASES I DISCUSS (and partial handling)
------------------------------------------------------------------------------------------
- Empty queue -> elevator thread waits ✅
- Multiple requests added concurrently -> synchronized addRequest ✅
- Requests processed FIFO (not optimal but simple) ✅
- Capacity not enforced (should reject/queue differently if overloaded)
- Direction scheduling not respected (can cause inefficiency)
- Starvation possible (greedy nearest may keep picking nearby floors)

------------------------------------------------------------------------------------------
8) EXTENSIBILITY (how to make it production-ready)
------------------------------------------------------------------------------------------
Possible upgrades:
- Separate external and internal requests
- Maintain two priority queues: upQueue (min-heap), downQueue (max-heap)
- Implement SCAN / LOOK elevator scheduling
- Add elevator states: IDLE, MOVING, MAINTENANCE
- Add door open/close + stop time
- Handle emergency stop, overload, maintenance mode
- Add load balancing by queue size (least pending requests)
- Add floor bounds validation

------------------------------------------------------------------------------------------
9) WALKTHROUGH (example flow)
------------------------------------------------------------------------------------------
User requests elevator from 5 -> 10
Controller finds closest elevator and adds request
Elevator wakes up, moves floor by floor until reaching destination
Elevator repeats for pending requests; if none, it waits

==========================================================================================
CODE STARTS BELOW
==========================================================================================
*/

import java.util.ArrayList;
import java.util.List;

public class Elevator_LLD {
    // Elevator(capacity Limit , ) -> Controller
    // Elevator Controller -> List<Elevator>
    // Request -> src, destination

    private enum Direction{
        UP,DOWN
    }

    private static class Request{
        public int startFloor;
        public int dstFloor;
        public Request(int startFloor , int dstFloor){
            this.startFloor = startFloor;
            this.dstFloor = dstFloor;
        }
    }

    private static class Elevator{
        public Direction direction;
        public int currentFloor;
        public int capacity;
        public int id;
        public List<Request> Requests;

        private Elevator(int id,int capacity)
        {
            currentFloor = 0;
            direction = Direction.UP;
            Requests = new ArrayList<>();
            this.capacity = capacity;
            this.id = id;
        }

        public synchronized void addRequest(Request request)
        {
            Requests.add(request);
            notify();
        }

        public synchronized Request getNextRequest(){
            return Requests.remove(0);
        }

        public synchronized void processRequests(){
            while(true)
            {
                while(!Requests.isEmpty())
                {
                    Request request = getNextRequest();
                    processRequest(request);
                }
                try{
                    wait();
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void processRequest(Request request)
        {
            int startFloor = currentFloor;
            int endFloor = request.dstFloor;

            if(startFloor<endFloor)
            {
                direction = Direction.UP;
                for (int i = startFloor; i <= endFloor; i++) {
                    currentFloor = i;
                    System.out.println("Elevator " + id + " reached floor " + currentFloor);
                    try {
                        Thread.sleep(1000); // Simulating elevator movement
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }else{
                direction = Direction.DOWN;
                for (int i = startFloor; i >= endFloor; i--) {
                    currentFloor = i;
                    System.out.println("Elevator " + id + " reached floor " + currentFloor);
                    try {
                        Thread.sleep(1000); // Simulating elevator movement
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void run(){
            processRequests();
        }
    }

    private static class ElevatorController{
        private final List<Elevator> elevators;

        public ElevatorController(int numElevators, int capacity) {
            elevators = new ArrayList<>();
            for(int i = 0 ; i<numElevators; i ++)
            {
                Elevator elevator = new Elevator(i + 1, capacity);
                elevators.add(elevator);
                new Thread(elevator::run).start();
            }
        }

        public void requestElevator(int sourceFloor , int destinationFloor){
            Elevator optimalElevator = findOptimalElevator(sourceFloor, destinationFloor);
            optimalElevator.addRequest(new Request(sourceFloor, destinationFloor));
        }

        public synchronized Elevator findOptimalElevator(int sourceFloor , int destinationFloor)
        {
            Elevator optimalElevator = null;
            int minDistance = Integer.MAX_VALUE;

            for(Elevator elevator : elevators)
            {
                int distance = Math.abs(sourceFloor - elevator.currentFloor);
                if(distance<minDistance)
                {
                    optimalElevator = elevator;
                    minDistance = distance;
                }
            }
            return optimalElevator;
        }
    }

    public static void main(String[] args) {
        ElevatorController controller = new ElevatorController(3, 5);
        controller.requestElevator(5, 10);
        controller.requestElevator(3, 7);
        controller.requestElevator(8, 2);
        controller.requestElevator(1, 9);
    }
}
