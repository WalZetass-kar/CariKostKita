-- =========================================================================
-- CARIKOSTKITA: SUPABASE FIX & UPDATE SCRIPT (ANTI-GAGAL)
-- Jalankan SELURUH script ini di Supabase Dashboard -> SQL Editor -> Run
-- =========================================================================

-- 1. PASTIKAN BUCKET STORAGE 'kost-images' TERSEDIA & DAPAT DIAKSES
INSERT INTO storage.buckets (id, name, public)
VALUES ('kost-images', 'kost-images', true)
ON CONFLICT (id) DO UPDATE SET public = true;

DO $$
BEGIN
    DROP POLICY IF EXISTS "Public Read Kost Images" ON storage.objects;
    CREATE POLICY "Public Read Kost Images"
    ON storage.objects FOR SELECT
    USING (bucket_id = 'kost-images');

    DROP POLICY IF EXISTS "Authenticated Users Upload Kost Images" ON storage.objects;
    CREATE POLICY "Authenticated Users Upload Kost Images"
    ON storage.objects FOR INSERT
    TO authenticated
    WITH CHECK (bucket_id = 'kost-images');

    DROP POLICY IF EXISTS "Users Update Own Kost Images" ON storage.objects;
    CREATE POLICY "Users Update Own Kost Images"
    ON storage.objects FOR UPDATE
    TO authenticated
    USING (bucket_id = 'kost-images');

    DROP POLICY IF EXISTS "Users Delete Own Kost Images" ON storage.objects;
    CREATE POLICY "Users Delete Own Kost Images"
    ON storage.objects FOR DELETE
    TO authenticated
    USING (bucket_id = 'kost-images');
END $$;


-- 2. PERBARUI ROW LEVEL SECURITY (RLS) PADA TABEL KOSTS
-- Menggunakan subquery native tanpa dependensi function eksternal
-- Menggunakan explicit casting ::TEXT agar tidak ada error perbedaan tipe data (UUID vs TEXT)
ALTER TABLE public.kosts ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "kosts_insert_owner" ON public.kosts;
DROP POLICY IF EXISTS "kosts_insert_authenticated" ON public.kosts;
CREATE POLICY "kosts_insert_authenticated" ON public.kosts FOR INSERT
TO authenticated
WITH CHECK (
    -- Pemilik kost dapat menambahkan data jika owner_id sesuai UID akun
    owner_id::TEXT = auth.uid()::TEXT
    -- ATAU pengguna memiliki role owner / developer / admin di tabel users
    OR EXISTS (
        SELECT 1 FROM public.users 
        WHERE id::TEXT = auth.uid()::TEXT 
        AND role::TEXT IN ('owner', 'developer', 'admin', 'pemilik')
    )
);

DROP POLICY IF EXISTS "kosts_update" ON public.kosts;
DROP POLICY IF EXISTS "kosts_update_authenticated" ON public.kosts;
CREATE POLICY "kosts_update_authenticated" ON public.kosts FOR UPDATE
TO authenticated
USING (
    owner_id::TEXT = auth.uid()::TEXT
    OR EXISTS (
        SELECT 1 FROM public.users 
        WHERE id::TEXT = auth.uid()::TEXT 
        AND role::TEXT IN ('developer', 'admin')
    )
);

DROP POLICY IF EXISTS "kosts_delete" ON public.kosts;
DROP POLICY IF EXISTS "kosts_delete_authenticated" ON public.kosts;
CREATE POLICY "kosts_delete_authenticated" ON public.kosts FOR DELETE
TO authenticated
USING (
    owner_id::TEXT = auth.uid()::TEXT
    OR EXISTS (
        SELECT 1 FROM public.users 
        WHERE id::TEXT = auth.uid()::TEXT 
        AND role::TEXT IN ('developer', 'admin')
    )
);

DROP POLICY IF EXISTS "kosts_select_public" ON public.kosts;
CREATE POLICY "kosts_select_public" ON public.kosts FOR SELECT
TO anon, authenticated
USING (true);


-- 3. PERBARUI RLS POLICY UNTUK ACTIVITY LOGS
ALTER TABLE public.activity_logs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "logs_developer" ON public.activity_logs;
DROP POLICY IF EXISTS "logs_authenticated_insert" ON public.activity_logs;
CREATE POLICY "logs_authenticated_insert" ON public.activity_logs FOR INSERT
TO authenticated
WITH CHECK (true);

DROP POLICY IF EXISTS "logs_authenticated_select" ON public.activity_logs;
CREATE POLICY "logs_authenticated_select" ON public.activity_logs FOR SELECT
TO authenticated
USING (
    user_id::TEXT = auth.uid()::TEXT 
    OR EXISTS (
        SELECT 1 FROM public.users 
        WHERE id::TEXT = auth.uid()::TEXT 
        AND role::TEXT IN ('developer', 'admin')
    )
);


