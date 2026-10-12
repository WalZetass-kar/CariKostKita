-- Requires 20261007_auth_identity_bridge.sql first.
-- Update a listing status for admins/developers and persist an atomic audit row.

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
        RAISE EXCEPTION 'Admin kost status update requires the identity bridge';
    END IF;
    FOR item IN
        SELECT * FROM (VALUES ('id', 'bigint')) AS required(column_name, expected_type)
    LOOP
        SELECT data_type INTO actual_type
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'kosts'
          AND column_name = item.column_name;
        IF actual_type IS DISTINCT FROM item.expected_type THEN
            RAISE EXCEPTION 'Admin kost status preflight failed: public.kosts.% must be % (found %)',
                item.column_name, item.expected_type, COALESCE(actual_type, 'missing');
        END IF;
    END LOOP;

    SELECT data_type, udt_schema, udt_name INTO status_type
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'kosts'
      AND column_name = 'status';
    IF status_type.data_type IS NULL
       OR status_type.data_type NOT IN ('text', 'character varying', 'USER-DEFINED') THEN
        RAISE EXCEPTION 'Admin kost status preflight failed: unsupported public.kosts.status type %',
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
            RAISE EXCEPTION 'Admin kost status preflight failed: enum is missing values %', missing_statuses;
        END IF;
    END IF;
END;
$$;

CREATE SCHEMA IF NOT EXISTS app_private;

CREATE TABLE IF NOT EXISTS app_private.admin_kost_status_actions (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kost_id bigint NOT NULL,
    actor_id bigint NOT NULL,
    previous_status text NOT NULL,
    new_status text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
REVOKE ALL ON TABLE app_private.admin_kost_status_actions FROM PUBLIC, anon, authenticated;
DO $$
DECLARE
    seq_name text := pg_get_serial_sequence('app_private.admin_kost_status_actions', 'id');
BEGIN
    IF seq_name IS NOT NULL THEN
        EXECUTE format('REVOKE ALL ON SEQUENCE %s FROM PUBLIC, anon, authenticated', seq_name);
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_update_kost_status(
    p_kost_id bigint,
    p_status text
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public, app_private
AS $$
DECLARE
    actor_id bigint;
    actor_role text;
    status_schema text;
    status_type text;
    previous_status text;
    updated_rows bigint;
BEGIN
    actor_id := public.current_app_user_id();
    actor_role := public.current_app_role();
    IF actor_id IS NULL OR actor_role IS NULL OR actor_role NOT IN ('developer', 'admin') THEN
        RAISE EXCEPTION 'admin role required' USING ERRCODE = '42501';
    END IF;
    IF p_status IS NULL OR p_status NOT IN ('TERSEDIA', 'PENUH') THEN
        RAISE EXCEPTION 'unsupported kost status' USING ERRCODE = '22023';
    END IF;

    SELECT status::text INTO previous_status
    FROM public.kosts
    WHERE id = p_kost_id
    FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'kost not found' USING ERRCODE = 'P0002';
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
        'UPDATE public.kosts SET status = $1::%I.%I WHERE id = $2',
        status_schema, status_type
    ) USING p_status, p_kost_id;
    GET DIAGNOSTICS updated_rows = ROW_COUNT;
    IF updated_rows = 0 THEN
        RAISE EXCEPTION 'kost not found' USING ERRCODE = 'P0002';
    END IF;

    INSERT INTO app_private.admin_kost_status_actions
        (kost_id, actor_id, previous_status, new_status)
    VALUES (p_kost_id, actor_id, previous_status, p_status);
END;
$$;

REVOKE ALL ON FUNCTION public.admin_update_kost_status(bigint, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_update_kost_status(bigint, text) TO authenticated;

COMMIT;
