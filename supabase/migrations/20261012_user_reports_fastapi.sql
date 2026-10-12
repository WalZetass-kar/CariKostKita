-- Secure the verified production user_reports schema for FastAPI submissions.
-- Apply only after 20261007_auth_identity_bridge.sql has been reviewed/applied.
-- chat_id intentionally is not added: the API validates the supplied chat, and
-- this RLS rule independently requires the reported pair to have a conversation.

BEGIN;

DO $$
DECLARE
    item record;
    actual_type text;
    seq_name text;
BEGIN
    IF to_regprocedure('public.current_app_user_id()') IS NULL THEN
        RAISE EXCEPTION 'user_reports migration requires public.current_app_user_id()';
    END IF;

    FOR item IN
        SELECT * FROM (VALUES
            ('user_reports', 'reporter_id'),
            ('user_reports', 'reported_user_id'),
            ('chats', 'id'), ('chats', 'user_id'), ('chats', 'owner_id')
        ) AS required(table_name, column_name)
    LOOP
        SELECT data_type INTO actual_type
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = item.table_name
          AND column_name = item.column_name;
        IF actual_type IS DISTINCT FROM 'bigint' THEN
            RAISE EXCEPTION 'user_reports migration preflight failed: public.%.% must be bigint (found %)',
                item.table_name, item.column_name, COALESCE(actual_type, 'missing');
        END IF;
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'user_reports' AND column_name = 'reason'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'user_reports' AND column_name = 'details'
    ) THEN
        RAISE EXCEPTION 'user_reports migration preflight failed: reason/details columns are required';
    END IF;

    seq_name := pg_get_serial_sequence('public.user_reports', 'id');
    IF seq_name IS NULL THEN
        RAISE EXCEPTION 'user_reports migration preflight failed: id must be identity/serial-backed';
    END IF;
    EXECUTE format('GRANT USAGE, SELECT ON SEQUENCE %s TO authenticated', seq_name);
END;
$$;

ALTER TABLE public.user_reports ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON TABLE public.user_reports FROM PUBLIC, anon, authenticated;
GRANT INSERT (reporter_id, reported_user_id, reason, details)
    ON TABLE public.user_reports TO authenticated;

DO $$
DECLARE policy_row record;
BEGIN
    FOR policy_row IN
        SELECT policyname FROM pg_policies
        WHERE schemaname = 'public' AND tablename = 'user_reports'
    LOOP
        EXECUTE format('DROP POLICY %I ON public.user_reports', policy_row.policyname);
    END LOOP;
END;
$$;

CREATE POLICY app_user_reports_insert
ON public.user_reports
FOR INSERT TO authenticated
WITH CHECK (
    reporter_id = public.current_app_user_id()
    AND reported_user_id <> public.current_app_user_id()
    AND EXISTS (
        SELECT 1
        FROM public.chats AS chat
        WHERE (chat.user_id = reporter_id AND chat.owner_id = reported_user_id)
           OR (chat.owner_id = reporter_id AND chat.user_id = reported_user_id)
    )
);

COMMIT;
