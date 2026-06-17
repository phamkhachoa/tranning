package edu.courseflow.chat.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ChatStompAuthInterceptor authInterceptor;
    private final String[] allowedOrigins;

    public ChatWebSocketConfig(ChatStompAuthInterceptor authInterceptor,
                               @Value("${courseflow.chat.websocket.allowed-origins:}") String allowedOrigins) {
        this.authInterceptor = authInterceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // TRAINING(websocket-day-12): Public WebSocket endpoint is /ws/chat through gateway.
        // Browser/mobile clients connect here first, then use STOMP destinations configured below.
        // Keep allowed origins strict outside local training.
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns(allowedOrigins.length == 0 ? new String[] {"http://localhost:*"} : allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // TRAINING(websocket-day-12): Destination contract:
        // - subscribe: /topic/courses/{courseId}/chat
        // - send: /app/courses/{courseId}/send
        // The simple broker is enough for training. Production can replace this with an external
        // broker relay when multiple chat-service instances need cross-node fanout.
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // TRAINING(websocket-day-12): All inbound CONNECT/SUBSCRIBE/SEND frames must pass
        // ChatStompAuthInterceptor so clients cannot bypass REST gateway auth by using WebSocket.
        registration.interceptors(authInterceptor);
    }
}
