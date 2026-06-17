package edu.courseflow.gateway;

import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

public interface GatewayExternalTokenVerifier {
    Mono<Jwt> verify(String token);
}
