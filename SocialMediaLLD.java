import java.util.*;

public class SocialMediaLLD {

    private static  class User {
        private int userId;
        private Set<Integer> following;

        public User(int userId) {
            this.userId = userId;
            this.following = new HashSet<>();
        }

        public void follow(int followeeId) {
            following.add(followeeId);
        }

        public void unfollow(int followeeId) {
            following.remove(followeeId);
        }

        public Set<Integer> getFollowing() {
            return following;
        }
    }

    private static class Post {
        private int postId;
        private int userId;
        private long timestamp;

        public Post(int postId, int userId, long timestamp) {
            this.postId = postId;
            this.userId = userId;
            this.timestamp = timestamp;
        }

        public int getUserId() {
            return userId;
        }

        public int getPostId() {
            return postId;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    private interface UserService {
        void follow(int followerId, int followeeId);

        void unfollow(int followerId, int followeeId);

        Set<Integer> getFollowers(int userId);
    }

    private interface PostService {
        void createPost(int userId, int postId);

        void deletePost(int postId);

        List<Integer> getUserPosts(int userId);

        Post getPostById(int postId);
    }

    private interface FeedService {
        List<Integer> getNewsFeed(int userId);

        List<Integer> getNewsFeedPaginated(int userId, int pageNumber);
    }

    private static class UserServiceImpl implements UserService {

        private Map<Integer, User> UserMap = new HashMap<>();

        @Override
        public void follow(int followerId, int followeeId) {
            UserMap.putIfAbsent(followerId, new User(followerId));
            UserMap.putIfAbsent(followeeId, new User(followeeId));
            UserMap.get(followerId).follow(followeeId);
        }

        @Override
        public void unfollow(int followerId, int followeeId) {
            if (UserMap.containsKey(followerId)) {
                UserMap.get(followerId).unfollow(followeeId);
            }
        }

        @Override
        public Set<Integer> getFollowers(int userId) {
            return UserMap.getOrDefault(userId, new User(userId)).getFollowing();
        }
    }

    private static class PostServiceImpl implements PostService {

        private Map<Integer, Post> PostsMap = new HashMap<>();
        private Map<Integer, List<Integer>> userPostsMap = new HashMap<>();

        @Override
        public void createPost(int userId, int postId) {
            Post post = new Post(postId, userId, System.currentTimeMillis());
            PostsMap.put(postId, post);
            userPostsMap.putIfAbsent(userId, new ArrayList<>());
            userPostsMap.get(userId).add(postId);
        }

        @Override
        public void deletePost(int postId) {
            Post post = PostsMap.remove(postId);
            if (post != null) {
                userPostsMap.get(post.getUserId()).remove(Integer.valueOf(postId));
            }
        }

        @Override
        public List<Integer> getUserPosts(int userId) {
            return userPostsMap.getOrDefault(userId, new ArrayList<>());
        }

        @Override
        public Post getPostById(int postId) {
            return PostsMap.get(postId);
        }
    }

    private static class FeedServiceImpl implements FeedService {

        private UserService userService;
        private PostService postService;

        public FeedServiceImpl(UserService userService, PostService postService) {
            this.userService = userService;
            this.postService = postService;
        }

        @Override
        public List<Integer> getNewsFeed(int userId) {
            List<Integer> feed = new ArrayList<>();
            Set<Integer> followers = userService.getFollowers(userId);

            for (int followerId : followers) {
                feed.addAll(postService.getUserPosts(followerId));
            }

            feed.sort((a, b) -> Long.compare(postService.getPostById(b).getTimestamp(),
                    postService.getPostById(a).getTimestamp()));
            return feed.subList(0, Math.min(feed.size(), 10));
        }

        @Override
        public List<Integer> getNewsFeedPaginated(int userId, int pageNumber) {
            List<Integer> feed = getNewsFeed(userId);
            int pageSize = 10;
            int start = (pageNumber - 1) * pageSize;
            int end = Math.min(start + pageSize, feed.size());
            return (start >= end) ? new ArrayList<>() : feed.subList(start, end);
        }
    }

    public static void main(String[] args) {
        // Initialize services
        UserService userService = new UserServiceImpl();
        PostService postService = new PostServiceImpl();
        FeedService feedService = new FeedServiceImpl(userService, postService);

        // Example operations
        System.out.println("=== Social Media Platform ===");

        // Create users and follow relationships
        userService.follow(1, 2); // User 1 follows User 2
        userService.follow(1, 3); // User 1 follows User 3
        userService.follow(2, 3); // User 2 follows User 3

        System.out.println("User 1 is following: " + userService.getFollowers(1)); // Expected: [2, 3]
        System.out.println("User 2 is following: " + userService.getFollowers(2)); // Expected: [3]

        // Create posts
        postService.createPost(2, 101); // User 2 posts with postId 101
        postService.createPost(2, 102); // User 2 posts with postId 102
        postService.createPost(3, 201); // User 3 posts with postId 201
        postService.createPost(3, 202); // User 3 posts with postId 202

        // Get user-specific posts
        System.out.println("User 2's posts: " + postService.getUserPosts(2)); // Expected: [101, 102]
        System.out.println("User 3's posts: " + postService.getUserPosts(3)); // Expected: [201, 202]

        // Get feed for User 1
        List<Integer> user1Feed = feedService.getNewsFeed(1);
        System.out.println("User 1's news feed: " + user1Feed); // Expected: [202, 201, 102, 101] (sorted by time)

        // Paginated feed for User 1
        List<Integer> user1FeedPage1 = feedService.getNewsFeedPaginated(1, 1);
        System.out.println("User 1's paginated feed (Page 1): " + user1FeedPage1); // First 10 items

        // Delete a post and verify changes in feed
        postService.deletePost(101);
        System.out.println("User 2's posts after deletion: " + postService.getUserPosts(2)); // Expected: [102]
        System.out.println("User 1's news feed after deletion: " + feedService.getNewsFeed(1)); // Expected: [202, 201, 102]

        // Unfollow a user and verify changes
        userService.unfollow(1, 3); // User 1 unfollows User 3
        System.out.println("User 1's following after unfollowing User 3: " + userService.getFollowers(1)); // Expected: [2]
        System.out.println("User 1's news feed after unfollowing User 3: " + feedService.getNewsFeed(1)); // Expected: [102]
    }

}
