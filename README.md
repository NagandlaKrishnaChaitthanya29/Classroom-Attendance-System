Classroom Attendance System

A Java-based classroom attendance project with a browser interface. The application organizes students, courses, class sessions, attendance records, teacher accounts, and student doubt requests.

## Features

- Teacher, student, and admin sign-in
- Student and course management, including course enrollment
- Class session creation and attendance marking
- Attendance status options: Present, Late, Absent, and Excused
- Student attendance reports and session summaries
- Teacher account management
- Student doubt submission and teacher replies
- Browser dashboard with attendance alerts, course reports, and CSV export
- MySQL persistence through JDBC and a Jakarta Servlet API

## Technologies

- Java (JDK 17 or newer recommended)
- HTML, CSS, and JavaScript
- MySQL
- MySQL Connector/J
- Jakarta Servlet API and Apache Tomcat 10 or newer

## Project structure

The project source files are in the src folder. The browser interface is index.html. The Java application also includes attendance-data.txt for saved application data.

## Setup

### 1. Prepare the Java project

Open the project in IntelliJ IDEA or another Java IDE. Add these dependencies to the project:

- MySQL Connector/J
- Jakarta Servlet API

Use a Java version that supports modern switch expressions and Stream.toList(); JDK 17 is recommended.

### 2. Configure MySQL

Install and start MySQL, then configure the application connection using environment variables:

| Variable | Example |
|---|---|
| ATTENDANCE_DB_URL | jdbc:mysql://localhost:3306/classroom |
| ATTENDANCE_DB_USER | Your MySQL username |
| ATTENDANCE_DB_PASSWORD | Your MySQL password |

Database.initialize() creates the application tables when the Java application or servlet initializes. The SQL file in src/ClassSession.sql contains database inspection queries.

### 3. Run the Java application

Run Main.java from the IDE. The console application loads saved data and presents the login and role menus.

### 4. Run the browser application

Deploy WebBackend.java to a Jakarta-compatible servlet container such as Tomcat 10+, then serve index.html through a local web server. The page is configured to send state to http://localhost:8080/api/state.

If you use another host or port, update the API URL in index.html. Browser data is also stored in local storage.

## Team work split

The team agreed to divide feature ownership as follows. Update these descriptions to reflect each person's actual completed work before using them as contribution claims.

| Team member | Assigned areas |
|---|---|
| **Nagandla Krishna Chaitthanya** — 25162201039 | Database and web interface: Database.java, ClassSession.sql, WebBackend.java, index.html; database and browser-state helper methods in Main.java |
| **Harshvardhan Singh Rathore** — 25162201019 | Student and course management: Student.java, Course.java; student/course operations in Main.java |
| **Prasad Yadav** — 25162201030 | Attendance: AttendanceStatus.java, ClassSession.java; session, marking, viewing, and reporting methods in Main.java |
| **Dhanraj Singh** — 25162201012 | Accounts and doubts: Teacher.java, User.java, Role.java, DoubtRequest.java; login, menus, teacher/admin and doubt workflows in Main.java; integration |

Because Main.java is shared, coordinate its edits on separate branches and review the combined changes before merging.

## GitHub collaboration

1. Clone the repository.
2. Create a branch for your task.
3. Edit the files assigned to your feature.
4. Commit and push the branch.
5. Open a pull request into main and request a review.

## Before sharing publicly

Review attendance-data.txt and remove or anonymize any real student information. Change demo account credentials and ensure database passwords are supplied through environment variables rather than committed to the repository. Do not upload compiled output folders such as out/ or machine-specific IDE workspace files.

## License

No license has been specified yet. Add a LICENSE file if you intend to grant permissions for others to use, modify, or redistribute this project.
