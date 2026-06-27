package org.rrc.pollution_response_system.websocket;

import org.rrc.pollution_response_system.entity.Notification;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class NotificationSocketController {

    // When backend sends message to /app/notify
    @MessageMapping("/notify")
    @SendTo("/topic/notifications")
    public Notification broadcastNotification(Notification notification) {
        // Just returns the notification to all subscribed users
        return notification;
    }
}
