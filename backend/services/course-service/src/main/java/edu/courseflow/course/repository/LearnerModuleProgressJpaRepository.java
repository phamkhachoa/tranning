package edu.courseflow.course.repository;

import edu.courseflow.course.model.LearnerModuleProgress;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LearnerModuleProgressJpaRepository extends JpaRepository<LearnerModuleProgress, UUID> {

    // TRAINING(repo-day-07): Idempotent module progress lookup before insert/update.
    Optional<LearnerModuleProgress> findByModuleIdAndStudentId(UUID moduleId, String studentId);

    // TRAINING(repo-day-07): Prerequisite guard for learner progress. Service decides whether a
    // module can be completed based on this persisted status.
    boolean existsByModuleIdAndStudentIdAndStatus(UUID moduleId, String studentId, String status);

    // TRAINING(repo-day-07): Course-level progress aggregate for GET /api/v1/courses/{courseId}/modules/progress.
    @Query("""
            select count(p)
            from LearnerModuleProgress p, CourseModule m
            where m.id = p.moduleId
              and m.courseId = :courseId
              and m.status = 'PUBLISHED'
              and p.studentId = :studentId
              and p.status = 'COMPLETED'
            """)
    int countCompletedPublishedModules(@Param("courseId") UUID courseId, @Param("studentId") String studentId);
}
