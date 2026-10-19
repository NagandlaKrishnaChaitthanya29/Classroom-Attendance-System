import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.management.relation.Role;

public class Main {

    public static void main(String[] args) {
         static final Scanner input = new Scanner(System.in);
    static final List<Student> students = new ArrayList<>();
    static final List<Course> courses = new ArrayList<>();
    static final List<ClassSession> sessions = new ArrayList<>();
    static final List<Teacher> teachers = new ArrayList<>();
    static int nextStudentId = 1;
    static int nextCourseId = 1;
    static int nextSessionId = 1;
    static int nextDoubtId = 1;
    static boolean databaseReady;
    static final List<DoubtRequest> doubtRequests = new ArrayList<>();
    static final Path DATA_FILE = Paths.get("attendance-data.txt");
    static final String TEACHER_USERNAME = "teacher";
    static final String TEACHER_PASSWORD_HASH = hashPassword("teach123");
    static final String ADMIN_USERNAME = "admin";
    static final String ADMIN_PASSWORD_HASH = hashPassword("admin123");
     loadData();
        if (!databaseReady) return;
        System.out.println("====================================");
        System.out.println("     CLASSROOM ATTENDANCE SYSTEM");
        System.out.println("====================================");

        boolean running = true;
        while (running) {
            User user = login();
            if (user == null) break;
            if (user.role == Role.STUDENT) {
                studentMenu(user.student);
                continue;
            }
            if (user.role == Role.ADMIN) {
                adminMenu();
                continue;
            }
            running = teacherMenu();
        }
        saveData();
        System.out.println("Goodbye!");
    }
       static User login() {
        while (true) {
            System.out.println("\n--- LOGIN ---");
            System.out.println("1. Teacher login");
            System.out.println("2. Student login");
            System.out.println("3. Admin login");
            System.out.println("0. Exit");
            int choice = readInt("Choose an option: ");
            if (choice == 0) return null;
            if (choice == 1) {
                String username = readLine("Teacher username: ");
                String password = readLine("Teacher password: ");
                Teacher teacher = findTeacher(username);
                boolean defaultTeacher = TEACHER_USERNAME.equals(username) && TEACHER_PASSWORD_HASH.equals(hashPassword(password));
                if (defaultTeacher || (teacher != null && teacher.passwordHash.equals(hashPassword(password)))) {
                    System.out.println("Teacher login successful.");
                    return new User(Role.TEACHER, null);
                }
                System.out.println("Incorrect teacher username or password.");
            } else if (choice == 2) {
                String number = readLine("Student number: ");
                String password = readLine("Student password: ");
                Student student = findStudentByNumber(number);
                if (student != null && student.passwordHash.equals(hashPassword(password))) {
                    System.out.println("Welcome, " + student.name + ".");
                    return new User(Role.STUDENT, student);
                }
                System.out.println("Incorrect student number or password.");
            } else if (choice == 3) {
                String username = readLine("Admin username: ");
                String password = readLine("Admin password: ");
                if (ADMIN_USERNAME.equals(username) && ADMIN_PASSWORD_HASH.equals(hashPassword(password))) {
                    System.out.println("Admin login successful.");
                    return new User(Role.ADMIN, null);
                }
                System.out.println("Incorrect admin username or password.");
            } else {
                System.out.println("Please choose a listed option.");
            }
        }
    }

