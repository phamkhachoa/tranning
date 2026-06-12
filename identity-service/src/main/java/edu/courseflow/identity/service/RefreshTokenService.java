package edu.courseflow.identity.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RefreshTokenService {

    public String createRefreshToken(String userId) {
        /*
         * Intern implementation guide:
         * 1. Generate a cryptographically random raw token.
         * 2. Hash it before saving.
         * 3. Store userId, tokenHash, expiresAt and device metadata.
         * 4. Return only the raw token to the client once.
         */
        throw notImplemented("Implement refresh-token creation.");
    }

    public String rotate(String rawRefreshToken) {
        /*
         * Intern implementation guide:
         * 1. Hash rawRefreshToken.
         * 2. Find active token by hash.
         * 3. Reject expired/revoked token.
         * 4. Create replacement token.
         * 5. Mark old token revoked and linked to replacement.
         */
        throw notImplemented("Implement refresh-token rotation.");
    }

    private ResponseStatusException notImplemented(String detail) {
        return new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, detail);
    }
}
