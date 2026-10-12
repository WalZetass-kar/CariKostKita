-- Requires 20261007_auth_identity_bridge.sql first.
-- Re-submit a listing using its bigint application ID and the verified owner.

BEGIN;

DO $$
DECLARE
    item record;
    actual_type text;
BEGIN
    IF to_regprocedure('public.current_app_user_id()') IS NULL
       OR to_regprocedure('public.current_app_role()') IS NULL THEN
        RAISE EXCEPTION 'Kost resubmission requires the identity bridge';
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
            RAISE EXCEPTION 'Kost resubmission preflight failed: public.kosts.% must be % (found %)',
                item.column_name, item.expected_type, COALESCE(actual_type, 'missing');
        END IF;
    END LOOP;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'kosts'
          AND column_name = 'verification_status'
    ) THEN
        RAISE EXCEPTION 'Kost resubmission preflight failed: public.kosts.verification_status is missing';
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION public.resubmit_kost_for_current_app_user(p_kost_id bigint)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    app_owner_id bigint;
BEGIN
    app_owner_id := public.current_app_user_id();
    IF app_owner_id IS NULL OR public.current_app_role() IS DISTINCT FROM 'owner' THEN
        RAISE EXCEPTION 'owner role required' USING ERRCODE = '42501';
    END IF;

    UPDATE public.kosts
    SET verification_status = 'PENDING'
    WHERE id = p_kost_id
      AND owner_id = app_owner_id
      AND verification_status::text IN ('REJECTED', 'REVISION_REQUIRED');

    IF NOT FOUND THEN
        RAISE EXCEPTION 'kost tidak dapat diajukan ulang' USING ERRCODE = '42501';
    END IF;
END;
$$;

REVOKE ALL ON FUNCTION public.resubmit_kost_for_current_app_user(bigint) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.resubmit_kost_for_current_app_user(bigint) TO authenticated;

COMMIT;
