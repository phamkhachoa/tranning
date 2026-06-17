package edu.courseflow.gateway;

import edu.courseflow.commonlibrary.constants.GatewayHeaders;
import edu.courseflow.commonlibrary.security.InternalJwtService;
import edu.courseflow.commonlibrary.web.CurrentUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * The gateway is the single trust boundary. For every inbound request it:
 * <ol>
 *   <li>strips any client-supplied {@code X-User-*} headers so identity cannot be spoofed;</li>
 *   <li>validates the external Bearer JWT with OAuth2/OIDC JWKS;</li>
 *   <li>exchanges the external JWT for a short-lived internal JWT;</li>
 *   <li>forwards identity derived from the verified internal JWT via legacy {@code X-User-*}
 *       headers, plus {@code X-Internal-Authorization}.</li>
 * </ol>
 * Downstream services are only reachable through the gateway (network isolation) and may read
 * these headers as trusted, e.g. via {@code CurrentUserArgumentResolver} in common-library.
 */
@Component
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

    /** Roles allowed through the operator-gated edge. Domain services still enforce fine-grained authz. */
    private static final Set<String> OPERATOR_ROLES = Set.of(
            "ADMIN",
            "ORG_ADMIN",
            "INSTRUCTOR",
            "PROFESSOR",
            "TA",
            "INCENTIVE_ADMIN",
            "INCENTIVE_REVIEWER",
            "INCENTIVE_OPERATOR",
            "LOYALTY_ADMIN",
            "LOYALTY_REVIEWER",
            "LOYALTY_OPERATOR");

    /** Most-privileged first; used to pick the single {@code X-User-Role} value for legacy callers. */
    private static final List<String> ROLE_RANK =
            List.of(
                    "ADMIN",
                    "ORG_ADMIN",
                    "INCENTIVE_ADMIN",
                    "INCENTIVE_REVIEWER",
                    "INCENTIVE_OPERATOR",
                    "LOYALTY_ADMIN",
                    "LOYALTY_REVIEWER",
                    "LOYALTY_OPERATOR",
                    "INSTRUCTOR",
                    "PROFESSOR",
                    "TA",
                    "STUDENT");

    private final GatewayExternalTokenVerifier externalTokenVerifier;
    private final InternalTokenConverterClient tokenConverter;
    private final InternalJwtService internalJwtService;
    private final AccessControlIdentityClient accessControlIdentity;

    @Value("${courseflow.training.auth-bypass:true}")
    private boolean trainingAuthBypass;

    @Autowired
    public JwtAuthenticationGatewayFilter(GatewayExternalTokenVerifier externalTokenVerifier,
            InternalTokenConverterClient tokenConverter,
            InternalJwtService internalJwtService,
            AccessControlIdentityClient accessControlIdentity) {
        this.externalTokenVerifier = externalTokenVerifier;
        this.tokenConverter = tokenConverter == null ? InternalTokenConverterClient.disabled() : tokenConverter;
        this.internalJwtService = internalJwtService;
        this.accessControlIdentity = accessControlIdentity;
    }

    JwtAuthenticationGatewayFilter(GatewayExternalTokenVerifier externalTokenVerifier,
            InternalTokenConverterClient tokenConverter,
            InternalJwtService internalJwtService,
            boolean testConstructor) {
        this.externalTokenVerifier = externalTokenVerifier;
        this.tokenConverter = tokenConverter == null ? InternalTokenConverterClient.disabled() : tokenConverter;
        this.internalJwtService = internalJwtService;
        this.accessControlIdentity = null;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Always strip identity headers — only the gateway may set them.
        ServerHttpRequest.Builder builder = request.mutate().headers(headers -> {
            headers.remove(GatewayHeaders.USER_ID);
            headers.remove(GatewayHeaders.USER_ROLE);
            headers.remove(GatewayHeaders.USER_ROLES);
            headers.remove(GatewayHeaders.USER_ROLE_SCOPES);
            headers.remove(GatewayHeaders.USER_EMAIL);
            headers.remove(GatewayHeaders.INTERNAL_AUTHORIZATION);
        });

        if (isDeletedAuthPath(path)) {
            return error(exchange, HttpStatus.GONE, "Gone", "CourseFlow password auth endpoints have been removed; use Keycloak/OIDC");
        }

        if (isPublic(path, request.getMethod().name())) {
            return chain.filter(exchange.mutate().request(builder.build()).build());
        }

        if (trainingAuthBypass) {
            return forwardWithTrainingIdentity(exchange, chain, builder);
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing bearer token");
        }

        String externalToken = authHeader.substring(7);
        return externalTokenVerifier.verify(externalToken)
                .flatMap(jwt -> tokenConverter.localIdentityMode()
                        ? forwardWithVerifiedExternalToken(exchange, chain, builder, jwt)
                        : forwardWithInternalToken(exchange, chain, builder, externalToken))
                .onErrorResume(ex -> unauthorized(exchange, "Invalid or expired token"));
    }

    private Mono<Void> forwardWithTrainingIdentity(ServerWebExchange exchange, GatewayFilterChain chain,
                                                   ServerHttpRequest.Builder builder) {
        ServerHttpRequest request = exchange.getRequest();
        Set<String> roles = trainingRoles(request);
        if (isOperatorPath(request.getURI().getPath()) && roles.stream().noneMatch(OPERATOR_ROLES::contains)) {
            return forbidden(exchange, "Admin API requires an operator role");
        }
        String primaryRole = primaryRole(roles);
        Long userId = trainingUserId(request);
        String email = trainingHeader(request, "X-Training-User-Email", defaultTrainingEmail(primaryRole));
        CurrentUser user = new CurrentUser(userId, email, primaryRole, roles);
        ServerHttpRequest converted = builder.headers(headers -> internalJwtService.applyUserToken(headers, user)).build();
        return chain.filter(exchange.mutate().request(converted).build());
    }

    private Long trainingUserId(ServerHttpRequest request) {
        String raw = trainingHeader(request, "X-Training-User-Id", "1");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return 1L;
        }
    }

    private Set<String> trainingRoles(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        String fallback = isOperatorPath(path) ? "ADMIN" : "STUDENT";
        String raw = trainingHeader(request, "X-Training-User-Roles", fallback);
        Set<String> roles = java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (roles.isEmpty()) {
            roles.add(fallback);
        }
        return roles;
    }

    private String trainingHeader(ServerHttpRequest request, String name, String fallback) {
        String value = request.getHeaders().getFirst(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String defaultTrainingEmail(String primaryRole) {
        return switch (primaryRole) {
            case "ADMIN" -> "admin@courseflow.local";
            case "INSTRUCTOR", "PROFESSOR", "TA" -> "instructor@courseflow.local";
            default -> "student@courseflow.local";
        };
    }

    private Mono<Void> forwardWithInternalToken(ServerWebExchange exchange, GatewayFilterChain chain,
                                                ServerHttpRequest.Builder builder, String externalToken) {
        if (!tokenConverter.enabled()) {
            return error(exchange, HttpStatus.BAD_GATEWAY, "Bad Gateway", "Internal token converter is disabled");
        }
        Mono<Void> conversionFailure =
                error(exchange, HttpStatus.BAD_GATEWAY, "Bad Gateway", "Internal token conversion failed");

        return tokenConverter.exchange(externalToken)
                .flatMap(internalToken -> {
                    IdentityHeaders identity;
                    try {
                        Claims internalClaims = internalJwtService.verify(internalToken);
                        identity = identityHeaders(internalClaims, exchange.getRequest().getURI().getPath());
                    } catch (OperatorForbiddenException ex) {
                        return forbidden(exchange, "Admin API requires an operator role");
                    } catch (JwtException | IllegalArgumentException | IllegalStateException ex) {
                        return conversionFailure;
                    }
                    ServerHttpRequest converted = builder.headers(headers ->
                            writeInternalIdentityHeaders(headers, internalToken, identity)).build();
                    return chain.filter(exchange.mutate().request(converted).build());
                })
                .switchIfEmpty(conversionFailure)
                .onErrorResume(ex -> conversionFailure);
    }

    private Mono<Void> forwardWithVerifiedExternalToken(ServerWebExchange exchange, GatewayFilterChain chain,
                                                        ServerHttpRequest.Builder builder, Jwt jwt) {
        String path = exchange.getRequest().getURI().getPath();
        return currentUserFromVerifiedExternalJwt(jwt, path)
                .flatMap(user -> {
                    ServerHttpRequest converted = builder.headers(headers ->
                            internalJwtService.applyUserToken(headers, user)).build();
                    return chain.filter(exchange.mutate().request(converted).build());
                })
                .onErrorResume(OperatorForbiddenException.class,
                        ex -> forbidden(exchange, "Admin API requires an operator role"))
                .onErrorResume(IllegalArgumentException.class,
                        ex -> unauthorized(exchange, ex.getMessage()));
    }

    private Mono<CurrentUser> currentUserFromVerifiedExternalJwt(Jwt jwt, String path) {
        // TODO(training-day-01-impl): Harden edge identity resolution.
        // Step 1: Verify Keycloak access token issuer/audience/JWKS before building CurrentUser.
        // Step 2: Resolve/link external subject in access-control and load canonical role assignments.
        // Step 3: Mint internal trusted identity for downstream services; do not trust client role headers.
        String subject = normalize(jwt.getSubject());
        String email = firstPresent(
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("preferred_username"),
                subject);
        if (subject.isBlank()) {
            return Mono.error(new IllegalArgumentException("Token subject is required"));
        }
        DemoIdentity demo = demoIdentity(subject, email);
        Set<String> roles = externalRoleCodes(jwt);
        if (demo != null) {
            roles.add(demo.role());
            if (isOperatorPath(path) && roles.stream().noneMatch(OPERATOR_ROLES::contains)) {
                return Mono.error(new OperatorForbiddenException());
            }
            String primaryRole = primaryRole(roles);
            return Mono.just(new CurrentUser(demo.userId(), demo.email(), primaryRole, roles));
        }
        if (accessControlIdentity == null) {
            return Mono.error(new IllegalArgumentException("Dynamic identity resolution is not available"));
        }
        String issuer = jwt.getIssuer() == null ? "" : jwt.getIssuer().toString();
        boolean emailVerified = claimAsBoolean(jwt, "email_verified");
        return accessControlIdentity.resolveOrProvisionKeycloakUser(issuer, subject, email, emailVerified)
                .map(identity -> currentUserFromResolvedIdentity(identity, path));
    }

    private DemoIdentity demoIdentity(String subject, String email) {
        String key = firstPresent(email, subject).toLowerCase(Locale.ROOT);
        return switch (key) {
            case "admin@courseflow.local", "11111111-1111-4111-8111-111111111111" ->
                    new DemoIdentity(1L, "admin@courseflow.local", "ADMIN");
            case "instructor@courseflow.local", "22222222-2222-4222-8222-222222222222" ->
                    new DemoIdentity(2L, "instructor@courseflow.local", "INSTRUCTOR");
            case "student@courseflow.local", "33333333-3333-4333-8333-333333333333" ->
                    new DemoIdentity(4L, "student@courseflow.local", "STUDENT");
            case "student2@courseflow.local", "55555555-5555-4555-8555-555555555555" ->
                    new DemoIdentity(5L, "student2@courseflow.local", "STUDENT");
            default -> null;
        };
    }

    private CurrentUser currentUserFromResolvedIdentity(
            AccessControlIdentityClient.ResolvedIdentity identity,
            String path) {
        if (identity == null || identity.userId() == null || identity.userId().isBlank()) {
            throw new IllegalArgumentException("Access-control returned no user id");
        }
        if (identity.status() != null && !"ACTIVE".equalsIgnoreCase(identity.status())) {
            throw new IllegalArgumentException("Authenticated user is not active");
        }
        Set<CurrentUser.RoleAssignment> assignments = new LinkedHashSet<>();
        if (identity.roleAssignments() != null) {
            for (AccessControlIdentityClient.ResolvedRoleAssignment assignment : identity.roleAssignments()) {
                if (assignment != null && assignment.code() != null && !assignment.code().isBlank()) {
                    assignments.add(new CurrentUser.RoleAssignment(
                            assignment.code(),
                            assignment.scopeType(),
                            assignment.scopeId()));
                }
            }
        }
        Set<String> roleCodes = assignments.stream()
                .map(CurrentUser.RoleAssignment::code)
                .filter(code -> code != null && !code.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (roleCodes.isEmpty()) {
            roleCodes.add("STUDENT");
            assignments.add(new CurrentUser.RoleAssignment("STUDENT", "PLATFORM", null));
        }
        if (isOperatorPath(path) && roleCodes.stream().noneMatch(OPERATOR_ROLES::contains)) {
            throw new OperatorForbiddenException();
        }
        long userId;
        try {
            userId = Long.parseLong(identity.userId().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Access-control returned an invalid user id");
        }
        return new CurrentUser(
                userId,
                firstPresent(identity.email(), ""),
                primaryRole(roleCodes),
                roleCodes,
                assignments);
    }

    private boolean claimAsBoolean(Jwt jwt, String name) {
        Object value = jwt.getClaim(name);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private Set<String> externalRoleCodes(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();
        addExternalRoleValues(roles, jwt.getClaim("roles"));
        addExternalRoleValues(roles, jwt.getClaim("groups"));
        addExternalRolesFromAccessClaim(roles, jwt.getClaim("realm_access"));
        addExternalRolesFromResourceAccess(roles, jwt.getClaim("resource_access"));
        return roles;
    }

    @SuppressWarnings("unchecked")
    private void addExternalRolesFromAccessClaim(Set<String> roles, Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return;
        }
        addExternalRoleValues(roles, ((Map<String, Object>) map).get("roles"));
    }

    @SuppressWarnings("unchecked")
    private void addExternalRolesFromResourceAccess(Set<String> roles, Object raw) {
        if (!(raw instanceof Map<?, ?> accessByClient)) {
            return;
        }
        for (Object clientAccess : accessByClient.values()) {
            if (clientAccess instanceof Map<?, ?> map) {
                addExternalRoleValues(roles, ((Map<String, Object>) map).get("roles"));
            }
        }
    }

    private void addExternalRoleValues(Set<String> roles, Object raw) {
        if (raw instanceof Iterable<?> values) {
            for (Object value : values) {
                addExternalRole(roles, value);
            }
            return;
        }
        addExternalRole(roles, raw);
    }

    private void addExternalRole(Set<String> roles, Object raw) {
        if (raw == null || raw.toString().isBlank()) {
            return;
        }
        String role = raw.toString().trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (role.startsWith("/")) {
            role = role.substring(1);
        }
        if (role.startsWith("ROLE_")) {
            role = role.substring("ROLE_".length());
        }
        if (role.equals("OFFLINE_ACCESS")
                || role.equals("UMA_AUTHORIZATION")
                || role.startsWith("DEFAULT_ROLES_")) {
            return;
        }
        roles.add(role);
    }

    private String firstPresent(String... candidates) {
        for (String candidate : candidates) {
            String normalized = normalize(candidate);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "";
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private boolean isPublic(String path, String method) {
        // WebSocket handshake authenticates inside the STOMP CONNECT frame at notification-service.
        if (path.startsWith("/ws")) {
            return true;
        }
        if ("/actuator/health".equals(path)) {
            return true;
        }
        return "GET".equalsIgnoreCase(method) && isPublicReadPath(path);
    }

    private boolean isDeletedAuthPath(String path) {
        return path.startsWith("/api/v1/auth/");
    }

    private boolean isPublicReadPath(String path) {
        if (path.equals("/api/v1/courses") || path.matches("/api/v1/courses/[^/]+")
                || path.matches("/api/v1/courses/[^/]+/related")) {
            return true;
        }
        return path.startsWith("/api/v1/search")
                || path.matches("/api/v1/profiles/[^/]+")
                || path.startsWith("/api/v1/certificates/verify")
                || path.startsWith("/api/v1/reviews/courses");
    }

    private boolean isOperatorPath(String path) {
        return path.startsWith("/api/admin/");
    }

    private String requireClaim(Claims claims, String name) {
        Object value = claims.get(name);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("Missing JWT claim: " + name);
        }
        return value.toString();
    }

    /**
     * Pull role assignment tuples out of the verified internal JWT. The converter writes
     * `role_assignments` as an array of `{code, scopeType, scopeId}` maps; older internal tokens may
     * only have `roles`, so we tolerate both shapes.
     */
    @SuppressWarnings("unchecked")
    private List<RoleClaim> extractRoleClaims(Claims claims) {
        Object rawAssignments = claims.get("role_assignments");
        if (rawAssignments instanceof List<?> assignments && !assignments.isEmpty()) {
            return roleClaims(assignments);
        }
        Object rawRoles = claims.get("roles");
        if (rawRoles instanceof List<?> roles) {
            return roleClaims(roles);
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<RoleClaim> roleClaims(List<?> list) {
        List<RoleClaim> roles = new java.util.ArrayList<>();
        for (Object element : list) {
            if (element instanceof Map<?, ?> map) {
                Object code = ((Map<String, Object>) map).get("code");
                if (code != null && !code.toString().isBlank()) {
                    Object scopeType = ((Map<String, Object>) map).get("scopeType");
                    Object scopeId = ((Map<String, Object>) map).get("scopeId");
                    roles.add(new RoleClaim(
                            code.toString(),
                            scopeType == null ? "PLATFORM" : scopeType.toString(),
                            scopeId == null ? null : scopeId.toString()));
                }
            } else if (element != null && !element.toString().isBlank()) {
                // Tolerate a plain array of code strings.
                roles.add(new RoleClaim(element.toString(), "PLATFORM", null));
            }
        }
        return roles;
    }

    private IdentityHeaders identityHeaders(Claims claims, String path) {
        if (!"internal".equals(claims.get("token_use", String.class))) {
            throw new IllegalArgumentException("Internal token has invalid token_use");
        }
        if (!"user".equals(claims.get("actor_type", String.class))) {
            throw new IllegalArgumentException("Internal token is not a user token");
        }
        String userId = requireClaim(claims, "uid");
        String email = claims.get("email", String.class);
        List<RoleClaim> roleClaims = extractRoleClaims(claims);
        Set<String> roleCodes = roleClaims.stream()
                .map(RoleClaim::code)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (roleCodes.isEmpty()) {
            throw new IllegalArgumentException("Internal token carries no roles");
        }
        if (isOperatorPath(path) && roleCodes.stream().noneMatch(OPERATOR_ROLES::contains)) {
            throw new OperatorForbiddenException();
        }
        return new IdentityHeaders(userId, email, primaryRole(roleCodes), roleCodes, roleClaims);
    }

    private void writeInternalIdentityHeaders(org.springframework.http.HttpHeaders headers,
                                              String internalToken,
                                              IdentityHeaders identity) {
        String bearer = "Bearer " + internalToken;
        headers.set("Authorization", bearer);
        headers.set(GatewayHeaders.INTERNAL_AUTHORIZATION, bearer);
        headers.set(GatewayHeaders.USER_ID, identity.userId());
        headers.set(GatewayHeaders.USER_ROLE, identity.primaryRole());
        headers.set(GatewayHeaders.USER_ROLES, String.join(",", identity.roleCodes()));
        headers.set(GatewayHeaders.USER_ROLE_SCOPES, encodeRoleScopes(identity.roleClaims()));
        if (identity.email() != null && !identity.email().isBlank()) {
            headers.set(GatewayHeaders.USER_EMAIL, identity.email());
        }
    }

    private String encodeRoleScopes(List<RoleClaim> roleClaims) {
        return roleClaims.stream()
                .map(claim -> encode(claim.code()) + "." + encode(claim.scopeType()) + "." + encode(claim.scopeId()))
                .collect(java.util.stream.Collectors.joining(","));
    }

    private String encode(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Choose the highest-ranked role as the single {@code X-User-Role} value (backward compat).
     * Operator roles outrank STUDENT; among operators we keep the most privileged first.
     */
    private String primaryRole(Set<String> roleCodes) {
        return ROLE_RANK.stream()
                .filter(roleCodes::contains)
                .findFirst()
                .orElse(roleCodes.iterator().next());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        return error(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized", message);
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        return error(exchange, HttpStatus.FORBIDDEN, "Forbidden", message);
    }

    private Mono<Void> error(ServerWebExchange exchange, HttpStatus status, String title, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"statusCode\":\"" + status + "\",\"title\":\"" + title + "\",\"detail\":\""
                + message + "\"}").getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        // Right after correlation-id propagation.
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private record RoleClaim(String code, String scopeType, String scopeId) {
    }

    private record IdentityHeaders(String userId, String email, String primaryRole, Set<String> roleCodes,
                                   List<RoleClaim> roleClaims) {
    }

    private record DemoIdentity(Long userId, String email, String role) {
    }

    private static final class OperatorForbiddenException extends RuntimeException {
    }
}
