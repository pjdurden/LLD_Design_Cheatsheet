import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SprintPlanner {

    private enum TaskType {
        STORY, FEATURE, BUG
    }

    private enum TaskStatus {
        TODO, INPROGRESS, DONE
    }

    private static class User {
        private final String userDetails;

        public User(String userDetails) {
            this.userDetails = userDetails;
        }

        @Override
        public String toString() {
            return userDetails;
        }
    }

    private static class Task {
        private final String taskDetails;
        private final TaskType taskType;
        private TaskStatus taskStatus;
        private final User assignedUser;
        private final Date startTime;
        private final Date endTime;

        public Task(String taskDetails, User assignedUser, TaskType taskType, TaskStatus taskStatus, Date startTime, Date endTime) {
            this.taskDetails = taskDetails;
            this.assignedUser = assignedUser;
            this.taskType = taskType;
            this.taskStatus = taskStatus;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public User getAssignedUser() {
            return assignedUser;
        }

        public TaskStatus getTaskStatus() {
            return taskStatus;
        }

        public boolean changeStatus(TaskStatus newStatus) {
            if ((this.taskStatus == TaskStatus.TODO && newStatus == TaskStatus.INPROGRESS) ||
                (this.taskStatus == TaskStatus.INPROGRESS && newStatus == TaskStatus.TODO) ||
                (this.taskStatus == TaskStatus.INPROGRESS && newStatus == TaskStatus.DONE)) {
                this.taskStatus = newStatus;
                return true;
            }
            return false;
        }

        public boolean isDelayed() {
            Date currentTime = new Date();
            return endTime.before(currentTime);
        }

        @Override
        public String toString() {
            return "Task{" +
                    "taskDetails='" + taskDetails + '\'' +
                    ", taskType=" + taskType +
                    ", taskStatus=" + taskStatus +
                    ", assignedUser=" + assignedUser +
                    ", endTime=" + endTime +
                    '}';
        }
    }

    private static class Sprint {
        private final String sprintGoal;
        private final Date startTime;
        private final Date endTime;
        private final List<Task> tasks;
        private final ConcurrentHashMap<User, List<Task>> userTasks;
        private static final int MAX_SPRINT_CAPACITY = 20;
        private static final int MAX_INPROGRESS_TASKS = 2;

        public Sprint(String sprintGoal, Date startTime, Date endTime) {
            this.sprintGoal = sprintGoal;
            this.startTime = startTime;
            this.endTime = endTime;
            this.tasks = Collections.synchronizedList(new ArrayList<>());
            this.userTasks = new ConcurrentHashMap<>();
        }

        public synchronized boolean addTask(Task task) {
            if (tasks.size() < MAX_SPRINT_CAPACITY) {
                User user = task.getAssignedUser();
                tasks.add(task);
                userTasks.computeIfAbsent(user, k -> Collections.synchronizedList(new ArrayList<>())).add(task);
                return true;
            }
            return false;
        }

        public synchronized boolean removeTask(Task task) {
            if (tasks.contains(task)) {
                tasks.remove(task);
                userTasks.get(task.getAssignedUser()).remove(task);
                return true;
            }
            return false;
        }

        public List<Task> showTasksAssigned(User user) {
            return userTasks.getOrDefault(user, Collections.emptyList());
        }

        public synchronized boolean changeTaskStatus(Task task, TaskStatus newStatus) {
            if (tasks.contains(task)) {
                if (newStatus == TaskStatus.INPROGRESS) {
                    long inProgressCount = userTasks.get(task.getAssignedUser()).stream()
                            .filter(t -> t.getTaskStatus() == TaskStatus.INPROGRESS)
                            .count();
                    if (inProgressCount >= MAX_INPROGRESS_TASKS) {
                        return false;
                    }
                }
                return task.changeStatus(newStatus);
            }
            return false;
        }

        public List<Task> getDelayedTasks() {
            List<Task> delayedTasks = new ArrayList<>();
            for (Task task : tasks) {
                if (task.isDelayed()) {
                    delayedTasks.add(task);
                }
            }
            return delayedTasks;
        }

        @Override
        public String toString() {
            return "Sprint{" +
                    "sprintGoal='" + sprintGoal + '\'' +
                    ", tasks=" + tasks +
                    '}';
        }
    }

    public static void main(String[] args) {
        SprintPlanner planner = new SprintPlanner();
        
        // Users
        User alice = new User("Alice");
        User bob = new User("Bob");

        // Sprint
        Sprint sprint = new Sprint("Complete MVP", new Date(), new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000));

        // Tasks
        Task task1 = new Task("Implement Login", alice, TaskType.FEATURE, TaskStatus.TODO, new Date(), new Date(System.currentTimeMillis() + 2L * 24 * 60 * 60 * 1000));
        Task task2 = new Task("Fix Bug #123", alice, TaskType.BUG, TaskStatus.TODO, new Date(), new Date(System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000));
        Task task3 = new Task("Create Database Schema", bob, TaskType.STORY, TaskStatus.TODO, new Date(), new Date(System.currentTimeMillis() + 1L * 24 * 60 * 60 * 1000));

        // Add Tasks to Sprint
        sprint.addTask(task1);
        sprint.addTask(task2);
        sprint.addTask(task3);

        // Assign and Display Tasks
        System.out.println("Tasks Assigned to Alice: " + sprint.showTasksAssigned(alice));
        System.out.println("Tasks Assigned to Bob: " + sprint.showTasksAssigned(bob));

        // Change Status
        sprint.changeTaskStatus(task1, TaskStatus.INPROGRESS);
        sprint.changeTaskStatus(task2, TaskStatus.INPROGRESS);
        System.out.println("Updated Tasks for Alice: " + sprint.showTasksAssigned(alice));

        // Delayed Tasks
        System.out.println("Delayed Tasks: " + sprint.getDelayedTasks());
    }
}
