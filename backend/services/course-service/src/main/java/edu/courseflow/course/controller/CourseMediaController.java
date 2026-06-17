package edu.courseflow.course.controller;

import edu.courseflow.commonlibrary.web.CurrentUser;
import edu.courseflow.course.dto.CourseDtos.PresignedDownloadDto;
import edu.courseflow.course.service.CourseModuleService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/courses/{courseId}/media")
public class CourseMediaController {

    private final CourseModuleService modules;

    public CourseMediaController(CourseModuleService modules) {
        this.modules = modules;
    }

    // TRAINING(controller-day-07): Media playback helper intended as
    // GET /api/v1/courses/{courseId}/media/assets/{mediaId}/download-url.
    // Purpose: learner player asks course-service to verify enrollment/course visibility before
    // returning a media-service signed download URL. Do not let clients call MinIO keys directly.
    @GetMapping("/assets/{mediaId}/download-url")
    public PresignedDownloadDto downloadUrl(@PathVariable UUID courseId,
                                            @PathVariable UUID mediaId,
                                            CurrentUser user) {
        return modules.downloadPublishedMedia(courseId, mediaId, user);
    }
}
