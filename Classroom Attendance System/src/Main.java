public class Main {

    public static void main(String[] args) {
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
    }
}