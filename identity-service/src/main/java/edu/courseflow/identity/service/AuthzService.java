package edu.courseflow.identity.service;

import edu.courseflow.identity.dto.AuthDtos.AuthzCheckRequest;
import edu.courseflow.identity.dto.AuthDtos.AuthzCheckResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthzService {

    public AuthzCheckResponse check(AuthzCheckRequest request) {
        /*
         * Intern implementation guide:
         * 1. Load role assignments for request.userId.
         * 2. Filter assignments by scopeType/scopeId.
         * 3. Load permissions granted to those roles.
         * 4. Return allowed=true only when a matching permission exists.
         * 5. Deny by default.
         */
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Implement RBAC/ABAC authorization check.");
    }
}
