package edu.courseflow.chat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class ChatDtos {

    private ChatDtos() {
    }

    public record ChatRoomDto(
            String id,
            String courseId,
            String title,
            String status,
            Instant createdAt
    ) {
    }

    public record ChatMessageDto(
            String id,
            String roomId,
            String courseId,
            String senderId,
            String senderName,
            String senderEmail,
            String messageType,
            String body,
            List<ChatAttachmentDto> attachments,
            String replyToMessageId,
            Instant editedAt,
            Instant deletedAt,
            Instant createdAt
    ) {
    }

    public record ChatAttachmentDto(
            // TRAINING(request-day-12): Attachments should reference media-service outputs. In a real
            // implementation, validate mediaId/storage ownership and avoid trusting arbitrary URLs.
            @Size(max = 80) String mediaId,
            @Size(max = 180) String fileName,
            @Size(max = 120) String contentType,
            @Size(max = 600) String url
    ) {
    }

    // TRAINING(request-day-12): This request is shared by REST POST /api/v1/chat/courses/{courseId}/messages
    // and STOMP SEND /app/courses/{courseId}/send. Keep it small: message body, optional media
    // attachments and optional reply target only. Never add senderId, senderRole, senderName,
    // courseId or roomId to client input; those values come from path, authenticated user and room lookup.
    public record SendMessageRequestDto(
            @NotBlank @Size(max = 2000) String body,
            @Size(max = 8) List<@Valid ChatAttachmentDto> attachments,
            @Size(max = 80) String replyToMessageId
    ) {
    }
}
