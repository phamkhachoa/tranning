package edu.courseflow.identity.controller;

import edu.courseflow.identity.dto.AuthDtos.AuthzCheckRequest;
import edu.courseflow.identity.dto.AuthDtos.AuthzCheckResponse;
import edu.courseflow.identity.service.AuthzService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/authz")
public class InternalAuthzController {

    private final AuthzService authzService;

    public InternalAuthzController(AuthzService authzService) {
        this.authzService = authzService;
    }

    @PostMapping("/check")
    public AuthzCheckResponse check(@Valid @RequestBody AuthzCheckRequest request) {
        return authzService.check(request);
    }
}
