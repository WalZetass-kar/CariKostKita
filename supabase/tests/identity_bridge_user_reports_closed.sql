-- Execute after the identity bridge and before the feature-specific report,
-- survey, and review policy migrations. These tables must be closed in the
-- intermediate state so legacy permissive grants/policies cannot survive.
DO $$
BEGIN
    IF NOT (SELECT relrowsecurity FROM pg_class WHERE oid = 'public.user_reports'::regclass) THEN
        RAISE EXCEPTION 'user_reports RLS is not enabled by the identity bridge';
    END IF;
    IF has_table_privilege('authenticated', 'public.user_reports', 'SELECT')
       OR has_table_privilege('authenticated', 'public.user_reports', 'INSERT')
       OR has_table_privilege('authenticated', 'public.user_reports', 'UPDATE')
       OR has_table_privilege('authenticated', 'public.user_reports', 'DELETE') THEN
        RAISE EXCEPTION 'user_reports has grants during the closed migration interval';
    END IF;
    IF EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = 'public' AND tablename = 'user_reports'
    ) THEN
        RAISE EXCEPTION 'legacy user_reports policy survived the identity bridge';
    END IF;
END;
$$;

DO $$
DECLARE seq_name text;
BEGIN
    seq_name := pg_get_serial_sequence('public.reviews', 'id');
    IF seq_name IS NOT NULL AND has_sequence_privilege('authenticated', seq_name, 'USAGE') THEN
        RAISE EXCEPTION 'authenticated role retains legacy reviews sequence usage';
    END IF;
    seq_name := pg_get_serial_sequence('public.user_reports', 'id');
    IF seq_name IS NOT NULL AND has_sequence_privilege('authenticated', seq_name, 'USAGE') THEN
        RAISE EXCEPTION 'authenticated role retains legacy user_reports sequence usage';
    END IF;
    seq_name := pg_get_serial_sequence('public.survey_requests', 'id');
    IF seq_name IS NOT NULL AND has_sequence_privilege('authenticated', seq_name, 'USAGE') THEN
        RAISE EXCEPTION 'authenticated role retains legacy survey sequence usage';
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT (SELECT relrowsecurity FROM pg_class WHERE oid = 'public.reviews'::regclass) THEN
        RAISE EXCEPTION 'reviews RLS is not enabled by the identity bridge';
    END IF;
    IF has_table_privilege('authenticated', 'public.reviews', 'SELECT')
       OR has_table_privilege('authenticated', 'public.reviews', 'INSERT')
       OR has_table_privilege('authenticated', 'public.reviews', 'UPDATE')
       OR has_table_privilege('authenticated', 'public.reviews', 'DELETE') THEN
        RAISE EXCEPTION 'reviews has grants during the closed migration interval';
    END IF;
    IF EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = 'public' AND tablename = 'reviews'
    ) THEN
        RAISE EXCEPTION 'legacy reviews policy survived the identity bridge';
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT (SELECT relrowsecurity FROM pg_class WHERE oid = 'public.survey_requests'::regclass) THEN
        RAISE EXCEPTION 'survey_requests RLS is not enabled by the identity bridge';
    END IF;
    IF has_table_privilege('authenticated', 'public.survey_requests', 'SELECT')
       OR has_table_privilege('authenticated', 'public.survey_requests', 'INSERT')
       OR has_table_privilege('authenticated', 'public.survey_requests', 'UPDATE')
       OR has_table_privilege('authenticated', 'public.survey_requests', 'DELETE') THEN
        RAISE EXCEPTION 'survey_requests has grants during the closed migration interval';
    END IF;
    IF EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = 'public' AND tablename = 'survey_requests'
    ) THEN
        RAISE EXCEPTION 'legacy survey_requests policy survived the identity bridge';
    END IF;
END;
$$;
