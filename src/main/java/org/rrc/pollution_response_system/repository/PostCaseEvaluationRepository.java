package org.rrc.pollution_response_system.repository;

import org.rrc.pollution_response_system.entity.PostCaseEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostCaseEvaluationRepository extends JpaRepository<PostCaseEvaluation, Long> {
    List<PostCaseEvaluation> findByReport_Id(Long reportId);
    List<PostCaseEvaluation> findBySubmittedBy(String username);
    void deleteByReport_Id(Long reportId);
}
