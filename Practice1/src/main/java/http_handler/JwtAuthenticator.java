package network.http;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpExchange;
import http_core.JwtService;

public class JwtAuthenticator extends Authenticator {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticator(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Result authenticate(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return new Failure(401);
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        try {
            String login = jwtService.verifyAndGetLogin(token);
            return new Success(new com.sun.net.httpserver.HttpPrincipal(login, "warehouse"));
        } catch (JWTVerificationException e) {
            return new Failure(401);
        }
    }
}