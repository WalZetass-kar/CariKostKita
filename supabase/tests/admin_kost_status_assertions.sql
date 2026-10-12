-- Execute after identity_bridge_schema.sql, 20261007_auth_identity_bridge.sql,
-- and 20261024_admin_kost_status_fastapi.sql.
INSERT INTO public.auth_user_links (auth_user_id, app_user_id) VALUES
    ('00000000-0000-0000-0000-000000000110', 110),
    ('00000000-0000-0000-0000-000000000111', 111)
ON CONFLICT DO NOTHING;

SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000110', false);
SELECT public.admin_update_kost_status(301, 'PENUH');
RESET ROLE;
DO $$
DECLARE actual_status text; actual_actor bigint; previous_value text; new_value text;
BEGIN
    SELECT status::text INTO actual_status FROM public.kosts WHERE id = 301;
    IF actual_status <> 'PENUH' THEN
        RAISE EXCEPTION 'developer did not update kost status';
    END IF;
    SELECT actor_id, previous_status, new_status
    INTO actual_actor, previous_value, new_value
    FROM app_private.admin_kost_status_actions WHERE kost_id = 301;
    IF actual_actor <> 110 OR previous_value <> 'TERSEDIA' OR new_value <> 'PENUH' THEN
        RAISE EXCEPTION 'admin kost status audit row mismatch';
    END IF;
    BEGIN
        PERFORM public.admin_update_kost_status(301, 'TIDAK_AKTIF');
        RAISE EXCEPTION 'unsupported admin status was accepted';
    EXCEPTION WHEN invalid_parameter_value THEN
        NULL;
    END;
END;
$$;

SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000111', false);
DO $$
BEGIN
    BEGIN
        PERFORM public.admin_update_kost_status(301, 'TERSEDIA');
        RAISE EXCEPTION 'moderator updated kost availability';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
END;
$$;
RESET ROLE;
