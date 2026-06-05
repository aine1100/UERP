-- =============================================================================
-- National Utility Billing System — PostgreSQL Database Routines
-- =============================================================================
-- This script is executed automatically on application startup by
-- DatabaseRoutineInstaller (after Hibernate creates tables).
--
-- Demonstrates three required DB concepts for academic evaluation:
--   1. TRIGGER     — automatic notification when a bill is generated
--   2. TRIGGER     — bill status update + customer notification on payment
--   3. PROCEDURE   — sp_send_overdue_reminders uses a CURSOR to scan unpaid bills
--
-- Blocks are separated by a line containing only: -- @split
-- =============================================================================

-- @split
-- -----------------------------------------------------------------------------
-- Helper: insert a row into notification_messages
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_insert_notification(
    p_customer_id UUID,
    p_bill_id UUID,
    p_type VARCHAR,
    p_message TEXT
) RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO notification_messages (
        id, customer_id, bill_id, notification_type, message, is_read, created_at, updated_at
    ) VALUES (
        gen_random_uuid(),
        p_customer_id,
        p_bill_id,
        p_type,
        p_message,
        FALSE,
        NOW(),
        NOW()
    );
END;
$$;

-- @split
-- -----------------------------------------------------------------------------
-- TRIGGER 1: After a bill is inserted (bill generation)
-- Required behavior: insert a notification message for the customer
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_trg_bill_after_insert()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    -- Finance must approve before the customer is notified
    IF NEW.status = 'PENDING_APPROVAL' THEN
        RETURN NEW;
    END IF;

    PERFORM fn_insert_notification(
        NEW.customer_id,
        NEW.id,
        'BILL_GENERATED',
        format(
            'A new utility bill %s has been generated for %s/%s. Total: RWF %s. Outstanding: RWF %s.',
            NEW.bill_reference,
            NEW.month,
            NEW.year,
            NEW.total_amount,
            NEW.outstanding_balance
        )
    );
    RETURN NEW;
END;
$$;

-- @split
DROP TRIGGER IF EXISTS trg_bill_after_insert ON bills;
CREATE TRIGGER trg_bill_after_insert
    AFTER INSERT ON bills
    FOR EACH ROW
    EXECUTE FUNCTION fn_trg_bill_after_insert();

-- @split
-- -----------------------------------------------------------------------------
-- TRIGGER 2: After a payment is inserted
-- Required behavior: update bill status on full payment and notify customer
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_trg_payment_after_insert()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_outstanding NUMERIC(12, 2);
    v_total NUMERIC(12, 2);
    v_customer_id UUID;
    v_bill_ref VARCHAR;
    v_bill_month INT;
    v_bill_year INT;
    v_new_status VARCHAR;
BEGIN
    -- Customer payments stay pending until finance approves in the application layer
    IF NEW.status IS DISTINCT FROM 'APPROVED' THEN
        RETURN NEW;
    END IF;

    SELECT outstanding_balance, total_amount, customer_id, bill_reference, month, year
    INTO v_outstanding, v_total, v_customer_id, v_bill_ref, v_bill_month, v_bill_year
    FROM bills
    WHERE id = NEW.bill_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Bill not found for payment: %', NEW.bill_id;
    END IF;

    IF NEW.amount_paid > v_outstanding THEN
        RAISE EXCEPTION 'Payment amount (%) exceeds outstanding balance (%)', NEW.amount_paid, v_outstanding;
    END IF;

    v_outstanding := v_outstanding - NEW.amount_paid;

    IF v_outstanding = 0 THEN
        v_new_status := 'PAID';
    ELSIF v_outstanding < v_total THEN
        v_new_status := 'PARTIAL';
    ELSE
        v_new_status := 'UNPAID';
    END IF;

    UPDATE bills
    SET outstanding_balance = v_outstanding,
        status = v_new_status,
        updated_at = NOW()
    WHERE id = NEW.bill_id;

    PERFORM fn_insert_notification(
        v_customer_id,
        NEW.bill_id,
        'PAYMENT_RECEIVED',
        format(
            'Payment of RWF %s received for bill %s (%s/%s) via %s. Remaining balance: RWF %s.',
            NEW.amount_paid,
            v_bill_ref,
            v_bill_month,
            v_bill_year,
            NEW.payment_method,
            v_outstanding
        )
    );

    IF v_outstanding = 0 THEN
        PERFORM fn_insert_notification(
            v_customer_id,
            NEW.bill_id,
            'BILL_PAID',
            format(
                'Bill %s (%s/%s) is fully paid. Amount paid: RWF %s. Thank you for your payment!',
                v_bill_ref,
                v_bill_month,
                v_bill_year,
                NEW.amount_paid
            )
        );
    END IF;

    RETURN NEW;
END;
$$;

-- @split
DROP TRIGGER IF EXISTS trg_payment_after_insert ON payments;
CREATE TRIGGER trg_payment_after_insert
    AFTER INSERT ON payments
    FOR EACH ROW
    EXECUTE FUNCTION fn_trg_payment_after_insert();

-- @split
-- Drop legacy signatures so Java can call sp_send_overdue_reminders(integer)
DROP PROCEDURE IF EXISTS sp_send_overdue_reminders();
DROP PROCEDURE IF EXISTS sp_send_overdue_reminders(INT);

-- @split
-- -----------------------------------------------------------------------------
-- STORED PROCEDURE with CURSOR: send reminders for unpaid bills
-- Uses a server-side cursor to iterate bills and insert OVERDUE_REMINDER messages.
-- Callable from Java via: CALL sp_send_overdue_reminders(0);
-- -----------------------------------------------------------------------------
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

        PERFORM fn_insert_notification(
            bill_row.customer_id,
            bill_row.id,
            'OVERDUE_REMINDER',
            format(
                'Reminder: bill %s (%s/%s) has an outstanding balance of RWF %s.',
                bill_row.bill_reference,
                bill_row.month,
                bill_row.year,
                bill_row.outstanding_balance
            )
        );
    END LOOP;

    CLOSE unpaid_bill_cursor;
END;
$$;
