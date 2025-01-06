import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class OnlinePolling {

    // User class
    private static class User {
        private final String userId;
        private final String name;

        public User(String userId, String name) {
            this.userId = userId;
            this.name = name;
        }

        public String getUserId() {
            return userId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            User user = (User) obj;
            return Objects.equals(userId, user.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId);
        }
    }

    // Poll class
    private static class Poll {
        private final int pollId;
        private String question;
        private final Map<String, Set<User>> votesMap; // Option -> Users who voted
        private final List<String> options;
        private final User author;
        private final Date createdAt;

        public Poll(int pollId, String question, List<String> options, User author) {
            this.pollId = pollId;
            this.question = question;
            this.options = new ArrayList<>(options);
            this.author = author;
            this.createdAt = new Date();
            this.votesMap = new ConcurrentHashMap<>();
            options.forEach(option -> votesMap.put(option, ConcurrentHashMap.newKeySet()));
        }

        public synchronized boolean updatePoll(String question, List<String> options, User user) {
            if (!user.equals(author)) return false;
            if (options.size() != this.options.size()) return false; // Prevent option count mismatch
            this.question = question;
            this.options.clear();
            this.options.addAll(options);
            votesMap.clear();
            options.forEach(option -> votesMap.put(option, ConcurrentHashMap.newKeySet()));
            return true;
        }

        public synchronized boolean vote(String option, User user) {
            if (!votesMap.containsKey(option)) return false;
            Set<User> voters = votesMap.get(option);
            if (voters.contains(user)) return false; // Prevent duplicate votes
            voters.add(user);
            return true;
        }

        public String viewResults() {
            Map<String, Integer> results = new LinkedHashMap<>();
            votesMap.forEach((option, users) -> results.put(option, users.size()));
            return "Poll ID: " + pollId + "\nQuestion: " + question + "\nResults: " + results;
        }

        public User getAuthor() {
            return author;
        }
    }

    // Poller Singleton
    private static class Poller {
        private static Poller instance;
        private final Map<Integer, Poll> polls;
        private final AtomicInteger pollIdCounter;

        private Poller() {
            this.polls = new ConcurrentHashMap<>();
            this.pollIdCounter = new AtomicInteger(0);
        }

        public static synchronized Poller getInstance() {
            if (instance == null) {
                instance = new Poller();
            }
            return instance;
        }

        public int createPoll(String question, List<String> options, User author) {
            if (question == null || question.isEmpty() || options == null || options.isEmpty()) {
                throw new IllegalArgumentException("Invalid question or options");
            }
            int pollId = pollIdCounter.incrementAndGet();
            Poll poll = new Poll(pollId, question, options, author);
            polls.put(pollId, poll);
            return pollId;
        }

        public boolean updatePoll(int pollId, String question, List<String> options, User user) {
            Poll poll = polls.get(pollId);
            if (poll == null) return false;
            return poll.updatePoll(question, options, user);
        }

        public boolean deletePoll(int pollId, User user) {
            Poll poll = polls.get(pollId);
            if (poll == null || !poll.getAuthor().equals(user)) return false;
            polls.remove(pollId);
            return true;
        }

        public boolean vote(int pollId, User user, String option) {
            Poll poll = polls.get(pollId);
            if (poll == null) return false;
            return poll.vote(option, user);
        }

        public String viewPollResults(int pollId) {
            Poll poll = polls.get(pollId);
            if (poll == null) return "Poll not found";
            return poll.viewResults();
        }
    }

    // Main Method for Testing
    public static void main(String[] args) {
        Poller poller = Poller.getInstance();
        User user1 = new User("1", "Alice");
        User user2 = new User("2", "Bob");

        // Create a poll
        int pollId = poller.createPoll("What is your favorite color?", List.of("Red", "Blue", "Green"), user1);
        System.out.println("Poll created with ID: " + pollId);

        // View poll results
        System.out.println(poller.viewPollResults(pollId));

        // Cast votes
        poller.vote(pollId, user1, "Red");
        poller.vote(pollId, user2, "Blue");

        // View updated poll results
        System.out.println(poller.viewPollResults(pollId));
    }
}
