package org.rrc.pollution_response_system.service;

import org.rrc.pollution_response_system.entity.PollutionCase;
import org.rrc.pollution_response_system.entity.User;
import org.rrc.pollution_response_system.repository.PollutionCaseRepository;
import org.rrc.pollution_response_system.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DailySummaryScheduler {

    private final PollutionCaseRepository reportRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    public DailySummaryScheduler(PollutionCaseRepository reportRepository,
                                 UserRepository userRepository,
                                 MailService mailService) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    // Every day at 08:00 server time
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDailyAdminSummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusDays(1);
        List<PollutionCase> lastDay = reportRepository.findByReportedAtGreaterThanEqual(since);

        Map<String, Long> byStatus = lastDay.stream()
                .collect(Collectors.groupingBy(r -> r.getInvestigationStatus() != null ? r.getInvestigationStatus().name() : "UNKNOWN", Collectors.counting()));

        long pending = byStatus.getOrDefault(PollutionCase.InvestigationStatus.PENDING.name(), 0L);
        long inProgress = byStatus.getOrDefault(PollutionCase.InvestigationStatus.IN_PROGRESS.name(), 0L);
        long resolved = byStatus.getOrDefault(PollutionCase.InvestigationStatus.RESOLVED.name(), 0L);

        StringBuilder body = new StringBuilder();
        body.append("Daily Case Summary (last 24h)\n");
        body.append("Window: ")
                .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(since))
                .append(" to ")
                .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(now))
                .append("\n\n");
        body.append("Totals by InvestigationStatus:\n")
                .append("- PENDING: ").append(pending).append("\n")
                .append("- IN_PROGRESS: ").append(inProgress).append("\n")
                .append("- RESOLVED: ").append(resolved).append("\n\n");

        // List up to 5 most recent cases
        body.append("Recent cases:\n");
        lastDay.stream()
                .sorted((a,b) -> b.getReportedAt().compareTo(a.getReportedAt()))
                .limit(5)
                .forEach(r -> body.append("#").append(r.getId())
                        .append(" ").append(r.getPollutionCategory())
                        .append(" [").append(r.getSeverity()).append("] ")
                        .append(r.getLocation() != null ? r.getLocation() : "")
                        .append(" @ ")
                        .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(r.getReportedAt()))
                        .append("\n"));

        String subject = "Daily Case Summary";
        userRepository.findByRole(User.Role.ADMIN).stream()
                .map(User::getEmail)
                .filter(e -> e != null && !e.isBlank())
                .forEach(email -> mailService.send(email, subject, body.toString()));
    }
}
