import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public final class Database {
    private static final String URL = setting("ATTENDANCE_DB_URL",
            "jdbc:mysql://localhost:3306/classroom");
    private static final String USER = setting("ATTENDANCE_DB_USER", "root");
    private static final String PASSWORD = setting("ATTENDANCE_DB_PASSWORD", "krishna");

    private Database() {}

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }

    public static Connection connect() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new SQLException("MySQL Connector/J is missing from the project libraries.", exception);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void initialize() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS teacher_accounts (" +
                    "username VARCHAR(80) PRIMARY KEY, name VARCHAR(160) NOT NULL, password_hash CHAR(64) NOT NULL) ENGINE=InnoDB");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS students (" +
                    "id INT PRIMARY KEY, student_number VARCHAR(50) NOT NULL UNIQUE, full_name VARCHAR(160) NOT NULL, " +
                    "password_hash CHAR(64) NOT NULL) ENGINE=InnoDB");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS courses (" +
                    "id INT PRIMARY KEY, course_code VARCHAR(50) NOT NULL UNIQUE, name VARCHAR(180) NOT NULL, " +
                    "instructor VARCHAR(160) NOT NULL DEFAULT '') ENGINE=InnoDB");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS course_students (" +
                    "course_id INT NOT NULL, student_id INT NOT NULL, PRIMARY KEY(course_id, student_id), " +
                    "FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE) ENGINE=InnoDB");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS class_sessions (" +
                    "id INT PRIMARY KEY, course_id INT NOT NULL, session_date DATE NOT NULL, start_time TIME NULL, " +
                    "topic VARCHAR(300) NOT NULL DEFAULT '', FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE CASCADE) ENGINE=InnoDB");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS attendance_records (" +
                    "session_id INT NOT NULL, student_id INT NOT NULL, status VARCHAR(20) NOT NULL, " +
                    "PRIMARY KEY(session_id, student_id), FOREIGN KEY(session_id) REFERENCES class_sessions(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE) ENGINE=InnoDB");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS doubt_requests (" +
                    "id INT PRIMARY KEY, student_id INT NOT NULL, message TEXT NOT NULL, created_at DATETIME NOT NULL, " +
                    "reply TEXT NOT NULL, replied_at DATETIME NULL, FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE) ENGINE=InnoDB");
        }
    }
}
