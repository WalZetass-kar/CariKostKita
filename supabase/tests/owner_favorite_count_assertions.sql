-- Execute after identity_bridge_schema.sql, 20261007_auth_identity_bridge.sql,
-- and 20261021_owner_favorite_count_fastapi.sql.
INSERT INTO public.auth_user_links (auth_user_id, app_user_id) VALUES
    ('00000000-0000-0000-0000-000000000102', 102),
    ('00000000-0000-0000-0000-000000000103', 103)
ON CONFLICT DO NOTHING;
UPDATE public.users SET role = 'owner' WHERE id = 102;
INSERT INTO public.kosts (id, owner_id, name, status, verification_status)
OVERRIDING SYSTEM VALUE VALUES
    (302, 102, 'Kost Owner Kedua', 'TERSEDIA', 'APPROVED'),
    (303, 101, 'Kost Pengguna', 'TERSEDIA', 'APPROVED');
INSERT INTO public.favorites (user_id, kost_id) VALUES
    (101, 301), (103, 301), (101, 302), (103, 303);

SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000102', false);
DO $$
DECLARE actual_count integer;
BEGIN
    SELECT public.owner_favorite_count_for_current_app_user() INTO actual_count;
    IF actual_count <> 3 THEN
        RAISE EXCEPTION 'owner favorite count escaped ownership: %', actual_count;
    END IF;
    IF public.owner_favorite_count() <> actual_count THEN
        RAISE EXCEPTION 'legacy compatibility RPC did not delegate to the bridge function';
    END IF;
END;
$$;

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000103', false);
DO $$
BEGIN
    PERFORM public.owner_favorite_count_for_current_app_user();
    RAISE EXCEPTION 'non-owner was allowed to read owner favorite count';
EXCEPTION WHEN insufficient_privilege THEN
    NULL;
END;
$$;
RESET ROLE;
