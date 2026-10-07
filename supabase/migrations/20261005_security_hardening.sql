-- =========================================================================
-- CARIKOSTKITA — SECURITY HARDENING (Oktober 2026)
-- Jalankan SELURUH file ini di Supabase Dashboard -> SQL Editor -> Run.
-- Aman dijalankan berulang (idempotent).
--
-- Menutup celah:
--   1. Pencari bisa membuat kost dan pemilik bisa menyetujui kost sendiri.
--   2. Siapa pun bisa menyisipkan / mengubah pesan di chat orang lain.
--   3. Semua user bisa menghapus / mengganti file storage milik orang lain.
--   4. User bisa menaikkan role sendiri (role, is_active, status verifikasi).
--   5. Kost pending / ditolak bisa dibaca publik.
-- Serta menambahkan fungsi yang dipakai aplikasi:
--   owner_favorite_count(), admin_delete_user(), trigger last_message & unread chat.
-- =========================================================================


-- -------------------------------------------------------------------------
-- 0. FUNGSI BANTU PERAN
--    SECURITY DEFINER agar tidak memicu rekursi RLS pada tabel users.
-- -------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.is_developer()
RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.users
        WHERE id::text = auth.uid()::text
          AND role::text IN ('developer', 'admin')
          AND COALESCE(is_active, true)
    );
$$;

CREATE OR REPLACE FUNCTION public.is_owner()
RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.users
        WHERE id::text = auth.uid()::text
          AND role::text IN ('owner', 'pemilik', 'pemilik_kost')
          AND COALESCE(is_active, true)
    );
$$;

GRANT EXECUTE ON FUNCTION public.is_developer() TO anon, authenticated;
GRANT EXECUTE ON FUNCTION public.is_owner() TO anon, authenticated;


-- -------------------------------------------------------------------------
-- 1. USERS
-- -------------------------------------------------------------------------
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "users_select" ON public.users;
DROP POLICY IF EXISTS "users_select_public" ON public.users;
CREATE POLICY "users_select_public" ON public.users FOR SELECT
TO anon, authenticated
USING (
    id::text = auth.uid()::text
    OR public.is_developer()
    -- Profil pemilik tampil di halaman detail kost
    OR role::text IN ('owner', 'pemilik', 'pemilik_kost')
    -- Lawan bicara di chat boleh melihat nama & foto
    OR EXISTS (
        SELECT 1 FROM public.chats c
        WHERE (c.pencari_id::text = auth.uid()::text AND c.owner_id::text = users.id::text)
           OR (c.owner_id::text = auth.uid()::text AND c.pencari_id::text = users.id::text)
    )
);

DROP POLICY IF EXISTS "users_insert_self" ON public.users;
CREATE POLICY "users_insert_self" ON public.users FOR INSERT
TO authenticated
WITH CHECK (id::text = auth.uid()::text);

DROP POLICY IF EXISTS "users_update" ON public.users;
DROP POLICY IF EXISTS "users_update_self_or_dev" ON public.users;
CREATE POLICY "users_update_self_or_dev" ON public.users FOR UPDATE
TO authenticated
USING (id::text = auth.uid()::text OR public.is_developer())
WITH CHECK (id::text = auth.uid()::text OR public.is_developer());

DROP POLICY IF EXISTS "users_developer_delete" ON public.users;
CREATE POLICY "users_developer_delete" ON public.users FOR DELETE
TO authenticated
USING (public.is_developer());

-- Kolom sensitif hanya boleh diubah developer.
-- SECURITY INVOKER (bawaan) agar current_user = peran pemanggil. Hanya request dari
-- aplikasi (anon / authenticated) yang dibatasi; SQL Editor & service_role tetap bebas.
CREATE OR REPLACE FUNCTION public.protect_user_columns()
RETURNS trigger
LANGUAGE plpgsql SET search_path = public
AS $$
BEGIN
    IF current_user NOT IN ('anon', 'authenticated') OR public.is_developer() THEN
        RETURN NEW;
    END IF;

    IF TG_OP = 'INSERT' THEN
        NEW.role := 'user';
        NEW.is_active := true;
        IF NEW.verification_status IS NOT NULL AND NEW.verification_status::text <> 'PENDING' THEN
            NEW.verification_status := NULL;
        END IF;
        NEW.catatan_revisi := NULL;
        RETURN NEW;
    END IF;

    NEW.id := OLD.id;
    NEW.role := OLD.role;
    NEW.is_active := OLD.is_active;
    NEW.catatan_revisi := OLD.catatan_revisi;
    -- User hanya boleh mengajukan diri (-> PENDING) dari status NONE / REJECTED / REVISION_REQUIRED
    IF NEW.verification_status IS DISTINCT FROM OLD.verification_status THEN
        IF NEW.verification_status::text <> 'PENDING'
           OR COALESCE(OLD.verification_status::text, 'NONE') IN ('PENDING', 'APPROVED') THEN
            NEW.verification_status := OLD.verification_status;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_protect_user_columns ON public.users;
