package com.national.utility.billing.dto.response;

import com.national.utility.billing.model.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Database notification message (created by triggers/procedures)")
public class NotificationResponse {

    private UUID id;
    private UUID customerId;
    private String customerName;
    private UUID billId;
    private String billReference;
    private NotificationType notificationType;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
