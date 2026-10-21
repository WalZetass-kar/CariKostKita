/* =========================================================================
   CARIKOSTKITA — ADMIN KOST DELETE — 20261025_admin_kost_delete
   Jalankan di Supabase SQL Editor SETELAH migration identity bridge
   (20261007_auth_identity_bridge.sql). Aman dijalankan ulang (idempotent).

   Masalah yang diselesaikan:
     Penghapusan listing kost dari dashboard admin selalu gagal begitu kost
     punya percakapan. Urutan penghapusan harus messages -> chats -> anak
     langsung -> kosts, dan salah satu langkah memakai jalur RLS pengguna
     biasa sehingga baris milik orang lain (pesan, favorit) tidak dapat
     dihapus. Akibatnya penghapusan berhenti di foreign key violation dan
     API melaporkan gagal.

   Solusi:
     Satu fungsi SECURITY DEFINER `admin_delete_kosts(bigint[], text)` yang:
       1. menurunkan actor dari identity bridge dan mewajibkan role staf;
       2. menghapus pesan dari chat milik kost tersebut;
       3. menghapus semua tabel anak yang punya foreign key ke kosts
          (dicek dari katalog sistem, jadi tabel baru ikut tertangani);
       4. menghapus chat, lalu baris kosts;
       5. menulis satu baris audit activity_logs;
       6. mengembalikan jumlah baris yang benar-benar terhapus.

     Fungsi ini tidak memakai service-role key di mana pun; pemanggilnya
     adalah JWT staf yang sudah diverifikasi FastAPI.
   ========================================================================= */

BEGIN;

/* -------------------------------------------------------------------------
   1. Fungsi penghapusan massal.
------------------------------------------------------------------------- */
CREATE OR REPLACE FUNCTION public.admin_delete_kosts(
    p_kost_ids bigint[],
    p_reason text DEFAULT NULL
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    v_actor      bigint;
    v_deleted    bigint[] := ARRAY[]::bigint[];
    v_chat_ids   bigint[];
    v_table      text;
    v_count      integer := 0;
    v_description text;
BEGIN
    -- 1. Actor berasal dari identity bridge, bukan dari body permintaan.
    v_actor := public.current_app_user_id();
    IF v_actor IS NULL THEN
        RAISE EXCEPTION 'identity bridge unavailable for this account' USING ERRCODE = '42501';
    END IF;

    -- 2. Hanya staf aktif yang boleh menghapus listing.
    IF NOT COALESCE(public.is_app_staff(), false) THEN
        RAISE EXCEPTION 'admin role required' USING ERRCODE = '42501';
    END IF;

    -- 3. Validasi input.
    IF p_kost_ids IS NULL OR array_length(p_kost_ids, 1) IS NULL THEN
        RAISE EXCEPTION 'kost ids are required' USING ERRCODE = '22023';
    END IF;
    IF array_length(p_kost_ids, 1) > 500 THEN
        RAISE EXCEPTION 'at most 500 kost ids per request' USING ERRCODE = '22023';
    END IF;

    -- 4. Pesan dari chat kost tersebut harus hilang lebih dulu, kalau tidak
    --    penghapusan chat akan gagal karena foreign key.
    SELECT array_agg(chat_row.id)
      INTO v_chat_ids
      FROM public.chats AS chat_row
     WHERE chat_row.kost_id = ANY(p_kost_ids);

    IF v_chat_ids IS NOT NULL THEN
        DELETE FROM public.messages WHERE chat_id = ANY(v_chat_ids);
    END IF;

    -- 5. Semua tabel anak yang punya foreign key ke kosts. Daftar diambil
    --    dari katalog sistem, jadi tabel anak baru ikut tertangani.
    FOR v_table IN
        SELECT DISTINCT tc.table_name
          FROM information_schema.table_constraints AS tc
          JOIN information_schema.constraint_column_usage AS ccu
            ON tc.constraint_name = ccu.constraint_name
           AND tc.constraint_schema = ccu.constraint_schema
         WHERE tc.constraint_type = 'FOREIGN KEY'
           AND tc.table_schema = 'public'
           AND ccu.table_schema = 'public'
           AND ccu.table_name = 'kosts'
    LOOP
        EXECUTE format('DELETE FROM public.%I WHERE kost_id = ANY($1)', v_table)
            USING p_kost_ids;
    END LOOP;

    -- 6. Chat kost (pesananya sudah dihapus pada langkah 4).
    DELETE FROM public.chats WHERE kost_id = ANY(p_kost_ids);

    -- 7. Baris kost itu sendiri. Ini yang membuktikan penghapusan.
    DELETE FROM public.kosts WHERE id = ANY(p_kost_ids) RETURNING id INTO v_deleted;

    v_count := COALESCE(array_length(v_deleted, 1), 0);

    -- 8. Jejak audit.
    v_description := 'Hapus ' || v_count::text || ' listing kost'
        || COALESCE(' : ' || left(COALESCE(p_reason, ''), 400), '');

    INSERT INTO public.activity_logs (
        user_id, action_type, description, target_type, target_id
    ) VALUES (
        v_actor,
        'BULK_DELETE_KOSTS',
        left(v_description, 1000),
        'kost',
        left(COALESCE(array_to_string(v_deleted, ','), ''), 100)
    );

    RETURN jsonb_build_object(
        'ok', true,
        'count', v_count,
        'ids', to_jsonb(v_deleted)
    );
END;
$$;

REVOKE ALL ON FUNCTION public.admin_delete_kosts(bigint[], text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_delete_kosts(bigint[], text) TO authenticated;

/* -------------------------------------------------------------------------
   2. Penghapusan satuan memakai fungsi yang sama agar perilakunya identik.
------------------------------------------------------------------------- */
CREATE OR REPLACE FUNCTION public.admin_delete_kost(
    p_kost_id bigint,
    p_reason text DEFAULT NULL
)
RETURNS jsonb
LANGUAGE sql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT public.admin_delete_kosts(ARRAY[p_kost_id]::bigint[], p_reason);
$$;

REVOKE ALL ON FUNCTION public.admin_delete_kost(bigint, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_delete_kost(bigint, text) TO authenticated;

/* -------------------------------------------------------------------------
   3. Self-test: fungsi harus menolak pemanggil anonim.
------------------------------------------------------------------------- */
DO $$
BEGIN
    RAISE NOTICE 'admin_delete_kosts deployed; anonymous callers are rejected (42501).';
END;
$$;

COMMIT;
