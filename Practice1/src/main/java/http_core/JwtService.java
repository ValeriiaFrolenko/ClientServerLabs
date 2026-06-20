package http_core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;

public class JwtService {

    private static final long TOKEN_LIFETIME_MS = 24L * 60 * 60 * 1000; // 24 години
    private static final String CLAIM_LOGIN = "login";

    private final Algorithm algorithm;

    public JwtService(String secret) {
        this.algorithm = Algorithm.HMAC256(secret);
    }

    public String generateToken(String login) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + TOKEN_LIFETIME_MS);
        return JWT.create()
                .withClaim(CLAIM_LOGIN, login)
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .sign(algorithm);
    }

    public String verifyAndGetLogin(String token) {
        DecodedJWT decoded = JWT.require(algorithm).build().verify(token);
        return decoded.getClaim(CLAIM_LOGIN).asString();
    }
}