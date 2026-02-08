/*
======================== LLD DESIGN INTERVIEW SCRIPT (SPRINT PLANNER / JIRA-LITE) ========================

------------------------------------------------------------------------------------------
1) CLARIFYING REQUIREMENTS (what I ask first)
------------------------------------------------------------------------------------------
"Before I design, I want to confirm the scope and rules for this sprint planning system."

Questions I ask:
1. Main features required in MVP?
   - Create sprint ✅
   - Add/remove tasks in sprint ✅
   - Assign tasks to users ✅
   - Move task status (TODO / INPROGRESS / DONE) ✅
   - View tasks assigned to a user ✅
   - Track delayed tasks ✅
2. Capacity constraints:
   - Max tasks per sprint? ✅ (20 in this design)
   - Max INPROGRESS tasks per user? ✅ (2 here)
3. Task transitions:
   - Allowed transitions? (TODO <-> INPROGRESS, INPROGRESS -> DONE ✅)
   - Should TODO -> DONE directly be allowed? ❌ (not allowed here)
4. Time tracking:
   - Task has start and end time ✅
   - Delay = endTime < now ✅
   - Sprint has start and end time ✅
5. Multi-sprint support needed? (not implemented: only one sprint in main demo)
6. Concurrency:
   - Multiple users updating tasks simultaneously? ✅ (basic synchronization is used)

Assumptions in this implementation:
- In-memory only
- One sprint object handles task operations
- Task identity is object reference (no taskId)
- No persistence, no audit logs, no comments, no priority/story points

------------------------------------------------------------------------------------------
2) REQUIREMENTS
------------------------------------------------------------------------------------------
Functional Requirements:
- Create Sprint with goal + startTime + endTime
- Add tasks to sprint (limit 20)
- Remove tasks from sprint
- View tasks assigned to a specific user
- Change task status with valid workflow rules
- Enforce per-user INPROGRESS limit (max 2)
- View delayed tasks

Non-Functional Requirements:
- Thread safety for sprint operations
- Maintainability: clear separation of Task and Sprint logic
- Extensible: add priorities, story points, labels, dependencies

------------------------------------------------------------------------------------------
3) IDENTIFY ENTITIES (core classes)
------------------------------------------------------------------------------------------
User
Task
Sprint

Enums:
TaskType (STORY, FEATURE, BUG)
TaskStatus (TODO, INPROGRESS, DONE)

SprintPlanner (driver class here)

------------------------------------------------------------------------------------------
4) RELATIONSHIPS (has-a / is-a)
------------------------------------------------------------------------------------------
Sprint has:
- List<Task> tasks
- Map<User, List<Task>> userTasks (reverse index)

Task has:
- assignedUser
- taskType
- taskStatus
- startTime/endTime

User is uniquely represented by userDetails string
(Note: in real design we’d have userId, equals/hashCode)

------------------------------------------------------------------------------------------
5) DESIGN CHOICES / PATTERNS DISCUSSED
------------------------------------------------------------------------------------------
Encapsulation of rules inside Task:
- Task.changeStatus() enforces valid transitions

Sprint-level business rules:
- Sprint enforces MAX_SPRINT_CAPACITY
- Sprint enforces MAX_INPROGRESS_TASKS per user

Thread safety:
- tasks list is synchronizedList
- userTasks map is ConcurrentHashMap
- key modifying methods are synchronized (add/remove/change status)

Indexing:
- userTasks map makes it fast to get tasks assigned to a user
  instead of scanning entire sprint each time

------------------------------------------------------------------------------------------
6) CORE APIs (entry points)
------------------------------------------------------------------------------------------
Sprint:
- addTask(task) -> boolean
- removeTask(task) -> boolean
- showTasksAssigned(user) -> List<Task>
- changeTaskStatus(task, newStatus) -> boolean
- getDelayedTasks() -> List<Task>

Task:
- changeStatus(newStatus) -> boolean
- isDelayed() -> boolean

------------------------------------------------------------------------------------------
7) EDGE CASES I DISCUSS (and handled here)
------------------------------------------------------------------------------------------
- Adding task when sprint is full -> false ✅
- Removing non-existing task -> false ✅
- Changing status for task not in sprint -> false ✅
- Invalid transition (TODO -> DONE directly) -> false ✅
- User exceeding INPROGRESS limit -> false ✅
- Delayed tasks computed using endTime < current time ✅

Limitations to mention:
- User class has no equals/hashCode:
  - Using User objects as map keys may break if different User objects represent same person ❌
- Task has no taskId:
  - removing tasks depends on object reference equality ❌
- No concept of sprint backlog vs sprint tasks
- No validation that task endTime is within sprint endTime

------------------------------------------------------------------------------------------
8) EXTENSIBILITY (what can be added next)
------------------------------------------------------------------------------------------
- Add taskId + sprintId (UUID)
- Add story points, priority, labels
- Add task dependencies (blocked-by)
- Add comments, activity logs, status history
- Add multiple sprints + planner service layer
- Add user identity system (userId based equals/hashCode)
- Add filtering: showTasksByStatus, showTasksByType
- Add burndown metrics / progress percentage

------------------------------------------------------------------------------------------
9) WALKTHROUGH (validate the flow)
------------------------------------------------------------------------------------------
Create sprint "Complete MVP"
Create tasks and assign to Alice & Bob
Add tasks to sprint
Alice moves two tasks to INPROGRESS (allowed)
Third INPROGRESS for Alice would be rejected
Delayed tasks are detected by comparing endTime with current time

==========================================================================================
CODE STARTS BELOW
==========================================================================================
*/

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
