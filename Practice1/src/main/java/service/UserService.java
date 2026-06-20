package service;

import database.UserRepository;
import model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.NoSuchElementException;

public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User register(String login, String password) {
        if (repository.findByLogin(login).isPresent()) {
            throw new IllegalStateException("User already exists: " + login);
        }
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = User.builder()
                .login(login)
                .passwordHash(passwordHash)
                .build();
        return repository.create(user);
    }

    public User authenticate(String login, String password) {
        User user = repository.findByLogin(login)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + login));
        if (!BCrypt.checkpw(password, user.passwordHash())) {
            throw new SecurityException("Invalid password");
        }
        return user;
    }
}