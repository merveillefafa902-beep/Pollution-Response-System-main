package org.rrc.pollution_response_system.service;

import org.rrc.pollution_response_system.repository.PollutionCaseRepository;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {
    private final PollutionCaseRepository repo;
    public AnalyticsService(PollutionCaseRepository repo){ this.repo = repo; }
    // Placeholder if future complex analytics (e.g. ML risk scoring) are needed.
}
