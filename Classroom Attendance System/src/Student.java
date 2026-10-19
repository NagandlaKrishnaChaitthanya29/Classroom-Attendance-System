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