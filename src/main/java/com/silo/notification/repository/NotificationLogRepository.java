package com.silo.notification.repository;

import com.silo.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    List<NotificationLog> findByMemberIdOrderBySentAtDesc(UUID memberId);

    @Modifying
    @Query("update NotificationLog n set n.readAt = :readAt "
            + "where n.memberId = :memberId and n.readAt is null")
    void markAllAsRead(@Param("memberId") UUID memberId, @Param("readAt") LocalDateTime readAt);
}
