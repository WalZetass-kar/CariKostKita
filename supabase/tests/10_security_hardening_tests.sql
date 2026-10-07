\set ON_ERROR_STOP 0
\pset tuples_only on
-- data awal (superuser, tanpa RLS)
INSERT INTO public.users (id,nama,role) VALUES
 ('00000000-0000-0000-0000-0000000000d1','Dev','developer'),
 ('00000000-0000-0000-0000-0000000000a1','Owner','owner'),
 ('00000000-0000-0000-0000-0000000000a2','Owner2','owner'),
 ('00000000-0000-0000-0000-0000000000b1','Seeker','user'),
 ('00000000-0000-0000-0000-0000000000e1','Attacker','user');
INSERT INTO auth.users (id) SELECT id FROM public.users;
INSERT INTO public.kosts (id,owner_id,nama_kost,harga,verification_status) VALUES
 ('10000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-0000000000a1','Kost Approved',900000,'APPROVED'),
 ('10000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-0000000000a1','Kost Pending',800000,'PENDING');
INSERT INTO public.chats (id,kost_id,pencari_id,owner_id) VALUES
 ('20000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-0000000000b1','00000000-0000-0000-0000-0000000000a1');
INSERT INTO storage.objects (bucket_id,name) VALUES ('kost-images','00000000-0000-0000-0000-0000000000b1/foto.jpg');

CREATE FUNCTION pg_temp.as_user(uid text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN PERFORM set_config('request.jwt.claim.sub', uid, false); EXECUTE 'SET ROLE ' || CASE WHEN uid='' THEN 'anon' ELSE 'authenticated' END; END $$;

\echo '--- T1 pencari membuat kost (harus DITOLAK)'
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000e1');
INSERT INTO public.kosts (owner_id,nama_kost) VALUES ('00000000-0000-0000-0000-0000000000e1','Kost Palsu');
RESET ROLE;

\echo '--- T2 pemilik insert kost sebagai APPROVED (harus jadi PENDING)'
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000a1');
INSERT INTO public.kosts (owner_id,nama_kost,verification_status) VALUES ('00000000-0000-0000-0000-0000000000a1','Kost Baru','APPROVED') RETURNING verification_status;
\echo '--- T3 pemilik menyetujui kost pending sendiri (harus tetap PENDING)'
UPDATE public.kosts SET verification_status='APPROVED' WHERE id='10000000-0000-0000-0000-000000000002' RETURNING verification_status;
\echo '--- T4 ubah harga kost approved (harus tetap APPROVED)'
UPDATE public.kosts SET harga=950000, kamar_tersedia=2 WHERE id='10000000-0000-0000-0000-000000000001' RETURNING verification_status;
\echo '--- T5 pemilik insert kost atas nama owner lain (harus DITOLAK)'
INSERT INTO public.kosts (owner_id,nama_kost) VALUES ('00000000-0000-0000-0000-0000000000a2','Kost Titipan');
RESET ROLE;

\echo '--- T6 owner lain mengubah kost (harus 0 baris)'
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000a2');
WITH u AS (UPDATE public.kosts SET harga=1 WHERE id='10000000-0000-0000-0000-000000000001' RETURNING 1) SELECT 'rows=' || count(*) FROM u;
RESET ROLE;

\echo '--- T7 tamu (anon) hanya melihat kost APPROVED'
SELECT pg_temp.as_user('');
SELECT string_agg(nama_kost, ', ' ORDER BY nama_kost) FROM public.kosts;
RESET ROLE;

\echo '--- T8 penyerang menyisipkan pesan ke chat orang lain (harus DITOLAK)'
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000e1');
INSERT INTO public.messages (chat_id,sender_id,message) VALUES ('20000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-0000000000e1','Transfer DP ke rekening saya');
RESET ROLE;

\echo '--- T9 pencari kirim pesan sah -> last_message & unread_owner naik'
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000b1');
INSERT INTO public.messages (chat_id,sender_id,message) VALUES ('20000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-0000000000b1','Masih ada kamar?');
SELECT 'last=' || last_message || ' unread_owner=' || unread_owner FROM public.chats;
RESET ROLE;

\echo '--- T10 pemilik mengubah ISI pesan pencari (isi harus tetap), boleh set is_read'
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000a1');
UPDATE public.messages SET message='Saya setuju bayar 5 juta', is_read=true RETURNING message || ' | read=' || is_read;
RESET ROLE;

\echo '--- T11 user menaikkan role sendiri jadi developer (harus tetap user)'
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000e1');
UPDATE public.users SET role='developer', is_active=true, nama='Attacker2' WHERE id='00000000-0000-0000-0000-0000000000e1' RETURNING role || ' | ' || nama;
\echo '--- T12 user mengajukan diri jadi pemilik (PENDING boleh)'
UPDATE public.users SET verification_status='PENDING' WHERE id='00000000-0000-0000-0000-0000000000e1' RETURNING verification_status;
\echo '--- T13 user mencoba langsung APPROVED (harus tetap PENDING)'
UPDATE public.users SET verification_status='APPROVED' WHERE id='00000000-0000-0000-0000-0000000000e1' RETURNING verification_status;
\echo '--- T14 penyerang melihat data pencari lain (harus kosong)'
SELECT 'seen=' || count(*) FROM public.users WHERE id='00000000-0000-0000-0000-0000000000b1';
\echo '--- T15 penyerang menghapus foto milik pencari (harus 0 baris)'
WITH d AS (DELETE FROM storage.objects WHERE name LIKE '00000000-0000-0000-0000-0000000000b1/%' RETURNING 1) SELECT 'rows=' || count(*) FROM d;
\echo '--- T16 upload ke folder orang lain (harus DITOLAK), ke folder sendiri OK'
INSERT INTO storage.objects (bucket_id,name) VALUES ('kost-images','00000000-0000-0000-0000-0000000000b1/hack.jpg');
INSERT INTO storage.objects (bucket_id,name) VALUES ('kost-images','00000000-0000-0000-0000-0000000000e1/ok.jpg') RETURNING 'uploaded ' || name;
\echo '--- T17 penyerang memanggil admin_delete_user (harus DITOLAK)'
SELECT public.admin_delete_user('00000000-0000-0000-0000-0000000000b1');
RESET ROLE;

\echo '--- T18 pemilik melihat lawan chat (pencari) = 1, owner_favorite_count'
INSERT INTO public.favorites (user_id,kost_id) VALUES ('00000000-0000-0000-0000-0000000000b1','10000000-0000-0000-0000-000000000001');
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000a1');
SELECT 'seeker_visible=' || count(*) FROM public.users WHERE id='00000000-0000-0000-0000-0000000000b1';
SELECT 'favorites=' || public.owner_favorite_count();
RESET ROLE;

\echo '--- T19 developer menyetujui kost & menghapus akun penyerang'
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000d1');
UPDATE public.kosts SET verification_status='APPROVED' WHERE id='10000000-0000-0000-0000-000000000002' RETURNING verification_status;
SELECT public.admin_delete_user('00000000-0000-0000-0000-0000000000e1');
RESET ROLE;
SELECT 'attacker_auth_rows=' || count(*) FROM auth.users WHERE id='00000000-0000-0000-0000-0000000000e1';
\echo '--- T20 pemilik ganti nama kost approved (harus kembali PENDING)'
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000a1');
UPDATE public.kosts SET nama_kost='Kost Approved Baru' WHERE id='10000000-0000-0000-0000-000000000001' RETURNING verification_status;
RESET ROLE;
