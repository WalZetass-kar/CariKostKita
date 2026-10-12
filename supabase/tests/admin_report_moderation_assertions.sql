-- Run after the identity bridge, owner-verification, and report moderation migrations.
INSERT INTO public.users (id, email, name, role, is_active)
OVERRIDING SYSTEM VALUE
VALUES (109, 'admin@example.test', 'Admin', 'admin', true)
ON CONFLICT (id) DO NOTHING;
INSERT INTO auth.users (id, email, email_confirmed_at)
VALUES ('00000000-0000-0000-0000-000000000109', 'admin@example.test', now())
ON CONFLICT (id) DO NOTHING;
INSERT INTO public.auth_user_links (auth_user_id, app_user_id)
VALUES ('00000000-0000-0000-0000-000000000109', 109)
ON CONFLICT (auth_user_id) DO NOTHING;
INSERT INTO public.reports (id, kost_id, reporter_id, reason, description)
OVERRIDING SYSTEM VALUE
VALUES (901, 301, 101, 'Informasi tidak sesuai', 'Alamat kost berbeda.');

SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000109', false);
SELECT public.review_kost_report(901, 'SELESAI', 'Alamat sudah dikoreksi');
DO $$
DECLARE report_status text; action_count bigint; note text; actor bigint;
BEGIN
    SELECT status INTO report_status FROM public.reports WHERE id = 901;
    SELECT count(*), max(action_note), max(moderator_id)
    INTO action_count, note, actor
    FROM app_private.report_moderation_actions WHERE report_id = 901;
    IF report_status IS DISTINCT FROM 'SELESAI'
       OR action_count <> 1
       OR note IS DISTINCT FROM 'Alamat sudah dikoreksi'
       OR actor IS DISTINCT FROM 109 THEN
        RAISE EXCEPTION 'report review or audit record was not saved atomically';
    END IF;
    IF has_table_privilege(current_user, 'app_private.report_moderation_actions', 'INSERT')
       OR has_sequence_privilege(current_user, 'app_private.report_moderation_actions_id_seq', 'USAGE') THEN
        RAISE EXCEPTION 'authenticated client unexpectedly has direct moderation-history write access';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM public.list_report_moderation_actions(ARRAY[901]::bigint[])
        WHERE report_id = 901 AND status = 'SELESAI' AND action_note = 'Alamat sudah dikoreksi'
    ) THEN
        RAISE EXCEPTION 'staff could not read the latest moderation action';
    END IF;
    BEGIN
        PERFORM public.review_kost_report(901, 'DITOLAK', 'Second decision');
        RAISE EXCEPTION 'final report was unexpectedly reviewed twice';
    EXCEPTION WHEN unique_violation THEN
        NULL;
    END;
END;
$$;

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000101', false);
DO $$
BEGIN
    BEGIN
        PERFORM public.review_kost_report(901, 'DITOLAK', 'Untrusted decision');
        RAISE EXCEPTION 'ordinary user unexpectedly moderated a report';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
    BEGIN
        PERFORM * FROM public.list_report_moderation_actions(ARRAY[901]::bigint[]);
        RAISE EXCEPTION 'ordinary user unexpectedly read moderation actions';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
END;
$$;
RESET ROLE;
