-- Execute after identity_bridge_schema.sql, 20261007_auth_identity_bridge.sql,
-- and 20261019_crash_reports_fastapi.sql.
INSERT INTO public.auth_user_links (auth_user_id, app_user_id) VALUES
    ('00000000-0000-0000-0000-000000000101', 101),
    ('00000000-0000-0000-0000-000000000110', 110)
ON CONFLICT DO NOTHING;

SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000101', false);
INSERT INTO public.crash_reports (user_id, app_version, platform, error_message, stack_trace)
VALUES (101, '1.2.3 (42)', 'android', 'IllegalStateException', 'sample trace');

DO $$
BEGIN
    IF (SELECT count(*) FROM public.crash_reports) <> 0 THEN
        RAISE EXCEPTION 'non-staff user can read crash reports';
    END IF;
    BEGIN
        INSERT INTO public.crash_reports (user_id, app_version, platform, error_message, stack_trace)
        VALUES (103, '1.2.3', 'android', 'Forged actor', 'forged trace');
        RAISE EXCEPTION 'cross-user crash report insert unexpectedly succeeded';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
    IF has_table_privilege(current_user, 'public.crash_reports', 'UPDATE')
       OR has_table_privilege(current_user, 'public.crash_reports', 'DELETE') THEN
        RAISE EXCEPTION 'crash report unexpectedly grants update/delete';
    END IF;
END;
$$;

RESET ROLE;
SET ROLE anon;
DO $$
BEGIN
    IF has_table_privilege(current_user, 'public.crash_reports', 'INSERT') THEN
        RAISE EXCEPTION 'anonymous crash report insert unexpectedly enabled';
    END IF;
END;
$$;
RESET ROLE;

SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000110', false);
DO $$
BEGIN
    IF (SELECT count(*) FROM public.crash_reports) <> 1 THEN
        RAISE EXCEPTION 'staff cannot read crash report aggregate source';
    END IF;
END;
$$;
RESET ROLE;
