-- Requires 20261007_auth_identity_bridge.sql first.
-- Replace the UUID-era owner comparison with the authenticated bigint identity.
-- Review preflight and ownership semantics before applying to production.

BEGIN;

DO $$
DECLARE
    item record;
    actual_type text;
BEGIN
    IF to_regprocedure('public.current_app_user_id()') IS NULL
       OR to_regprocedure('public.current_app_role()') IS NULL THEN
        RAISE EXCEPTION 'Owner favorite count requires the identity bridge';
    END IF;
    FOR item IN
        SELECT * FROM (VALUES
            ('kosts', 'id', 'bigint'),
            ('kosts', 'owner_id', 'bigint'),
            ('favorites', 'user_id', 'bigint'),
            ('favorites', 'kost_id', 'bigint')
        ) AS required(table_name, column_name, expected_type)
    LOOP
        SELECT data_type INTO actual_type
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = item.table_name
          AND column_name = item.column_name;
        IF actual_type IS DISTINCT FROM item.expected_type THEN
            RAISE EXCEPTION 'Owner favorite count preflight failed: public.%.% must be % (found %)',
                item.table_name, item.column_name, item.expected_type, COALESCE(actual_type, 'missing');
        END IF;
    END LOOP;
END;
$$;

CREATE OR REPLACE FUNCTION public.owner_favorite_count_for_current_app_user()
RETURNS integer
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    app_owner_id bigint;
    favorite_count integer;
BEGIN
    app_owner_id := public.current_app_user_id();
    IF app_owner_id IS NULL OR public.current_app_role() IS DISTINCT FROM 'owner' THEN
        RAISE EXCEPTION 'owner role required' USING ERRCODE = '42501';
    END IF;

    SELECT count(*)::integer INTO favorite_count
    FROM public.favorites AS favorite
    JOIN public.kosts AS kost ON kost.id = favorite.kost_id
    WHERE kost.owner_id = app_owner_id;
    RETURN favorite_count;
END;
$$;

-- Preserve the old client RPC name as a safe compatibility wrapper.
CREATE OR REPLACE FUNCTION public.owner_favorite_count()
RETURNS integer
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT public.owner_favorite_count_for_current_app_user();
$$;

REVOKE ALL ON FUNCTION public.owner_favorite_count_for_current_app_user() FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.owner_favorite_count() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.owner_favorite_count_for_current_app_user() TO authenticated;
GRANT EXECUTE ON FUNCTION public.owner_favorite_count() TO authenticated;

COMMIT;
