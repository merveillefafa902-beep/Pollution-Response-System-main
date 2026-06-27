package org.rrc.pollution_response_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "pollution_cases")
public class PollutionCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pollutionCategory;          
    @Column(length = 1000)
    private String description;
    private String location;              
    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "investigation_status", nullable = false, length = 20)
    private InvestigationStatus investigationStatus = InvestigationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Severity severity = Severity.MEDIUM;

    private String reporter;              

    private String mediaPath;

    @ManyToOne
    @JoinColumn(name = "officer_id")
    private User assignedAuthority;


    @ManyToOne
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(name = "reported_at")
    private LocalDateTime reportedAt = LocalDateTime.now();

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "assignment_notes", length = 500)
    private String assignmentNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    private Priority priority = Priority.NORMAL;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "environmental_impact", length = 1000)
    private String environmentalImpact;

    @Column(name = "inspector_notes", length = 1000)
    private String inspectorNotes;

    // Constructors
    public PollutionCase() {}

    public PollutionCase(String pollutionCategory, String description, String location,
                          Double latitude, Double longitude, InvestigationStatus investigationStatus, String reporter) {
        this.pollutionCategory = pollutionCategory;
        this.description = description;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.investigationStatus = investigationStatus;
        this.reporter = reporter;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPollutionCategory() { return pollutionCategory; }
    public void setPollutionCategory(String pollutionCategory) { this.pollutionCategory = pollutionCategory; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public InvestigationStatus getInvestigationStatus() { return investigationStatus; }
    public void setInvestigationStatus(InvestigationStatus investigationStatus) { this.investigationStatus = investigationStatus; }

    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }

    public User getAssignedAuthority() { return assignedAuthority; }
    public void setAssignedAuthority(User assignedAuthority) { this.assignedAuthority = assignedAuthority; }


    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }

    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }
    public String getMediaPath() { return mediaPath; }
    public void setMediaPath(String mediaPath) { this.mediaPath = mediaPath; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public String getEnvironmentalImpact() { return environmentalImpact; }
    public void setEnvironmentalImpact(String environmentalImpact) { this.environmentalImpact = environmentalImpact; }

    public String getInspectorNotes() { return inspectorNotes; }
    public void setInspectorNotes(String inspectorNotes) { this.inspectorNotes = inspectorNotes; }

    // helper for distance duplicate detection (rough planar distance in degrees)
    public boolean isNear(Double lat, Double lon, double delta) {
        if (lat == null || lon == null || this.latitude == null || this.longitude == null) return false;
        return Math.abs(this.latitude - lat) <= delta && Math.abs(this.longitude - lon) <= delta;
    }

    public enum InvestigationStatus {
        PENDING,
        IN_PROGRESS,
        RESOLVED,
        REJECTED
    }

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public String getAssignmentNotes() { return assignmentNotes; }
    public void setAssignmentNotes(String assignmentNotes) { this.assignmentNotes = assignmentNotes; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PollutionCase that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
