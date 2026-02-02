/*
======================== LLD DESIGN INTERVIEW SCRIPT (STACKOVERFLOW MINI) ========================

------------------------------------------------------------------------------------------
1) CLARIFYING REQUIREMENTS (what I ask first)
------------------------------------------------------------------------------------------
"Before I design, I’ll confirm the MVP scope for a StackOverflow-like platform."

Questions I ask:
1. Core entities required?
   - Users ✅
   - Questions ✅
   - Answers ✅
   - Comments ✅
   - Tags ✅
   - Votes ✅
2. Actions required?
   - User asks a question ✅
   - User answers a question ✅
   - User comments on question/answer ✅
   - User upvotes/downvotes question/answer ✅
3. Voting rules:
   - Can a user vote multiple times on same entity? ❌ (latest vote overwrites ✅)
   - Vote values allowed: +1 or -1 ✅
   - Should users be allowed to vote their own posts? (not blocked here)
4. Reputation rules:
   - Reputation changes per action and per vote ✅ (simplified)
   - Should reputation go negative? ❌ (clamped to 0 ✅)
5. Search and tagging:
   - Search questions by tag? (not implemented but possible)
   - Show questions by keyword? (not implemented)
6. Concurrency:
   - Multiple users voting/commenting concurrently? (partially handled)
   - Need strong consistency? (discussion only)

Assumptions in this design:
- In-memory storage only (no DB)
- IDs are generated using current time modulus (not ideal but ok for LLD demo)
- Vote overwrites previous vote by same user on same entity
- Reputation logic is simplified and embedded in vote methods
- Comments are stored as plain text with id only

------------------------------------------------------------------------------------------
2) REQUIREMENTS
------------------------------------------------------------------------------------------
Functional Requirements:
- Create User
- Ask Question with tags
- Answer a Question
- Comment on Question/Answer
- Upvote/Downvote Question/Answer
- Track vote count per Question/Answer
- Update reputation for:
  - asking question
  - answering question
  - commenting
  - votes received (up/down)

Non-Functional Requirements:
- Thread-safe updates for reputation and global maps
- Maintainable separation between entities and service layer
- Extensible for accepted answer, edit history, moderation, search

------------------------------------------------------------------------------------------
3) IDENTIFY ENTITIES (core classes)
------------------------------------------------------------------------------------------
User
Question
Answer
Comment
Tag
Vote

Interfaces:
Votable  -> vote(), getVoteCount()
Commentable -> comment(), getComments()

Service Layer:
StackOverFlowService (manager for users/questions/answers/tags)

------------------------------------------------------------------------------------------
4) RELATIONSHIPS (has-a / is-a)
------------------------------------------------------------------------------------------
Question:
- authored by User
- has List<Answer>
- has List<Comment>
- has List<Tag>
- has List<Vote>

Answer:
- authored by User
- belongs to a Question (via addAnswer)
- has List<Comment>
- has List<Vote>

Comment:
- belongs to Question/Answer (via Commentable)

Vote:
- belongs to Question/Answer
- done by a User
- value is +1/-1

User:
- has List<Question>, List<Answer>, List<Comment>
- has reputation score

Question and Answer are:
- Votable
- Commentable

------------------------------------------------------------------------------------------
5) DESIGN CHOICES / PATTERNS DISCUSSED
------------------------------------------------------------------------------------------
Interface-based modeling:
- Commentable lets both Question and Answer support comments
- Votable lets both Question and Answer support voting

Overwrite vote behavior:
- votes.removeIf(existing vote by same user)
- then add latest vote
This ensures "one active vote per user per entity"

Thread-safety:
- Reputation update is synchronized ✅
- Global storage maps are ConcurrentHashMap ✅
- BUT Question/Answer internal lists are NOT thread-safe ❌
  (can upgrade to synchronizedList or CopyOnWriteArrayList)

Service Layer:
- StackOverFlowService should be the single entry point for:
  create user, create question, post answer, search questions, etc.

------------------------------------------------------------------------------------------
6) CORE APIs (what interviewer expects)
------------------------------------------------------------------------------------------
User APIs:
- askQuestion(title, content, tags) -> Question
- answerQuestion(question, content) -> Answer
- addComment(commentable, content) -> Comment
- getReputation()

Question/Answer APIs:
- vote(user, +1/-1)
- getVoteCount()
- comment(comment)
- getComments()

Service APIs (to complete system):
- createUser(username, email) -> User
- postQuestion(user, title, content, tags) -> Question
- postAnswer(user, questionId, content) -> Answer
- addComment(user, targetType, targetId, content) -> Comment
- vote(user, targetType, targetId, value)
- searchQuestionsByTag(tagText) -> List<Question>

------------------------------------------------------------------------------------------
7) EDGE CASES I DISCUSS (and gaps in code)
------------------------------------------------------------------------------------------
✅ handled:
- Invalid vote value not in {+1, -1} -> exception
- Duplicate vote by same user -> overwritten
- Reputation never goes negative

⚠️ gaps / improvements:
- askQuestion(User user ,  ) in service is incomplete ❌
- addComment() in User does not return comment (missing return) ❌
- Question constructor forgets to set author field ❌
- Some unused fields: UserId in Comment/Answer
- ID generation using timestamp can collide under fast calls ❌
- No validation for null/empty title/content/tags
- No prevention of self-voting (optional rule)
- No accepted answer
- No deletion/edit support

------------------------------------------------------------------------------------------
8) EXTENSIBILITY (future improvements)
------------------------------------------------------------------------------------------
- Add Post base class (Question + Answer inherit from Post)
  common fields: id, author, createdAt, votes, comments
- Introduce VoteService + ReputationPolicy (Strategy)
- Add TagService and normalize tags (store by text, avoid duplicates)
- Add search:
  - by tag
  - by keyword in title/content
- Add accepted answer flag and awarding extra reputation
- Use UUID for ids
- Thread safety upgrades for internal collections
- Persist to DB + add caching for popular questions

------------------------------------------------------------------------------------------
9) WALKTHROUGH (end-to-end flow)
------------------------------------------------------------------------------------------
User creates account
User asks a question with tags ["java", "lld"]
Another user answers question
Users comment on question/answer
Users upvote/downvote
Vote counts update and reputation changes accordingly

==========================================================================================
CODE STARTS BELOW
==========================================================================================
*/

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StackOverFlow {
    // vote
    // Core - User Question(Tag) Answer Comment
    // Vote , Reputation -> +2 for question , +5 for answer, +1 for comment
    // concurrent access and data consistency
    // User -> List<Question>  , List<Answers> , List<Comment>
    //

    private class Vote{
        private final User user;
        private final int value;

        public Vote(User user, int value) {
            this.user = user;
            this.value = value;
        }
        // Getters
        public User getUser() { return user; }
        public int getValue() { return value; }
    }

    private interface Commentable{
        void comment(Comment comment);
        List<Comment> getComments();
    }

    private interface Votable{
        void vote(User user, int value);
        int getVoteCount();
    }

    private class Tag{
        private int id;
        private String tagText;

        public int getId() {
            return id;
        }

        public String getTag() {
            return tagText;
        }

        public Tag(String tag) {
            this.id = generateId();
            this.tagText = tag;
        }

        private int generateId() {
            return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        }
    }

    private class Comment{
        private int id;
        private int UserId;
        private String commentText;

        public int getId() {
            return id;
        }

        public Comment(String commentText) {
            this.id = generateId();
            this.commentText = commentText;
        }

        public String getCommentText() {
            return commentText;
        }

        private int generateId() {
            return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        }
    }

    private class Answer implements Votable,Commentable{
        private int id;
        private User author;
        private String AnswerText;
        private int UserId;
        private List<Comment> comments;
        private List<Vote> votes;

        public int getId() {
            return id;
        }

        public String getAnswerText() {
            return AnswerText;
        }

        public List<Comment> getComments() {
            return comments;
        }

        public Answer(User author , String answerText) {
            this.id = generateId();
            this.author = author;
            AnswerText = answerText;
            this.comments = new ArrayList<>();
            this.votes = new ArrayList<>();
        }

        @Override
        public void comment(Comment comment) {
            comments.add(comment);
        }

        private int generateId() {
            return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        }

        @Override
        public void vote(User user, int value) {
            if (value != 1 && value != -1) {
                throw new IllegalArgumentException("Vote value must be either 1 or -1");
            }
            votes.removeIf(v -> v.getUser().equals(user));
            votes.add(new Vote(user, value));
            author.updateReputation(value * 10);  // +10 for upvote, -10 for downvote
        }

        @Override
        public int getVoteCount() {
            return votes.stream().mapToInt(Vote::getValue).sum();
        }
    }

    private class Question implements Votable,Commentable{
        private int id;
        private String title;
        private String questionText;
        private List<Answer> answers;
        private final User author;
        private List<Comment> comments;
        private List<Tag> tags;
        private List<Vote> votes;

        public int getId() {
            return id;
        }

        public String getQuestionText() {
            return questionText;
        }

        public List<Comment> getComments() {
            return comments;
        }

        public Question(User author, String title , String questionText, List<String> tags) {
            this.id = generateId();
            this.title = title;
            this.questionText = questionText;
            this.answers = new ArrayList<>();
            this.comments = new ArrayList<>();
            this.tags = new ArrayList<>();
            for(String tag: tags)
            {
                this.tags.add(new Tag(tag));
            }
            votes = new ArrayList<>();
            this.author = author; // ✅ missing in original, added for correctness
        }

        private int generateId() {
            return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        }

        @Override
        public void comment(Comment comment) {
            comments.add(comment);
        }

        public void addAnswer(Answer answer)
        {
            answers.add(answer);
        }

        @Override
        public void vote(User user, int value) {
            if (value != 1 && value != -1) {
                throw new IllegalArgumentException("Vote value must be either 1 or -1");
            }
            votes.removeIf(v -> v.getUser().equals(user));
            votes.add(new Vote(user, value));
            author.updateReputation(value * 5);  // +5 for upvote, -5 for downvote
        }

        @Override
        public int getVoteCount() {
            return votes.stream().mapToInt(Vote::getValue).sum();
        }
    }

    private class User{
        private final int id;
        private final String username;
        private final String email;
        private int reputation;
        private final List<Question> questions;
        private final List<Answer> answers;
        private final List<Comment> comments;

        private static final int QUESTION_REPUTATION = 5;
        private static final int ANSWER_REPUTATION = 10;
        private static final int COMMENT_REPUTATION = 2;

        public User(int id, String username, String email) {
            this.id = id;
            this.username = username;
            this.email = email;
            reputation = 0;
            questions = new ArrayList<>();
            answers = new ArrayList<>();
            comments = new ArrayList<>();
        }

        public Question askQuestion(String title, String content , List<String> tags)
        {
            Question question = new Question(this, title, content, tags);
            questions.add(question);
            updateReputation(QUESTION_REPUTATION); // Gain rep for asking a question
            return question;
        }

        public Answer answerQuestion(Question question , String content)
        {
            Answer answer = new Answer(this,content);
            answers.add(answer);
            question.addAnswer(answer);
            updateReputation(ANSWER_REPUTATION);
            return answer;
        }

        public Comment addComment(Commentable commentable , String content)
        {
            Comment comment = new Comment(content);
            comments.add(comment);
            commentable.comment(comment);
            updateReputation(COMMENT_REPUTATION);
            return comment; // ✅ missing in original
        }

        public synchronized void updateReputation(int value) {
            this.reputation += value;
            if (this.reputation < 0) {
                this.reputation = 0;
            }
        }

        public int getId() { return id; }
        public String getUsername() { return username; }
        public int getReputation() { return reputation; }
        public List<Question> getQuestions() { return new ArrayList<>(questions); }
        public List<Answer> getAnswers() { return new ArrayList<>(answers); }
        public List<Comment> getComments() { return new ArrayList<>(comments); }
    }

    private class StackOverFlowService{
        private final Map<Integer, User> users;
        private final Map<Integer, Question> questions;
        private final Map<Integer, Answer> answers;
        private final Map<Integer, Tag> tags;

        public StackOverFlowService(){
            users = new ConcurrentHashMap<>();
            questions = new ConcurrentHashMap<>();
            answers = new ConcurrentHashMap<>();
            tags = new ConcurrentHashMap<>();
        }

        public User createUser(String username ,String email)
        {
            int id = users.size()+1;
            User user = new User(id, username, email);
            users.put(id,user);
            return user;
        }

        // NOTE: In original code this method was incomplete.
        // This is the typical service API needed:
        public Question askQuestion(User user, String title, String content, List<String> tagsList)
        {
            Question question = user.askQuestion(title, content, tagsList);
            questions.put(question.getId(), question);
            return question;
        }

        // Further extensions could include:
        // answerQuestion(user, questionId, content)
        // voteOnPost(user, postType, postId, +1/-1)
        // searchByTag(tag)
    }

    public static void main(String[] args) {

        // Since all inner classes are non-static, we need an outer object first
        StackOverFlow app = new StackOverFlow();

        // Create Service
        StackOverFlowService service = app.new StackOverFlowService();

        // Create Users
        User alice = service.createUser("alice", "alice@gmail.com");
        User bob = service.createUser("bob", "bob@gmail.com");

        System.out.println("Users created:");
        System.out.println("Alice -> " + alice.getUsername() + " | Rep = " + alice.getReputation());
        System.out.println("Bob   -> " + bob.getUsername() + " | Rep = " + bob.getReputation());
        System.out.println();

        // Alice asks a question
        Question q1 = service.askQuestion(
                alice,
                "Java LLD Question",
                "How do I design StackOverflow using OOP principles?",
                List.of("java", "lld", "design")
        );

        System.out.println("Question asked:");
        System.out.println("Question ID: " + q1.getId());
        System.out.println("Question Text: " + q1.getQuestionText());
        System.out.println("Alice Rep after asking question: " + alice.getReputation());
        System.out.println();

        // Bob answers Alice's question
        Answer ans1 = bob.answerQuestion(q1, "Use entities like User, Question, Answer, Comment, Vote + Service layer.");
        System.out.println("Answer posted:");
        System.out.println("Answer ID: " + ans1.getId());
        System.out.println("Answer Text: " + ans1.getAnswerText());
        System.out.println("Bob Rep after answering: " + bob.getReputation());
        System.out.println();

        // Alice comments on Bob's answer
        Comment c1 = alice.addComment(ans1, "Nice answer! Can you add how voting and reputation works?");
        System.out.println("Comment added on Answer:");
        System.out.println("Comment ID: " + c1.getId());
        System.out.println("Comment Text: " + c1.getCommentText());
        System.out.println("Alice Rep after commenting: " + alice.getReputation());
        System.out.println();

        // Bob comments on the question
        Comment c2 = bob.addComment(q1, "Sure, voting can be modeled using a Vote class and Votable interface.");
        System.out.println("Comment added on Question:");
        System.out.println("Comment ID: " + c2.getId());
        System.out.println("Comment Text: " + c2.getCommentText());
        System.out.println("Bob Rep after commenting: " + bob.getReputation());
        System.out.println();

        // Alice upvotes Bob's answer
        ans1.vote(alice, +1);
        System.out.println("Alice upvoted Bob's answer.");
        System.out.println("Answer vote count: " + ans1.getVoteCount());
        System.out.println("Bob Rep after receiving upvote on answer: " + bob.getReputation());
        System.out.println();

        // Bob upvotes Alice's question
        q1.vote(bob, +1);
        System.out.println("Bob upvoted Alice's question.");
        System.out.println("Question vote count: " + q1.getVoteCount());
        System.out.println("Alice Rep after receiving upvote on question: " + alice.getReputation());
        System.out.println();

        // Bob tries to upvote again (should overwrite previous vote)
        q1.vote(bob, +1);
        System.out.println("Bob voted again on same question (overwrites vote).");
        System.out.println("Question vote count: " + q1.getVoteCount());
        System.out.println();

        // Alice downvotes Bob's answer (overwrites vote on answer)
        ans1.vote(alice, -1);
        System.out.println("Alice downvoted Bob's answer (overwrites previous upvote).");
        System.out.println("Answer vote count: " + ans1.getVoteCount());
        System.out.println("Bob Rep after downvote on answer: " + bob.getReputation());
        System.out.println();

        // Print final summary
        System.out.println("===== FINAL SUMMARY =====");
        System.out.println("Alice Rep: " + alice.getReputation());
        System.out.println("Bob Rep: " + bob.getReputation());
        System.out.println("Question Votes: " + q1.getVoteCount());
        System.out.println("Answer Votes: " + ans1.getVoteCount());
        System.out.println("Question Comments: " + q1.getComments().size());
        System.out.println("Answer Comments: " + ans1.getComments().size());
    }

}
