public class Main {

    public static void main(String[] args) {
       
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