package edu.courseflow.course.repository;

import edu.courseflow.course.model.CourseModule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseModuleJpaRepository extends JpaRepository<CourseModule, UUID> {

    // TRAINING(repo-day-07): Used by learner/player APIs to show only active curriculum modules
    // for GET /api/v1/courses/{courseId}/modules and /player.
    List<CourseModule> findByCourseIdAndStatusOrderByPositionAsc(UUID courseId, String status);

    // TRAINING(repo-day-07): Used by admin authoring/reorder screens where draft and archived
    // states may need to be inspected by staff.
    List<CourseModule> findByCourseIdOrderByPositionAsc(UUID courseId);

    Optional<CourseModule> findByIdAndCourseId(UUID id, UUID courseId);

    int countByCourseIdAndStatus(UUID courseId, String status);

    // TRAINING(repo-day-07): Position helper for module creation. Learners should see stable order;
    // authoring code should not trust client-provided gaps or duplicate positions.
    @Query("""
            select coalesce(max(m.position), -1) + 1
            from CourseModule m
            where m.courseId = :courseId
              and m.status <> 'ARCHIVED'
            """)
    int nextPosition(@Param("courseId") UUID courseId);
}
