class Teacher {
        final String username;
        final String name;
        final String passwordHash;
        Teacher(String username, String name, String passwordHash) {
            this.username = username; this.name = name; this.passwordHash = passwordHash;
        }
    }