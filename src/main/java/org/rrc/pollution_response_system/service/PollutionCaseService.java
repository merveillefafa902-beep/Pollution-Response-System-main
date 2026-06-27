package org.rrc.pollution_response_system.service;

import org.rrc.pollution_response_system.entity.PollutionCase;
import org.rrc.pollution_response_system.entity.Region;
import org.rrc.pollution_response_system.entity.User;
import org.rrc.pollution_response_system.repository.PollutionCaseRepository;
import org.rrc.pollution_response_system.repository.RegionRepository;
import org.rrc.pollution_response_system.repository.UserRepository;
import org.rrc.pollution_response_system.repository.NotificationRepository;
import org.rrc.pollution_response_system.repository.PostCaseEvaluationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PollutionCaseService {

    private final PollutionCaseRepository reportRepository;
    private final RegionRepository regionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PostCaseEvaluationRepository postCaseEvaluationRepository;

    public PollutionCaseService(PollutionCaseRepository reportRepository,
            RegionRepository regionRepository,
            SimpMessagingTemplate messagingTemplate,
            NotificationService notificationService,
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            PostCaseEvaluationRepository postCaseEvaluationRepository) {
        this.reportRepository = reportRepository;
        this.regionRepository = regionRepository;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.postCaseEvaluationRepository = postCaseEvaluationRepository;
    }

    // ✅ Citizen submits new report (with duplicate detection)

    public PollutionCase createReport(PollutionCase report) {
        // Initial InvestigationStatus defaults to PENDING (entity sets it)
        // Enforce server-side timestamp to prevent spoofing
        report.setReportedAt(java.time.LocalDateTime.now());

        // Active duplicate prevention: if a recent nearby report of same type exists,
        // reject
        if (report.getLatitude() != null && report.getLongitude() != null) {
            double delta = 0.01; // ~1km bounding box approximation
            LocalDateTime windowEnd = LocalDateTime.now();
            LocalDateTime windowStart = windowEnd.minusMinutes(30);
            var possibles = reportRepository.findPotentialDuplicates(report.getLatitude(), report.getLongitude(), delta,
                    windowStart, windowEnd);
            boolean duplicate = possibles.stream().anyMatch(r -> (r.getPollutionCategory() != null
                    && r.getPollutionCategory().equalsIgnoreCase(report.getPollutionCategory())));
            if (duplicate) {
                throw new IllegalStateException("Duplicate case detected within time/location window");
            }
        }
        PollutionCase saved = reportRepository.save(report);
        // Broadcast new report to subscribers
        messagingTemplate.convertAndSend("/topic/reports", saved);

        // Notify Authorities and Admins about new report
        notificationService.sendNotification(
                new org.rrc.pollution_response_system.entity.Notification(
                        "New Pollution Case: " + saved.getPollutionCategory() + " at " + saved.getLocation(),
                        User.Role.ENVIRONMENTAL_AUTHORITY.name(),
                        saved));
        notificationService.sendNotification(
                new org.rrc.pollution_response_system.entity.Notification(
                        "New Pollution Case: " + saved.getPollutionCategory() + " at " + saved.getLocation(),
                        User.Role.ADMIN.name(),
                        saved));

        return saved;
    }

    // ✅ Authority or Admin retrieves all reports
    public List<PollutionCase> getAllReports() {
        return reportRepository.findAll();
    }

    // ✅ Authority verifies and updates report InvestigationStatus
    public PollutionCase updateStatus(Long id, PollutionCase.InvestigationStatus InvestigationStatus) {
        Optional<PollutionCase> optionalReport = reportRepository.findById(id);
        if (optionalReport.isPresent()) {
            PollutionCase report = optionalReport.get();
            // Enforce valid lifecycle transitions
            if (!isValidTransition(report.getInvestigationStatus(), InvestigationStatus)) {
                throw new IllegalStateException(
                        "Invalid InvestigationStatus transition from " + report.getInvestigationStatus() + " to " + InvestigationStatus);
            }
            // Role-based transition enforcement
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null)
                throw new AccessDeniedException("Unauthenticated");
            var authorities = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
            boolean isAdmin = authorities.contains("ROLE_ADMIN");
            boolean isAuthority = authorities.contains("ROLE_ENVIRONMENTAL_AUTHORITY");


            // Allow rules:
            // ADMIN: any valid transition (including backward)
            // AUTHORITY: Can move between pending, in-progress, and resolved states
            PollutionCase.InvestigationStatus from = report.getInvestigationStatus();
            boolean allowed = isAdmin || isAuthority;
            if (!allowed) {
                throw new AccessDeniedException("Role not permitted to perform this InvestigationStatus transition");
            }
            report.setInvestigationStatus(InvestigationStatus);
            PollutionCase saved = reportRepository.save(report);
            messagingTemplate.convertAndSend("/topic/reports", saved);
            // Broadcast a notification to relevant roles
            String targetRole = switch (InvestigationStatus) {
                case PENDING, IN_PROGRESS, REJECTED -> User.Role.ENVIRONMENTAL_AUTHORITY.name();
                case RESOLVED -> User.Role.ADMIN.name();
            };
            notificationService.sendNotification(
                    new org.rrc.pollution_response_system.entity.Notification(
                            "Report #" + saved.getId() + " InvestigationStatus updated to " + saved.getInvestigationStatus(),
                            targetRole,
                            saved));
            // Also notify authorities when resolved -> admin closure or responder transitions
            if (InvestigationStatus == PollutionCase.InvestigationStatus.RESOLVED) {
                notificationService.sendNotification(
                        new org.rrc.pollution_response_system.entity.Notification(
                                "Report #" + saved.getId() + " has been resolved.",
                                User.Role.ENVIRONMENTAL_AUTHORITY.name(),
                                saved));
            }

            // Notify the reporter (Citizen) about InvestigationStatus update
            if (saved.getReporter() != null) {
                String msg = "Your report #" + saved.getId() + " is now " + saved.getInvestigationStatus();
                notificationService.sendToUser(saved.getReporter(), msg, saved);
            }
            return saved;
        }
        return null;
    }

    // ✅ Authority assigns themselves or another authority
    public PollutionCase assignAuthority(Long reportId, User authority) {
        Optional<PollutionCase> reportOpt = reportRepository.findById(reportId);
        if (reportOpt.isPresent()) {
            PollutionCase report = reportOpt.get();
            report.setAssignedAuthority(authority);
            PollutionCase saved = reportRepository.save(report);
            messagingTemplate.convertAndSend("/topic/reports", saved);

            notificationService.sendNotification(
                    new org.rrc.pollution_response_system.entity.Notification(
                            "Authority assigned to report #" + saved.getId(),
                            User.Role.ENVIRONMENTAL_AUTHORITY.name(),
                            saved));

            // Notify the specific authority assigned
            if (authority.getUsername() != null) {
                notificationService.sendToUser(authority.getUsername(),
                        "You have been assigned to Report #" + saved.getId(), saved);
            } else if (authority.getId() != null) {
                userRepository.findById(authority.getId()).ifPresent(u -> notificationService.sendToUser(u.getUsername(),
                        "You have been assigned to Report #" + saved.getId(), saved));
            }


            return saved;
        }
        return null;
    }



    // ✅ Delete report (admin privilege)
    @Transactional
    public void deleteReport(Long id) {
        if (!reportRepository.existsById(id)) {
            throw new jakarta.persistence.EntityNotFoundException("Report not found with ID: " + id);
        }
        postCaseEvaluationRepository.deleteByReport_Id(id);
        notificationRepository.deleteByReport_Id(id);
        reportRepository.deleteById(id);
    }

    // ✅ Get all reports by region
    public List<PollutionCase> getReportsByRegion(Long regionId) {
        return reportRepository.findByRegion_Id(regionId);
    }

    // ✅ Get reports (reporter)
    public List<PollutionCase> getReportsByReporter(String reporter) {
        return reportRepository.findByReporter(reporter);
    }



    // ✅ Analytics helpers
    public Map<String, Long> countsByStatus() {
        return reportRepository.countGroupedByStatus().stream()
                .collect(Collectors.toMap(
                        row -> ((PollutionCase.InvestigationStatus) row[0]).name(),
                        row -> (Long) row[1]));
    }

    public Map<String, Long> countsByType() {
        return reportRepository.countGroupedByType().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]));
    }

    public Map<String, Long> countsByRegion() {
        return reportRepository.countGroupedByRegion().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]));
    }

    // Suggest similar reports within 30 minutes and ~1km
    public List<PollutionCase> findSimilar(Double lat, Double lon, String type) {
        if (lat == null || lon == null)
            return List.of();
        LocalDateTime windowEnd = LocalDateTime.now();
        LocalDateTime windowStart = windowEnd.minusMinutes(30);
        double delta = 0.01;
        return reportRepository.findPotentialDuplicates(lat, lon, delta, windowStart, windowEnd)
                .stream()
                .filter(r -> type == null || type.equalsIgnoreCase(r.getPollutionCategory()))
                .collect(Collectors.toList());
    }

    // ✅ Helper method to get user by username
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    private boolean isValidTransition(PollutionCase.InvestigationStatus from, PollutionCase.InvestigationStatus to) {
        if (from == to)
            return true;

        // Allow backward transitions for corrections.
        return switch (from) {
            case PENDING -> to == PollutionCase.InvestigationStatus.IN_PROGRESS || to == PollutionCase.InvestigationStatus.REJECTED;
            case IN_PROGRESS -> to == PollutionCase.InvestigationStatus.RESOLVED || to == PollutionCase.InvestigationStatus.PENDING || to == PollutionCase.InvestigationStatus.REJECTED;
            case RESOLVED -> false;
            case REJECTED -> to == PollutionCase.InvestigationStatus.PENDING;
        };
    }
}
