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
