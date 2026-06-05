--
-- PostgreSQL database dump
--

\restrict VtgL7tPbf2VEumWvtRRwzJexFuPaAZ40bb5U3jZNKRU1tUzFIDgL58R4aIOmcmh

-- Dumped from database version 17.10
-- Dumped by pg_dump version 17.10

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: fn_insert_notification(uuid, uuid, character varying, text); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_insert_notification(p_customer_id uuid, p_bill_id uuid, p_type character varying, p_message text) RETURNS void
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


--
-- Name: fn_trg_bill_after_insert(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_trg_bill_after_insert() RETURNS trigger
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


--
-- Name: fn_trg_payment_after_insert(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_trg_payment_after_insert() RETURNS trigger
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


--
-- Name: sp_send_overdue_reminders(integer); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.sp_send_overdue_reminders(IN p_overdue_days integer)
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


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: bills; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bills (
    amount numeric(12,2) NOT NULL,
    consumption numeric(12,2) NOT NULL,
    month integer NOT NULL,
    outstanding_balance numeric(12,2) NOT NULL,
    penalty numeric(12,2) NOT NULL,
    tax numeric(12,2) NOT NULL,
    total_amount numeric(12,2) NOT NULL,
    year integer NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    reading_id uuid NOT NULL,
    bill_reference character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT bills_status_check CHECK (((status)::text = ANY ((ARRAY['UNPAID'::character varying, 'PARTIAL'::character varying, 'PAID'::character varying])::text[])))
);


--
-- Name: customers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customers (
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    user_id uuid,
    cell character varying(255) NOT NULL,
    district character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    full_names character varying(255) NOT NULL,
    national_id character varying(255) NOT NULL,
    phone_number character varying(255) NOT NULL,
    province character varying(255) NOT NULL,
    sector character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    village character varying(255) NOT NULL,
    CONSTRAINT customers_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


--
-- Name: meters; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meters (
    installation_date date NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    meter_number character varying(255) NOT NULL,
    meter_type character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT meters_meter_type_check CHECK (((meter_type)::text = ANY ((ARRAY['WATER'::character varying, 'ELECTRICITY'::character varying])::text[]))),
    CONSTRAINT meters_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


--
-- Name: notification_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notification_messages (
    is_read boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    bill_id uuid,
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    message text NOT NULL,
    notification_type character varying(255) NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    CONSTRAINT notification_messages_notification_type_check CHECK (((notification_type)::text = ANY ((ARRAY['BILL_GENERATED'::character varying, 'PAYMENT_RECEIVED'::character varying, 'BILL_PAID'::character varying, 'OVERDUE_REMINDER'::character varying])::text[])))
);


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    amount_paid numeric(12,2) NOT NULL,
    payment_date date NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    bill_id uuid NOT NULL,
    id uuid NOT NULL,
    payment_method character varying(255) NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT payments_payment_method_check CHECK (((payment_method)::text = ANY ((ARRAY['CASH'::character varying, 'BANK_TRANSFER'::character varying, 'MOBILE_MONEY'::character varying, 'CARD'::character varying])::text[])))
);


--
-- Name: readings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.readings (
    current_reading numeric(12,2) NOT NULL,
    month integer NOT NULL,
    previous_reading numeric(12,2) NOT NULL,
    reading_date date NOT NULL,
    year integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    meter_id uuid NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_tokens (
    expiry_date timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    token character varying(255) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: tariffs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tariffs (
    effective_from date NOT NULL,
    fixed_service_charge numeric(12,2) NOT NULL,
    late_penalty_fee numeric(12,2) NOT NULL,
    rate_per_unit numeric(12,4) NOT NULL,
    vat_percentage numeric(5,2) NOT NULL,
    version integer NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    status character varying(255) NOT NULL,
    utility_type character varying(255) NOT NULL,
    CONSTRAINT tariffs_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[]))),
    CONSTRAINT tariffs_utility_type_check CHECK (((utility_type)::text = ANY ((ARRAY['WATER'::character varying, 'ELECTRICITY'::character varying])::text[])))
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    otp_verified boolean,
    created_at timestamp(6) without time zone,
    invite_token_expiry timestamp(6) without time zone,
    reset_token_expiry timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    email character varying(255) NOT NULL,
    full_names character varying(255) NOT NULL,
    invite_token character varying(255),
    password character varying(255),
    phone_number character varying(255) NOT NULL,
    reset_token character varying(255),
    role character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['ADMIN'::character varying, 'OPERATOR'::character varying, 'FINANCE'::character varying, 'CUSTOMER'::character varying])::text[]))),
    CONSTRAINT users_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'INVITED'::character varying])::text[])))
);


