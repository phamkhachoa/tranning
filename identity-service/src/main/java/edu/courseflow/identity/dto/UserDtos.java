package edu.courseflow.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public final class UserDtos {

    private UserDtos() {
    }

    public record CreateUserRequest(
            @Email @NotBlank String email,
            @NotBlank String password,
            @NotBlank String displayName
    ) {
    }

    public record UserResponse(
            String id,
            String email,
            String displayName,
            List<String> roles
    ) {
    }
}
