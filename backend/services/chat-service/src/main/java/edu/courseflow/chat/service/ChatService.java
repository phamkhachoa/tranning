package edu.courseflow.chat.service;

import edu.courseflow.chat.dto.ChatDtos.ChatMessageDto;
import edu.courseflow.chat.dto.ChatDtos.ChatRoomDto;
import edu.courseflow.chat.dto.ChatDtos.SendMessageRequestDto;
import edu.courseflow.chat.mapper.ChatMapper;
import edu.courseflow.chat.model.ChatMessage;
import edu.courseflow.chat.model.ChatRoom;
import edu.courseflow.chat.repository.ChatMessageRepository;
import edu.courseflow.chat.repository.ChatRoomRepository;
import edu.courseflow.commonlibrary.exception.BadRequestException;
import edu.courseflow.commonlibrary.security.CourseAccessClient;
import edu.courseflow.commonlibrary.web.CurrentUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    private final ChatRoomRepository rooms;
    private final ChatMessageRepository messages;
    private final ChatMapper mapper;
    private final CourseAccessClient courseAccess;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(ChatRoomRepository rooms, ChatMessageRepository messages, ChatMapper mapper,
                       CourseAccessClient courseAccess, SimpMessagingTemplate messagingTemplate) {
        this.rooms = rooms;
        this.messages = messages;
        this.mapper = mapper;
        this.courseAccess = courseAccess;
        this.messagingTemplate = messagingTemplate;
    }

    public ChatRoomDto getRoom(UUID courseId, CurrentUser user) {
        // TRAINING(service-day-12): Room bootstrap is already wired for the mini project.
        // Flow: controller path -> course access guard -> get-or-create room -> room DTO.
        // Exercise work continues in sendMessage where reply/attachment/broadcast rules are stricter.
        courseAccess.requireCourseAccess(user, courseId);
        return mapper.toDto(getOrCreateRoom(courseId));
    }

    public List<ChatMessageDto> listMessages(UUID courseId, Instant before, Integer limit, CurrentUser user) {
        // TRAINING(service-day-12): History pagination is the read side of chat.
        // Flow: authorize course access -> clamp limit -> Mongo newest-first cursor query -> reverse for UI.
        // Repository query is ChatMessageRepository#findByCourseIdAndDeletedAtIsNull... .
        courseAccess.requireCourseAccess(user, courseId);
        Pageable pageable = PageRequest.of(0, clampLimit(limit));
        List<ChatMessage> page = before == null
                ? messages.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(courseId.toString(), pageable).getContent()
                : messages.findByCourseIdAndDeletedAtIsNullAndCreatedAtBeforeOrderByCreatedAtDesc(
                        courseId.toString(), before, pageable).getContent();
        List<ChatMessage> ordered = new ArrayList<>(page);
        Collections.reverse(ordered);
        return mapper.toMessageDtos(ordered);
    }

    public ChatMessageDto sendMessage(UUID courseId, SendMessageRequestDto request, CurrentUser user) {
        // TODO(training-day-12-impl): Harden the canonical send path for both REST and STOMP.
        // Step 1: Keep senderId/senderEmail/senderName derived from CurrentUser; reject client identity fields.
        // Step 2: Validate body after trim and verify replyToMessageId belongs to this course and is not deleted.
        // Step 3: Validate each attachment against media-service metadata instead of trusting arbitrary URLs.
        // Step 4: Persist MongoDB history first, then broadcast saved DTO to /topic/courses/{courseId}/chat.
        // Step 5: If broadcast fails, keep the saved message and surface realtime failure separately.
        courseAccess.requireCourseAccess(user, courseId);
        if (request.body() == null || request.body().trim().isBlank()) {
            throw new BadRequestException("CHAT_MESSAGE_EMPTY");
        }
        ChatRoom room = getOrCreateRoom(courseId);
        Instant now = Instant.now();
        ChatMessage message = new ChatMessage();
        message.setRoomId(room.getId());
        message.setCourseId(courseId.toString());
        message.setSenderId(String.valueOf(user.id()));
        message.setSenderName(displayName(user));
        message.setSenderEmail(user.email());
        message.setMessageType("TEXT");
        message.setBody(request.body().trim());
        message.setAttachments(mapper.toAttachments(request.attachments()));
        message.setReplyToMessageId(blankToNull(request.replyToMessageId()));
        message.setCreatedAt(now);

        ChatMessageDto dto = mapper.toDto(messages.save(message));
        messagingTemplate.convertAndSend("/topic/courses/" + courseId + "/chat", dto);
        return dto;
    }

    private ChatRoom getOrCreateRoom(UUID courseId) {
        // TRAINING(service-day-12): Room identity is stable by courseId. Runtime get-or-create is
        // acceptable here; a later production design could create rooms from a course-published event.
        String courseKey = courseId.toString();
        return rooms.findByCourseId(courseKey).orElseGet(() -> createRoom(courseKey));
    }

    private ChatRoom createRoom(String courseId) {
        // TRAINING(service-day-12): ChatRoom.courseId has a unique Mongo index. Keep
        // DuplicateKeyException handling because two users may open the first room at the same time.
        ChatRoom room = new ChatRoom();
        room.setCourseId(courseId);
        room.setTitle("Course chat");
        room.setStatus("ACTIVE");
        room.setCreatedAt(Instant.now());
        try {
            return rooms.save(room);
        } catch (DuplicateKeyException ex) {
            return rooms.findByCourseId(courseId).orElseThrow(() -> ex);
        }
    }

    private int clampLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(MAX_LIMIT, limit));
    }

    private String displayName(CurrentUser user) {
        if (user.email() == null || user.email().isBlank()) {
            return "User " + user.id();
        }
        int at = user.email().indexOf('@');
        return at > 0 ? user.email().substring(0, at) : user.email();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