-- 4. PERBARUI RLS POLICY UNTUK HAPUS AKUN (TABEL USERS)
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "users_developer_delete" ON public.users;
CREATE POLICY "users_developer_delete" ON public.users FOR DELETE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.users 
        WHERE id::TEXT = auth.uid()::TEXT 
        AND role::TEXT IN ('developer', 'admin')
    )
);


-- 5. PERBARUI RLS POLICY UNTUK TABEL FAVORITES (ANTI ERROR 42501)
ALTER TABLE public.favorites ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "favorites_all" ON public.favorites;
DROP POLICY IF EXISTS "favorites_own" ON public.favorites;
DROP POLICY IF EXISTS "favorites_select" ON public.favorites;
DROP POLICY IF EXISTS "favorites_insert" ON public.favorites;
DROP POLICY IF EXISTS "favorites_delete" ON public.favorites;

CREATE POLICY "favorites_select" ON public.favorites FOR SELECT
TO authenticated
USING (user_id::TEXT = auth.uid()::TEXT);

CREATE POLICY "favorites_insert" ON public.favorites FOR INSERT
TO authenticated
WITH CHECK (user_id::TEXT = auth.uid()::TEXT);

CREATE POLICY "favorites_delete" ON public.favorites FOR DELETE
TO authenticated
USING (user_id::TEXT = auth.uid()::TEXT);


-- 6. PERBARUI RLS POLICY UNTUK TABEL CHATS (PERCAKAPAN)
ALTER TABLE public.chats ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "chats_participants" ON public.chats;
DROP POLICY IF EXISTS "chats_select" ON public.chats;
DROP POLICY IF EXISTS "chats_insert" ON public.chats;
DROP POLICY IF EXISTS "chats_update" ON public.chats;
DROP POLICY IF EXISTS "chats_delete" ON public.chats;

CREATE POLICY "chats_select" ON public.chats FOR SELECT
TO authenticated
USING (
    pencari_id::TEXT = auth.uid()::TEXT 
    OR owner_id::TEXT = auth.uid()::TEXT
    OR EXISTS (
        SELECT 1 FROM public.users 
        WHERE id::TEXT = auth.uid()::TEXT 
        AND role::TEXT IN ('developer', 'admin')
    )
);

CREATE POLICY "chats_insert" ON public.chats FOR INSERT
TO authenticated
WITH CHECK (
    pencari_id::TEXT = auth.uid()::TEXT 
    OR owner_id::TEXT = auth.uid()::TEXT
    OR EXISTS (
        SELECT 1 FROM public.users 
        WHERE id::TEXT = auth.uid()::TEXT 
        AND role::TEXT IN ('developer', 'admin')
    )
);

CREATE POLICY "chats_update" ON public.chats FOR UPDATE
TO authenticated
USING (
    pencari_id::TEXT = auth.uid()::TEXT 
    OR owner_id::TEXT = auth.uid()::TEXT
    OR EXISTS (
        SELECT 1 FROM public.users 
        WHERE id::TEXT = auth.uid()::TEXT 
        AND role::TEXT IN ('developer', 'admin')
    )
);


-- 7. PERBARUI RLS POLICY UNTUK TABEL MESSAGES (PESAN CHAT)
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "messages_participants" ON public.messages;
DROP POLICY IF EXISTS "messages_select" ON public.messages;
DROP POLICY IF EXISTS "messages_insert" ON public.messages;
DROP POLICY IF EXISTS "messages_update" ON public.messages;

CREATE POLICY "messages_select" ON public.messages FOR SELECT
TO authenticated
USING (
    sender_id::TEXT = auth.uid()::TEXT
    OR EXISTS (
        SELECT 1 FROM public.chats c
        WHERE c.id::TEXT = messages.chat_id::TEXT
        AND (c.pencari_id::TEXT = auth.uid()::TEXT OR c.owner_id::TEXT = auth.uid()::TEXT)
    )
    OR EXISTS (
        SELECT 1 FROM public.users 
        WHERE id::TEXT = auth.uid()::TEXT 
        AND role::TEXT IN ('developer', 'admin')
    )
);

CREATE POLICY "messages_insert" ON public.messages FOR INSERT
TO authenticated
WITH CHECK (
    sender_id::TEXT = auth.uid()::TEXT
);

CREATE POLICY "messages_update" ON public.messages FOR UPDATE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.chats c
        WHERE c.id::TEXT = messages.chat_id::TEXT
        AND (c.pencari_id::TEXT = auth.uid()::TEXT OR c.owner_id::TEXT = auth.uid()::TEXT)
    )
);


-- 8. AKTIFKAN REALTIME REPLICATION UNTUK CHAT & MESSAGES
DO $$
BEGIN
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.chats;
    EXCEPTION WHEN duplicate_object THEN
        -- Abaikan jika sudah ada
    END;

    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.messages;
    EXCEPTION WHEN duplicate_object THEN
        -- Abaikan jika sudah ada
    END;
END $$;