CREATE TRIGGER trg_protect_user_columns
BEFORE INSERT OR UPDATE ON public.users
FOR EACH ROW EXECUTE FUNCTION public.protect_user_columns();


-- -------------------------------------------------------------------------
-- 2. KOSTS
-- -------------------------------------------------------------------------
ALTER TABLE public.kosts ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "kosts_select_public" ON public.kosts;
CREATE POLICY "kosts_select_public" ON public.kosts FOR SELECT
TO anon, authenticated
USING (
    verification_status::text = 'APPROVED'
    OR owner_id::text = auth.uid()::text
    OR public.is_developer()
);

DROP POLICY IF EXISTS "kosts_insert_owner" ON public.kosts;
DROP POLICY IF EXISTS "kosts_insert_authenticated" ON public.kosts;
CREATE POLICY "kosts_insert_owner" ON public.kosts FOR INSERT
TO authenticated
WITH CHECK (
    owner_id::text = auth.uid()::text
    AND (public.is_owner() OR public.is_developer())
);

DROP POLICY IF EXISTS "kosts_update" ON public.kosts;
DROP POLICY IF EXISTS "kosts_update_authenticated" ON public.kosts;
DROP POLICY IF EXISTS "kosts_update_owner_or_dev" ON public.kosts;
CREATE POLICY "kosts_update_owner_or_dev" ON public.kosts FOR UPDATE
TO authenticated
USING (owner_id::text = auth.uid()::text OR public.is_developer())
WITH CHECK (owner_id::text = auth.uid()::text OR public.is_developer());

DROP POLICY IF EXISTS "kosts_delete" ON public.kosts;
DROP POLICY IF EXISTS "kosts_delete_authenticated" ON public.kosts;
DROP POLICY IF EXISTS "kosts_delete_owner_or_dev" ON public.kosts;
CREATE POLICY "kosts_delete_owner_or_dev" ON public.kosts FOR DELETE
TO authenticated
USING (owner_id::text = auth.uid()::text OR public.is_developer());

-- Moderasi ditentukan server, bukan aplikasi:
--   * Kost baru dari pemilik selalu PENDING.
--   * Perubahan isi listing (nama, alamat, foto, lokasi, deskripsi, tipe) -> PENDING lagi.
--   * Perubahan harga, jumlah kamar, dan status ketersediaan TIDAK perlu review ulang,
--     supaya pemilik rajin memperbarui data kamar kosong.
-- SECURITY INVOKER (bawaan) agar current_user = peran pemanggil. Hanya request dari
-- aplikasi (anon / authenticated) yang dibatasi; SQL Editor & service_role tetap bebas.
CREATE OR REPLACE FUNCTION public.guard_kost_moderation()
RETURNS trigger
LANGUAGE plpgsql SET search_path = public
AS $$
BEGIN
    IF current_user NOT IN ('anon', 'authenticated') OR public.is_developer() THEN
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
        NEW.catatan_revisi := OLD.catatan_revisi;
    ELSE
        NEW.verification_status := OLD.verification_status;
        NEW.catatan_revisi := OLD.catatan_revisi;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_guard_kost_moderation ON public.kosts;
CREATE TRIGGER trg_guard_kost_moderation
BEFORE INSERT OR UPDATE ON public.kosts
FOR EACH ROW EXECUTE FUNCTION public.guard_kost_moderation();


-- -------------------------------------------------------------------------
-- 3. CHATS & MESSAGES
-- -------------------------------------------------------------------------
ALTER TABLE public.chats ENABLE ROW LEVEL SECURITY;