    static void adminMenu() {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n--- ADMIN MENU ---");
            System.out.println("1. Add teacher account");
            System.out.println("2. List teacher accounts");
            System.out.println("0. Log out");
            switch (readInt("Choose an option: ")) {
                case 1 -> addTeacher();
                case 2 -> listTeachers();
                case 0 -> loggedIn = false;
                default -> System.out.println("Please choose a listed option.");
            }
        }
    }

    static void addTeacher() {
        System.out.println("\n--- ADD TEACHER ---");
        String username = readNonEmpty("Teacher username: ");
        if (username.equalsIgnoreCase(TEACHER_USERNAME) || findTeacher(username) != null) {
            System.out.println("That username is already in use.");
            return;
        }
        String name = readNonEmpty("Teacher name: ");
        String password = readNonEmpty("Set teacher password: ");
        teachers.add(new Teacher(username, name, hashPassword(password)));
        saveData();
        System.out.println("Teacher account created. The teacher can now log in with that username and password.");
    }

    static void listTeachers() {
        System.out.println("\nTEACHER ACCOUNTS");
        System.out.printf("- %s (Default account)%n", TEACHER_USERNAME);
        if (teachers.isEmpty()) System.out.println("No additional teacher accounts yet.");
        for (Teacher teacher : teachers) System.out.printf("- %s (%s)%n", teacher.username, teacher.name);
    }

    static void studentMenu(Student student) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n--- STUDENT MENU ---");
            System.out.println("1. View my attendance");
            System.out.println("2. Raise a doubt for my teacher");
            System.out.println("3. View my doubt requests");
            System.out.println("0. Log out");
            switch (readInt("Choose an option: ")) {
                case 1 -> studentReport(student);
                case 2 -> raiseDoubt(student);
                case 3 -> viewMyDoubts(student);
                case 0 -> loggedIn = false;
                default -> System.out.println("Please choose a listed option.");
            }
        }
    }

    static void raiseDoubt(Student student) {
        System.out.println("\n--- RAISE A DOUBT ---");
        String message = readNonEmpty("Write your doubt: ");
        doubtRequests.add(new DoubtRequest(nextDoubtId++, student.id, message, LocalDateTime.now()));
        saveData();
        System.out.println("Your doubt was sent to the teacher.");
    }

    static void viewMyDoubts(Student student) {
        List<DoubtRequest> mine = doubtRequests.stream().filter(doubt -> doubt.studentId == student.id).toList();
        if (mine.isEmpty()) {
            System.out.println("You have not submitted any doubts yet.");
            return;
        }
        System.out.println("\n--- MY DOUBT REQUESTS ---");
        for (DoubtRequest doubt : mine) {
            System.out.printf("Request #%d (%s)%n  Doubt: %s%n  Teacher reply: %s%n",
                    doubt.id, doubt.createdAt, doubt.message,
                    doubt.reply.isBlank() ? "Waiting for a reply" : doubt.reply);
        }
    }

    static void manageDoubts() {
        if (doubtRequests.isEmpty()) {
            System.out.println("There are no student doubt requests yet.");
            return;
        }
        System.out.println("\n--- STUDENT DOUBT REQUESTS ---");
        for (DoubtRequest doubt : doubtRequests) {
            Student student = findStudentById(doubt.studentId);
            System.out.printf("#%d | %s (%s) | %s%n  Doubt: %s%n  Reply: %s%n",
                    doubt.id, student == null ? "Unknown student" : student.name,
                    student == null ? "" : student.studentNumber, doubt.createdAt, doubt.message,
                    doubt.reply.isBlank() ? "Not answered" : doubt.reply);
        }
        int id = readInt("Enter a request number to reply (0 to cancel): ");
        if (id == 0) return;
        DoubtRequest selected = findDoubtById(id);
        if (selected == null) {
            System.out.println("Doubt request not found.");
            return;
        }
        selected.reply = readNonEmpty("Write your reply: ");
        selected.repliedAt = LocalDateTime.now();
        saveData();
        System.out.println("Reply saved. The student can see it after logging in.");
    }

    static void printMenu() {
        System.out.println("\n--- MENU ---");
        System.out.println("1. Add student");
        System.out.println("2. List students");
        System.out.println("3. Add course");
        System.out.println("4. Enroll student in a course");
        System.out.println("5. List courses");
        System.out.println("6. Create class session");
        System.out.println("7. Mark attendance");
        System.out.println("8. View session attendance");
        System.out.println("9. Student attendance report");
        System.out.println("11. View and reply to student doubts");
        System.out.println("10. Log out");
        System.out.println("0. Exit program");
    }

    // Returns true after logout so the login screen is shown again, false to exit.
    static boolean teacherMenu() {
        while (true) {
            printMenu();
            switch (readInt("Choose an option: ")) {
                case 1 -> addStudent();
                case 2 -> listStudents();
                case 3 -> addCourse();
                case 4 -> enrollStudent();
                case 5 -> listCourses();
                case 6 -> createSession();
                case 7 -> markAttendance();
                case 8 -> viewSessionAttendance();
                case 9 -> studentReport();
                case 11 -> manageDoubts();
                case 10 -> {
                    saveData();
                    System.out.println("Teacher logged out.");
                    return true;
                }
                case 0 -> {
                    saveData();
                    return false;
                }
                default -> System.out.println("Please choose a listed option.");
            }
            System.out.println();
        }
    }
        class Student {
        final int id;
        final String studentNumber;
        final String name;
        final String passwordHash;
        Student(int id, String studentNumber, String name, String passwordHash) {
            this.id = id; this.studentNumber = studentNumber; this.name = name; this.passwordHash = passwordHash;
        }
        @Override public String toString() { return name + " (" + studentNumber + ")"; }
    }

    static void listStudents() {
        if (students.isEmpty()) {
            System.out.println("No students have been added yet.");
            return;
        }
        System.out.println("\nSTUDENTS");
        for (Student student : students) {
            System.out.printf("%d. %s (%s)%n", student.id, student.name, student.studentNumber);
        }
    }
    static void addCourse() {
        String code = readNonEmpty("Course code: ");
        if (findCourseByCode(code) != null) {
            System.out.println("That course code is already in use.");
            return;
        }
        String name = readNonEmpty("Course name: ");
        String instructor = readLine("Instructor name (optional): ");
        courses.add(new Course(nextCourseId++, code, name, instructor));
        saveData();
        System.out.println("Course added.");
    }
    static void enrollStudent() {
        if (students.isEmpty() || courses.isEmpty()) {
            System.out.println("Add at least one student and one course first.");
            return;
        }
        listStudents();
        Student student = findStudentById(readInt("Enter student number from the list: "));
        if (student == null) {
            System.out.println("Student not found.");
            return;
        }
        listCourses();
        Course course = findCourseById(readInt("Enter course number from the list: "));
        if (course == null) {
            System.out.println("Course not found.");
            return;
        }
        if (course.studentIds.add(student.id)) System.out.println("Student enrolled in " + course.name + ".");
        else System.out.println("That student is already enrolled in this course.");
        saveData();
    }

    static void listCourses() {
        if (courses.isEmpty()) {
            System.out.println("No courses have been added yet.");
            return;
        }
        System.out.println("\nCOURSES");
        for (Course course : courses) {
            System.out.printf("%d. %s — %s | Instructor: %s | Students: %d%n",
                    course.id, course.code, course.name,
                    course.instructor.isBlank() ? "Not specified" : course.instructor,
                    course.studentIds.size());
        }
    }

    static void createSession() {
        if (courses.isEmpty()) {
            System.out.println("Add a course first.");
            return;
        }
        listCourses();
        Course course = findCourseById(readInt("Enter course number: "));
        if (course == null) {
            System.out.println("Course not found.");
            return;
        }
        LocalDate date = readDate("Session date (YYYY-MM-DD): ");
        String timeText = readLine("Start time (HH:MM, optional): ");
        LocalTime time = null;
        if (!timeText.isBlank()) {
            try {
                time = LocalTime.parse(timeText);
            } catch (DateTimeParseException exception) {
                System.out.println("Invalid time. Use HH:MM, for example 09:30.");
                return;
            }
        }
        String topic = readLine("Lesson topic (optional): ");
        ClassSession session = new ClassSession(nextSessionId++, course, date, time, topic);
        sessions.add(session);
        saveData();
        System.out.println("Class session created. Session number: " + session.id);
    }

    static void markAttendance() {
        ClassSession session = chooseSession();
        if (session == null) return;
        List<Student> enrolled = getEnrolledStudents(session.course);
        if (enrolled.isEmpty()) {
            System.out.println("No students are enrolled in this course. Use option 4 first.");
            return;
        }
        System.out.println("Mark attendance for " + session.course.name + " on " + session.date);
        for (Student student : enrolled) {
            while (true) {
                String status = readLine(student.name + " [P]resent, [L]ate, [A]bsent, [E]xcused: ").trim().toUpperCase();
                switch (status) {
                    case "P", "PRESENT" -> { session.attendance.put(student.id, AttendanceStatus.PRESENT); }
                    case "L", "LATE" -> { session.attendance.put(student.id, AttendanceStatus.LATE); }
                    case "A", "ABSENT" -> { session.attendance.put(student.id, AttendanceStatus.ABSENT); }
                    case "E", "EXCUSED" -> { session.attendance.put(student.id, AttendanceStatus.EXCUSED); }
                    default -> {
                        System.out.println("Enter P, L, A, or E.");
                        continue;
                    }
                }
                break;
            }
        }
        saveData();
        System.out.println("Attendance saved.");
    }

    static void viewSessionAttendance() {
        ClassSession session = chooseSession();
        if (session == null) return;
        System.out.printf("\n%s — %s — %s%n", session.course.code, session.course.name, session.date);
        if (!session.topic.isBlank()) System.out.println("Topic: " + session.topic);
        if (session.attendance.isEmpty()) {
            System.out.println("Attendance has not been marked for this session.");
            return;
        }
        for (Student student : getEnrolledStudents(session.course)) {
            AttendanceStatus status = session.attendance.get(student.id);
            System.out.printf("%-25s %s%n", student.name, status == null ? "NOT MARKED" : status);
        }
        printStatusCounts(session);
    }

    static void studentReport() {
        if (students.isEmpty()) {
            System.out.println("No students have been added yet.");
            return;
        }
        listStudents();
        Student student = findStudentById(readInt("Enter student ID from the list: "));
        if (student == null) {
            System.out.println("Student not found.");
            return;
        }
        printStudentReport(student);
    }

    static void studentReport(Student student) {
        printStudentReport(student);
    }

    static void printStudentReport(Student student) {
        int present = 0, late = 0, absent = 0, excused = 0;
        System.out.println("\nATTENDANCE REPORT: " + student.name + " (" + student.studentNumber + ")");
        for (ClassSession session : sessions) {
            AttendanceStatus status = session.attendance.get(student.id);
            if (status == null) continue;
            System.out.printf("%s | %s | %s | %s%n", session.date, session.course.code, session.topic, status);
            switch (status) {
                case PRESENT -> present++;
                case LATE -> late++;
                case ABSENT -> absent++;
                case EXCUSED -> excused++;
            }
        }
        int recorded = present + late + absent + excused;
        double rate = recorded == 0 ? 0 : (present + late) * 100.0 / recorded;
        System.out.printf("Sessions recorded: %d | Present: %d | Late: %d | Absent: %d | Excused: %d%n",
                recorded, present, late, absent, excused);
        System.out.printf("Attendance rate (present + late): %.1f%%%n", rate);
    }

    static void printStatusCounts(ClassSession session) {
        int present = 0, late = 0, absent = 0, excused = 0;
        for (AttendanceStatus status : session.attendance.values()) {
            switch (status) {
                case PRESENT -> present++;
                case LATE -> late++;
                case ABSENT -> absent++;
                case EXCUSED -> excused++;
            }
        }
        System.out.printf("Present: %d | Late: %d | Absent: %d | Excused: %d%n", present, late, absent, excused);
    }

    static ClassSession chooseSession() {
        if (sessions.isEmpty()) {
            System.out.println("No class sessions exist yet. Create one first.");
            return null;
        }
        System.out.println("\nCLASS SESSIONS");
        for (ClassSession session : sessions) {
            System.out.printf("%d. %s — %s — %s %s%n", session.id, session.course.code,
                    session.course.name, session.date, session.topic.isBlank() ? "" : "(" + session.topic + ")");
        }
        ClassSession session = findSessionById(readInt("Enter session number: "));
        if (session == null) System.out.println("Session not found.");
        return session;
    }

    static List<Student> getEnrolledStudents(Course course) {
        List<Student> result = new ArrayList<>();
        for (Student student : students) if (course.studentIds.contains(student.id)) result.add(student);
        return result;
    }

    static Student findStudentById(int id) {
        for (Student student : students) if (student.id == id) return student;
        return null;
    }
    static Student findStudentByNumber(String number) {
        for (Student student : students) if (student.studentNumber.equalsIgnoreCase(number)) return student;
        return null;
    }
    static Course findCourseById(int id) {
        for (Course course : courses) if (course.id == id) return course;
        return null;
    }
    static Course findCourseByCode(String code) {
        for (Course course : courses) if (course.code.equalsIgnoreCase(code)) return course;
        return null;
    }
    static ClassSession findSessionById(int id) {
        for (ClassSession session : sessions) if (session.id == id) return session;
        return null;
    }
    static Teacher findTeacher(String username) {
        for (Teacher teacher : teachers) if (teacher.username.equalsIgnoreCase(username)) return teacher;
        return null;
    }
    static DoubtRequest findDoubtById(int id) {
        for (DoubtRequest doubt : doubtRequests) if (doubt.id == id) return doubt;
        return null;
    }

    static String readLine(String prompt) {
        System.out.print(prompt);
        return input.nextLine().trim();
    }
    static String readNonEmpty(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isBlank()) return value;
            System.out.println("This field cannot be empty.");
        }
    }
    static int readInt(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try { return Integer.parseInt(value); }
            catch (NumberFormatException exception) { System.out.println("Enter a whole number."); }
        }
    }
    static LocalDate readDate(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try { return LocalDate.parse(value); }
            catch (DateTimeParseException exception) { System.out.println("Use YYYY-MM-DD, for example 2026-10-05."); }
        }
    }

    static void saveData() {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate("DELETE FROM doubt_requests");
                    statement.executeUpdate("DELETE FROM attendance_records");
                    statement.executeUpdate("DELETE FROM class_sessions");
                    statement.executeUpdate("DELETE FROM course_students");
                    statement.executeUpdate("DELETE FROM students");
                    statement.executeUpdate("DELETE FROM courses");
                    statement.executeUpdate("DELETE FROM teacher_accounts");
                }
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO teacher_accounts(username,name,password_hash) VALUES(?,?,?)")) {
                    for (Teacher teacher : teachers) {
                        ps.setString(1, teacher.username); ps.setString(2, teacher.name); ps.setString(3, teacher.passwordHash); ps.addBatch();
                    }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO students(id,student_number,full_name,password_hash) VALUES(?,?,?,?)")) {
                    for (Student student : students) {
                        ps.setInt(1, student.id); ps.setString(2, student.studentNumber); ps.setString(3, student.name); ps.setString(4, student.passwordHash); ps.addBatch();
                    }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO courses(id,course_code,name,instructor) VALUES(?,?,?,?)")) {
                    for (Course course : courses) {
                        ps.setInt(1, course.id); ps.setString(2, course.code); ps.setString(3, course.name); ps.setString(4, course.instructor); ps.addBatch();
                    }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO course_students(course_id,student_id) VALUES(?,?)")) {
                    for (Course course : courses) for (int studentId : course.studentIds) {
                        ps.setInt(1, course.id); ps.setInt(2, studentId); ps.addBatch();
                    }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO class_sessions(id,course_id,session_date,start_time,topic) VALUES(?,?,?,?,?)")) {
                    for (ClassSession session : sessions) {
                        ps.setInt(1, session.id); ps.setInt(2, session.course.id); ps.setDate(3, Date.valueOf(session.date));
                        if (session.time == null) ps.setNull(4, java.sql.Types.TIME); else ps.setTime(4, Time.valueOf(session.time));
                        ps.setString(5, session.topic); ps.addBatch();
                    }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO attendance_records(session_id,student_id,status) VALUES(?,?,?)")) {
                    for (ClassSession session : sessions) for (Map.Entry<Integer, AttendanceStatus> entry : session.attendance.entrySet()) {
                        ps.setInt(1, session.id); ps.setInt(2, entry.getKey()); ps.setString(3, entry.getValue().name()); ps.addBatch();
                    }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO doubt_requests(id,student_id,message,created_at,reply,replied_at) VALUES(?,?,?,?,?,?)")) {
                    for (DoubtRequest doubt : doubtRequests) {
                        ps.setInt(1, doubt.id); ps.setInt(2, doubt.studentId); ps.setString(3, doubt.message);
                        ps.setTimestamp(4, Timestamp.valueOf(doubt.createdAt)); ps.setString(5, doubt.reply);
                        if (doubt.repliedAt == null) ps.setNull(6, java.sql.Types.TIMESTAMP); else ps.setTimestamp(6, Timestamp.valueOf(doubt.repliedAt));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            System.out.println("Could not save records to MySQL: " + exception.getMessage());
        }
    }

    static void loadData() {
        try {
            Database.initialize();
            try (Connection connection = Database.connect()) {
                try (var statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT username,name,password_hash FROM teacher_accounts")) {
                    while (rs.next()) teachers.add(new Teacher(rs.getString(1), rs.getString(2), rs.getString(3)));
                }
                try (var statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT id,student_number,full_name,password_hash FROM students ORDER BY id")) {
                    while (rs.next()) students.add(new Student(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)));
                }
                try (var statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT id,course_code,name,instructor FROM courses ORDER BY id")) {
                    while (rs.next()) courses.add(new Course(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)));
                }
                try (var statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT course_id,student_id FROM course_students")) {
                    while (rs.next()) { Course course = findCourseById(rs.getInt(1)); if (course != null) course.studentIds.add(rs.getInt(2)); }
                }
                try (var statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT id,course_id,session_date,start_time,topic FROM class_sessions ORDER BY id")) {
                    while (rs.next()) {
                        Course course = findCourseById(rs.getInt(2));
                        if (course != null) {
                            Time time = rs.getTime(4);
                            sessions.add(new ClassSession(rs.getInt(1), course, rs.getDate(3).toLocalDate(),
                                    time == null ? null : time.toLocalTime(), rs.getString(5)));
                        }
                    }
                }
                try (var statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT session_id,student_id,status FROM attendance_records")) {
                    while (rs.next()) {
                        ClassSession session = findSessionById(rs.getInt(1));
                        if (session != null) session.attendance.put(rs.getInt(2), AttendanceStatus.valueOf(rs.getString(3)));
                    }
                }
                try (var statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT id,student_id,message,created_at,reply,replied_at FROM doubt_requests ORDER BY id")) {
                    while (rs.next()) {
                        Timestamp repliedAt = rs.getTimestamp(6);
                        doubtRequests.add(new DoubtRequest(rs.getInt(1), rs.getInt(2), rs.getString(3),
                                rs.getTimestamp(4).toLocalDateTime(), rs.getString(5), repliedAt == null ? null : repliedAt.toLocalDateTime()));
                    }
                }
            }
            if (students.isEmpty() && courses.isEmpty() && sessions.isEmpty() && teachers.isEmpty()
                    && doubtRequests.isEmpty() && Files.exists(DATA_FILE)) {
                loadLegacyTextFile();
                saveData();
            }
            nextStudentId = students.stream().mapToInt(student -> student.id).max().orElse(0) + 1;
            nextCourseId = courses.stream().mapToInt(course -> course.id).max().orElse(0) + 1;
            nextSessionId = sessions.stream().mapToInt(session -> session.id).max().orElse(0) + 1;
            nextDoubtId = doubtRequests.stream().mapToInt(doubt -> doubt.id).max().orElse(0) + 1;
            databaseReady = true;
            System.out.println("MySQL database connected. Records loaded.");
        } catch (SQLException exception) {
            databaseReady = false;
            System.out.println("Could not load MySQL database: " + exception.getMessage());
            System.out.println("Check that MySQL is running and Connector/J is added to IntelliJ.");
        }
    }


    static void loadLegacyTextFile() {
        Map<Integer, List<Integer>> enrollments = new LinkedHashMap<>();
        Map<Integer, Map<Integer, AttendanceStatus>> savedAttendance = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(DATA_FILE, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", -1);
                try {
                    switch (parts[0]) {
                        case "META" -> {
                            nextStudentId = Integer.parseInt(parts[1]); nextCourseId = Integer.parseInt(parts[2]); nextSessionId = Integer.parseInt(parts[3]);
                        }
                        case "TEACHER" -> teachers.add(new Teacher(decode(parts[1]), decode(parts[2]), parts[3]));
                        case "STUDENT" -> {
                            String number = decode(parts[2]);
                            String passwordHash = parts.length > 4 && !parts[4].isBlank() ? parts[4] : hashPassword(number);
                            students.add(new Student(Integer.parseInt(parts[1]), number, decode(parts[3]), passwordHash));
                        }
                        case "COURSE" -> {
                            int id = Integer.parseInt(parts[1]);
                            courses.add(new Course(id, decode(parts[2]), decode(parts[3]), decode(parts[4])));
                            enrollments.put(id, parseIds(parts[5]));
                        }
                        case "SESSION" -> {
                            Course course = findCourseById(Integer.parseInt(parts[2]));
                            if (course != null) sessions.add(new ClassSession(Integer.parseInt(parts[1]), course,
                                    LocalDate.parse(parts[3]), parts[4].isBlank() ? null : LocalTime.parse(parts[4]), decode(parts[5])));
                        }
                        case "ATTENDANCE" -> savedAttendance.computeIfAbsent(Integer.parseInt(parts[1]), ignored -> new LinkedHashMap<>())
                                .put(Integer.parseInt(parts[2]), AttendanceStatus.valueOf(parts[3]));
                        case "DOUBT_COUNTER" -> nextDoubtId = Integer.parseInt(parts[1]);
                        case "DOUBT" -> doubtRequests.add(new DoubtRequest(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                                decode(parts[4]), LocalDateTime.parse(parts[3]), decode(parts[5]),
                                parts[6].isBlank() ? null : LocalDateTime.parse(parts[6])));
                        default -> { }
                    }
                } catch (RuntimeException exception) {
                    System.out.println("Skipped invalid legacy record: " + line);
                }
            }
            for (Course course : courses) course.studentIds.addAll(enrollments.getOrDefault(course.id, List.of()));
            for (ClassSession session : sessions) session.attendance.putAll(savedAttendance.getOrDefault(session.id, Map.of()));
            System.out.println("Imported existing records from attendance-data.txt into MySQL.");
        } catch (IOException exception) {
            System.out.println("Could not import the old text file: " + exception.getMessage());
        }
    }

    static String encode(String value) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static String decode(String value) {
        return new String(java.util.Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    static String joinIds(Set<Integer> ids) {
        return ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    static List<Integer> parseIds(String value) {
        List<Integer> ids = new ArrayList<>();
        if (!value.isBlank()) for (String part : value.split(",")) ids.add(Integer.parseInt(part));
        return ids;
    }

    static String hashPassword(String password) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : digest) result.append(String.format("%02x", value));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }


}

    
}