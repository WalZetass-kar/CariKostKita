-- =========================================================================
-- CARIKOSTKITA — FITUR SIAP INDUSTRI (Oktober 2026)
-- Jalankan SETELAH 20261005_security_hardening.sql. Aman dijalankan berulang.
--
--   1. Peran Moderator (terpisah dari Super Admin / developer)
--   2. Blokir & laporkan pengguna di chat
--   3. Hapus akun sendiri (kebijakan Google Play)
--   4. Dokumen verifikasi pemilik (KTP, selfie, bukti kepemilikan) di bucket privat
--   5. Rincian biaya & aturan kost
--   6. Jadwal survei, ulasan & rating
--   7. Event analitik, statistik per kost, funnel admin
--   8. Laporan crash dari aplikasi
--   9. Batas laju (rate limit) pesan & laporan
--  10. Ajukan ulang kost yang ditolak, waktu pengajuan untuk SLA moderasi
-- =========================================================================


-- -------------------------------------------------------------------------
-- 1. PERAN: is_staff() = developer, admin, moderator
--    is_developer() tetap = Super Admin (developer, admin)
-- -------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.is_staff()
RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.users
        WHERE id::text = auth.uid()::text
          AND role::text IN ('developer', 'admin', 'moderator')
          AND COALESCE(is_active, true)
    );
$$;
GRANT EXECUTE ON FUNCTION public.is_staff() TO anon, authenticated;

ALTER TABLE public.users ADD COLUMN IF NOT EXISTS pengajuan_at timestamptz;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS ktp_path text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS selfie_path text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS kepemilikan_path text;

-- Moderator boleh memverifikasi pemilik (user <-> owner) & menonaktifkan akun,
-- tetapi tidak boleh membuat developer/moderator lain. Super Admin bebas.
CREATE OR REPLACE FUNCTION public.protect_user_columns()
RETURNS trigger
LANGUAGE plpgsql SET search_path = public
AS $$
BEGIN
    IF current_user NOT IN ('anon', 'authenticated') OR public.is_developer() THEN
        IF TG_OP = 'UPDATE' AND NEW.verification_status::text = 'PENDING'
           AND OLD.verification_status IS DISTINCT FROM NEW.verification_status THEN
            NEW.pengajuan_at := now();
        END IF;
        RETURN NEW;
    END IF;

    IF TG_OP = 'INSERT' THEN
        NEW.role := 'user';
        NEW.is_active := true;
        IF NEW.verification_status IS NOT NULL AND NEW.verification_status::text <> 'PENDING' THEN
            NEW.verification_status := NULL;
        END IF;
        IF NEW.verification_status::text = 'PENDING' THEN NEW.pengajuan_at := now(); END IF;
        NEW.catatan_revisi := NULL;
        RETURN NEW;
    END IF;

    NEW.id := OLD.id;

    IF public.is_staff() AND OLD.id::text <> auth.uid()::text THEN
        -- Moderator: hanya user <-> owner, tidak bisa mengangkat staf
        IF NEW.role::text NOT IN ('user', 'owner') OR OLD.role::text NOT IN ('user', 'owner') THEN
            NEW.role := OLD.role;
        END IF;
        RETURN NEW;
    END IF;

    NEW.role := OLD.role;
    NEW.is_active := OLD.is_active;
    NEW.catatan_revisi := OLD.catatan_revisi;
    IF NEW.verification_status IS DISTINCT FROM OLD.verification_status THEN
        IF NEW.verification_status::text <> 'PENDING'
           OR COALESCE(OLD.verification_status::text, 'NONE') IN ('PENDING', 'APPROVED') THEN
            NEW.verification_status := OLD.verification_status;
        ELSE
            NEW.pengajuan_at := now();
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

