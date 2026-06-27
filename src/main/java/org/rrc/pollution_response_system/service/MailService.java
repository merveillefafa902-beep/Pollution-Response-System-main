package org.rrc.pollution_response_system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    @Value("${app.mail.from:noreply@localhost}")
    private String from;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) return; // skip invalid address
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject == null ? "Notification" : subject);
        msg.setText(body == null ? "" : body);
        try {
            mailSender.send(msg);
        } catch (Exception ex) {
            // Swallow exceptions to avoid disrupting main workflow; could log later
        }
    }
}
