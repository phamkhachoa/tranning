package edu.courseflow.gateway;

import edu.courseflow.commonlibrary.security.InternalJwtService;
import edu.courseflow.commonlibrary.security.InternalScopes;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
class AccessControlIdentityClient {

    private static final List<RoleAssignmentHint> DEFAULT_LEARNER_ROLE =
            List.of(new RoleAssignmentHint("STUDENT", "PLATFORM", null));

    private final WebClient client;
    private final InternalJwtService internalJwt;
    private final long timeoutMs;

    AccessControlIdentityClient(
            WebClient.Builder webClientBuilder,
            InternalJwtService internalJwt,
            @Value("${courseflow.gateway.access-control-service-url:${ACCESS_CONTROL_SERVICE_URI:${ACCESS_CONTROL_SERVICE_URL:http://localhost:8102}}}")
            String accessControlServiceUrl,
            @Value("${courseflow.gateway.access-control-timeout-ms:${ACCESS_CONTROL_TIMEOUT_MS:1500}}")
            long timeoutMs) {
        this.client = webClientBuilder.clone()
                .baseUrl(accessControlServiceUrl)
                .build();
        this.internalJwt = internalJwt;
        this.timeoutMs = Math.max(250, Math.min(timeoutMs, 5000));
    }

    Mono<ResolvedIdentity> resolveOrProvisionKeycloakUser(
            String issuer,
            String subject,
            String email,
            boolean emailVerified) {
        ResolveIdentityRequest resolveRequest = new ResolveIdentityRequest(
                issuer,
                subject,
                email,
                emailVerified,
                List.of());
        return post("/internal/identities/resolve", resolveRequest, InternalScopes.IDENTITY_RESOLVE)
                .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                    ProvisionKeycloakUserRequest provisionRequest = new ProvisionKeycloakUserRequest(
                            issuer,
                            subject,
                            email,
                            emailVerified,
                            DEFAULT_LEARNER_ROLE);
                    return post(
                            "/internal/identities/provision-keycloak-user",
                            provisionRequest,
                            InternalScopes.IDENTITY_PROVISION);
                });
    }

    private Mono<ResolvedIdentity> post(String path, Object body, String scope) {
        // TODO(training-day-17-impl): Harden service-to-service calls that sit on the auth path.
        // Step 1: Keep timeout small and configurable; auth/identity resolution must not hang the
        //         whole request when access-control is slow.
        // Step 2: Retry only safe transient failures and only when the operation is idempotent.
        //         Provisioning needs an idempotency key or unique external identity constraint.
        // Step 3: Fail closed for protected APIs: if identity/authz dependency is unavailable,
        //         return 401/403/503 rather than silently allowing the request.
        return client.post()
                .uri(path)
                .headers(headers -> internalJwt.applyServiceToken(headers, Set.of(scope)))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(ResolvedIdentity.class)
                .timeout(Duration.ofMillis(timeoutMs));
    }

    private record ResolveIdentityRequest(
            String issuer,
            String subject,
            String email,
            Boolean emailVerified,
            List<RoleAssignmentHint> roleAssignments) {
    }

    private record ProvisionKeycloakUserRequest(
            String issuer,
            String subject,
            String email,
            Boolean emailVerified,
            List<RoleAssignmentHint> roleAssignments) {
    }

    private record RoleAssignmentHint(String code, String scopeType, String scopeId) {
    }

    record ResolvedIdentity(
            String userId,
            String externalIssuer,
            String externalSubject,
            String email,
            String status,
            List<ResolvedRoleAssignment> roleAssignments) {
    }

    record ResolvedRoleAssignment(String code, String scopeType, String scopeId) {
    }
}