-- Staf melihat & memoderasi; Super Admin tetap satu-satunya yang bisa menghapus akun
DROP POLICY IF EXISTS "users_select_public" ON public.users;
CREATE POLICY "users_select_public" ON public.users FOR SELECT
TO anon, authenticated
USING (
    id::text = auth.uid()::text
    OR public.is_staff()
    OR role::text IN ('owner', 'pemilik', 'pemilik_kost')
    OR EXISTS (
        SELECT 1 FROM public.chats c
        WHERE (c.pencari_id::text = auth.uid()::text AND c.owner_id::text = users.id::text)
           OR (c.owner_id::text = auth.uid()::text AND c.pencari_id::text = users.id::text)
    )
);

DROP POLICY IF EXISTS "users_update_self_or_dev" ON public.users;
CREATE POLICY "users_update_self_or_dev" ON public.users FOR UPDATE
TO authenticated
USING (id::text = auth.uid()::text OR public.is_staff())
WITH CHECK (id::text = auth.uid()::text OR public.is_staff());

DROP POLICY IF EXISTS "kosts_select_public" ON public.kosts;
CREATE POLICY "kosts_select_public" ON public.kosts FOR SELECT
TO anon, authenticated
USING (
    verification_status::text = 'APPROVED'
    OR owner_id::text = auth.uid()::text
    OR public.is_staff()
);

DROP POLICY IF EXISTS "kosts_update_owner_or_dev" ON public.kosts;
CREATE POLICY "kosts_update_owner_or_dev" ON public.kosts FOR UPDATE
TO authenticated
USING (owner_id::text = auth.uid()::text OR public.is_staff())
WITH CHECK (owner_id::text = auth.uid()::text OR public.is_staff());

CREATE OR REPLACE FUNCTION public.guard_kost_moderation()
RETURNS trigger
LANGUAGE plpgsql SET search_path = public
AS $$
BEGIN
    IF current_user NOT IN ('anon', 'authenticated') OR public.is_staff() THEN
        RETURN NEW;
    END IF;

    IF TG_OP = 'INSERT' THEN
        NEW.verification_status := 'PENDING';
        NEW.catatan_revisi := NULL;
        RETURN NEW;
    END IF;

    NEW.owner_id := OLD.owner_id;

    IF (NEW.nama_kost, NEW.alamat, NEW.deskripsi, NEW.tipe_kost, NEW.latitude, NEW.longitude,
        NEW.provinsi, NEW.kota, NEW.kecamatan, NEW.kelurahan, NEW.thumbnail_url, NEW.image_urls)
       IS DISTINCT FROM
       (OLD.nama_kost, OLD.alamat, OLD.deskripsi, OLD.tipe_kost, OLD.latitude, OLD.longitude,
        OLD.provinsi, OLD.kota, OLD.kecamatan, OLD.kelurahan, OLD.thumbnail_url, OLD.image_urls) THEN
        NEW.verification_status := 'PENDING';
    ELSE
        NEW.verification_status := OLD.verification_status;
    END IF;
    NEW.catatan_revisi := OLD.catatan_revisi;
    RETURN NEW;
END;
$$;

DROP POLICY IF EXISTS "chats_select" ON public.chats;
CREATE POLICY "chats_select" ON public.chats FOR SELECT
TO authenticated
USING (pencari_id::text = auth.uid()::text OR owner_id::text = auth.uid()::text OR public.is_staff());

DROP POLICY IF EXISTS "messages_select" ON public.messages;
CREATE POLICY "messages_select" ON public.messages FOR SELECT
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.chats c
        WHERE c.id::text = messages.chat_id::text
          AND (c.pencari_id::text = auth.uid()::text OR c.owner_id::text = auth.uid()::text)
    )
    OR public.is_staff()
);

DROP POLICY IF EXISTS "logs_authenticated_select" ON public.activity_logs;
CREATE POLICY "logs_authenticated_select" ON public.activity_logs FOR SELECT
TO authenticated
USING (user_id::text = auth.uid()::text OR public.is_staff());

DROP POLICY IF EXISTS "reports_select" ON public.reports;
CREATE POLICY "reports_select" ON public.reports FOR SELECT
TO authenticated
USING (reporter_id::text = auth.uid()::text OR public.is_staff());

