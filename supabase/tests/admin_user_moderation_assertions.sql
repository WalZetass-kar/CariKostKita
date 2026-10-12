-- Run after identity_bridge_schema.sql, 20261007_auth_identity_bridge.sql,
-- 20261015_owner_verification_fastapi.sql, and 20261017_admin_user_moderation.sql.
INSERT INTO public.users (id, email, name, role, is_active)
OVERRIDING SYSTEM VALUE
VALUES
    (109, 'admin@example.test', 'Admin', 'admin', true),
    (110, 'developer@example.test', 'Developer', 'developer', true)
ON CONFLICT (id) DO NOTHING;
INSERT INTO auth.users (id, email, email_confirmed_at)
VALUES
    ('00000000-0000-0000-0000-000000000109', 'admin@example.test', now()),
    ('00000000-0000-0000-0000-000000000110', 'developer@example.test', now())
ON CONFLICT (id) DO NOTHING;
INSERT INTO public.auth_user_links (auth_user_id, app_user_id)
VALUES
    ('00000000-0000-0000-0000-000000000109', 109),
    ('00000000-0000-0000-0000-000000000110', 110)
ON CONFLICT (auth_user_id) DO NOTHING;

SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000109', false);
SELECT public.admin_moderate_user(103, NULL, false, 'Uji penangguhan oleh staf');
DO $$
BEGIN
    IF (SELECT is_active FROM public.users WHERE id = 103) IS DISTINCT FROM false THEN
        RAISE EXCEPTION 'staff moderation did not suspend ordinary user';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM app_private.admin_user_actions
        WHERE target_user_id = 103 AND actor_user_id = 109
          AND action = 'STATUS_CHANGED' AND old_value = 'true'
          AND new_value = 'false' AND reason = 'Uji penangguhan oleh staf'
    ) THEN RAISE EXCEPTION 'staff action was not audited with trusted actor'; END IF;
    BEGIN
        PERFORM public.admin_moderate_user(103, 'moderator', NULL, 'Role change');
        RAISE EXCEPTION 'non-super-admin changed a role';
    EXCEPTION WHEN insufficient_privilege THEN NULL;
    END;
    BEGIN
        PERFORM public.admin_moderate_user(109, NULL, false, 'Self');
        RAISE EXCEPTION 'self moderation unexpectedly succeeded';
    EXCEPTION WHEN insufficient_privilege THEN NULL;
    END;
    IF has_table_privilege(current_user, 'app_private.admin_user_actions', 'INSERT')
       OR has_sequence_privilege(current_user, 'app_private.admin_user_actions_id_seq', 'USAGE') THEN
        RAISE EXCEPTION 'client can directly write moderation audit records';
    END IF;
END;
$$;

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000110', false);
SELECT public.admin_moderate_user(103, 'moderator', NULL, 'Promosi yang disetujui');
DO $$
BEGIN
    IF (SELECT role FROM public.users WHERE id = 103) IS DISTINCT FROM 'moderator' THEN
        RAISE EXCEPTION 'super admin role change did not apply';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM app_private.admin_user_actions
        WHERE target_user_id = 103 AND actor_user_id = 110
          AND action = 'ROLE_CHANGED' AND old_value = 'user'
          AND new_value = 'moderator'
    ) THEN RAISE EXCEPTION 'super admin role change was not audited'; END IF;
    BEGIN
        PERFORM public.admin_moderate_user(110, NULL, false, 'Staff target');
        RAISE EXCEPTION 'staff account was suspended';
    EXCEPTION WHEN insufficient_privilege THEN NULL;
    END;
END;
$$;
RESET ROLE;
