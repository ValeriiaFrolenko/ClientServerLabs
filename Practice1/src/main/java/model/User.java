package model;

public record User(
        long id,
        String login,
        String passwordHash
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long id;
        private String login;
        private String passwordHash;

        public Builder id(long id) { this.id = id; return this; }
        public Builder login(String login) { this.login = login; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }

        public User build() {
            return new User(id, login, passwordHash);
        }
    }
}