--
-- Data for Name: bills; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.bills (amount, consumption, month, outstanding_balance, penalty, tax, total_amount, year, created_at, updated_at, customer_id, id, reading_id, bill_reference, status) FROM stdin;
54162.63	150.25	4	64411.90	500.00	9749.27	64411.90	2026	2026-06-05 12:37:39.625412	2026-06-05 12:37:39.625412	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	d1f2b11c-c8b9-46d8-ad6f-b770c380bf84	19469e2c-60a2-4dfc-bd6d-828386fafd14	BILL-057589C4	UNPAID
513232.50	1650.75	6	606114.35	500.00	92381.85	606114.35	2026	2026-06-05 13:04:34.892032	2026-06-05 13:04:34.892032	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	631fe24c-c628-4916-8f30-68419bcfc37d	044ebabd-85b9-47db-8320-18230c4e7aba	BILL-99BA84C0	UNPAID
54162.63	150.25	5	30000.00	500.00	9749.27	64411.90	2026	2026-06-05 12:37:14.859256	2026-06-05 13:13:39.152555	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	e19190de-4e60-4d48-b671-38811ce7d1af	412f99e4-a172-41c7-925c-32a5885b0289	BILL-683AB481	PARTIAL
54162.63	150.25	6	0.00	500.00	9749.27	64411.90	2026	2026-06-05 12:05:22.222049	2026-06-05 13:22:17.778556	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	d57c2079-d40a-4497-8074-9f238db5ac4e	2b731b14-d660-4f43-a902-7f69a2d92fc2	BILL-732AC3A9	PAID
\.


--
-- Data for Name: customers; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.customers (created_at, updated_at, id, user_id, cell, district, email, full_names, national_id, phone_number, province, sector, status, village) FROM stdin;
2026-06-05 11:44:53.264554	2026-06-05 11:44:53.264554	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	e4b9203a-c315-4a6c-a53c-6ff63312e6b6	Gacuriro	Gasabo	adushiimire@gmail.com	Test Customer	1199887766554433	0781234567	KIGALI	Kinyinya	ACTIVE	Agatare
\.


--
-- Data for Name: meters; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.meters (installation_date, created_at, updated_at, customer_id, id, meter_number, meter_type, status) FROM stdin;
2024-01-15	2026-06-05 11:56:00.848806	2026-06-05 11:56:00.848806	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	02fdeaaa-9b03-44db-b83d-8c72338f2ab4	12345678901	ELECTRICITY	ACTIVE
2024-01-15	2026-06-05 13:01:29.470552	2026-06-05 13:01:29.470552	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	98626f7a-33e7-4810-af31-17331622f50e	12345678902	WATER	ACTIVE
\.


