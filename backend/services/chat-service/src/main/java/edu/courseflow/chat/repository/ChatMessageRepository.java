package edu.courseflow.chat.repository;

import edu.courseflow.chat.model.ChatMessage;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    // TRAINING(repo-day-12): Read model for
    // GET /api/v1/chat/courses/{courseId}/messages without cursor. Query newest-first for Mongo
    // efficiency; ChatService reverses the page before returning it to the UI.
    Page<ChatMessage> findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(String courseId, Pageable pageable);

    // TRAINING(repo-day-12): Cursor query for older chat messages. The cursor is createdAt from the
    // oldest message currently loaded by the client; deleted messages stay hidden from learner UI.
    Page<ChatMessage> findByCourseIdAndDeletedAtIsNullAndCreatedAtBeforeOrderByCreatedAtDesc(
            String courseId, Instant before, Pageable pageable);
}
