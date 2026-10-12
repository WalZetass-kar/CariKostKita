-- Requires 20261007_auth_identity_bridge.sql first.
-- Allows an owner to toggle availability on their own listing only.

BEGIN;

DO $$
DECLARE
    item record;
    actual_type text;
    status_type record;
    missing_statuses text[];
BEGIN
    IF to_regprocedure('public.current_app_user_id()') IS NULL
       OR to_regprocedure('public.current_app_role()') IS NULL THEN
        RAISE EXCEPTION 'Owner kost status update requires the identity bridge';
    END IF;
    FOR item IN
        SELECT * FROM (VALUES
            ('id', 'bigint'),
            ('owner_id', 'bigint')
        ) AS required(column_name, expected_type)
    LOOP
        SELECT data_type INTO actual_type
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'kosts'
          AND column_name = item.column_name;
        IF actual_type IS DISTINCT FROM item.expected_type THEN
            RAISE EXCEPTION 'Owner kost status preflight failed: public.kosts.% must be % (found %)',
                item.column_name, item.expected_type, COALESCE(actual_type, 'missing');
        END IF;
    END LOOP;

    SELECT data_type, udt_schema, udt_name
    INTO status_type
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'kosts'
      AND column_name = 'status';
    IF status_type.data_type IS NULL
       OR status_type.data_type NOT IN ('text', 'character varying', 'USER-DEFINED') THEN
        RAISE EXCEPTION 'Owner kost status preflight failed: unsupported public.kosts.status type %',
            COALESCE(status_type.data_type, 'missing');
    END IF;
    IF status_type.data_type = 'USER-DEFINED' THEN
        SELECT array_agg(expected.label ORDER BY expected.label)
        INTO missing_statuses
        FROM unnest(ARRAY['TERSEDIA', 'PENUH']) AS expected(label)
        WHERE NOT EXISTS (
            SELECT 1
            FROM pg_catalog.pg_type AS t
            JOIN pg_catalog.pg_namespace AS n ON n.oid = t.typnamespace
            JOIN pg_catalog.pg_enum AS e ON e.enumtypid = t.oid
            WHERE n.nspname = status_type.udt_schema
              AND t.typname = status_type.udt_name
              AND e.enumlabel = expected.label
        );
        IF missing_statuses IS NOT NULL THEN
            RAISE EXCEPTION 'Owner kost status preflight failed: enum is missing values %', missing_statuses;
        END IF;
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION public.update_my_kost_status_for_current_app_user(
    p_kost_id bigint,
    p_status text
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    app_owner_id bigint;
    status_schema text;
    status_type text;
    updated_rows bigint;
BEGIN
    app_owner_id := public.current_app_user_id();
    IF app_owner_id IS NULL OR public.current_app_role() IS DISTINCT FROM 'owner' THEN
        RAISE EXCEPTION 'owner role required' USING ERRCODE = '42501';
    END IF;
    IF p_status IS NULL OR p_status NOT IN ('TERSEDIA', 'PENUH') THEN
        RAISE EXCEPTION 'unsupported kost status' USING ERRCODE = '22023';
    END IF;

    SELECT udt_schema, udt_name INTO status_schema, status_type
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'kosts'
      AND column_name = 'status';
    IF status_schema IS NULL OR status_type IS NULL THEN
        RAISE EXCEPTION 'kost status column is unavailable';
    END IF;

    EXECUTE format(
        'UPDATE public.kosts SET status = $1::%I.%I WHERE id = $2 AND owner_id = $3',
        status_schema, status_type
    ) USING p_status, p_kost_id, app_owner_id;
    GET DIAGNOSTICS updated_rows = ROW_COUNT;
    IF updated_rows = 0 THEN
        RAISE EXCEPTION 'kost does not belong to the active owner' USING ERRCODE = '42501';
    END IF;
END;
$$;

REVOKE ALL ON FUNCTION public.update_my_kost_status_for_current_app_user(bigint, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.update_my_kost_status_for_current_app_user(bigint, text) TO authenticated;

COMMIT;