-- Memakai is_developer() (SECURITY DEFINER) agar tidak terjadi rekursi
-- antara policy users <-> chats.
DROP POLICY IF EXISTS "chats_participants" ON public.chats;
DROP POLICY IF EXISTS "chats_select" ON public.chats;
CREATE POLICY "chats_select" ON public.chats FOR SELECT
TO authenticated
USING (
    pencari_id::text = auth.uid()::text
    OR owner_id::text = auth.uid()::text
    OR public.is_developer()
);

DROP POLICY IF EXISTS "chats_insert" ON public.chats;
CREATE POLICY "chats_insert" ON public.chats FOR INSERT
TO authenticated
WITH CHECK (
    pencari_id::text = auth.uid()::text
    AND pencari_id::text <> owner_id::text
    AND EXISTS (
        SELECT 1 FROM public.kosts k
        WHERE k.id::text = chats.kost_id::text
          AND k.owner_id::text = chats.owner_id::text
    )
);

DROP POLICY IF EXISTS "chats_update" ON public.chats;
CREATE POLICY "chats_update" ON public.chats FOR UPDATE
TO authenticated
USING (pencari_id::text = auth.uid()::text OR owner_id::text = auth.uid()::text)
WITH CHECK (pencari_id::text = auth.uid()::text OR owner_id::text = auth.uid()::text);

ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "messages_participants" ON public.messages;
DROP POLICY IF EXISTS "messages_select" ON public.messages;
CREATE POLICY "messages_select" ON public.messages FOR SELECT
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.chats c
        WHERE c.id::text = messages.chat_id::text
          AND (c.pencari_id::text = auth.uid()::text OR c.owner_id::text = auth.uid()::text)
    )
    OR public.is_developer()
);

DROP POLICY IF EXISTS "messages_insert" ON public.messages;
CREATE POLICY "messages_insert" ON public.messages FOR INSERT
TO authenticated
WITH CHECK (
    sender_id::text = auth.uid()::text
    AND EXISTS (
        SELECT 1 FROM public.chats c
        WHERE c.id::text = messages.chat_id::text
          AND (c.pencari_id::text = auth.uid()::text OR c.owner_id::text = auth.uid()::text)
    )
);

DROP POLICY IF EXISTS "messages_update" ON public.messages;
CREATE POLICY "messages_update" ON public.messages FOR UPDATE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.chats c
        WHERE c.id::text = messages.chat_id::text
          AND (c.pencari_id::text = auth.uid()::text OR c.owner_id::text = auth.uid()::text)
    )
);

-- Isi pesan tidak boleh diubah; yang boleh berubah hanya status dibaca.
CREATE OR REPLACE FUNCTION public.protect_message_content()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.message := OLD.message;
    NEW.sender_id := OLD.sender_id;
    NEW.chat_id := OLD.chat_id;
    NEW.created_at := OLD.created_at;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_protect_message_content ON public.messages;
CREATE TRIGGER trg_protect_message_content
BEFORE UPDATE ON public.messages
FOR EACH ROW EXECUTE FUNCTION public.protect_message_content();

-- last_message & unread diperbarui atomik di server (tidak ada race antar perangkat).
CREATE OR REPLACE FUNCTION public.on_message_inserted()
RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
    UPDATE public.chats c
    SET last_message = NEW.message,
        last_message_at = COALESCE(NEW.created_at, now()),
        unread_owner = CASE WHEN NEW.sender_id::text = c.pencari_id::text
                            THEN COALESCE(c.unread_owner, 0) + 1 ELSE c.unread_owner END,
        unread_pencari = CASE WHEN NEW.sender_id::text = c.owner_id::text
                              THEN COALESCE(c.unread_pencari, 0) + 1 ELSE c.unread_pencari END
    WHERE c.id::text = NEW.chat_id::text;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_on_message_inserted ON public.messages;
CREATE TRIGGER trg_on_message_inserted
AFTER INSERT ON public.messages
FOR EACH ROW EXECUTE FUNCTION public.on_message_inserted();

DO $$
BEGIN
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.chats;
    EXCEPTION WHEN duplicate_object THEN NULL;
    END;
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.messages;
    EXCEPTION WHEN duplicate_object THEN NULL;
    END;