DROP POLICY IF EXISTS "reports_update_dev" ON public.reports;
CREATE POLICY "reports_update_dev" ON public.reports FOR UPDATE
TO authenticated
USING (public.is_staff());


-- -------------------------------------------------------------------------
-- 2. BLOKIR & LAPORKAN PENGGUNA
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.user_blocks (
    blocker_id uuid NOT NULL,
    blocked_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (blocker_id, blocked_id)
);
ALTER TABLE public.user_blocks ENABLE ROW LEVEL SECURITY;
GRANT SELECT, INSERT, DELETE ON public.user_blocks TO authenticated;

DROP POLICY IF EXISTS "blocks_own" ON public.user_blocks;
CREATE POLICY "blocks_own" ON public.user_blocks FOR ALL
TO authenticated
USING (blocker_id::text = auth.uid()::text OR blocked_id::text = auth.uid()::text)
WITH CHECK (blocker_id::text = auth.uid()::text AND blocker_id <> blocked_id);

CREATE OR REPLACE FUNCTION public.is_blocked_between(a uuid, b uuid)
RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.user_blocks
        WHERE (blocker_id = a AND blocked_id = b) OR (blocker_id = b AND blocked_id = a)
    );
$$;
GRANT EXECUTE ON FUNCTION public.is_blocked_between(uuid, uuid) TO authenticated;

-- Pesan ditolak bila salah satu pihak memblokir pihak lain
DROP POLICY IF EXISTS "messages_insert" ON public.messages;
CREATE POLICY "messages_insert" ON public.messages FOR INSERT
TO authenticated
WITH CHECK (
    sender_id::text = auth.uid()::text
    AND EXISTS (
        SELECT 1 FROM public.chats c
        WHERE c.id::text = messages.chat_id::text
          AND (c.pencari_id::text = auth.uid()::text OR c.owner_id::text = auth.uid()::text)
          AND NOT public.is_blocked_between(c.pencari_id::uuid, c.owner_id::uuid)
    )
);

