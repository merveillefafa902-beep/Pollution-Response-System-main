package org.rrc.pollution_response_system.service;

import org.rrc.pollution_response_system.entity.Notification;
import org.rrc.pollution_response_system.entity.User;
import org.rrc.pollution_response_system.repository.NotificationRepository;
import org.rrc.pollution_response_system.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final MailService mailService;

    public NotificationService(NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate,
            UserRepository userRepository,
            MailService mailService) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    // ✅ Send new notification + broadcast via WebSocket + email to role recipients
    public Notification sendNotification(Notification notification) {
        Notification saved = notificationRepository.save(notification);
        messagingTemplate.convertAndSend("/topic/notifications", saved);

        // Email fan-out to users with the recipient role (if valid and emails exist)
        if (saved.getRecipientRole() != null) {
            try {
                User.Role role = User.Role.valueOf(saved.getRecipientRole());
                List<User> recipients = userRepository.findByRole(role);
                if (!recipients.isEmpty()) {
                    String subject = "Pollution Notification";
                    String body = buildEmailBody(saved);
                    recipients.stream()
                            .map(User::getEmail)
                            .filter(email -> email != null && !email.isBlank())
                            .forEach(email -> mailService.send(email, subject, body));
                }
            } catch (IllegalArgumentException ignored) {
                // Unknown role string, skip email
            }
        }
        return saved;
    }

    public void sendToUser(String username, String message,
            org.rrc.pollution_response_system.entity.PollutionCase report) {
        Notification n = new Notification();
        n.setRecipientUsername(username);
        n.setMessage(message);
        n.setReport(report);
        n.setSentAt(java.time.LocalDateTime.now());

        Notification saved = notificationRepository.save(n);
        messagingTemplate.convertAndSend("/topic/notifications", saved);

        // Try to email if user exists
        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                mailService.send(user.getEmail(), "Pollution Notification", buildEmailBody(saved));
            }
        });
    }

    private String buildEmailBody(Notification n) {
        StringBuilder sb = new StringBuilder();
        sb.append(n.getMessage() == null ? "New notification" : n.getMessage()).append("\n\n");
        if (n.getReport() != null) {
            var r = n.getReport();
            sb.append("Report #").append(r.getId()).append("\n");
            if (r.getPollutionCategory() != null)
                sb.append("Type: ").append(r.getPollutionCategory()).append("\n");
            if (r.getSeverity() != null)
                sb.append("Severity: ").append(r.getSeverity()).append("\n");
            if (r.getInvestigationStatus() != null)
                sb.append("InvestigationStatus: ").append(r.getInvestigationStatus()).append("\n");
            if (r.getLocation() != null)
                sb.append("Location: ").append(r.getLocation()).append("\n");
            if (r.getReportedAt() != null)
                sb.append("Reported At: ")
                        .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(r.getReportedAt()))
                        .append("\n");
        }
        sb.append("\nThis is an automated alert from the Real-Time Pollution Response System.");
        return sb.toString();
    }

    public List<Notification> getNotificationsByRole(String role) {
        return notificationRepository.findByRecipientRoleOrderBySentAtDesc(role);
    }

    public List<Notification> getUnreadNotifications(User user) {
        if (user == null)
            return java.util.Collections.emptyList();
        List<String> roles = user.getRoles().stream().map(Enum::name).toList();
        return notificationRepository.findUnreadForUser(user.getUsername(), roles);
    }

    public List<Notification> getAllNotifications(User user) {
        if (user == null)
            return java.util.Collections.emptyList();
        List<String> roles = user.getRoles().stream().map(Enum::name).toList();
        return notificationRepository.findAllForUser(user.getUsername(), roles);
    }

    // Legacy support (to be deprecated or used for system-wide stats)
    public List<Notification> getUnreadNotifications() {
        return notificationRepository.findByReadFalse();
    }

    public long getUnreadCount() {
        return notificationRepository.countByReadFalse();
    }

    public Notification markAsRead(Long id) {
        Notification n = notificationRepository.findById(id).orElse(null);
        if (n != null) {
            n.setRead(true);
            notificationRepository.save(n);
        }
        return n;
    }

    public void markAllAsRead(User user) {
        if (user == null)
            return;
        List<Notification> unread = getUnreadNotifications(user);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    // Legacy support
    public void markAllAsRead() {
        List<Notification> unread = notificationRepository.findByReadFalse();
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    public void deleteAll(User user) {
        if (user == null)
            return;
        // Only delete notifications explicitly targeted at this user
        // We cannot delete role-based notifications as they are shared
        List<String> roles = user.getRoles().stream().map(Enum::name).toList();
        List<Notification> myNotifications = notificationRepository.findUnreadForUser(user.getUsername(), roles);

        // Split into deletable and shared
        List<Notification> toDelete = myNotifications.stream()
                .filter(n -> n.getRecipientUsername() != null && n.getRecipientUsername().equals(user.getUsername()))
                .toList();

        notificationRepository.deleteAll(toDelete);

        // For shared role-based, just mark them as read so they vanish from unread list
        List<Notification> toMarkRead = myNotifications.stream()
                .filter(n -> !toDelete.contains(n))
                .filter(n -> !n.isRead())
                .toList();

        toMarkRead.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(toMarkRead);
    }

    public void deleteAll() {
        notificationRepository.deleteAll();
    }
}
