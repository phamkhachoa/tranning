package edu.courseflow.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

    private final JwtGatewayProperties properties;

    public JwtAuthenticationGatewayFilter(JwtGatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        ServerHttpRequest.Builder builder = request.mutate().headers(headers -> {
            /*
             * Training note:
             * Always remove identity headers supplied by the client. Only the gateway is allowed
             * to create X-User-* headers after it verifies the JWT.
             */
            headers.remove("X-User-Id");
            headers.remove("X-User-Email");
            headers.remove("X-User-Roles");
        });

        if (isPublic(path)) {
            return chain.filter(exchange.mutate().request(builder.build()).build());
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return error(exchange, HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(properties.secretKey())
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(authHeader.substring(7))
                    .getPayload();
            builder.header("X-User-Id", claims.get("uid", String.class));
            builder.header("X-User-Email", claims.getSubject());
            builder.header("X-User-Roles", roles(claims));
            return chain.filter(exchange.mutate().request(builder.build()).build());
        } catch (JwtException | IllegalArgumentException ex) {
            return error(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }

    private boolean isPublic(String path) {
        return "/actuator/health".equals(path) || path.startsWith("/api/v1/auth/");
    }

    @SuppressWarnings("unchecked")
    private String roles(Claims claims) {
        Object raw = claims.get("roles");
        if (raw instanceof List<?> list) {
            return String.join(",", ((List<Object>) list).stream().map(Object::toString).toList());
        }
        return "";
    }

    private Mono<Void> error(ServerWebExchange exchange, HttpStatus status, String detail) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"status\":" + status.value() + ",\"detail\":\"" + detail + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
