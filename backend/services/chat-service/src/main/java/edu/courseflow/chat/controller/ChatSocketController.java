package edu.courseflow.chat.controller;

import edu.courseflow.chat.dto.ChatDtos.SendMessageRequestDto;
import edu.courseflow.chat.security.ChatPrincipal;
import edu.courseflow.chat.service.ChatService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class ChatSocketController {

    private final ChatService chat;

    public ChatSocketController(ChatService chat) {
        this.chat = chat;
    }

    // TRAINING(controller-day-12): Realtime API contract:
    // 1. Client connects to WebSocket endpoint /ws/chat with STOMP header Authorization: Bearer <accessToken>.
    // 2. Client subscribes to /topic/courses/{courseId}/chat to receive persisted messages.
    // 3. Client sends to /app/courses/{courseId}/send with the same SendMessageRequestDto used by REST.
    // Purpose: realtime send path for learner web/mobile. The payload must not contain senderId,
    // senderName, role, roomId or courseId. ChatStompAuthInterceptor must authenticate CONNECT and
    // authorize both SUBSCRIBE and SEND against course membership/staff access.
    @MessageMapping("/courses/{courseId}/send")
    public void send(@DestinationVariable UUID courseId,
                     @Valid @Payload SendMessageRequestDto request,
                     Principal principal) {
        if (!(principal instanceof ChatPrincipal chatUser)) {
            throw new MessagingException("Authenticated STOMP user required");
        }
        chat.sendMessage(courseId, request, chatUser.toCurrentUser());
    }
}
