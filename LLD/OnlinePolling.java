/*
======================== LLD DESIGN INTERVIEW SCRIPT (ONLINE POLLING SYSTEM) ========================

------------------------------------------------------------------------------------------
1) CLARIFYING REQUIREMENTS (what I ask first)
------------------------------------------------------------------------------------------
"Before I start designing, I’ll clarify the required features and constraints."

Questions I ask:
1. Poll lifecycle:
   - Can users create polls? ✅
   - Can polls be updated? ✅ (only by author)
   - Can polls be deleted? ✅ (only by author)
2. Voting rules:
   - Is multiple voting allowed? ❌ (one vote per option per user in current code)
   - Should user be allowed to change vote? (not supported here)
   - Should user be allowed to vote multiple options? (not supported here)
3. Poll configuration:
   - Fixed options count or options can be changed? (this code restricts option count changes)
   - Should poll allow adding/removing options? (not allowed here)
4. Visibility:
   - Can anyone view results or only author? (anyone can view here)
5. Expiry:
   - Should polls have an end time? (not implemented)
6. Scale:
   - How many polls/users expected? (in-memory is fine for LLD interview)
7. Concurrency:
   - Multiple users can vote at same time? ✅ (thread-safe collections + synchronized)

Assumptions in this implementation:
- In-memory storage using ConcurrentHashMap
- Poll IDs are auto generated sequentially
- Only author can update/delete poll
- Voting prevents duplicate votes for the same option by same user
- No authentication layer (User object is passed in directly)

------------------------------------------------------------------------------------------
2) REQUIREMENTS
------------------------------------------------------------------------------------------
Functional Requirements:
- Create a poll with question + list of options
- Update poll question/options (author only)
- Delete poll (author only)
- Vote on a poll option
- View poll results (option -> vote count)

Non-Functional Requirements:
- Thread safety: multiple votes concurrently should remain correct
- Extensible: allow future features like vote change, poll expiry, anonymous voting
- Maintainable: clean separation of Poll vs Poller(service)

------------------------------------------------------------------------------------------
3) IDENTIFY ENTITIES (core classes)
------------------------------------------------------------------------------------------
User
Poll
Poller (singleton service / manager)

Data structures:
votesMap: option -> Set<User>
options: List<String>
polls: pollId -> Poll

------------------------------------------------------------------------------------------
4) RELATIONSHIPS (has-a / is-a)
------------------------------------------------------------------------------------------
Poller has Map<Integer, Poll> polls
Poll has:
- question
- options
- votesMap (option -> set of users)
- author
- createdAt

User is identified uniquely by userId (equals/hashCode overridden)

------------------------------------------------------------------------------------------
5) DESIGN CHOICES / PATTERNS DISCUSSED
------------------------------------------------------------------------------------------
Singleton Pattern:
- Poller is singleton: centralized poll storage and operations

Thread Safety:
- polls stored in ConcurrentHashMap
- votesMap stored in ConcurrentHashMap
- each option's voters are stored in ConcurrentHashMap.newKeySet()
- Poll methods vote() and updatePoll() are synchronized for atomicity

Data modeling choice:
- votesMap stores actual User objects to prevent duplicates via Set semantics
  (Alternative: store only userId strings for memory optimization)

------------------------------------------------------------------------------------------
6) CORE APIs (entry points)
------------------------------------------------------------------------------------------
Poller.createPoll(question, options, author) -> pollId
Poller.updatePoll(pollId, question, options, user) -> boolean
Poller.deletePoll(pollId, user) -> boolean
Poller.vote(pollId, user, option) -> boolean
Poller.viewPollResults(pollId) -> String

Poll internal:
Poll.updatePoll(question, options, user) -> boolean
Poll.vote(option, user) -> boolean
Poll.viewResults() -> String

------------------------------------------------------------------------------------------
7) EDGE CASES I DISCUSS (and handled here)
------------------------------------------------------------------------------------------
- Invalid pollId -> false / "Poll not found" ✅
- Invalid question/options at create -> IllegalArgumentException ✅
- Voting on invalid option -> false ✅
- Duplicate vote attempt -> false ✅
- Unauthorized update/delete -> false ✅
- Updating poll with mismatched option count -> false ✅
  (prevents breaking vote structure)

Limitations / Improvements:
- Vote changing is not supported
- "One vote total per poll" is not enforced, only one vote per option
- No poll close/expiry logic
- updatePoll currently resets all votes (could be discussed as product decision)

------------------------------------------------------------------------------------------
8) EXTENSIBILITY (future improvements)
------------------------------------------------------------------------------------------
- Enforce "one vote per poll total" (track votedUserIds at poll level)
- Allow vote change (remove from old option + add to new option)
- Add poll expiry time and prevent vote after end time
- Anonymous polls (store only counts, not users)
- Add persistence using DB and caching
- Add API layer (REST endpoints)
- Add pagination/search on polls

------------------------------------------------------------------------------------------
9) WALKTHROUGH (example flow)
------------------------------------------------------------------------------------------
User1 creates poll with options [Red, Blue, Green]
Both users vote once
System stores each vote in votesMap[option].add(user)
Viewing results aggregates size of each voters set

==========================================================================================
CODE STARTS BELOW
==========================================================================================
*/

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
