package org.rrc.pollution_response_system.controller;

import jakarta.annotation.PostConstruct;
import org.rrc.pollution_response_system.entity.PollutionCase;
import org.rrc.pollution_response_system.service.PollutionCaseService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ViewController {

    private final PollutionCaseService reportService;

    public ViewController(PollutionCaseService reportService) {
        this.reportService = reportService;
    }

    @PostConstruct
    public void init() {
        System.out.println("✅ ViewController loaded successfully!");
    }

    // ✅ Landing page route
    @GetMapping("/")
    public String home() {
        return "landing";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        System.out.println("✅ /dashboard endpoint called!");

        String username = auth != null ? auth.getName() : "Guest";
        model.addAttribute("username", username);

        List<PollutionCase> all = reportService.getAllReports();

        long pending = all.stream().filter(r -> r.getInvestigationStatus() == PollutionCase.InvestigationStatus.PENDING).count();
        long inProgress = all.stream().filter(r -> r.getInvestigationStatus() == PollutionCase.InvestigationStatus.IN_PROGRESS).count();
        long resolved = all.stream().filter(r -> r.getInvestigationStatus() == PollutionCase.InvestigationStatus.RESOLVED).count();

        model.addAttribute("cards", List.of(
                Map.of("title", "Pending", "value", String.valueOf(pending)),
                Map.of("title", "In Progress", "value", String.valueOf(inProgress)),
                Map.of("title", "Resolved", "value", String.valueOf(resolved))));

        List<Map<String, Object>> recent = all.stream()
                .sorted(Comparator.comparing(PollutionCase::getReportedAt).reversed())
                .limit(10)
                .map(r -> {
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", r.getId());
                    m.put("location", r.getLocation());
                    m.put("type", r.getPollutionCategory());
                    m.put("status", r.getInvestigationStatus().name());
                    m.put("latitude", r.getLatitude());
                    m.put("longitude", r.getLongitude());
                    return m;
                })
                .collect(Collectors.toList());

        model.addAttribute("recentReports", recent);
        return "dashboard";
    }

    @GetMapping("/reports")
    public String reports() {
        return "reports";
    }


    @GetMapping("/regions")
    public String regions() {
        return "regions";
    }

    @GetMapping("/notifications")
    public String notifications() {
        return "notifications";
    }

    @GetMapping("/users")
    public String users() {
        return "users";
    }

    @GetMapping("/case-map")
    public String caseMap() {
        return "incident-map";
    }

    @GetMapping("/report-submit")
    public String reportSubmit() {
        return "report-submit";
    }

    @GetMapping("/my-reports")
    public String myReports() {
        return "my-reports";
    }




    @GetMapping("/analytics")
    public String analytics() {
        return "analytics";
    }

    // /settings and /login are handled by SettingsController and AuthController
    // respectively
}
