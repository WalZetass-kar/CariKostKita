-- Secure reviews for the verified bigint schema and expose the missing app fields.
-- Apply after the identity bridge and 20261013_surveys_fastapi.sql.

BEGIN;

DO $$
DECLARE
    item record;
    actual_type text;
    id_default text;
    id_identity text;
    duplicate_pairs bigint;
    invalid_ratings bigint;
BEGIN
    IF to_regprocedure('public.current_app_user_id()') IS NULL THEN
        RAISE EXCEPTION 'review migration requires public.current_app_user_id()';
    END IF;
    FOR item IN
        SELECT * FROM (VALUES
            ('reviews', 'id'), ('reviews', 'kost_id'), ('reviews', 'user_id'), ('reviews', 'rating'),
            ('kosts', 'id'), ('survey_requests', 'kost_id'),
            ('survey_requests', 'pencari_id'), ('survey_requests', 'status')
        ) AS required(table_name, column_name)
    LOOP
        SELECT data_type INTO actual_type
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = item.table_name
          AND column_name = item.column_name;
        IF item.table_name = 'reviews' AND item.column_name = 'rating'
           AND actual_type IS DISTINCT FROM 'smallint' THEN
            RAISE EXCEPTION 'review migration preflight failed: public.reviews.rating must be smallint (found %)',
                COALESCE(actual_type, 'missing');
        ELSIF item.column_name <> 'status' AND actual_type IS DISTINCT FROM 'bigint'
           AND NOT (item.table_name = 'reviews' AND item.column_name = 'rating') THEN
            RAISE EXCEPTION 'review migration preflight failed: public.%.% must be bigint (found %)',
                item.table_name, item.column_name, COALESCE(actual_type, 'missing');
        ELSIF item.column_name = 'status' AND actual_type NOT IN ('text', 'character varying') THEN
            RAISE EXCEPTION 'review migration preflight failed: survey_requests.status must be text (found %)',
                COALESCE(actual_type, 'missing');
        END IF;
    END LOOP;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'reviews' AND column_name = 'rating'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'reviews' AND column_name = 'created_at'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'reviews' AND column_name = 'updated_at'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'name'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'kosts' AND column_name = 'rating_avg'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'kosts' AND column_name = 'rating_count'
    ) THEN
        RAISE EXCEPTION 'review migration preflight failed: required rating/name/timestamp columns are incomplete';
    END IF;
    SELECT column_default, is_identity INTO id_default, id_identity
    FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'reviews' AND column_name = 'id';
    IF COALESCE(id_identity, 'NO') <> 'YES'
       AND COALESCE(id_default, '') NOT LIKE 'nextval(%' THEN
        RAISE EXCEPTION 'review migration preflight failed: reviews.id must have an identity/sequence default';
    END IF;
    SELECT count(*) INTO duplicate_pairs
    FROM (
        SELECT kost_id, user_id FROM public.reviews
        GROUP BY kost_id, user_id HAVING count(*) > 1
    ) AS duplicates;
    IF duplicate_pairs > 0 THEN
        RAISE EXCEPTION 'review migration preflight failed: % duplicate kost/user pairs; reconcile before retrying', duplicate_pairs;
    END IF;
    SELECT count(*) INTO invalid_ratings FROM public.reviews WHERE rating NOT BETWEEN 1 AND 5;
    IF invalid_ratings > 0 THEN
        RAISE EXCEPTION 'review migration preflight failed: % existing ratings are outside 1..5', invalid_ratings;
    END IF;
END;
$$;

ALTER TABLE public.reviews ADD COLUMN IF NOT EXISTS nama_user text;
ALTER TABLE public.reviews ADD COLUMN IF NOT EXISTS komentar text;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.reviews'::regclass AND conname = 'reviews_rating_range'
    ) THEN
        ALTER TABLE public.reviews ADD CONSTRAINT reviews_rating_range CHECK (rating BETWEEN 1 AND 5);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.reviews'::regclass AND conname = 'reviews_comment_length'
    ) THEN
        ALTER TABLE public.reviews ADD CONSTRAINT reviews_comment_length
            CHECK (komentar IS NULL OR char_length(komentar) <= 1000);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.reviews'::regclass AND conname = 'reviews_one_per_kost_user'
    ) THEN
        ALTER TABLE public.reviews ADD CONSTRAINT reviews_one_per_kost_user UNIQUE (kost_id, user_id);
    END IF;
