\set ON_ERROR_STOP 0
\pset tuples_only on
-- Prasyarat: 10_security_hardening_tests.sql sudah dijalankan (data & pg_temp.as_user)
CREATE OR REPLACE FUNCTION pg_temp.as_user(uid text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN PERFORM set_config('request.jwt.claim.sub', uid, false); EXECUTE 'SET ROLE ' || CASE WHEN uid='' THEN 'anon' ELSE 'authenticated' END; END $$;
RESET ROLE;
INSERT INTO public.users (id,nama,role) VALUES ('00000000-0000-0000-0000-0000000000c1','Moderator','moderator') ON CONFLICT DO NOTHING;
INSERT INTO public.users (id,nama,role) VALUES ('00000000-0000-0000-0000-0000000000e2','Attacker2','user') ON CONFLICT DO NOTHING;
INSERT INTO auth.users (id) VALUES ('00000000-0000-0000-0000-0000000000c1'),('00000000-0000-0000-0000-0000000000e2') ON CONFLICT DO NOTHING;
UPDATE public.kosts SET verification_status='APPROVED' WHERE id='10000000-0000-0000-0000-000000000001';

\echo '--- U1 moderator menyetujui pemilik (user->owner) OK'
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000c1');
UPDATE public.users SET role='owner', verification_status='APPROVED' WHERE id='00000000-0000-0000-0000-0000000000e2' RETURNING role;
\echo '--- U2 moderator mengangkat diri/orang jadi developer (harus tetap)'
UPDATE public.users SET role='developer' WHERE id='00000000-0000-0000-0000-0000000000e2' RETURNING role;
\echo '--- U3 moderator menghapus akun lewat admin_delete_user (harus DITOLAK)'
SELECT public.admin_delete_user('00000000-0000-0000-0000-0000000000e2');
RESET ROLE;

\echo '--- U4 pemilik memblokir pencari, pencari kirim pesan (harus DITOLAK)'
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000a1');
INSERT INTO public.user_blocks (blocker_id,blocked_id) VALUES ('00000000-0000-0000-0000-0000000000a1','00000000-0000-0000-0000-0000000000b1') RETURNING 'blocked';
RESET ROLE;
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000b1');
INSERT INTO public.messages (chat_id,sender_id,message) VALUES ('20000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-0000000000b1','halo?');
RESET ROLE;
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000a1');
DELETE FROM public.user_blocks WHERE blocker_id='00000000-0000-0000-0000-0000000000a1';
RESET ROLE;

\echo '--- U5 pencari minta survei (OK), ulas sebelum SELESAI (harus DITOLAK)'
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000b1');
INSERT INTO public.survey_requests (id,kost_id,pencari_id,owner_id,jadwal) VALUES ('30000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-0000000000b1','00000000-0000-0000-0000-0000000000a1', now() + interval '2 day') RETURNING status;
INSERT INTO public.reviews (kost_id,user_id,rating,komentar) VALUES ('10000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-0000000000b1',5,'Bagus');
\echo '--- U6 pencari mengubah sendiri status jadi SELESAI (harus DITOLAK)'
UPDATE public.survey_requests SET status='SELESAI' WHERE id='30000000-0000-0000-0000-000000000001';
RESET ROLE;
\echo '--- U7 pemilik konfirmasi lalu selesai (OK)'
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000a1');
UPDATE public.survey_requests SET status='DIKONFIRMASI' WHERE id='30000000-0000-0000-0000-000000000001' RETURNING status;
UPDATE public.survey_requests SET status='SELESAI' WHERE id='30000000-0000-0000-0000-000000000001' RETURNING status;
RESET ROLE;
\echo '--- U8 pencari menulis ulasan setelah SELESAI (OK) -> rating kost terbarui'
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000b1');
INSERT INTO public.reviews (kost_id,user_id,rating,komentar) VALUES ('10000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-0000000000b1',4,'Bersih') RETURNING rating;
RESET ROLE;
SELECT 'rating_avg=' || rating_avg || ' count=' || rating_count FROM public.kosts WHERE id='10000000-0000-0000-0000-000000000001';

\echo '--- U9 penyerang ulas tanpa survei (harus DITOLAK)'
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000e2');
INSERT INTO public.reviews (kost_id,user_id,rating) VALUES ('10000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-0000000000e2',1);
\echo '--- U10 dokumen verifikasi orang lain tidak terbaca (0), staf bisa'
RESET ROLE;
INSERT INTO storage.objects (bucket_id,name) VALUES ('verification-docs','00000000-0000-0000-0000-0000000000b1/ktp.jpg');
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000e2');
SELECT 'attacker_sees=' || count(*) FROM storage.objects WHERE bucket_id='verification-docs';
RESET ROLE;
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000c1');
SELECT 'moderator_sees=' || count(*) FROM storage.objects WHERE bucket_id='verification-docs';
RESET ROLE;

\echo '--- U11 event analitik & statistik pemilik'
SELECT pg_temp.as_user('');
INSERT INTO public.app_events (event,kost_id) VALUES ('view_detail','10000000-0000-0000-0000-000000000001');
\echo '--- U12 anon tidak bisa membaca event (0)'
SELECT 'anon_events=' || count(*) FROM public.app_events;
RESET ROLE;
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000a1');
SELECT nama_kost || ' dilihat=' || dilihat || ' survei=' || survei FROM public.owner_kost_stats() WHERE dilihat > 0;
\echo '--- U13 pemilik memanggil admin_funnel (harus DITOLAK)'
SELECT * FROM public.admin_funnel(30);
RESET ROLE;

\echo '--- U14 rate limit: pesan ke-21 dalam 1 menit (harus DITOLAK)'
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000b1');
DO $$ BEGIN FOR i IN 1..21 LOOP INSERT INTO public.messages (chat_id,sender_id,message) VALUES ('20000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-0000000000b1','spam ' || i); END LOOP; END $$;
RESET ROLE;

\echo '--- U15 pemilik ajukan ulang kost ditolak (OK -> PENDING)'
UPDATE public.kosts SET verification_status='REJECTED' WHERE id='10000000-0000-0000-0000-000000000002';
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000a1');
SELECT public.resubmit_kost('10000000-0000-0000-0000-000000000002');
RESET ROLE;
SELECT 'status=' || verification_status FROM public.kosts WHERE id='10000000-0000-0000-0000-000000000002';

\echo '--- U16 pengguna menghapus akunnya sendiri'
SELECT pg_temp.as_user('00000000-0000-0000-0000-0000000000e2');
SELECT public.delete_my_account();
RESET ROLE;
SELECT 'profile_rows=' || count(*) FROM public.users WHERE id='00000000-0000-0000-0000-0000000000e2';
SELECT 'auth_rows=' || count(*) FROM auth.users WHERE id='00000000-0000-0000-0000-0000000000e2';
