-- Migrate Android survey scheduling onto the verified numeric production schema.
-- Apply after 20261007_auth_identity_bridge.sql and 20261012_user_reports_fastapi.sql.

BEGIN;

DO $$
DECLARE
    item record;
    actual_type text;
    id_default text;
    id_identity text;
    active_duplicates bigint;
BEGIN
    IF to_regprocedure('public.current_app_user_id()') IS NULL THEN
        RAISE EXCEPTION 'survey migration requires public.current_app_user_id()';
    END IF;
    FOR item IN
        SELECT * FROM (VALUES
            ('survey_requests', 'id'), ('survey_requests', 'kost_id'),
            ('survey_requests', 'owner_id'), ('survey_requests', 'pencari_id'),
            ('kosts', 'id'), ('kosts', 'owner_id'),
            ('user_blocks', 'blocker_id'), ('user_blocks', 'blocked_id')
        ) AS required(table_name, column_name)
    LOOP
        SELECT data_type INTO actual_type
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = item.table_name
          AND column_name = item.column_name;
        IF actual_type IS DISTINCT FROM 'bigint' THEN
            RAISE EXCEPTION 'survey migration preflight failed: public.%.% must be bigint (found %)',
                item.table_name, item.column_name, COALESCE(actual_type, 'missing');
        END IF;
    END LOOP;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'survey_requests' AND column_name = 'status'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'survey_requests' AND column_name = 'created_at'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'survey_requests' AND column_name = 'updated_at'
    ) THEN
        RAISE EXCEPTION 'survey migration preflight failed: status/timestamp columns are required';
    END IF;
    SELECT column_default, is_identity INTO id_default, id_identity
    FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'survey_requests' AND column_name = 'id';
    IF COALESCE(id_identity, 'NO') <> 'YES'
       AND COALESCE(id_default, '') NOT LIKE 'nextval(%' THEN
        RAISE EXCEPTION 'survey migration preflight failed: id must have an identity/sequence default';
    END IF;
    SELECT count(*) INTO active_duplicates
    FROM (
        SELECT kost_id, pencari_id
        FROM public.survey_requests
        WHERE status::text IN ('MENUNGGU', 'DIKONFIRMASI')
        GROUP BY kost_id, pencari_id
        HAVING count(*) > 1
    ) AS duplicate_pairs;
    IF active_duplicates > 0 THEN
        RAISE EXCEPTION 'survey migration preflight failed: % duplicate active seeker/kost pairs; reconcile them before retrying', active_duplicates;
    END IF;
END;
$$;

-- Production does not currently expose these legacy Android columns. Keep old
-- rows valid (nullable schedule) and require a future schedule on new inserts.
ALTER TABLE public.survey_requests ADD COLUMN IF NOT EXISTS jadwal timestamptz;
ALTER TABLE public.survey_requests ADD COLUMN IF NOT EXISTS catatan text;
ALTER TABLE public.survey_requests ADD COLUMN IF NOT EXISTS alasan text;
DROP TRIGGER IF EXISTS trg_guard_survey_status ON public.survey_requests;

CREATE UNIQUE INDEX IF NOT EXISTS survey_requests_one_active_per_kost_seeker
    ON public.survey_requests (kost_id, pencari_id)
    WHERE status::text IN ('MENUNGGU', 'DIKONFIRMASI');

ALTER TABLE public.survey_requests ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON TABLE public.survey_requests FROM PUBLIC, anon, authenticated;
DO $$
DECLARE policy_row record; column_row record;
BEGIN
    FOR policy_row IN
        SELECT policyname FROM pg_policies
        WHERE schemaname = 'public' AND tablename = 'survey_requests'
    LOOP
        EXECUTE format('DROP POLICY %I ON public.survey_requests', policy_row.policyname);
    END LOOP;
    FOR column_row IN
        SELECT column_name FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'survey_requests'
    LOOP
        EXECUTE format('REVOKE ALL (%I) ON TABLE public.survey_requests FROM PUBLIC, anon, authenticated', column_row.column_name);
    END LOOP;
END;
$$;

GRANT SELECT (id, kost_id, pencari_id, owner_id, jadwal, catatan, status, alasan, created_at, updated_at)
    ON TABLE public.survey_requests TO authenticated;
