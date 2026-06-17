package edu.courseflow.course.controller;

import edu.courseflow.commonlibrary.web.CurrentUser;
import edu.courseflow.course.dto.LearningDtos.LearnerNextActionDto;
import edu.courseflow.course.service.LearnerNextActionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LearnerNextActionController {

    private final LearnerNextActionService nextActions;

    public LearnerNextActionController(LearnerNextActionService nextActions) {
        this.nextActions = nextActions;
    }

    // TRAINING(controller-day-07): Learner dashboard helper intended as
    // GET /api/v1/learning/next-action.
    // Purpose: web/mobile home screen shows the next lesson/quiz/assignment for CurrentUser.
    // It must derive learner identity from CurrentUser and aggregate from enrollments/progress.
    @GetMapping("/internal/learning/next-action")
    public LearnerNextActionDto nextAction(CurrentUser user) {
        return nextActions.nextAction(user);
    }
}
