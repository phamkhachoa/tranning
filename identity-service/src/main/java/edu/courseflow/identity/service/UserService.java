package edu.courseflow.identity.service;

import edu.courseflow.identity.dto.AuthDtos.ProfileResponse;
import edu.courseflow.identity.dto.UserDtos.CreateUserRequest;
import edu.courseflow.identity.dto.UserDtos.UserResponse;
import edu.courseflow.identity.model.User;
import edu.courseflow.identity.model.UserStatus;
import edu.courseflow.identity.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(CreateUserRequest request) {
        /*
         * Intern task:
         * This is the only completed identity-service use case.
         * Notice the layer split:
         * - Controller validates HTTP shape and delegates.
         * - Service owns business rules such as unique email and password hashing.
         * - Repository hides storage details.
         */
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User saved = users.save(new User(
                UUID.randomUUID(),
                request.email().toLowerCase(),
                passwordEncoder.encode(request.password()),
                request.displayName(),
                UserStatus.ACTIVE,
                List.of("STUDENT")));

        return new UserResponse(
                saved.getId().toString(),
                saved.getEmail(),
                saved.getDisplayName(),
                saved.getRoles());
    }

    public ProfileResponse currentProfile(String userId) {
        /*
         * Intern implementation guide:
         * 1. Get userId from Authentication or trusted X-User-Id header after gateway/JWT verification.
         * 2. Load user by id.
         * 3. Load roles and permissions if the profile screen needs them.
         * 4. Return a safe response without passwordHash.
         */
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Implement current user profile lookup.");
    }

    public List<ProfileResponse> listUsers() {
        /*
         * Intern implementation guide:
         * 1. Protect this endpoint for ADMIN/operator only.
         * 2. Add pagination and keyword search.
         * 3. Add filters for role and status.
         * 4. Return safe user summaries without passwordHash.
         */
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Implement backoffice user listing.");
    }
}
