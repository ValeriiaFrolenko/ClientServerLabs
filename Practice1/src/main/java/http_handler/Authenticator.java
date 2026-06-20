package http_handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import http_core.HttpResponses;
import http_core.JwtService;

import java.io.IOException;

public class Authenticator implements HttpHandler {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final HttpHandler delegate;

    public Authenticator(JwtService jwtService, HttpHandler delegate) {
        this.jwtService = jwtService;
        this.delegate = delegate;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            HttpResponses.sendError(exchange, 401, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        try {
            String login = jwtService.verifyAndGetLogin(token);
            exchange.setAttribute("login", login);
        } catch (JWTVerificationException e) {
            HttpResponses.sendError(exchange, 401, "Invalid or expired token");
            return;
        }

        delegate.handle(exchange);
    }
}