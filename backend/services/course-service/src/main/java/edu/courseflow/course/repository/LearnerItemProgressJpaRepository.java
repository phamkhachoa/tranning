package edu.courseflow.course.repository;

import edu.courseflow.course.model.LearnerItemProgress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearnerItemProgressJpaRepository extends JpaRepository<LearnerItemProgress, UUID> {

    // TRAINING(repo-day-07): Idempotent item completion lookup for
    // POST /api/v1/courses/{courseId}/modules/{moduleId}/items/{itemId}/progress.
    Optional<LearnerItemProgress> findByItemIdAndStudentId(UUID itemId, String studentId);

    // TRAINING(repo-day-07): Course progress read model. CourseModuleService derives percent from
    // stored rows instead of accepting client-provided completion percent.
    List<LearnerItemProgress> findByCourseIdAndStudentId(UUID courseId, String studentId);

    // TRAINING(repo-day-07): Module completion recomputation after one item changes.
    List<LearnerItemProgress> findByModuleIdAndStudentId(UUID moduleId, String studentId);
}
