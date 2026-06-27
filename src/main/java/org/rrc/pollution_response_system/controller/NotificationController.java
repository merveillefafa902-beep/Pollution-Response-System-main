package org.rrc.pollution_response_system.controller;

import org.rrc.pollution_response_system.entity.Notification;
import org.rrc.pollution_response_system.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;
    private final org.rrc.pollution_response_system.service.UserService userService;

    public NotificationController(NotificationService notificationService,
            org.rrc.pollution_response_system.service.UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    // ✅ Send new notification
    @PostMapping
    public Notification sendNotification(@RequestBody Notification notification) {
        return notificationService.sendNotification(notification);
    }

    // ✅ Get notifications for specific role
    @GetMapping("/by-role")
    public List<Notification> getNotificationsByRole(@RequestParam String role) {
        return notificationService.getNotificationsByRole(role);
    }

    // ✅ Get all notifications for current user
    @GetMapping
    public List<Notification> getAllNotifications(java.security.Principal principal) {
        if (principal == null)
            return new java.util.ArrayList<>();
        return userService.getUserByUsername(principal.getName())
                .map(notificationService::getAllNotifications)
                .orElse(new java.util.ArrayList<>());
    }

    // ✅ Get all unread notifications for current user
    @GetMapping("/unread")
    public List<Notification> getUnreadNotifications(java.security.Principal principal) {
        if (principal == null)
            return java.util.Collections.emptyList();
        String username = principal.getName();
        return userService.getUserByUsername(username)
                .map(notificationService::getUnreadNotifications)
                .orElse(java.util.Collections.emptyList());
    }

    // ✅ Mark notification as read
    @PutMapping("/{id}/read")
    public Notification markAsRead(@PathVariable Long id) {
        return notificationService.markAsRead(id);
    }

    // ✅ Get unread count
    @GetMapping("/unread/count")
    public long getUnreadCount() {
        return notificationService.getUnreadCount();
    }

    // ✅ Mark all as read for current user
    @PutMapping("/mark-all-read")
    public void markAllAsRead(java.security.Principal principal) {
        if (principal == null)
            return;
        userService.getUserByUsername(principal.getName()).ifPresent(notificationService::markAllAsRead);
    }

    // ✅ Delete all notifications for current user (or mark read if shared)
    @DeleteMapping("/clear-all")
    public void clearAll(java.security.Principal principal) {
        if (principal == null)
            return;
        userService.getUserByUsername(principal.getName()).ifPresent(notificationService::deleteAll);
    }
}
