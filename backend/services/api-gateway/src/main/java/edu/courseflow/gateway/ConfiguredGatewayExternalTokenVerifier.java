package edu.courseflow.gateway;

import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ConfiguredGatewayExternalTokenVerifier implements GatewayExternalTokenVerifier {

    private final ExternalTokenProperties properties;
    private final ReactiveJwtDecoder oidcDecoder;

    public ConfiguredGatewayExternalTokenVerifier(ExternalTokenProperties properties) {
        this.properties = properties;
        this.oidcDecoder = oidcDecoder(properties);
    }

    @Override
    public Mono<Jwt> verify(String token) {
        if (token == null || token.isBlank()) {
            return Mono.error(new IllegalArgumentException("Bearer token is missing"));
        }
        return oidcDecoder.decode(token.trim());
    }

    private ReactiveJwtDecoder oidcDecoder(ExternalTokenProperties properties) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
        decoder.setJwtValidator(OAuth2AudienceValidator.issuerAndAudience(
                properties.oidcIssuer(),
                properties.audiences()));
        return decoder;
    }
}
