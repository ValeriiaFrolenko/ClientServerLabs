package database;

import model.User;

import java.util.Optional;

public class UserRepository {

    private final JdbcTemplate jdbc;

    private static final JdbcTemplate.RowMapper<User> USER_MAPPER = rs -> User.builder()
            .id(rs.getLong("id"))
            .login(rs.getString("login"))
            .passwordHash(rs.getString("password_hash"))
            .build();

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public User create(User user) {
        long id = jdbc.insert(
                "INSERT INTO users (login, password_hash) VALUES (?, ?)",
                user.login(),
                user.passwordHash()
        );
        return User.builder()
                .id(id)
                .login(user.login())
                .passwordHash(user.passwordHash())
                .build();
    }

    public Optional<User> findByLogin(String login) {
        return jdbc.queryOne(
                "SELECT * FROM users WHERE login = ?",
                USER_MAPPER,
                login
        );
    }
}