--
-- Data for Name: notification_messages; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.notification_messages (is_read, created_at, bill_id, customer_id, id, message, notification_type, updated_at) FROM stdin;
f	2026-06-05 12:05:22.19612	d57c2079-d40a-4497-8074-9f238db5ac4e	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	d9c17537-e1eb-433f-afff-65dd6f6f6e1c	A new utility bill BILL-732AC3A9 has been generated for 6/2026. Total: RWF 64411.90. Outstanding: RWF 64411.90.	BILL_GENERATED	2026-06-05 12:05:22.19612
f	2026-06-05 12:08:44.824093	d57c2079-d40a-4497-8074-9f238db5ac4e	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	483b471e-ac64-4678-af9f-c41f5e104fe6	Payment of RWF 30000.00 received for bill BILL-732AC3A9 via MOBILE_MONEY. Remaining balance: RWF 34411.90.	PAYMENT_RECEIVED	2026-06-05 12:08:44.824093
f	2026-06-05 12:32:54.857147	d57c2079-d40a-4497-8074-9f238db5ac4e	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	e6f4bfa9-c7a4-4c9e-94c8-ec7f410f110f	Reminder: bill BILL-732AC3A9 (6/2026) has an outstanding balance of RWF 34411.90.	OVERDUE_REMINDER	2026-06-05 12:32:54.857147
f	2026-06-05 12:37:14.855109	e19190de-4e60-4d48-b671-38811ce7d1af	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	117566ab-942a-4553-bc0c-5cb5bfde24b1	A new utility bill BILL-683AB481 has been generated for 5/2026. Total: RWF 64411.90. Outstanding: RWF 64411.90.	BILL_GENERATED	2026-06-05 12:37:14.855109
f	2026-06-05 12:37:39.625595	d1f2b11c-c8b9-46d8-ad6f-b770c380bf84	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	e549633e-8467-40b2-afa0-0990c71d5d3b	A new utility bill BILL-057589C4 has been generated for 4/2026. Total: RWF 64411.90. Outstanding: RWF 64411.90.	BILL_GENERATED	2026-06-05 12:37:39.625595
f	2026-06-05 12:37:54.773935	e19190de-4e60-4d48-b671-38811ce7d1af	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	8f39e5b9-4678-4a15-8536-3873c945f48b	Reminder: bill BILL-683AB481 (5/2026) has an outstanding balance of RWF 64411.90.	OVERDUE_REMINDER	2026-06-05 12:37:54.773935
f	2026-06-05 12:37:54.773935	d1f2b11c-c8b9-46d8-ad6f-b770c380bf84	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	5ef113cf-4e30-48e6-804b-f15eb5d5bf0a	Reminder: bill BILL-057589C4 (4/2026) has an outstanding balance of RWF 64411.90.	OVERDUE_REMINDER	2026-06-05 12:37:54.773935
f	2026-06-05 13:04:34.890776	631fe24c-c628-4916-8f30-68419bcfc37d	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	74f79811-66dc-4f5a-9a50-f4498e97f9f6	A new utility bill BILL-99BA84C0 has been generated for 6/2026. Total: RWF 606114.35. Outstanding: RWF 606114.35.	BILL_GENERATED	2026-06-05 13:04:34.890776
f	2026-06-05 13:07:25.29385	631fe24c-c628-4916-8f30-68419bcfc37d	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	c777ed08-9f72-46ff-bff2-bc0faf4b6c48	Reminder: bill BILL-99BA84C0 (6/2026) has an outstanding balance of RWF 606114.35.	OVERDUE_REMINDER	2026-06-05 13:07:25.29385
f	2026-06-05 13:13:39.152555	e19190de-4e60-4d48-b671-38811ce7d1af	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	3b6b2e5e-88c4-4af2-a425-5a6db1a98a6a	Payment of RWF 34411.90 received for bill BILL-683AB481 via MOBILE_MONEY. Remaining balance: RWF 30000.00.	PAYMENT_RECEIVED	2026-06-05 13:13:39.152555
f	2026-06-05 13:21:51.803602	d57c2079-d40a-4497-8074-9f238db5ac4e	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	1d97343c-988a-4683-bc42-aec27fda2f5a	Payment of RWF 30000.00 received for bill BILL-732AC3A9 via MOBILE_MONEY. Remaining balance: RWF 4411.90.	PAYMENT_RECEIVED	2026-06-05 13:21:51.803602
f	2026-06-05 13:22:17.778556	d57c2079-d40a-4497-8074-9f238db5ac4e	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	698b5349-9acd-409c-a20b-59f5698d1797	Payment of RWF 4411.90 received for bill BILL-732AC3A9 via MOBILE_MONEY. Remaining balance: RWF 0.00.	PAYMENT_RECEIVED	2026-06-05 13:22:17.778556
f	2026-06-05 13:22:17.778556	d57c2079-d40a-4497-8074-9f238db5ac4e	74ff31f7-81d3-4727-b55c-4b6c5bd8d082	701c96a0-fcf7-40c1-b325-0ff7d134087c	Bill BILL-732AC3A9 is fully paid. Thank you for your payment!	BILL_PAID	2026-06-05 13:22:17.778556
\.


