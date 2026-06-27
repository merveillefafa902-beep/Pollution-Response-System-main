package org.rrc.pollution_response_system.repository;

import org.rrc.pollution_response_system.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientRole(String recipientRole);

    List<Notification> findByReadFalse();

    List<Notification> findByRecipientRoleOrderBySentAtDesc(String recipientRole);

    @org.springframework.data.jpa.repository.Query("SELECT n FROM Notification n WHERE n.read = false AND (n.recipientUsername = :username OR n.recipientRole IN :roles) ORDER BY n.sentAt DESC")
    List<Notification> findUnreadForUser(
            @org.springframework.data.repository.query.Param("username") String username,
            @org.springframework.data.repository.query.Param("roles") java.util.Collection<String> roles);

    @org.springframework.data.jpa.repository.Query("SELECT n FROM Notification n WHERE (n.recipientUsername = :username OR n.recipientRole IN :roles) ORDER BY n.sentAt DESC")
    List<Notification> findAllForUser(
            @org.springframework.data.repository.query.Param("username") String username,
            @org.springframework.data.repository.query.Param("roles") java.util.Collection<String> roles);

    long countByReadFalse();

    void deleteByReport_Id(Long reportId);

}

