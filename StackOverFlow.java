import java.util.ArrayList;
import java.util.List;

public class StackOverFlow {
    // Core - User Question(Tag) Answer Comment 
    // Vote , Reputation -> +2 for question , +5 for answer, +1 for comment
    // concurrent access and data consistency
    // User -> List<Question>  , List<Answers> , List<Comment> 
    // 

    private interface Commentable{
        public void comment(Comment comment);
        List<Comment> getComments();

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

        public Tag(int id, String tag) {
            this.id = id;
            this.tagText = tag;
        }
        
    }

    private class Comment{
        private int id;
        private int UserId;
        private String commentText;
        public int getId() {
            return id;
        }
        public Comment(int id, String commentText) {
            this.id = id;
            this.commentText = commentText;
        }
        public String getCommentText() {
            return commentText;
        }
    }

    private class Answer implements Commentable{
        private int id;
        private String AnswerText;
        private int UserId;
        private List<Comment> comments;
        public int getId() {
            return id;
        }
        public String getAnswerText() {
            return AnswerText;
        }
        public List<Comment> getComments() {
            return comments;
        }
        public Answer(int id, String answerText) {
            this.id = id;
            AnswerText = answerText;
            this.comments = new ArrayList<>();
        }
        

    }




    

}