CREATE TABLE IF NOT EXISTS public.user_reports (
    id bigserial PRIMARY KEY,
    reporter_id uuid NOT NULL,
    reported_id uuid NOT NULL,
    chat_id uuid,
    alasan text NOT NULL,
    detail text,
    status text NOT NULL DEFAULT 'BARU',
    tindakan text,
    created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE public.user_reports ENABLE ROW LEVEL SECURITY;
GRANT SELECT, INSERT, UPDATE ON public.user_reports TO authenticated;
GRANT USAGE ON SEQUENCE public.user_reports_id_seq TO authenticated;

DROP POLICY IF EXISTS "user_reports_insert" ON public.user_reports;
CREATE POLICY "user_reports_insert" ON public.user_reports FOR INSERT
TO authenticated
WITH CHECK (reporter_id::text = auth.uid()::text AND reporter_id <> reported_id);

DROP POLICY IF EXISTS "user_reports_select" ON public.user_reports;
CREATE POLICY "user_reports_select" ON public.user_reports FOR SELECT
TO authenticated
USING (reporter_id::text = auth.uid()::text OR public.is_staff());

DROP POLICY IF EXISTS "user_reports_update" ON public.user_reports;
CREATE POLICY "user_reports_update" ON public.user_reports FOR UPDATE
TO authenticated
USING (public.is_staff());


-- -------------------------------------------------------------------------
-- 3. HAPUS AKUN SENDIRI
-- -------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.delete_my_account()
RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, auth
AS $$
DECLARE
    me uuid := auth.uid();
BEGIN
    IF me IS NULL THEN
        RAISE EXCEPTION 'not signed in' USING ERRCODE = '42501';
    END IF;
    DELETE FROM public.favorites WHERE user_id::text = me::text;
    DELETE FROM public.user_blocks WHERE blocker_id = me OR blocked_id = me;
    -- Kost milik akun ini ikut dihapus agar tidak ada listing yatim
    DELETE FROM public.kosts WHERE owner_id::text = me::text;
    DELETE FROM public.users WHERE id::text = me::text;
    DELETE FROM auth.users WHERE id = me;
END;
$$;
REVOKE ALL ON FUNCTION public.delete_my_account() FROM public, anon;
GRANT EXECUTE ON FUNCTION public.delete_my_account() TO authenticated;


-- -------------------------------------------------------------------------
-- 4. DOKUMEN VERIFIKASI PEMILIK — bucket PRIVAT, folder {user_id}/...
-- -------------------------------------------------------------------------
INSERT INTO storage.buckets (id, name, public)
VALUES ('verification-docs', 'verification-docs', false)
ON CONFLICT (id) DO UPDATE SET public = false;

DROP POLICY IF EXISTS "Verification docs upload own" ON storage.objects;
CREATE POLICY "Verification docs upload own" ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (bucket_id = 'verification-docs' AND (storage.foldername(name))[1] = auth.uid()::text);

DROP POLICY IF EXISTS "Verification docs read own or staff" ON storage.objects;
CREATE POLICY "Verification docs read own or staff" ON storage.objects FOR SELECT
TO authenticated
USING (bucket_id = 'verification-docs'
       AND ((storage.foldername(name))[1] = auth.uid()::text OR public.is_staff()));

DROP POLICY IF EXISTS "Verification docs update own" ON storage.objects;
CREATE POLICY "Verification docs update own" ON storage.objects FOR UPDATE
TO authenticated
USING (bucket_id = 'verification-docs' AND (storage.foldername(name))[1] = auth.uid()::text);

-- Kebijakan baca publik bucket kost-images hanya untuk bucket itu
DROP POLICY IF EXISTS "Public Read Kost Images" ON storage.objects;
CREATE POLICY "Public Read Kost Images" ON storage.objects FOR SELECT
USING (bucket_id = 'kost-images');


-- -------------------------------------------------------------------------
-- 5. RINCIAN BIAYA & ATURAN KOST (tidak memicu review ulang)
-- -------------------------------------------------------------------------
ALTER TABLE public.kosts ADD COLUMN IF NOT EXISTS deposit integer;
ALTER TABLE public.kosts ADD COLUMN IF NOT EXISTS minimal_sewa_bulan integer;
ALTER TABLE public.kosts ADD COLUMN IF NOT EXISTS biaya_tambahan text;
ALTER TABLE public.kosts ADD COLUMN IF NOT EXISTS aturan text[];
ALTER TABLE public.kosts ADD COLUMN IF NOT EXISTS rating_avg numeric(3,2);
ALTER TABLE public.kosts ADD COLUMN IF NOT EXISTS rating_count integer NOT NULL DEFAULT 0;


-- -------------------------------------------------------------------------
-- 6a. JADWAL SURVEI
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.survey_requests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    kost_id uuid NOT NULL,
    pencari_id uuid NOT NULL,
    owner_id uuid NOT NULL,
    jadwal timestamptz NOT NULL,
    catatan text,
    status text NOT NULL DEFAULT 'MENUNGGU',
    alasan text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE public.survey_requests ENABLE ROW LEVEL SECURITY;
GRANT SELECT, INSERT, UPDATE ON public.survey_requests TO authenticated;

DROP POLICY IF EXISTS "survey_insert" ON public.survey_requests;
CREATE POLICY "survey_insert" ON public.survey_requests FOR INSERT
TO authenticated
WITH CHECK (
    pencari_id::text = auth.uid()::text
    AND status = 'MENUNGGU'
    AND jadwal > now()
    AND EXISTS (SELECT 1 FROM public.kosts k
                WHERE k.id::text = survey_requests.kost_id::text
                  AND k.owner_id::text = survey_requests.owner_id::text
                  AND k.owner_id::text <> auth.uid()::text
                  AND k.verification_status::text = 'APPROVED')
    AND NOT public.is_blocked_between(pencari_id, owner_id)
);

DROP POLICY IF EXISTS "survey_select" ON public.survey_requests;
CREATE POLICY "survey_select" ON public.survey_requests FOR SELECT
TO authenticated
USING (pencari_id::text = auth.uid()::text OR owner_id::text = auth.uid()::text OR public.is_staff());

DROP POLICY IF EXISTS "survey_update" ON public.survey_requests;
CREATE POLICY "survey_update" ON public.survey_requests FOR UPDATE
TO authenticated
USING (pencari_id::text = auth.uid()::text OR owner_id::text = auth.uid()::text);

-- Transisi status: pemilik konfirmasi/tolak/selesai, pencari batalkan
CREATE OR REPLACE FUNCTION public.guard_survey_status()
RETURNS trigger
LANGUAGE plpgsql SET search_path = public
AS $$
DECLARE
    me text := auth.uid()::text;
BEGIN
    NEW.id := OLD.id; NEW.kost_id := OLD.kost_id; NEW.pencari_id := OLD.pencari_id;
    NEW.owner_id := OLD.owner_id; NEW.created_at := OLD.created_at; NEW.updated_at := now();
    IF current_user NOT IN ('anon', 'authenticated') THEN RETURN NEW; END IF;
    IF NEW.status = OLD.status THEN
        NEW.jadwal := OLD.jadwal;
        RETURN NEW;
    END IF;
    IF me = OLD.owner_id::text AND OLD.status = 'MENUNGGU' AND NEW.status IN ('DIKONFIRMASI', 'DITOLAK') THEN
        RETURN NEW;
    END IF;
    IF me = OLD.owner_id::text AND OLD.status = 'DIKONFIRMASI' AND NEW.status IN ('SELESAI', 'DITOLAK') THEN
        RETURN NEW;
    END IF;
    IF me = OLD.pencari_id::text AND OLD.status IN ('MENUNGGU', 'DIKONFIRMASI') AND NEW.status = 'DIBATALKAN' THEN
        RETURN NEW;
    END IF;
    RAISE EXCEPTION 'perubahan status survei tidak diizinkan' USING ERRCODE = '42501';
END;
$$;

DROP TRIGGER IF EXISTS trg_guard_survey_status ON public.survey_requests;
CREATE TRIGGER trg_guard_survey_status
BEFORE UPDATE ON public.survey_requests
FOR EACH ROW EXECUTE FUNCTION public.guard_survey_status();


-- -------------------------------------------------------------------------
-- 6b. ULASAN & RATING — hanya pencari yang survei-nya sudah SELESAI
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.reviews (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    kost_id uuid NOT NULL,
    user_id uuid NOT NULL,
    nama_user text,
    rating smallint NOT NULL CHECK (rating BETWEEN 1 AND 5),
    komentar text CHECK (char_length(komentar) <= 1000),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (kost_id, user_id)
);
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;
GRANT SELECT ON public.reviews TO anon, authenticated;
GRANT INSERT, UPDATE, DELETE ON public.reviews TO authenticated;

DROP POLICY IF EXISTS "reviews_select" ON public.reviews;
CREATE POLICY "reviews_select" ON public.reviews FOR SELECT
TO anon, authenticated USING (true);

DROP POLICY IF EXISTS "reviews_insert" ON public.reviews;
CREATE POLICY "reviews_insert" ON public.reviews FOR INSERT
TO authenticated
WITH CHECK (
    user_id::text = auth.uid()::text
    AND EXISTS (SELECT 1 FROM public.survey_requests s
                WHERE s.kost_id = reviews.kost_id
                  AND s.pencari_id::text = auth.uid()::text
                  AND s.status = 'SELESAI')
);

DROP POLICY IF EXISTS "reviews_update_own" ON public.reviews;
CREATE POLICY "reviews_update_own" ON public.reviews FOR UPDATE
TO authenticated USING (user_id::text = auth.uid()::text);

DROP POLICY IF EXISTS "reviews_delete_own_or_staff" ON public.reviews;
CREATE POLICY "reviews_delete_own_or_staff" ON public.reviews FOR DELETE
TO authenticated USING (user_id::text = auth.uid()::text OR public.is_staff());

CREATE OR REPLACE FUNCTION public.refresh_kost_rating()
RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
    target uuid := COALESCE(NEW.kost_id, OLD.kost_id);
BEGIN
    UPDATE public.kosts k
    SET rating_avg = (SELECT ROUND(AVG(rating)::numeric, 2) FROM public.reviews WHERE kost_id = target),
        rating_count = (SELECT COUNT(*) FROM public.reviews WHERE kost_id = target)
    WHERE k.id = target;
    RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS trg_refresh_kost_rating ON public.reviews;
CREATE TRIGGER trg_refresh_kost_rating
AFTER INSERT OR UPDATE OR DELETE ON public.reviews
FOR EACH ROW EXECUTE FUNCTION public.refresh_kost_rating();


-- -------------------------------------------------------------------------
-- 7. EVENT ANALITIK, STATISTIK PER KOST, FUNNEL ADMIN
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.app_events (
    id bigserial PRIMARY KEY,
    user_id uuid,
    event text NOT NULL CHECK (event IN ('search', 'view_detail', 'chat_start', 'survey_request', 'favorite_add')),
    kost_id uuid,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_app_events_kost ON public.app_events (kost_id, event);
CREATE INDEX IF NOT EXISTS idx_app_events_time ON public.app_events (created_at);
ALTER TABLE public.app_events ENABLE ROW LEVEL SECURITY;
GRANT INSERT ON public.app_events TO anon, authenticated;
GRANT USAGE ON SEQUENCE public.app_events_id_seq TO anon, authenticated;
GRANT SELECT ON public.app_events TO authenticated;

DROP POLICY IF EXISTS "events_insert" ON public.app_events;
CREATE POLICY "events_insert" ON public.app_events FOR INSERT
TO anon, authenticated
WITH CHECK (user_id IS NULL OR user_id::text = auth.uid()::text);

DROP POLICY IF EXISTS "events_select_staff" ON public.app_events;
CREATE POLICY "events_select_staff" ON public.app_events FOR SELECT
TO authenticated USING (public.is_staff());

-- Statistik 30 hari per kost milik pemilik yang login
CREATE OR REPLACE FUNCTION public.owner_kost_stats()
RETURNS TABLE (kost_id uuid, nama_kost text, dilihat bigint, disimpan bigint, chat bigint, survei bigint)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
AS $$
    SELECT k.id, k.nama_kost,
           (SELECT COUNT(*) FROM public.app_events e WHERE e.kost_id = k.id AND e.event = 'view_detail'
                AND e.created_at > now() - interval '30 days'),
           (SELECT COUNT(*) FROM public.favorites f WHERE f.kost_id::text = k.id::text),
           (SELECT COUNT(*) FROM public.chats c WHERE c.kost_id::text = k.id::text),
           (SELECT COUNT(*) FROM public.survey_requests s WHERE s.kost_id = k.id)
    FROM public.kosts k
    WHERE k.owner_id::text = auth.uid()::text
    ORDER BY k.created_at DESC;
$$;
GRANT EXECUTE ON FUNCTION public.owner_kost_stats() TO authenticated;

-- Funnel produk N hari terakhir (hanya staf)
CREATE OR REPLACE FUNCTION public.admin_funnel(days integer DEFAULT 30)
RETURNS TABLE (event text, total bigint)
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public
AS $$
BEGIN
    IF NOT public.is_staff() THEN
        RAISE EXCEPTION 'forbidden' USING ERRCODE = '42501';
    END IF;
    RETURN QUERY
    SELECT e.event, COUNT(*)::bigint FROM public.app_events e
    WHERE e.created_at > now() - make_interval(days => days)
    GROUP BY e.event;
END;
$$;
GRANT EXECUTE ON FUNCTION public.admin_funnel(integer) TO authenticated;


-- -------------------------------------------------------------------------
-- 8. LAPORAN CRASH
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.crash_reports (
    id bigserial PRIMARY KEY,
    user_id uuid,
    app_version text,
    device text,
    android_version text,
    message text,
    stacktrace text CHECK (char_length(stacktrace) <= 20000),
    created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE public.crash_reports ENABLE ROW LEVEL SECURITY;
GRANT INSERT ON public.crash_reports TO anon, authenticated;
GRANT USAGE ON SEQUENCE public.crash_reports_id_seq TO anon, authenticated;
GRANT SELECT ON public.crash_reports TO authenticated;

DROP POLICY IF EXISTS "crash_insert" ON public.crash_reports;
CREATE POLICY "crash_insert" ON public.crash_reports FOR INSERT
TO anon, authenticated WITH CHECK (user_id IS NULL OR user_id::text = auth.uid()::text);

DROP POLICY IF EXISTS "crash_select_staff" ON public.crash_reports;
CREATE POLICY "crash_select_staff" ON public.crash_reports FOR SELECT
TO authenticated USING (public.is_staff());


-- -------------------------------------------------------------------------
-- 9. RATE LIMIT (anti-spam) — pesan, laporan, survei
-- -------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.rate_limit_messages()
RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
    IF (SELECT COUNT(*) FROM public.messages
        WHERE sender_id = NEW.sender_id AND created_at > now() - interval '1 minute') >= 20 THEN
        RAISE EXCEPTION 'rate limit: terlalu banyak pesan' USING ERRCODE = 'P0429';
    END IF;
    RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS trg_rate_limit_messages ON public.messages;
CREATE TRIGGER trg_rate_limit_messages BEFORE INSERT ON public.messages
FOR EACH ROW EXECUTE FUNCTION public.rate_limit_messages();

-- Satu fungsi per tabel: PL/pgSQL tidak bisa membaca kolom yang tidak ada di NEW
CREATE OR REPLACE FUNCTION public.rate_limit_reports()
RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
    IF (SELECT COUNT(*) FROM public.reports
        WHERE reporter_id = NEW.reporter_id AND created_at > now() - interval '1 hour') >= 5 THEN
        RAISE EXCEPTION 'rate limit: terlalu banyak laporan' USING ERRCODE = 'P0429';
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.rate_limit_user_reports()
RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
    IF (SELECT COUNT(*) FROM public.user_reports
        WHERE reporter_id = NEW.reporter_id AND created_at > now() - interval '1 hour') >= 5 THEN
        RAISE EXCEPTION 'rate limit: terlalu banyak laporan' USING ERRCODE = 'P0429';
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.rate_limit_surveys()
RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
    IF (SELECT COUNT(*) FROM public.survey_requests
        WHERE pencari_id = NEW.pencari_id AND created_at > now() - interval '1 day') >= 10 THEN
        RAISE EXCEPTION 'rate limit: terlalu banyak permintaan survei' USING ERRCODE = 'P0429';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_rate_limit_reports ON public.reports;
CREATE TRIGGER trg_rate_limit_reports BEFORE INSERT ON public.reports
FOR EACH ROW EXECUTE FUNCTION public.rate_limit_reports();
DROP TRIGGER IF EXISTS trg_rate_limit_user_reports ON public.user_reports;
CREATE TRIGGER trg_rate_limit_user_reports BEFORE INSERT ON public.user_reports
FOR EACH ROW EXECUTE FUNCTION public.rate_limit_user_reports();
DROP TRIGGER IF EXISTS trg_rate_limit_surveys ON public.survey_requests;
CREATE TRIGGER trg_rate_limit_surveys BEFORE INSERT ON public.survey_requests
FOR EACH ROW EXECUTE FUNCTION public.rate_limit_surveys();


-- -------------------------------------------------------------------------
-- 10. AJUKAN ULANG KOST & HAPUS AKUN OLEH SUPER ADMIN SAJA
-- -------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.resubmit_kost(target_id uuid)
RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
    UPDATE public.kosts
    SET verification_status = 'PENDING'
    WHERE id = target_id
      AND owner_id::text = auth.uid()::text
      AND verification_status::text IN ('REJECTED', 'REVISION_REQUIRED');
    IF NOT FOUND THEN
        RAISE EXCEPTION 'kost tidak dapat diajukan ulang' USING ERRCODE = '42501';
    END IF;
END;
$$;
GRANT EXECUTE ON FUNCTION public.resubmit_kost(uuid) TO authenticated;
