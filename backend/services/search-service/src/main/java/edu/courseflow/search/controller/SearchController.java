package edu.courseflow.search.controller;

import edu.courseflow.commonlibrary.web.CurrentUser;
import edu.courseflow.search.dto.SearchDtos.CourseRecommendationDto;
import edu.courseflow.search.dto.SearchDtos.CourseSearchDto;
import edu.courseflow.search.dto.SearchDtos.CourseSearchPageDto;
import edu.courseflow.search.dto.SearchDtos.IndexCourseRequestDto;
import edu.courseflow.search.service.SearchService;
import edu.courseflow.search.web.Authz;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {

    private final SearchService search;

    public SearchController(SearchService search) {
        this.search = search;
    }

    /**
     * TRAINING(controller-day-13): Public search API exposed through gateway as
     * GET /api/v1/search/courses?q=&page=&size=.
     * Purpose: learner web/mobile course discovery. Execute Elasticsearch full-text search and
     * return page hits plus total hit count; do not use SQL LIKE or load all courses into memory.
     */
    @GetMapping("/public/search/courses")
    public CourseSearchPageDto publicCourses(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return search.searchPublicCourses(q, page, size);
    }

    /**
     * TRAINING(controller-day-13): Autocomplete API exposed through gateway as
     * GET /api/v1/search/courses/suggest?q=&limit=.
     * Purpose: fast typeahead for web/mobile search boxes using an Elasticsearch prefix-style query.
     */
    @GetMapping("/public/search/courses/suggest")
    public List<CourseSearchDto> suggestCourses(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "6") int limit) {
        return search.suggestPublicCourses(q, limit);
    }

    /**
     * TRAINING(controller-day-13): Recommendation-lite API exposed through gateway as
     * GET /api/v1/search/courses/recommendations?q=&level=&departmentId=&limit=.
     * Purpose: demonstrate quick search-based recommendation without building an ML pipeline.
     */
    @GetMapping("/public/search/courses/recommendations")
    public List<CourseRecommendationDto> recommendedCourses(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String departmentId,
            @RequestParam(defaultValue = "6") int limit) {
        return search.recommendPublicCourses(q, level, departmentId, limit);
    }

    /**
     * TRAINING(controller-day-13): Manual backfill API exposed through gateway as
     * POST /api/admin/v1/search/courses.
     * Purpose: staff/admin can reindex one course for debug. Normal sync comes from Debezium CDC,
     * so this endpoint is not the primary write path.
     */
    @PostMapping("/internal/search/courses")
    public CourseSearchDto indexCourse(@Valid @RequestBody IndexCourseRequestDto request, CurrentUser user) {
        Authz.requireStaff(user);
        return search.indexCourse(request);
    }
}
