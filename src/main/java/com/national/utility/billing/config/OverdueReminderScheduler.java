package com.national.utility.billing.config;

import com.national.utility.billing.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Re-checks overdue bills every 5 minutes (after DB routines are installed on startup).
 * Skips bills that already received OVERDUE_REMINDER.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notifications.overdue-schedule-enabled", havingValue = "true", matchIfMissing = true)
public class OverdueReminderScheduler {

    private final NotificationService notificationService;
    private final AppProperties appProperties;

    @Scheduled(
            initialDelayString = "${app.notifications.overdue-check-interval-ms:300000}",
            fixedRateString = "${app.notifications.overdue-check-interval-ms:300000}")
    public void sendOverdueRemindersPeriodically() {
        try {
            int count = notificationService.sendOverdueReminders();
            if (count > 0) {
                log.info("Periodic overdue check — {} new reminder(s) emailed (bills older than {} days)",
                        count, appProperties.getNotifications().getOverdueDays());
            }
        } catch (Exception ex) {
            log.warn("Periodic overdue reminder check failed: {}", ex.getMessage());
        }
    }
}
