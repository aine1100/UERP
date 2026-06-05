-- Run in psql connected to database "utility":
--   psql -U postgres -d utility -f fix-timestamps.sql

-- =============================================================================
-- 1) Backfill audit columns (safe when Hibernate added nullable columns with NULLs)
-- =============================================================================

DO $$
DECLARE
    t TEXT;
    tables TEXT[] := ARRAY[
        'users', 'customers', 'meters', 'readings', 'bills',
        'payments', 'tariffs', 'refresh_tokens', 'notification_messages'
    ];
BEGIN
    FOREACH t IN ARRAY tables LOOP
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = t AND column_name = 'created_at'
        ) THEN
            EXECUTE format(
                'UPDATE %I SET created_at = NOW() WHERE created_at IS NULL', t);
        END IF;

        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = t AND column_name = 'updated_at'
        ) THEN
            EXECUTE format(
                'UPDATE %I SET updated_at = COALESCE(created_at, NOW()) WHERE updated_at IS NULL', t);
        END IF;
    END LOOP;
END $$;

-- Enforce NOT NULL where columns exist
ALTER TABLE IF EXISTS users
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE IF EXISTS customers
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE IF EXISTS meters
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE IF EXISTS readings
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE IF EXISTS bills
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE IF EXISTS payments
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE IF EXISTS tariffs
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE IF EXISTS refresh_tokens
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE IF EXISTS notification_messages
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

-- =============================================================================
-- 2) Reinstall overdue procedure (integer argument)
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_send_overdue_reminders();
DROP PROCEDURE IF EXISTS sp_send_overdue_reminders(INT);

CREATE OR REPLACE PROCEDURE sp_send_overdue_reminders(p_overdue_days INT)
LANGUAGE plpgsql
AS $$
DECLARE
    unpaid_bill_cursor CURSOR FOR
        SELECT b.id,
               b.customer_id,
               b.bill_reference,
               b.outstanding_balance,
               b.month,
               b.year
        FROM bills b
        WHERE b.status IN ('UNPAID', 'PARTIAL')
          AND b.outstanding_balance > 0
          AND b.created_at < NOW() - make_interval(days => p_overdue_days)
          AND NOT EXISTS (
              SELECT 1 FROM notification_messages n
              WHERE n.bill_id = b.id
                AND n.notification_type = 'OVERDUE_REMINDER'
          )
        ORDER BY b.created_at;
    bill_row RECORD;
BEGIN
    OPEN unpaid_bill_cursor;
    LOOP
        FETCH unpaid_bill_cursor INTO bill_row;
        EXIT WHEN NOT FOUND;

        INSERT INTO notification_messages (
            id, customer_id, bill_id, notification_type, message, is_read, created_at, updated_at
        ) VALUES (
            gen_random_uuid(),
            bill_row.customer_id,
            bill_row.id,
            'OVERDUE_REMINDER',
            format(
                'Reminder: bill %s (%s/%s) has an outstanding balance of RWF %s.',
                bill_row.bill_reference,
                bill_row.month,
                bill_row.year,
                bill_row.outstanding_balance
            ),
            FALSE,
            NOW(),
            NOW()
        );
    END LOOP;
    CLOSE unpaid_bill_cursor;
END;
$$;

-- Test (0 = all unpaid bills qualify)
CALL sp_send_overdue_reminders(0);