END $$;


-- -------------------------------------------------------------------------
-- 4. ACTIVITY LOGS & REPORTS
-- -------------------------------------------------------------------------
ALTER TABLE public.activity_logs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "logs_authenticated_select" ON public.activity_logs;
CREATE POLICY "logs_authenticated_select" ON public.activity_logs FOR SELECT
TO authenticated
USING (user_id::text = auth.uid()::text OR public.is_developer());

DROP POLICY IF EXISTS "logs_authenticated_insert" ON public.activity_logs;
CREATE POLICY "logs_authenticated_insert" ON public.activity_logs FOR INSERT
TO authenticated
WITH CHECK (user_id::text = auth.uid()::text);

ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "reports_insert_reporter" ON public.reports;
CREATE POLICY "reports_insert_reporter" ON public.reports FOR INSERT
TO authenticated
WITH CHECK (reporter_id::text = auth.uid()::text);

DROP POLICY IF EXISTS "reports_select" ON public.reports;
CREATE POLICY "reports_select" ON public.reports FOR SELECT
TO authenticated
USING (reporter_id::text = auth.uid()::text OR public.is_developer());

DROP POLICY IF EXISTS "reports_update_dev" ON public.reports;
CREATE POLICY "reports_update_dev" ON public.reports FOR UPDATE
TO authenticated
USING (public.is_developer());


-- -------------------------------------------------------------------------
-- 5. STORAGE — file disimpan di folder {user_id}/...
-- -------------------------------------------------------------------------
INSERT INTO storage.buckets (id, name, public)
VALUES ('kost-images', 'kost-images', true)
ON CONFLICT (id) DO UPDATE SET public = true;

DROP POLICY IF EXISTS "Public Read Kost Images" ON storage.objects;
CREATE POLICY "Public Read Kost Images" ON storage.objects FOR SELECT
USING (bucket_id = 'kost-images');

DROP POLICY IF EXISTS "Authenticated Users Upload Kost Images" ON storage.objects;
CREATE POLICY "Authenticated Users Upload Kost Images" ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'kost-images'
    AND (storage.foldername(name))[1] = auth.uid()::text
);

DROP POLICY IF EXISTS "Users Update Own Kost Images" ON storage.objects;
CREATE POLICY "Users Update Own Kost Images" ON storage.objects FOR UPDATE
TO authenticated
USING (
    bucket_id = 'kost-images'
    AND ((storage.foldername(name))[1] = auth.uid()::text OR public.is_developer())
);

DROP POLICY IF EXISTS "Users Delete Own Kost Images" ON storage.objects;
CREATE POLICY "Users Delete Own Kost Images" ON storage.objects FOR DELETE
TO authenticated
USING (
    bucket_id = 'kost-images'
    AND ((storage.foldername(name))[1] = auth.uid()::text OR public.is_developer())
);


-- -------------------------------------------------------------------------
-- 6. FUNGSI RPC UNTUK APLIKASI
-- -------------------------------------------------------------------------

-- Jumlah favorit pada seluruh kost milik pemilik yang sedang login.
CREATE OR REPLACE FUNCTION public.owner_favorite_count()
RETURNS integer
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
AS $$
    SELECT COUNT(*)::integer
    FROM public.favorites f
    JOIN public.kosts k ON k.id::text = f.kost_id::text
    WHERE k.owner_id::text = auth.uid()::text;
$$;
GRANT EXECUTE ON FUNCTION public.owner_favorite_count() TO authenticated;

-- Hapus akun sepenuhnya (profil + akun login). Hanya developer.
CREATE OR REPLACE FUNCTION public.admin_delete_user(target_id uuid)
RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, auth
AS $$
BEGIN
    IF NOT public.is_developer() THEN
        RAISE EXCEPTION 'forbidden' USING ERRCODE = '42501';
    END IF;
    IF target_id::text = auth.uid()::text THEN
        RAISE EXCEPTION 'cannot delete yourself' USING ERRCODE = '42501';
    END IF;
    DELETE FROM public.users WHERE id::text = target_id::text;
    DELETE FROM auth.users WHERE id = target_id;
END;
$$;
REVOKE ALL ON FUNCTION public.admin_delete_user(uuid) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.admin_delete_user(uuid) TO authenticated;
