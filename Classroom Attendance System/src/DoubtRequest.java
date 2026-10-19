import java.time.LocalDateTime;

class DoubtRequest {
    final int id;
    final int studentId;
    final String message;
    final LocalDateTime createdAt;
    String reply;
    LocalDateTime repliedAt;

    DoubtRequest(int id, int studentId, String message, LocalDateTime createdAt) {
        this(id, studentId, message, createdAt, "", null);
    }

    DoubtRequest(int id, int studentId, String message, LocalDateTime createdAt, String reply,
            LocalDateTime repliedAt) {
        this.id = id;
        this.studentId = studentId;
        this.message = message;
        this.createdAt = createdAt;
        this.reply = reply;
        this.repliedAt = repliedAt;
    }
}