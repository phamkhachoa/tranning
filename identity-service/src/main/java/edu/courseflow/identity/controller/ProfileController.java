package edu.courseflow.identity.controller;

import edu.courseflow.identity.dto.AuthDtos.ProfileResponse;
import edu.courseflow.identity.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ProfileResponse me(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        return userService.currentProfile(userId);
    }
}
