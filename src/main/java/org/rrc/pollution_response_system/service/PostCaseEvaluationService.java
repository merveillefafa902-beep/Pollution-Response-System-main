package org.rrc.pollution_response_system.service;

import org.rrc.pollution_response_system.entity.PollutionCase;
import org.rrc.pollution_response_system.entity.PostCaseEvaluation;
import org.rrc.pollution_response_system.repository.PollutionCaseRepository;
import org.rrc.pollution_response_system.repository.PostCaseEvaluationRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostCaseEvaluationService {

    private final PostCaseEvaluationRepository evaluationRepository;
    private final PollutionCaseRepository reportRepository;

    public PostCaseEvaluationService(PostCaseEvaluationRepository evaluationRepository,
                                         PollutionCaseRepository reportRepository) {
        this.evaluationRepository = evaluationRepository;
        this.reportRepository = reportRepository;
    }

    public PostCaseEvaluation submitEvaluation(Long reportId, int rating, String comments, Authentication auth){
        if(auth == null) throw new AccessDeniedException("Unauthenticated");
        var report = reportRepository.findById(reportId).orElseThrow(() -> new IllegalArgumentException("Report not found"));
        if(report.getInvestigationStatus() != PollutionCase.InvestigationStatus.RESOLVED){
            throw new IllegalStateException("Evaluation allowed only after case is resolved");
        }
        // Allow reporter to submit; also allow admin/authority to record evaluations if needed
        String username = auth.getName();
        boolean isOwner = report.getReporter() != null && report.getReporter().equalsIgnoreCase(username);
        boolean privileged = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_ENVIRONMENTAL_AUTHORITY"));
        if(!isOwner && !privileged){
            throw new AccessDeniedException("Only the original reporter or an authority can submit evaluation");
        }
        PostCaseEvaluation eval = new PostCaseEvaluation();
        eval.setReport(report);
        eval.setSubmittedBy(username);
        eval.setRating(Math.max(1, Math.min(5, rating)));
        eval.setComments(comments);
        return evaluationRepository.save(eval);
    }

    public List<PostCaseEvaluation> getEvaluationsForReport(Long reportId){
        return evaluationRepository.findByReport_Id(reportId);
    }
}
