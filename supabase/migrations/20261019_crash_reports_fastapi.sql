-- Requires 20261007_auth_identity_bridge.sql first.
-- Align crash ingestion to the verified bigint identity and current PostgREST columns.
-- Review the production policy/column diff before applying; this file is not applied automatically.

BEGIN;

DO $$
DECLARE
    item record;
    actual_type text;
BEGIN
    IF to_regclass('public.crash_reports') IS NULL THEN
        RAISE EXCEPTION 'Crash report migration preflight failed: public.crash_reports is missing';
    END IF;
    IF to_regprocedure('public.current_app_user_id()') IS NULL
       OR to_regprocedure('public.is_app_staff()') IS NULL THEN
        RAISE EXCEPTION 'Crash report migration requires the identity bridge';
    END IF;

    FOR item IN
        SELECT * FROM (VALUES
            ('id', 'bigint'),
            ('user_id', 'bigint'),
            ('app_version', 'text'),
            ('platform', 'text'),
            ('error_message', 'text'),
            ('stack_trace', 'text'),
            ('device_info', 'jsonb'),
            ('created_at', 'timestamp with time zone')
        ) AS required(column_name, expected_type)
    LOOP
        SELECT data_type INTO actual_type
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'crash_reports'
          AND column_name = item.column_name;

        IF actual_type IS NULL THEN
            RAISE EXCEPTION 'Crash report preflight failed: public.crash_reports.% is missing',
                item.column_name;
        END IF;

        IF item.expected_type = 'text' AND actual_type NOT IN ('text', 'character varying') THEN
            RAISE EXCEPTION 'Crash report preflight failed: public.crash_reports.% must be text-like (found %)',
                item.column_name, actual_type;
        ELSIF item.expected_type = 'jsonb' AND actual_type NOT IN ('jsonb', 'json', 'text') THEN
            RAISE EXCEPTION 'Crash report preflight failed: public.crash_reports.% must be json-like (found %)',
                item.column_name, actual_type;
        ELSIF item.expected_type = 'timestamp with time zone' AND actual_type NOT IN ('timestamp with time zone', 'timestamp without time zone') THEN
            RAISE EXCEPTION 'Crash report preflight failed: public.crash_reports.% must be a timestamp (found %)',
                item.column_name, actual_type;
        ELSIF item.expected_type = 'bigint' AND actual_type NOT IN ('bigint', 'integer') THEN
            RAISE EXCEPTION 'Crash report preflight failed: public.crash_reports.% must be bigint (found %)',
                item.column_name, actual_type;
        END IF;
    END LOOP;
END;
$$;

ALTER TABLE public.crash_reports ENABLE ROW LEVEL SECURITY;

DO $$
DECLARE
    policy_row record;
BEGIN
    FOR policy_row IN
        SELECT policyname FROM pg_policies
        WHERE schemaname = 'public' AND tablename = 'crash_reports'
    LOOP
        EXECUTE format('DROP POLICY %I ON public.crash_reports', policy_row.policyname);
    END LOOP;
END;
$$;

REVOKE ALL ON TABLE public.crash_reports FROM PUBLIC, anon, authenticated;
GRANT INSERT, SELECT ON TABLE public.crash_reports TO authenticated;
DO $$
DECLARE
    sequence_name text := pg_get_serial_sequence('public.crash_reports', 'id');
BEGIN
    IF sequence_name IS NOT NULL THEN
        EXECUTE format('REVOKE ALL ON SEQUENCE %s FROM PUBLIC, anon, authenticated', sequence_name);
        EXECUTE format('GRANT USAGE ON SEQUENCE %s TO authenticated', sequence_name);
    END IF;
END;
$$;

CREATE POLICY crash_reports_insert_own ON public.crash_reports
FOR INSERT TO authenticated
WITH CHECK (user_id = public.current_app_user_id());

CREATE POLICY crash_reports_select_staff ON public.crash_reports
FOR SELECT TO authenticated
USING (public.is_app_staff());

COMMIT;
