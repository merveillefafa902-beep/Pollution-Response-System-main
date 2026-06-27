package org.rrc.pollution_response_system.controller;

import org.rrc.pollution_response_system.entity.PollutionCase;
import org.rrc.pollution_response_system.repository.PollutionCaseRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final PollutionCaseRepository repo;

    public AnalyticsController(PollutionCaseRepository repo) {
        this.repo = repo;
    }

    // Role-limited endpoints for authorities/admins
    @PreAuthorize("hasAnyRole('ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @GetMapping("/InvestigationStatus")
    public Map<String, Long> byStatus() {
        return repo.countGroupedByStatus().stream()
                .collect(Collectors.toMap(o -> ((PollutionCase.InvestigationStatus) o[0]).name(), o -> (Long) o[1]));
    }

    @PreAuthorize("hasAnyRole('ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @GetMapping("/type")
    public Map<String, Long> byType() {
        return repo.countGroupedByType().stream()
                .collect(Collectors.toMap(o -> (String) o[0], o -> (Long) o[1]));
    }

    @PreAuthorize("hasAnyRole('ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @GetMapping("/region")
    public Map<String, Long> byRegion() {
        return repo.countGroupedByRegion().stream()
                .collect(Collectors.toMap(o -> (String) o[0], o -> (Long) o[1]));
    }

    @PreAuthorize("hasAnyRole('ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @GetMapping("/severity")
    public Map<String, Long> bySeverity() {
        return repo.countGroupedBySeverity().stream()
                .collect(Collectors.toMap(o -> String.valueOf(o[0]), o -> (Long) o[1]));
    }

    // Timeseries over last N days (default 7). bucket=day|hour
    @PreAuthorize("hasAnyRole('ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @GetMapping("/timeseries")
    public List<Map<String, Object>> timeSeries(@RequestParam(name = "days", defaultValue = "7") int days,
            @RequestParam(name = "bucket", defaultValue = "day") String bucket) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1).atStartOfDay();
        List<PollutionCase> reports = repo.findByReportedAtGreaterThanEqual(start);

        if ("hour".equalsIgnoreCase(bucket)) {
            Map<LocalDateTime, Long> counts = reports.stream()
                    .collect(Collectors.groupingBy(r -> r.getReportedAt().withMinute(0).withSecond(0).withNano(0),
                            Collectors.counting()));
            // build 24*days hours
            List<Map<String, Object>> series = new ArrayList<>();
            LocalDateTime t = start;
            LocalDateTime end = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
            while (!t.isAfter(end)) {
                Map<String, Object> p = new HashMap<>();
                p.put("t", t.toString());
                p.put("count", counts.getOrDefault(t, 0L));
                series.add(p);
                t = t.plusHours(1);
            }
            return series;
        }

        // bucket by day
        Map<LocalDate, Long> counts = reports.stream()
                .collect(Collectors.groupingBy(r -> r.getReportedAt().toLocalDate(), Collectors.counting()));
        List<Map<String, Object>> series = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = LocalDate.now().minusDays(days - 1 - i);
            Map<String, Object> p = new HashMap<>();
            p.put("t", d.toString());
            p.put("count", counts.getOrDefault(d, 0L));
            series.add(p);
        }
        return series;
    }
}
