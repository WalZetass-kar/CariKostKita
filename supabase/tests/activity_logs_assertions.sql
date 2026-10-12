-- Execute after identity_bridge_schema.sql, 20261007_auth_identity_bridge.sql,
-- and 20261018_activity_logs_fastapi.sql.
INSERT INTO public.auth_user_links (auth_user_id, app_user_id) VALUES
    ('00000000-0000-0000-0000-000000000101', 101),
    ('00000000-0000-0000-0000-000000000103', 103)
ON CONFLICT DO NOTHING;
UPDATE public.users SET role = 'developer' WHERE id = 103;

SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000101', false);
INSERT INTO public.activity_logs (user_id, actor_name, action_type, description, target_type, target_id)
VALUES (101, 'Label bebas', 'FAVORITE_ADDED', 'Menambahkan favorit', 'KOST', '301');

DO $$
BEGIN
    IF (SELECT count(*) FROM public.activity_logs) <> 1 THEN
        RAISE EXCEPTION 'actor cannot read their own activity log';
    END IF;
    BEGIN
        INSERT INTO public.activity_logs (user_id, action_type, description)
        VALUES (103, 'FORGED', 'Cross-user actor');
        RAISE EXCEPTION 'cross-user activity log insert unexpectedly succeeded';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
    IF has_table_privilege(current_user, 'public.activity_logs', 'UPDATE')
       OR has_table_privilege(current_user, 'public.activity_logs', 'DELETE') THEN
        RAISE EXCEPTION 'activity log unexpectedly grants update/delete';
    END IF;
END;
$$;

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000103', false);
DO $$
BEGIN
    IF (SELECT count(*) FROM public.activity_logs) <> 1 THEN
        RAISE EXCEPTION 'staff cannot read activity logs under RLS';
    END IF;
END;
$$;
RESET ROLE;
