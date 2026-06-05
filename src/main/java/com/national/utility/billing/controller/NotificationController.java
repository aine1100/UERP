package com.national.utility.billing.controller;

import com.national.utility.billing.dto.response.ApiResponse;
import com.national.utility.billing.dto.response.NotificationResponse;
import com.national.utility.billing.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Exposes rows from {@code notification_messages}, populated by DB triggers/procedures.
 * Customers can list their notifications and request email delivery.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("@authz.adminOrAny('FINANCE', 'CUSTOMER')")
    @Operation(summary = "List notifications (customers see their own)")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Notifications retrieved", notificationService.getNotifications(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.adminOrAny('FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get a notification by ID")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotification(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Notification retrieved", notificationService.getNotificationById(id)));
    }

    @PostMapping("/{id}/send-email")
    @PreAuthorize("@authz.adminOrAny('FINANCE', 'CUSTOMER')")
    @Operation(summary = "Send a notification message to the customer's email")
    public ResponseEntity<ApiResponse<Void>> sendNotificationEmail(@PathVariable UUID id) {
        notificationService.emailNotificationToCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("Notification sent to customer email", null));
    }

    @PostMapping("/send-overdue-reminders")
    @PreAuthorize("@authz.adminOrAny('FINANCE')")
    @Operation(summary = "Run overdue reminders (DB procedure + one consolidated email per customer)")
    public ResponseEntity<ApiResponse<Integer>> sendOverdueReminders() {
        int count = notificationService.sendOverdueReminders();
        return ResponseEntity.ok(ApiResponse.success(
                count + " overdue in-app notification(s) created; affected customers emailed once with all overdue months",
                count));
    }
}
