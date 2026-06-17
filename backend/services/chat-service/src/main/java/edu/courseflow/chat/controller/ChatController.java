package edu.courseflow.chat.controller;

import edu.courseflow.chat.dto.ChatDtos.ChatMessageDto;
import edu.courseflow.chat.dto.ChatDtos.ChatRoomDto;
import edu.courseflow.chat.dto.ChatDtos.SendMessageRequestDto;
import edu.courseflow.chat.service.ChatService;
import edu.courseflow.commonlibrary.web.CurrentUser;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    // TRAINING(controller-day-12): Public API via gateway should be
    // GET /api/v1/chat/courses/{courseId}/room.
    // Purpose: web/learning app calls this before opening a course chat panel to ensure the room
    // exists and to display room metadata. Internal service path stays /internal/chat/... after
    // gateway rewrite. Do not accept roomId or userId from client; derive access from CurrentUser.
    @GetMapping("/internal/chat/courses/{courseId}/room")
    public ChatRoomDto getRoom(@PathVariable UUID courseId, CurrentUser user) {
        return chat.getRoom(courseId, user);
    }

    // TRAINING(controller-day-12): Public API via gateway should be
    // GET /api/v1/chat/courses/{courseId}/messages?before={isoInstant}&limit={1..100}.
    // Purpose: load initial history and older-message pagination for web, mobile and admin support.
    // Result must be oldest -> newest so UI can append naturally. Query must filter by course room
    // and deletedAt=null; never return another course's messages.
    @GetMapping("/internal/chat/courses/{courseId}/messages")
    public List<ChatMessageDto> listMessages(@PathVariable UUID courseId,
                                             @RequestParam(required = false)
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
                                             @RequestParam(required = false) Integer limit,
                                             CurrentUser user) {
        return chat.listMessages(courseId, before, limit, user);
    }

    // TRAINING(controller-day-12): Public API via gateway should be
    // POST /api/v1/chat/courses/{courseId}/messages.
    // Purpose: HTTP fallback for sending a message when WebSocket is unavailable, and a simple API
    // for mobile clients. Body should be { body, attachments?, replyToMessageId? }. senderId,
    // senderName, senderEmail, roomId and courseId must come from path + CurrentUser/server state.
    // The same service method should persist to MongoDB and broadcast to /topic/courses/{courseId}/chat.
    @PostMapping("/internal/chat/courses/{courseId}/messages")
    public ChatMessageDto sendMessage(@PathVariable UUID courseId,
                                      @Valid @RequestBody SendMessageRequestDto request,
                                      CurrentUser user) {
        return chat.sendMessage(courseId, request, user);
    }
}
