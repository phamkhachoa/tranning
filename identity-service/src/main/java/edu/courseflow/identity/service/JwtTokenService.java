package edu.courseflow.identity.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JwtTokenService {

    public String createAccessToken(String userId, String email, List<String> roles) {
        /*
         * Intern implementation guide:
         * 1. Add JJWT dependencies to pom.xml.
         * 2. Read secret, issuer and TTL from application.yml.
         * 3. Build claims: iss, sub, uid, roles, iat, exp.
         * 4. Sign with HS256.
         */
        throw notImplemented("Implement JWT access-token creation.");
    }

    public VerifiedToken verify(String accessToken) {
        /*
         * Intern implementation guide:
         * 1. Parse signed JWT.
         * 2. Verify signature, issuer and expiration.
         * 3. Extract uid, subject/email and roles.
         * 4. Return VerifiedToken for the security filter.
         */
        throw notImplemented("Implement JWT verification.");
    }

    private ResponseStatusException notImplemented(String detail) {
        return new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, detail);
    }

    public record VerifiedToken(String userId, String email, List<String> roles) {
    }
}
