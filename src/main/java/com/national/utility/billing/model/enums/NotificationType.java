package com.national.utility.billing.model.enums;

/**
 * Types of in-app / database notification messages.
 * Rows are inserted automatically by PostgreSQL triggers and stored procedures
 * (see {@code db/routines.sql} and {@link com.national.utility.billing.config.DatabaseRoutineInstaller}).
 */
public enum NotificationType {
    BILL_GENERATED,
    PAYMENT_RECEIVED,
    BILL_PAID,
    OVERDUE_REMINDER
}
