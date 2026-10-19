import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tomcat Servlet that handles /api/state, stores the JSON state in web_app_state,
 * AND synchronizes all 7 relational tables (students, courses, class_sessions, etc.) in MySQL!
 */
@WebServlet("/api/state")
public class WebBackend extends HttpServlet {

    private static final String EMPTY_STATE = "{\"students\":[],\"courses\":[],\"sessions\":[],\"doubts\":[],\"teachers\":[],\"ids\":{\"student\":1,\"course\":1,\"session\":1,\"doubt\":1},\"threshold\":75}";

    @Override
    public void init() throws ServletException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Database.initialize();
            try (Connection connection = Database.connect();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS web_app_state (" +
                        "state_id TINYINT PRIMARY KEY, state_json LONGTEXT NOT NULL) ENGINE=InnoDB");
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT IGNORE INTO web_app_state(state_id,state_json) VALUES(1,?)")) {
                    insert.setString(1, EMPTY_STATE);
                    insert.executeUpdate();
                }
            }
            System.out.println("WebBackend Servlet initialized and connected to MySQL successfully!");
        } catch (ClassNotFoundException e) {
            throw new ServletException("MySQL JDBC Driver not found in Tomcat lib!", e);
        } catch (SQLException e) {
            throw new ServletException("Failed to initialize MySQL database on Tomcat startup!", e);
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        addCors(resp);
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String json = readState();
            send(resp, HttpServletResponse.SC_OK, "application/json; charset=utf-8", json);
        } catch (SQLException exception) {
            exception.printStackTrace();
            send(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "application/json; charset=utf-8", "{\"error\":\"MySQL operation failed\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String json = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (json.length() < 2 || json.charAt(0) != '{' || json.charAt(json.length() - 1) != '}') {
                send(resp, HttpServletResponse.SC_BAD_REQUEST, "application/json; charset=utf-8", "{\"error\":\"Invalid state data\"}");
                return;
            }
            saveStateAndSyncTables(json);
            send(resp, HttpServletResponse.SC_OK, "application/json; charset=utf-8", "{\"saved\":true}");
        } catch (Exception exception) {
            exception.printStackTrace();
            send(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "application/json; charset=utf-8", "{\"error\":\"MySQL operation failed\"}");
        }
    }

    private static String readState() throws SQLException {
        try (Connection connection = Database.connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT state_json FROM web_app_state WHERE state_id=1")) {
            return result.next() ? result.getString(1) : EMPTY_STATE;
        }
    }

    @SuppressWarnings("unchecked")
    private static void saveStateAndSyncTables(String json) throws SQLException {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                // 1. Save raw JSON state in web_app_state
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO web_app_state(state_id,state_json) VALUES(1,?) " +
                                "ON DUPLICATE KEY UPDATE state_json=VALUES(state_json)")) {
                    statement.setString(1, json);
                    statement.executeUpdate();
                }

                // 2. Parse JSON and sync into the 7 relational tables
                Object parsed = new SimpleJsonParser(json).parse();
                if (parsed instanceof Map<?, ?> root) {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("DELETE FROM doubt_requests");
                        st.executeUpdate("DELETE FROM attendance_records");
                        st.executeUpdate("DELETE FROM class_sessions");
                        st.executeUpdate("DELETE FROM course_students");
                        st.executeUpdate("DELETE FROM students");
                        st.executeUpdate("DELETE FROM courses");
                        st.executeUpdate("DELETE FROM teacher_accounts");
                    }

                    // Sync teacher_accounts
                    List<?> teachers = asList(root.get("teachers"));
                    try (PreparedStatement ps = connection.prepareStatement(
                            "INSERT IGNORE INTO teacher_accounts(username,name,password_hash) VALUES(?,?,?)")) {
                        for (Object item : teachers) {
                            if (item instanceof Map<?, ?> t) {
                                String username = str(t, "username", str(t, "user", ""));
                                if (username.isBlank()) continue;
                                String name = str(t, "name", username);
                                String pass = str(t, "passwordHash", str(t, "password", "teach123"));
                                ps.setString(1, username);
                                ps.setString(2, name);
                                ps.setString(3, ensureHash(pass));
                                ps.addBatch();
                            }
                        }
                        ps.executeBatch();
                    }

                    // Sync students (Auto-increments ID if missing, checks all field names)
                    List<?> students = asList(root.get("students"));
                    try (PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO students(id,student_number,full_name,password_hash) VALUES(?,?,?,?) " +
                                    "ON DUPLICATE KEY UPDATE student_number=VALUES(student_number), full_name=VALUES(full_name)")) {
                        int autoId = 1;
                        for (Object item : students) {
                            if (item instanceof Map<?, ?> s) {
                                int id = num(s, "id", autoId);
                                String number = str(s, "studentNumber", str(s, "number", str(s, "studentNo", str(s, "roll", String.valueOf(id)))));
                                String name = str(s, "name", str(s, "fullName", "Student " + id));
                                String pass = str(s, "passwordHash", str(s, "password", number));
                                ps.setInt(1, id);
                                ps.setString(2, number);
                                ps.setString(3, name);
                                ps.setString(4, ensureHash(pass));
                                ps.addBatch();
                                autoId++;
                            }
                        }
                        ps.executeBatch();
                    }

                    // Sync courses & course_students
                    List<?> courses = asList(root.get("courses"));
                    try (PreparedStatement psCourse = connection.prepareStatement(
                            "INSERT INTO courses(id,course_code,name,instructor) VALUES(?,?,?,?) " +
                                    "ON DUPLICATE KEY UPDATE course_code=VALUES(course_code), name=VALUES(name), instructor=VALUES(instructor)");
                         PreparedStatement psEnroll = connection.prepareStatement(
                                 "INSERT IGNORE INTO course_students(course_id,student_id) VALUES(?,?)")) {
                        int autoCourseId = 1;
                        for (Object item : courses) {
                            if (item instanceof Map<?, ?> c) {
                                int courseId = num(c, "id", autoCourseId++);
                                String code = str(c, "code", str(c, "courseCode", "COURSE-" + courseId));
                                String name = str(c, "name", str(c, "title", code));
                                String instructor = str(c, "instructor", str(c, "teacher", ""));
                                psCourse.setInt(1, courseId);
                                psCourse.setString(2, code);
                                psCourse.setString(3, name);
                                psCourse.setString(4, instructor);
                                psCourse.addBatch();

                                List<?> studentIds = asList(c.get("studentIds") != null ? c.get("studentIds") : c.get("students"));
                                for (Object sidObj : studentIds) {
                                    int sid = toInt(sidObj, 0);
                                    if (sid > 0) {
                                        psEnroll.setInt(1, courseId);
                                        psEnroll.setInt(2, sid);
                                        psEnroll.addBatch();
                                    }
                                }
                            }
                        }
                        psCourse.executeBatch();
                        psEnroll.executeBatch();
                    }

                    // Sync class_sessions & attendance_records
                    List<?> sessions = asList(root.get("sessions"));
                    try (PreparedStatement psSession = connection.prepareStatement(
                            "INSERT IGNORE INTO class_sessions(id,course_id,session_date,start_time,topic) VALUES(?,?,?,?,?)");
                         PreparedStatement psAtt = connection.prepareStatement(
                                 "INSERT IGNORE INTO attendance_records(session_id,student_id,status) VALUES(?,?,?)")) {
                        int autoSessionId = 1;
                        for (Object item : sessions) {
                            if (item instanceof Map<?, ?> ses) {
                                int sessionId = num(ses, "id", autoSessionId++);
                                int courseId = num(ses, "courseId", 0);
                                if (courseId == 0 && ses.get("course") instanceof Map<?, ?> cMap) {
                                    courseId = num(cMap, "id", 1);
                                }
                                String dateStr = str(ses, "date", str(ses, "sessionDate", LocalDate.now().toString()));
                                String timeStr = str(ses, "time", str(ses, "startTime", ""));
                                String topic = str(ses, "topic", "");

                                psSession.setInt(1, sessionId);
                                psSession.setInt(2, courseId);
                                psSession.setDate(3, parseSqlDate(dateStr));
                                Time sqlTime = parseSqlTime(timeStr);
                                if (sqlTime == null) psSession.setNull(4, Types.TIME);
                                else psSession.setTime(4, sqlTime);
                                psSession.setString(5, topic);
                                psSession.addBatch();

                                Object attObj = ses.get("attendance") != null ? ses.get("attendance") : ses.get("records");
                                if (attObj instanceof Map<?, ?> attMap) {
                                    for (Map.Entry<?, ?> entry : attMap.entrySet()) {
                                        int studentId = toInt(entry.getKey(), 0);
                                        String status = normalizeStatus(String.valueOf(entry.getValue()));
                                        if (studentId > 0 && !status.isEmpty()) {
                                            psAtt.setInt(1, sessionId);
                                            psAtt.setInt(2, studentId);
                                            psAtt.setString(3, status);
                                            psAtt.addBatch();
                                        }
                                    }
                                }
                            }
                        }
                        psSession.executeBatch();
                        psAtt.executeBatch();
                    }

                    // Sync doubt_requests
                    List<?> doubts = asList(root.get("doubts"));
                    try (PreparedStatement psDoubt = connection.prepareStatement(
                            "INSERT IGNORE INTO doubt_requests(id,student_id,message,created_at,reply,replied_at) VALUES(?,?,?,?,?,?)")) {
                        int autoDoubtId = 1;
                        for (Object item : doubts) {
                            if (item instanceof Map<?, ?> d) {
                                int id = num(d, "id", autoDoubtId++);
                                int studentId = num(d, "studentId", 1);
                                String message = str(d, "message", str(d, "text", str(d, "question", "")));
                                String createdAt = str(d, "createdAt", str(d, "date", ""));
                                String reply = str(d, "reply", str(d, "answer", ""));
                                String repliedAt = str(d, "repliedAt", "");

                                psDoubt.setInt(1, id);
                                psDoubt.setInt(2, studentId);
                                psDoubt.setString(3, message);
                                psDoubt.setTimestamp(4, parseSqlTimestamp(createdAt, true));
                                psDoubt.setString(5, reply);
                                Timestamp repTs = parseSqlTimestamp(repliedAt, false);
                                if (repTs == null) psDoubt.setNull(6, Types.TIMESTAMP);
                                else psDoubt.setTimestamp(6, repTs);
                                psDoubt.addBatch();
                            }
                        }
                        psDoubt.executeBatch();
                    }
                }
                connection.commit();
                System.out.println("Successfully synced frontend state to all 7 MySQL tables!");
            } catch (Exception e) {
                connection.rollback();
                e.printStackTrace(); // Prints exact reason in Tomcat black window if anything fails
                throw new SQLException("Failed to sync state into relational tables: " + e.getMessage(), e);
            }
        }
    }
}