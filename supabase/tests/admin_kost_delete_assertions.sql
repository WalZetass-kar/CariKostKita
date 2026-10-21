-- Execute after identity_bridge_schema.sql, 20261007_auth_identity_bridge.sql,
-- and 20261025_admin_kost_delete.sql.
--
-- Verifies the atomic admin kost delete:
--   1. a developer-linked staff account may delete a kost together with its
--      chat and messages (the rows RLS blocks for the caller);
--   2. the audit row is written with the real actor;
--   3. a non-staff account is rejected;
--   4. an anonymous caller is rejected;
--   5. an empty id list is rejected.
INSERT INTO public.auth_user_links (auth_user_id, app_user_id) VALUES
    ('00000000-0000-0000-0000-000000000120', 120),
    ('00000000-0000-0000-0000-000000000121', 121)
ON CONFLICT DO NOTHING;

SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000120', false);
SELECT public.admin_delete_kosts(ARRAY[401]::bigint[], 'assertion delete');
RESET ROLE;

DO $$
DECLARE
    actual_actor bigint;
    actual_action text;
    actual_description text;
    remaining bigint;
BEGIN
    SELECT count(*) INTO remaining FROM public.kosts WHERE id = 401;
    IF remaining <> 0 THEN
        RAISE EXCEPTION 'admin_delete_kosts did not remove the kost row';
    END IF;

    SELECT count(*) INTO remaining FROM public.chats WHERE kost_id = 401;
    IF remaining <> 0 THEN
        RAISE EXCEPTION 'admin_delete_kosts left the chat of the kost behind';
    END IF;

    SELECT count(*) INTO remaining FROM public.messages WHERE chat_id IN (
        SELECT id FROM public.chats WHERE kost_id = 401
    );
    IF remaining <> 0 THEN
        RAISE EXCEPTION 'admin_delete_kosts left messages behind';
    END IF;

    SELECT user_id, action_type, description
      INTO actual_actor, actual_action, actual_description
      FROM public.activity_logs
     WHERE action_type = 'BULK_DELETE_KOSTS'
     ORDER BY id DESC
     LIMIT 1;

    IF actual_actor IS DISTINCT FROM 120 THEN
        RAISE EXCEPTION 'admin_delete_kosts audit actor mismatch: %', actual_actor;
    END IF;
    IF actual_action <> 'BULK_DELETE_KOSTS' THEN
        RAISE EXCEPTION 'admin_delete_kosts audit action mismatch: %', actual_action;
    END IF;
    IF actual_description NOT LIKE '%assertion delete%' THEN
        RAISE EXCEPTION 'admin_delete_kosts audit description mismatch: %', actual_description;
    END IF;
END;
$$;

-- Non-staff account must be rejected.
SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000121', false);
DO $$
BEGIN
    BEGIN
        PERFORM public.admin_delete_kosts(ARRAY[402]::bigint[], 'should be rejected');
        RAISE EXCEPTION 'non-staff account deleted a kost';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
END;
$$;
RESET ROLE;

-- Anonymous caller must be rejected.
SET ROLE anon;
DO $$
BEGIN
    BEGIN
        PERFORM public.admin_delete_kosts(ARRAY[402]::bigint[], 'should be rejected');
        RAISE EXCEPTION 'anonymous caller deleted a kost';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
END;
$$;
RESET ROLE;

-- An empty id list must be rejected.
SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000120', false);
DO $$
BEGIN
    BEGIN
        PERFORM public.admin_delete_kosts(ARRAY[]::bigint[], 'should be rejected');
        RAISE EXCEPTION 'empty kost id list was accepted';
    EXCEPTION WHEN invalid_parameter_value THEN
        NULL;
    END;
END;
$$;
RESET ROLE;
