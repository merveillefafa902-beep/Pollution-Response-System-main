package org.rrc.pollution_response_system.controller;

import org.rrc.pollution_response_system.entity.PollutionCase;
import org.rrc.pollution_response_system.entity.User;
import org.rrc.pollution_response_system.service.PollutionCaseService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class PollutionCaseController {

    private final PollutionCaseService reportService;

    public PollutionCaseController(PollutionCaseService reportService) {
        this.reportService = reportService;
    }

    // ✅ Create new report (any authenticated role can submit, including CITIZEN)
    @PreAuthorize("hasAnyRole('CITIZEN','ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @PostMapping
    public PollutionCase createReport(@RequestBody PollutionCase report, Authentication auth) {
        // Ensure reporter attribution if not provided
        if (report.getReporter() == null && auth != null) {
            report.setReporter(auth.getName());
        }
        return reportService.createReport(report);
    }

    // ✅ Get all reports (restricted to Authority & Admin)
    @PreAuthorize("hasAnyRole('ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @GetMapping
    public List<PollutionCase> getAllReports() {
        return reportService.getAllReports();
    }

    // ✅ Get all reports for case map (all authenticated users can view)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/public")
    public List<PollutionCase> getPublicReports() {
        return reportService.getAllReports();
    }

    // ✅ Update report InvestigationStatus (Responder, Authority, Admin) – service enforces transition per role
    @PreAuthorize("hasAnyRole('ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @PutMapping("/{id}/status")
    public PollutionCase updateStatus(@PathVariable Long id, @RequestParam String status) {
        PollutionCase.InvestigationStatus s = PollutionCase.InvestigationStatus.valueOf(status.toUpperCase());
        return reportService.updateStatus(id, s);
    }

    // ✅ Assign authority (Authority or Admin)
    @PreAuthorize("hasAnyRole('ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @PutMapping("/{id}/assign-authority")
    public PollutionCase assignAuthority(@PathVariable Long id, @RequestBody User authority) {
        return reportService.assignAuthority(id, authority);
    }



    // ✅ Delete report (Admin only)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReport(@PathVariable Long id) {
        try {
            reportService.deleteReport(id);
            Map<String, String> body = new HashMap<>();
            body.put("message", "Report deleted successfully");
            return ResponseEntity.ok(body);
        } catch (jakarta.persistence.EntityNotFoundException ex) {
            Map<String, String> body = new HashMap<>();
            body.put("error", ex.getMessage() != null ? ex.getMessage() : "Report not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        } catch (Exception ex) {
            System.err.println("Exception deleting report ID " + id + ":");
            ex.printStackTrace();
            Map<String, String> body = new HashMap<>();
            body.put("error", ex.getMessage() != null ? ex.getMessage() : "Internal server error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    // ✅ Get reports by region (Authority & Admin)
    @PreAuthorize("hasAnyRole('ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @GetMapping("/region/{regionId}")
    public List<PollutionCase> getReportsByRegion(@PathVariable Long regionId) {
        return reportService.getReportsByRegion(regionId);
    }

    // ✅ Get reports by reporter (self or admin/authority override)
    @PreAuthorize("hasAnyRole('CITIZEN','ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @GetMapping("/by-reporter")
    public List<PollutionCase> getReportsByReporter(@RequestParam String reporter, Authentication auth) {
        // Citizens & responders can only fetch their own reports
        if (auth != null && reporter != null && !auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_ENVIRONMENTAL_AUTHORITY"))) {
            if (!auth.getName().equalsIgnoreCase(reporter)) {
                return List.of();
            }
        }
        return reportService.getReportsByReporter(reporter);
    }

    // ✅ Convenience endpoint for authenticated user's own reports
    @PreAuthorize("hasAnyRole('CITIZEN','ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @GetMapping("/my")
    public List<PollutionCase> myReports(Authentication auth) {
        if (auth == null) return List.of();
        return reportService.getReportsByReporter(auth.getName());
    }



    // ✅ Analytics: counts grouped by InvestigationStatus (Authority & Admin)
    @PreAuthorize("hasAnyRole('ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @GetMapping("/stats/InvestigationStatus")
    public Map<String, Long> getCountsByStatus() {
        return reportService.countsByStatus();
    }

    // ✅ Analytics: counts grouped by type (Authority & Admin)
    @PreAuthorize("hasAnyRole('ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @GetMapping("/stats/type")
    public Map<String, Long> getCountsByType() {
        return reportService.countsByType();
    }

    // ✅ Analytics: counts grouped by region (Authority & Admin)
    @PreAuthorize("hasAnyRole('ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @GetMapping("/stats/region")
    public Map<String, Long> getCountsByRegion() {
        return reportService.countsByRegion();
    }

    // ✅ Create report with media (any authenticated role, primarily CITIZEN)
    @PreAuthorize("hasAnyRole('CITIZEN','ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @PostMapping(path = "/with-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createReportWithMedia(@RequestParam String pollutionCategory,
                                                   @RequestParam(required = false) String description,
                                                   @RequestParam(required = false) String location,
                                                   @RequestParam(required = false) Double latitude,
                                                   @RequestParam(required = false) Double longitude,
                                                   @RequestParam(required = false) String severity,
                                                   @RequestParam(required = false) String reporter,
                                                   @RequestPart(required = false) MultipartFile file,
                                                   Authentication auth) throws Exception {
        PollutionCase report = new PollutionCase();
        report.setPollutionCategory(pollutionCategory);
        report.setDescription(description);
        report.setLocation(location);
        report.setLatitude(latitude);
        report.setLongitude(longitude);
        if(severity != null && !severity.isBlank()){
            try { report.setSeverity(PollutionCase.Severity.valueOf(severity.toUpperCase())); } catch(Exception e){}
        }
        // Auto-assign reporter from authentication if not explicitly provided
        report.setReporter(reporter != null && !reporter.isBlank() ? reporter : (auth != null ? auth.getName() : null));

        if (file != null && !file.isEmpty()) {
            // Basic validation: type and size (<= 10MB)
            long maxSize = 10L * 1024 * 1024;
            if (file.getSize() > maxSize) throw new IllegalArgumentException("File too large (max 10MB)");
            String contentType = file.getContentType() != null ? file.getContentType() : "";
            if (!(contentType.startsWith("image/") || contentType.equals("video/mp4"))) {
                throw new IllegalArgumentException("Unsupported file type");
            }
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);
            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
            String sanitized = original.replaceAll("[^a-zA-Z0-9._-]", "_");
            String filename = System.currentTimeMillis() + "_" + sanitized;
            Path target = uploadDir.resolve(filename).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            report.setMediaPath("/uploads/" + filename);
        }

        try {
            PollutionCase created = reportService.createReport(report);
            return ResponseEntity.ok(created);
        } catch (IllegalStateException dup) {
            // Duplicate detected; return HTTP 409 with message
            Map<String, Object> body = new HashMap<>();
            body.put("error", dup.getMessage());
            body.put("duplicates", reportService.findSimilar(latitude, longitude, pollutionCategory));
            return ResponseEntity.status(409).body(body);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create report: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // ✅ Suggest similar cases (any authenticated role)
    @PreAuthorize("hasAnyRole('CITIZEN','ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @GetMapping("/similar")
    public List<PollutionCase> findSimilar(@RequestParam Double lat,
                                            @RequestParam Double lon,
                                            @RequestParam(required = false) String type) {
        return reportService.findSimilar(lat, lon, type);
    }

    // ✅ Explicit duplicate pre-check endpoint (any authenticated role)
    @PreAuthorize("hasAnyRole('CITIZEN','ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @GetMapping("/check-duplicate")
    public Map<String, Object> checkDuplicate(@RequestParam Double lat,
                                              @RequestParam Double lon,
                                              @RequestParam String type) {
        List<PollutionCase> similars = reportService.findSimilar(lat, lon, type);
        boolean exists = !similars.isEmpty();
        Map<String, Object> res = new HashMap<>();
        res.put("duplicate", exists);
        res.put("similarReports", similars);
        return res;
    }
}

