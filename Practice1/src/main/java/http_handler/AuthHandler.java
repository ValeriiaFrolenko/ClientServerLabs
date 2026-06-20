package http_handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.UserService;
import model.User;
import http_core.HttpResponses;
import http_core.JwtService;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class AuthHandler implements HttpHandler {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthHandler(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpResponses.sendError(exchange, 405, "Method not allowed");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path.equals("/login")) {
            handleLogin(exchange);
        } else if (path.equals("/register")) {
            handleRegister(exchange);
        } else {
            HttpResponses.sendError(exchange, 404, "Not found");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String[] credentials = parseCredentials(exchange);
        if (credentials == null) return;

        try {
            User user = userService.authenticate(credentials[0], credentials[1]);
            String token = jwtService.generateToken(user.login());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("token", token);
            HttpResponses.sendJson(exchange, 200, body);
        } catch (NoSuchElementException | SecurityException e) {
            HttpResponses.sendError(exchange, 401, "Invalid login or password");
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        String[] credentials = parseCredentials(exchange);
        if (credentials == null) return;

        try {
            User created = userService.register(credentials[0], credentials[1]);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", created.id());
            body.put("login", created.login());
            HttpResponses.sendJson(exchange, 201, body);
        } catch (IllegalStateException e) {
            HttpResponses.sendError(exchange, 409, e.getMessage());
        }
    }

    private String[] parseCredentials(HttpExchange exchange) throws IOException {
        try {
            Map<String, Object> body = HttpResponses.readJsonBody(exchange);
            String login = (String) body.get("login");
            String password = (String) body.get("password");
            if (login == null || password == null) {
                HttpResponses.sendError(exchange, 400, "Fields 'login' and 'password' are required");
                return null;
            }
            return new String[]{login, password};
        } catch (Exception e) {
            HttpResponses.sendError(exchange, 400, "Malformed JSON body");
            return null;
        }
    }
}