GRANT INSERT (kost_id, pencari_id, owner_id, jadwal, catatan, status)
    ON TABLE public.survey_requests TO authenticated;
GRANT UPDATE (status, alasan) ON TABLE public.survey_requests TO authenticated;
DO $$
DECLARE seq_name text;
BEGIN
    seq_name := pg_get_serial_sequence('public.survey_requests', 'id');
    IF seq_name IS NULL THEN
        RAISE EXCEPTION 'survey migration failed: id sequence is unavailable';
    END IF;
    EXECUTE format('GRANT USAGE, SELECT ON SEQUENCE %s TO authenticated', seq_name);
END;
$$;

CREATE POLICY app_surveys_select ON public.survey_requests
FOR SELECT TO authenticated
USING (pencari_id = public.current_app_user_id() OR owner_id = public.current_app_user_id());

CREATE POLICY app_surveys_insert ON public.survey_requests
FOR INSERT TO authenticated
WITH CHECK (
    pencari_id = public.current_app_user_id()
    AND pencari_id <> owner_id
    AND status::text = 'MENUNGGU'
    AND jadwal IS NOT NULL
    AND jadwal > now()
    AND EXISTS (
        SELECT 1 FROM public.kosts AS kost
        WHERE kost.id = survey_requests.kost_id
          AND kost.owner_id = survey_requests.owner_id
          AND kost.verification_status::text = 'APPROVED'
          AND COALESCE(kost.status::text, '') <> 'TIDAK_AKTIF'
    )
    AND NOT EXISTS (
        SELECT 1 FROM public.user_blocks AS block
        WHERE (block.blocker_id = survey_requests.pencari_id AND block.blocked_id = survey_requests.owner_id)
           OR (block.blocker_id = survey_requests.owner_id AND block.blocked_id = survey_requests.pencari_id)
    )
);

CREATE POLICY app_surveys_update ON public.survey_requests
FOR UPDATE TO authenticated
USING (pencari_id = public.current_app_user_id() OR owner_id = public.current_app_user_id())
WITH CHECK (pencari_id = public.current_app_user_id() OR owner_id = public.current_app_user_id());

CREATE OR REPLACE FUNCTION public.guard_app_survey_update()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog, public
AS $$
DECLARE app_user_id bigint := public.current_app_user_id();
BEGIN
    NEW.id := OLD.id;
    NEW.kost_id := OLD.kost_id;
    NEW.pencari_id := OLD.pencari_id;
    NEW.owner_id := OLD.owner_id;
    NEW.jadwal := OLD.jadwal;
    NEW.catatan := OLD.catatan;
    NEW.created_at := OLD.created_at;
    NEW.updated_at := now();

    IF NEW.status::text = OLD.status::text THEN
        RAISE EXCEPTION 'survey status must transition to a different allowed state' USING ERRCODE = '42501';
    END IF;
    IF app_user_id = OLD.owner_id
       AND OLD.status::text = 'MENUNGGU'
       AND NEW.status::text IN ('DIKONFIRMASI', 'DITOLAK') THEN
        RETURN NEW;
    END IF;
    IF app_user_id = OLD.owner_id
       AND OLD.status::text = 'DIKONFIRMASI'
       AND NEW.status::text IN ('SELESAI', 'DITOLAK') THEN
        RETURN NEW;
    END IF;
    IF app_user_id = OLD.pencari_id
       AND OLD.status::text IN ('MENUNGGU', 'DIKONFIRMASI')
       AND NEW.status::text = 'DIBATALKAN' THEN
        RETURN NEW;
    END IF;
    RAISE EXCEPTION 'survey status transition is not allowed for this participant' USING ERRCODE = '42501';
END;
$$;
REVOKE ALL ON FUNCTION public.guard_app_survey_update() FROM PUBLIC, anon, authenticated;
DROP TRIGGER IF EXISTS trg_guard_app_survey_update ON public.survey_requests;
CREATE TRIGGER trg_guard_app_survey_update
BEFORE UPDATE ON public.survey_requests
FOR EACH ROW EXECUTE FUNCTION public.guard_app_survey_update();

COMMIT;
