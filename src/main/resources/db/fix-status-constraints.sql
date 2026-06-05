-- Fix CHECK constraints after adding finance-approval statuses in Java.
-- Hibernate ddl-auto=update does not widen existing PostgreSQL CHECK constraints.

ALTER TABLE bills DROP CONSTRAINT IF EXISTS bills_status_check;
ALTER TABLE bills ADD CONSTRAINT bills_status_check
    CHECK (status IN ('PENDING_APPROVAL', 'REJECTED', 'UNPAID', 'PARTIAL', 'PAID'));

ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check;
ALTER TABLE payments ADD CONSTRAINT payments_status_check
    CHECK (status IN ('PENDING_APPROVAL', 'APPROVED', 'REJECTED'));

-- Backfill any NULL payment statuses from before the column existed
UPDATE payments SET status = 'APPROVED' WHERE status IS NULL;