END;
$$;

ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON TABLE public.reviews FROM PUBLIC, anon, authenticated;
DO $$
DECLARE policy_row record; column_row record; seq_name text;
BEGIN
    FOR policy_row IN
        SELECT policyname FROM pg_policies
        WHERE schemaname = 'public' AND tablename = 'reviews'
    LOOP
        EXECUTE format('DROP POLICY %I ON public.reviews', policy_row.policyname);
    END LOOP;
    FOR column_row IN
        SELECT column_name FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'reviews'
    LOOP
        EXECUTE format('REVOKE ALL (%I) ON TABLE public.reviews FROM PUBLIC, anon, authenticated', column_row.column_name);
    END LOOP;
    seq_name := pg_get_serial_sequence('public.reviews', 'id');
    IF seq_name IS NULL THEN
        RAISE EXCEPTION 'review migration failed: id sequence is unavailable';
    END IF;
    EXECUTE format('REVOKE ALL ON SEQUENCE %s FROM PUBLIC, anon, authenticated', seq_name);
END;
$$;

GRANT SELECT (id, kost_id, nama_user, rating, komentar, created_at)
    ON TABLE public.reviews TO anon, authenticated;

CREATE POLICY app_reviews_public_select ON public.reviews
FOR SELECT TO anon, authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.kosts AS kost
        WHERE kost.id = reviews.kost_id
          AND kost.verification_status::text = 'APPROVED'
          AND COALESCE(kost.status::text, '') <> 'TIDAK_AKTIF'
    )
);

CREATE OR REPLACE FUNCTION public.submit_app_review(
    p_kost_id bigint,
    p_rating smallint,
    p_komentar text DEFAULT NULL
)
RETURNS TABLE (
    id bigint,
    kost_id bigint,
    nama_user text,
    rating smallint,
    komentar text,
    created_at timestamptz
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    app_user_id bigint := public.current_app_user_id();
    reviewer_name text;
    clean_comment text := NULLIF(btrim(p_komentar), '');
BEGIN
    IF app_user_id IS NULL THEN
        RAISE EXCEPTION 'linked active user required' USING ERRCODE = '42501';
    END IF;
    IF p_rating IS NULL OR p_rating NOT BETWEEN 1 AND 5 THEN
        RAISE EXCEPTION 'rating must be between 1 and 5' USING ERRCODE = '22023';
    END IF;
    IF clean_comment IS NOT NULL AND char_length(clean_comment) > 1000 THEN
        RAISE EXCEPTION 'review comment is too long' USING ERRCODE = '22023';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM public.kosts AS kost
        WHERE kost.id = p_kost_id
          AND kost.verification_status::text = 'APPROVED'
          AND COALESCE(kost.status::text, '') <> 'TIDAK_AKTIF'
    ) THEN
        RAISE EXCEPTION 'kost not found or unavailable' USING ERRCODE = 'P0002';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM public.survey_requests AS survey
        WHERE survey.kost_id = p_kost_id
          AND survey.pencari_id = app_user_id
          AND survey.status::text = 'SELESAI'
    ) THEN
        RAISE EXCEPTION 'completed survey required' USING ERRCODE = '42501';
    END IF;
    SELECT app_user.name INTO reviewer_name
    FROM public.users AS app_user WHERE app_user.id = app_user_id;
    reviewer_name := COALESCE(NULLIF(btrim(reviewer_name), ''), 'Pengguna');

    RETURN QUERY
    INSERT INTO public.reviews AS existing_review (kost_id, user_id, nama_user, rating, komentar)
    VALUES (p_kost_id, app_user_id, reviewer_name, p_rating, clean_comment)
    ON CONFLICT ON CONSTRAINT reviews_one_per_kost_user DO UPDATE
       SET nama_user = EXCLUDED.nama_user,
           rating = EXCLUDED.rating,
           komentar = EXCLUDED.komentar,
           updated_at = now()
    RETURNING existing_review.id, existing_review.kost_id, existing_review.nama_user,
              existing_review.rating, existing_review.komentar, existing_review.created_at;
END;
$$;
REVOKE ALL ON FUNCTION public.submit_app_review(bigint, smallint, text) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.submit_app_review(bigint, smallint, text) TO authenticated;

CREATE OR REPLACE FUNCTION public.refresh_app_kost_rating()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE target_kost_id bigint := COALESCE(NEW.kost_id, OLD.kost_id);
BEGIN
    UPDATE public.kosts AS kost
    SET rating_avg = (SELECT round(avg(review.rating)::numeric, 2) FROM public.reviews AS review WHERE review.kost_id = target_kost_id),
        rating_count = (SELECT count(*)::integer FROM public.reviews AS review WHERE review.kost_id = target_kost_id)
    WHERE kost.id = target_kost_id;
    RETURN NULL;
END;
$$;
REVOKE ALL ON FUNCTION public.refresh_app_kost_rating() FROM PUBLIC, anon, authenticated;
DROP TRIGGER IF EXISTS trg_refresh_kost_rating ON public.reviews;
CREATE TRIGGER trg_refresh_kost_rating
AFTER INSERT OR UPDATE OR DELETE ON public.reviews
FOR EACH ROW EXECUTE FUNCTION public.refresh_app_kost_rating();

COMMIT;