--
-- Data for Name: payments; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.payments (amount_paid, payment_date, created_at, bill_id, id, payment_method, updated_at, status) FROM stdin;
30000.00	2026-06-05	2026-06-05 12:08:44.832961	d57c2079-d40a-4497-8074-9f238db5ac4e	53fff4e3-7227-4a96-9eb9-379835ab36d9	MOBILE_MONEY	2026-06-05 12:08:44.832961	APPROVED
34411.90	2025-05-10	2026-06-05 13:13:39.16272	e19190de-4e60-4d48-b671-38811ce7d1af	3399e566-4cf3-4ef7-9e4f-9cb35fbc3183	MOBILE_MONEY	2026-06-05 13:13:39.16272	APPROVED
30000.00	2025-05-10	2026-06-05 13:21:51.807144	d57c2079-d40a-4497-8074-9f238db5ac4e	5d35b0f6-9b19-4be8-a65b-95399a7d1eb2	MOBILE_MONEY	2026-06-05 13:21:51.807144	APPROVED
4411.90	2025-05-10	2026-06-05 13:22:17.782412	d57c2079-d40a-4497-8074-9f238db5ac4e	1f6901b9-b5c1-4ccb-84e5-4987e465b556	MOBILE_MONEY	2026-06-05 13:22:17.782412	APPROVED
\.


--
-- Data for Name: readings; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.readings (current_reading, month, previous_reading, reading_date, year, created_at, id, meter_id, updated_at) FROM stdin;
1650.75	6	1500.50	2026-06-05	2026	2026-06-05 12:05:22.212794	2b731b14-d660-4f43-a902-7f69a2d92fc2	02fdeaaa-9b03-44db-b83d-8c72338f2ab4	2026-06-05 12:05:22.212794
1650.75	5	1500.50	2026-05-05	2026	2026-06-05 12:37:14.850348	412f99e4-a172-41c7-925c-32a5885b0289	02fdeaaa-9b03-44db-b83d-8c72338f2ab4	2026-06-05 12:37:14.850348
1650.75	4	1500.50	2026-04-05	2026	2026-06-05 12:37:39.623418	19469e2c-60a2-4dfc-bd6d-828386fafd14	02fdeaaa-9b03-44db-b83d-8c72338f2ab4	2026-06-05 12:37:39.623418
1650.75	6	0.00	2025-06-05	2026	2026-06-05 13:04:34.886035	044ebabd-85b9-47db-8320-18230c4e7aba	98626f7a-33e7-4810-af31-17331622f50e	2026-06-05 13:04:34.886035
\.


