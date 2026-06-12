package edu.courseflow.identity.controller;

import edu.courseflow.identity.dto.AuthDtos.ProfileResponse;
import edu.courseflow.identity.service.UserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/backoffice/users")
public class UserAdminController {

    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<ProfileResponse> listUsers() {
        return userService.listUsers();
    }
}
