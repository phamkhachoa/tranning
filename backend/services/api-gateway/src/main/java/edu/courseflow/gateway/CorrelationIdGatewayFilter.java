package edu.courseflow.gateway;

import edu.courseflow.commonlibrary.constants.GatewayHeaders;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Ensures every request entering the platform carries a correlation id so a single trace id can
 * be followed across services and Kafka. Runs before authentication.
 */
@Component
public class CorrelationIdGatewayFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // TODO(training-day-16-impl): Turn correlation id into a usable debugging path.
        // Step 1: Keep accepting X-Correlation-Id from clients for manual evidence, but generate one
        //         when missing and always return it in the response header.
        // Step 2: Add structured log fields around route id, method, path template, status and
        //         latency. Do not log access tokens, raw query strings or request bodies.
        // Step 3: Verify the same id appears in gateway logs, downstream service logs and any
        //         async/event payload that is part of the request flow.
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(GatewayHeaders.CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        String finalId = correlationId;
        ServerHttpRequest mutated = request.mutate()
                .header(GatewayHeaders.CORRELATION_ID, finalId)
                .build();
        exchange.getResponse().getHeaders().set(GatewayHeaders.CORRELATION_ID, finalId);
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
