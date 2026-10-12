-- Execute after identity_bridge_schema.sql and 20261007_auth_identity_bridge.sql.
-- Mirrors GET /api/v1/favorites/availability queries under the caller's JWT.
INSERT INTO public.auth_user_links (auth_user_id, app_user_id) VALUES
    ('00000000-0000-0000-0000-000000000101', 101),
    ('00000000-0000-0000-0000-000000000103', 103)
ON CONFLICT DO NOTHING;
INSERT INTO public.kosts (id, owner_id, name, status, verification_status)
OVERRIDING SYSTEM VALUE VALUES (302, 102, 'Kost Pending', 'TERSEDIA', 'PENDING');
INSERT INTO public.favorites (user_id, kost_id) VALUES
    (101, 301), (101, 302), (103, 301);

SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000101', false);
DO $$
DECLARE
    own_favorites bigint[];
    visible_kosts bigint[];
    available_count bigint;
BEGIN
    SELECT array_agg(kost_id ORDER BY kost_id) INTO own_favorites
    FROM public.favorites WHERE user_id = public.current_app_user_id();
    IF own_favorites IS DISTINCT FROM ARRAY[301, 302]::bigint[] THEN
        RAISE EXCEPTION 'favorite query escaped or omitted caller scope: %', own_favorites;
    END IF;

    SELECT array_agg(id ORDER BY id) INTO visible_kosts
    FROM public.kosts WHERE id = ANY (own_favorites);
    IF visible_kosts IS DISTINCT FROM ARRAY[301]::bigint[] THEN
        RAISE EXCEPTION 'kost RLS exposed a pending listing or hid approved listing: %', visible_kosts;
    END IF;

    SELECT count(*) INTO available_count FROM public.kosts
    WHERE id = ANY (own_favorites)
      AND verification_status = 'APPROVED'
      AND status = 'TERSEDIA';
    IF available_count <> 1 THEN
        RAISE EXCEPTION 'favorite availability returned unexpected count: %', available_count;
    END IF;
END;
$$;

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000103', false);
DO $$
DECLARE
    own_favorites bigint[];
BEGIN
    SELECT array_agg(kost_id ORDER BY kost_id) INTO own_favorites
    FROM public.favorites WHERE user_id = public.current_app_user_id();
    IF own_favorites IS DISTINCT FROM ARRAY[301]::bigint[] THEN
        RAISE EXCEPTION 'second user received another account favorites: %', own_favorites;
    END IF;
END;
$$;
RESET ROLE;
