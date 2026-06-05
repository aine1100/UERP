package com.national.utility.billing.model;

import com.national.utility.billing.model.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
/**
 * Persistent notification stored in PostgreSQL.
 * <p>
 * Most rows are created by database triggers/procedures — not by Java code directly —
 * so billing rules stay enforced at the DB layer for demonstration to invigilators.
 */
@Entity
@Table(name = "notification_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id")
    private Bill bill;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

}
