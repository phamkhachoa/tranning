package edu.courseflow.gateway;

import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
class HttpInternalTokenConverterClient implements InternalTokenConverterClient {

    static final String TOKEN_EXCHANGE_GRANT = "urn:ietf:params:oauth:grant-type:token-exchange";
    static final String ACCESS_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:access_token";

    private final WebClient webClient;
    private final TokenConverterProperties properties;

    HttpInternalTokenConverterClient(WebClient.Builder webClientBuilder,
            TokenConverterProperties properties) {
        this.webClient = webClientBuilder.baseUrl(properties.uri()).build();
        this.properties = properties;
    }

    @Override
    public boolean enabled() {
        return properties.mode() == TokenConverterProperties.Mode.REQUIRED;
    }

    @Override
    public boolean required() {
        return properties.mode() == TokenConverterProperties.Mode.REQUIRED;
    }

    @Override
    public boolean localIdentityMode() {
        return properties.mode() == TokenConverterProperties.Mode.LOCAL;
    }

    @Override
    public Mono<String> exchange(String subjectToken) {
        // TODO(training-day-17-impl): Treat token exchange like a critical downstream dependency.
        // Step 1: Keep timeout lower than the gateway request timeout and expose it via config.
        // Step 2: Add metrics for success/error/timeout without labeling by token, subject or email.
        // Step 3: If retry is added, retry only network/5xx transient errors and keep retry count
        //         tiny. A bad/expired token should fail once and return unauthorized.
        if (!enabled()) {
            return Mono.empty();
        }
        return webClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", TOKEN_EXCHANGE_GRANT)
                        .with("subject_token_type", ACCESS_TOKEN_TYPE)
                        .with("subject_token", subjectToken)
                        .with("audience", properties.audience())
                        .with("client_id", properties.clientId())
                        .with("client_secret", properties.clientSecret()))
                .retrieve()
                .bodyToMono(TokenExchangeResponse.class)
                .map(TokenExchangeResponse::access_token)
                .filter(token -> token != null && !token.isBlank())
                .timeout(Duration.ofMillis(properties.timeoutMs()));
    }

    record TokenExchangeResponse(String access_token, String issued_token_type, String token_type, long expires_in,
            String scope) {
    }
}
