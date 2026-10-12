-- Execute after identity_bridge_schema.sql, 20261007_auth_identity_bridge.sql,
-- and 20261023_owner_kost_status_fastapi.sql.
INSERT INTO public.auth_user_links (auth_user_id, app_user_id) VALUES
    ('00000000-0000-0000-0000-000000000102', 102),
    ('00000000-0000-0000-0000-000000000103', 103)
ON CONFLICT DO NOTHING;
INSERT INTO public.kosts (id, owner_id, name, status, verification_status)
OVERRIDING SYSTEM VALUE VALUES
    (302, 102, 'Kost Pemilik', 'TERSEDIA', 'APPROVED'),
    (303, 101, 'Kost Pengguna Lain', 'TERSEDIA', 'APPROVED');

SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000102', false);
SELECT public.update_my_kost_status_for_current_app_user(302, 'PENUH');
DO $$
BEGIN
    IF (SELECT status::text FROM public.kosts WHERE id = 302) <> 'PENUH' THEN
        RAISE EXCEPTION 'owner could not update own kost status';
    END IF;
    IF (SELECT status::text FROM public.kosts WHERE id = 303) <> 'TERSEDIA' THEN
        RAISE EXCEPTION 'owner status update modified another listing';
    END IF;
    BEGIN
        PERFORM public.update_my_kost_status_for_current_app_user(302, 'TIDAK_AKTIF');
        RAISE EXCEPTION 'unsupported owner status was accepted';
    EXCEPTION WHEN invalid_parameter_value THEN
        NULL;
    END;
    BEGIN
        PERFORM public.update_my_kost_status_for_current_app_user(303, 'PENUH');
        RAISE EXCEPTION 'owner updated another owner listing';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
END;
$$;

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000103', false);
DO $$
BEGIN
    BEGIN
        PERFORM public.update_my_kost_status_for_current_app_user(303, 'PENUH');
        RAISE EXCEPTION 'non-owner updated kost status';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
END;
$$;
RESET ROLE;
