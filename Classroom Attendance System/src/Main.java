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

    }
}