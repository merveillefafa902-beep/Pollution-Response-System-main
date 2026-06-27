package org.rrc.pollution_response_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    @Column(name = "recipient_role")
    private String recipientRole;

    @Column(name = "recipient_username")
    private String recipientUsername;

    @Column(name = "is_read") // ✅ FIXED: Avoid SQL keyword conflict
    private boolean read = false;

    private LocalDateTime sentAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "report_id")
    private PollutionCase report;

    // Constructors
    public Notification() {
    }

    public Notification(String message, String recipientRole, PollutionCase report) {
        this.message = message;
        this.recipientRole = recipientRole;
        this.report = report;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRecipientRole() {
        return recipientRole;
    }

    public void setRecipientRole(String recipientRole) {
        this.recipientRole = recipientRole;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public void setRecipientUsername(String recipientUsername) {
        this.recipientUsername = recipientUsername;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public PollutionCase getReport() {
        return report;
    }

    public void setReport(PollutionCase report) {
        this.report = report;
    }
}
