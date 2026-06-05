package com.national.utility.billing.repository;

import com.national.utility.billing.model.NotificationMessage;
import com.national.utility.billing.model.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, UUID> {

    Page<NotificationMessage> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    Page<NotificationMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<NotificationMessage> findByBillIdAndNotificationTypeIn(UUID billId, List<NotificationType> types);

    Optional<NotificationMessage> findByIdAndCustomerId(UUID id, UUID customerId);

    List<NotificationMessage> findByNotificationTypeAndCreatedAtAfter(
            NotificationType notificationType, LocalDateTime createdAt);
}