--
-- Data for Name: refresh_tokens; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.refresh_tokens (expiry_date, id, user_id, token, created_at, updated_at) FROM stdin;
2026-06-12 13:20:44.660987	4fca6186-da82-4bab-9488-8e4fae9b6d55	e4b9203a-c315-4a6c-a53c-6ff63312e6b6	bde4d1c1-f7a7-45fb-b087-993f6aed49a6	2026-06-05 13:20:44.662988	2026-06-05 13:20:44.662988
2026-06-12 13:35:31.199485	7be625c9-755d-43cd-ac01-8e4ffcd378ea	17132418-e993-448b-8ce0-eca2965783f5	ed74858f-8369-4c7d-928a-41b8fe4c1774	2026-06-05 13:35:31.214859	2026-06-05 13:35:31.214859
2026-06-12 13:48:02.644744	c3bb0218-18f5-484c-a106-cd13889f3bf0	b96dc820-a9c2-4391-ba87-0807c07d3ef8	f31b70dc-6aaf-4f94-97c3-016e683af816	2026-06-05 13:48:02.64992	2026-06-05 13:48:02.64992
2026-06-12 13:51:10.725079	f0fd4391-a9ed-405e-83aa-ff8e4d8d1e67	bb782088-4f16-4d2a-bb97-c3bbc264b058	e35c355f-bb61-4ca1-9012-7cfe02c70c0f	2026-06-05 13:51:10.725079	2026-06-05 13:51:10.725079
\.


--
-- Data for Name: tariffs; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tariffs (effective_from, fixed_service_charge, late_penalty_fee, rate_per_unit, vat_percentage, version, created_at, updated_at, id, status, utility_type) FROM stdin;
2025-01-01	1500.00	500.00	350.5000	18.00	1	2026-06-05 12:05:09.766186	2026-06-05 12:05:09.766186	a74f7288-f49c-4b8b-b30a-250dc36e19b4	ACTIVE	ELECTRICITY
2025-01-01	1500.00	500.00	300.0000	18.00	1	2026-06-05 12:55:15.784472	2026-06-05 12:57:07.909853	33c6c846-fbec-40a6-a774-ff25f9ca3ff1	INACTIVE	WATER
2026-01-01	1500.00	500.00	310.0000	18.00	2	2026-06-05 12:57:07.919931	2026-06-05 12:57:07.919931	7e3aa48b-62c4-4f57-947c-562e5aaa06da	ACTIVE	WATER
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.users (otp_verified, created_at, invite_token_expiry, reset_token_expiry, updated_at, id, email, full_names, invite_token, password, phone_number, reset_token, role, status) FROM stdin;
f	2026-06-05 11:39:26.553879	\N	\N	2026-06-05 11:41:18.746682	17132418-e993-448b-8ce0-eca2965783f5	ainedushimire@gmail.com	System Administrator	\N	$2a$10$OPRyqwX/I7pBEJNhUDFv8O6qFCJnXyWUrYEEmfHI.ZiGxFitv2b5S	0780000000	\N	ADMIN	ACTIVE
f	2026-06-05 11:59:54.574839	\N	\N	2026-06-05 12:00:57.351229	bb782088-4f16-4d2a-bb97-c3bbc264b058	raceb35799@aspensif.com	Jane Uwase	\N	$2a$10$7igGGDSp9p7o.P37Ip8LVekck7BWHkbmYR4...0xeRIBMA7JTXlta	0722634809	\N	OPERATOR	ACTIVE
f	2026-06-05 11:44:53.263289	\N	\N	2026-06-05 13:20:26.412786	e4b9203a-c315-4a6c-a53c-6ff63312e6b6	adushiimire@gmail.com	Test Customer	\N	$2a$10$aZL3UZOaEUx6b9n7VAoA9.f/p51IFM2IB6frrOxwmhX4T6n2bFMNm	0781234567	\N	CUSTOMER	ACTIVE
f	2026-06-05 13:40:35.494523	\N	\N	2026-06-05 13:47:27.58418	b96dc820-a9c2-4391-ba87-0807c07d3ef8	havabe8400@fixscal.com	Test Finance	\N	$2a$10$5N0OZiSIVEVlvzQjX639suvIVw/L.IZ5fIosoE5blhyVjddH3CLSu	0798380290	\N	FINANCE	ACTIVE
\.


