package edu.courseflow.identity.service;

import edu.courseflow.identity.dto.AuthDtos.LoginRequest;
import edu.courseflow.identity.dto.AuthDtos.RefreshRequest;
import edu.courseflow.identity.dto.AuthDtos.RegisterRequest;
import edu.courseflow.identity.dto.AuthDtos.TokenResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserService userService;

    public AuthService(UserService userService) {
        this.userService = userService;
    }

    public TokenResponse register(RegisterRequest request) {
        /*
         * Intern implementation guide:
         * 1. Reuse UserService.createUser or extract a shared private method.
         * 2. Decide whether register should immediately login the user or only create an account.
         * 3. If it logs in immediately, call JwtTokenService.createAccessToken and
         *    RefreshTokenService.createRefreshToken.
         * 4. Return TokenResponse.
         */
        throw notImplemented("Implement register flow: create user, assign STUDENT role, then optionally issue tokens.");
    }

    public TokenResponse login(LoginRequest request) {
        /*
         * Intern implementation guide:
         * 1. Load user by email from UserRepository.
         * 2. Reject LOCKED/DISABLED users.
         * 3. Compare raw password with BCrypt hash using PasswordEncoder.matches.
         * 4. Load user roles.
         * 5. Issue access token and refresh token.
         * 6. Audit LOGIN_SUCCESS or LOGIN_FAILED.
         */
        throw notImplemented("Implement login flow: load user, verify password, issue access and refresh tokens.");
    }

    public TokenResponse refresh(RefreshRequest request) {
        /*
         * Intern implementation guide:
         * 1. Hash the raw refresh token from the request.
         * 2. Find it in refresh_tokens.
         * 3. Check expiresAt and revokedAt.
         * 4. Create a new access token.
         * 5. Rotate refresh token: create a new one and revoke the old one.
         * 6. Return the new token pair.
         */
        throw notImplemented("Implement refresh-token rotation.");
    }

    private ResponseStatusException notImplemented(String detail) {
        return new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, detail);
    }
}
