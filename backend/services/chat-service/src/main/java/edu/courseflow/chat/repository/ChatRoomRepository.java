package edu.courseflow.chat.repository;

import edu.courseflow.chat.model.ChatRoom;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    // TRAINING(repo-day-12): Used by ChatService#getOrCreateRoom after
    // GET /api/v1/chat/courses/{courseId}/room or first message send. ChatRoom.courseId has a
    // unique index, so this lookup plus DuplicateKeyException handling makes room creation idempotent.
    Optional<ChatRoom> findByCourseId(String courseId);
}