--
-- Name: bills bills_bill_reference_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT bills_bill_reference_key UNIQUE (bill_reference);


--
-- Name: bills bills_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT bills_pkey PRIMARY KEY (id);


--
-- Name: bills bills_reading_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT bills_reading_id_key UNIQUE (reading_id);


--
-- Name: customers customers_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_email_key UNIQUE (email);


--
-- Name: customers customers_national_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_national_id_key UNIQUE (national_id);


--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (id);


--
-- Name: customers customers_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_user_id_key UNIQUE (user_id);


--
-- Name: meters meters_meter_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meters
    ADD CONSTRAINT meters_meter_number_key UNIQUE (meter_number);


--
-- Name: meters meters_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meters
    ADD CONSTRAINT meters_pkey PRIMARY KEY (id);


--
-- Name: notification_messages notification_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_messages
    ADD CONSTRAINT notification_messages_pkey PRIMARY KEY (id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: readings readings_meter_id_month_year_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.readings
    ADD CONSTRAINT readings_meter_id_month_year_key UNIQUE (meter_id, month, year);


--
-- Name: readings readings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.readings
    ADD CONSTRAINT readings_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_key UNIQUE (token);


--
-- Name: tariffs tariffs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tariffs
    ADD CONSTRAINT tariffs_pkey PRIMARY KEY (id);


--
-- Name: readings ukqussssx9rjdo3gcnbrd36h3ri; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.readings
    ADD CONSTRAINT ukqussssx9rjdo3gcnbrd36h3ri UNIQUE (meter_id, month, year);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: bills trg_bill_after_insert; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_bill_after_insert AFTER INSERT ON public.bills FOR EACH ROW EXECUTE FUNCTION public.fn_trg_bill_after_insert();


--
-- Name: payments trg_payment_after_insert; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_payment_after_insert AFTER INSERT ON public.payments FOR EACH ROW EXECUTE FUNCTION public.fn_trg_payment_after_insert();


--
-- Name: refresh_tokens fk1lih5y2npsf8u5o3vhdb9y0os; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk1lih5y2npsf8u5o3vhdb9y0os FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: payments fk9565r6579khpdjxnyla0l2ycd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk9565r6579khpdjxnyla0l2ycd FOREIGN KEY (bill_id) REFERENCES public.bills(id);


--
-- Name: notification_messages fkcyt0x254omaj89egunq8dn4mm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_messages
    ADD CONSTRAINT fkcyt0x254omaj89egunq8dn4mm FOREIGN KEY (bill_id) REFERENCES public.bills(id);


--
-- Name: meters fkdgg79dhtsr0eumbce7ipw58lj; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meters
    ADD CONSTRAINT fkdgg79dhtsr0eumbce7ipw58lj FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: notification_messages fkjrh5907vin89u8nuptt5khppo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_messages
    ADD CONSTRAINT fkjrh5907vin89u8nuptt5khppo FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: bills fkoy9sc2dmxj2qwjeiiilf3yuxp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fkoy9sc2dmxj2qwjeiiilf3yuxp FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: customers fkrh1g1a20omjmn6kurd35o3eit; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT fkrh1g1a20omjmn6kurd35o3eit FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: readings fksdb48n5bdiwskw91j5hq07llm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.readings
    ADD CONSTRAINT fksdb48n5bdiwskw91j5hq07llm FOREIGN KEY (meter_id) REFERENCES public.meters(id);


--
-- Name: bills fksg030723h00wtnhpft1era6ci; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fksg030723h00wtnhpft1era6ci FOREIGN KEY (reading_id) REFERENCES public.readings(id);


--
-- PostgreSQL database dump complete
--

\unrestrict VtgL7tPbf2VEumWvtRRwzJexFuPaAZ40bb5U3jZNKRU1tUzFIDgL58R4aIOmcmh

