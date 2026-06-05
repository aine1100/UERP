package com.national.utility.billing.config;

import com.national.utility.billing.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Installs PostgreSQL triggers, functions, and stored procedures after Hibernate
 * has created the schema ({@code ddl-auto=create/update}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseRoutineInstaller {

    private static final Pattern SPLIT_ON_MARKER_LINE = Pattern.compile("(?m)^\\s*-- @split\\s*$");

    private final JdbcTemplate jdbcTemplate;
    private final NotificationService notificationService;
    private final AppProperties appProperties;

    @Order(1)
    @EventListener(ApplicationReadyEvent.class)
    public void installRoutines() throws IOException {
        String script = new ClassPathResource("db/routines.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        String[] blocks = SPLIT_ON_MARKER_LINE.split(script);
        int installed = 0;

        for (String block : blocks) {
            String sql = block.trim();
            if (!containsExecutableSql(sql)) {
                continue;
            }
            jdbcTemplate.execute(sql);
            installed++;
        }

        log.info("PostgreSQL billing routines installed ({} blocks).", installed);
        runInitialOverdueCheck();
    }

    private void runInitialOverdueCheck() {
        if (!appProperties.getNotifications().isOverdueScheduleEnabled()) {
            return;
        }
        try {
            int count = notificationService.sendOverdueReminders();
            log.info("Initial overdue reminder check — {} notification(s) sent", count);
        } catch (Exception ex) {
            log.warn("Initial overdue reminder check failed: {}", ex.getMessage());
        }
    }

    private boolean containsExecutableSql(String sql) {
        return sql.lines()
                .map(String::trim)
                .anyMatch(line -> !line.isEmpty()
                        && !line.startsWith("--")
                        && line.matches("(?i)(CREATE|DROP|ALTER|CALL|INSERT|UPDATE|DELETE)\\b.*"));
    }
}
