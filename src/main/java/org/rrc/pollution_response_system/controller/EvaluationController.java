package org.rrc.pollution_response_system.controller;

import org.rrc.pollution_response_system.entity.PostCaseEvaluation;
import org.rrc.pollution_response_system.service.PostCaseEvaluationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
@CrossOrigin(origins = "*")
public class EvaluationController {

    private final PostCaseEvaluationService evaluationService;

    public EvaluationController(PostCaseEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    // Citizens submit evaluation for a report they created (checked indirectly by auth-based policy in UI; backend checks InvestigationStatus)
    @PreAuthorize("hasAnyRole('CITIZEN','FIELD_INSPECTOR','ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @PostMapping("/report/{reportId}")
    public PostCaseEvaluation submit(@PathVariable Long reportId,
                                         @RequestParam int rating,
                                         @RequestParam(required = false) String comments,
                                         Authentication auth){
        return evaluationService.submitEvaluation(reportId, rating, comments, auth);
    }

    // Admin/Authority can view evaluations on a report
    @PreAuthorize("hasAnyRole('ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @GetMapping("/report/{reportId}")
    public List<PostCaseEvaluation> list(@PathVariable Long reportId){
        return evaluationService.getEvaluationsForReport(reportId);
    }
}
