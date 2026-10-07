# Checklist Rilis CariKostKita

## Sebelum beta tertutup
- [ ] Jalankan `supabase/migrations/20261005_security_hardening.sql` lalu `20261006_industry_features.sql` (urutan ini).
- [ ] Periksa Authentication → Policies di Supabase: tidak ada policy lama dengan nama lain yang lebih longgar.
- [ ] Supabase → Authentication → Rate Limits: batasi sign-up/sign-in (mis. 30/jam per IP) dan aktifkan CAPTCHA (hCaptcha/Turnstile) bila tersedia di paket.
- [ ] Redirect URL: `carikostkita://reset-callback`, `carikostkita://login-callback`.
- [ ] Angkat minimal satu Moderator (Dashboard Developer → Data → Pengguna → ketuk badge peran).
- [ ] Buat `keystore.properties` + keystore rilis; aktifkan Play App Signing.
- [ ] Ganti `support_email`, isi `privacy_policy_url` & `terms_url` di `strings.xml`.
- [ ] Host Kebijakan Privasi & Syarat Layanan; isi Data Safety (lihat `docs/legal/`).
- [ ] CI hijau: unit test, build rilis R8, dan tes RLS.
- [ ] Uji build rilis (R8) di minimal 2 perangkat fisik: login, cari, chat, unggah foto, notifikasi.

## Backup & pemulihan
- [ ] Paket Supabase dengan backup harian; aktifkan Point-in-Time Recovery bila memakai paket Pro.
- [ ] Uji pemulihan sekali ke project staging sebelum rilis publik.
- [ ] Ekspor bucket `kost-images` & `verification-docs` berkala (Supabase CLI / skrip S3).

## Pemantauan setelah rilis
- [ ] Kartu "Funnel & kesehatan" di Dashboard Developer: crash 7 hari = 0.
- [ ] Antrean moderasi tidak ada yang "Lewat SLA" (> 24 jam).
- [ ] Laporan pengguna (`user_reports`) & laporan kost ditinjau harian.

## Aksesibilitas (uji manual)
- [ ] TalkBack: semua tombol ikon terbaca (Favorit, Bagikan, Filter, Peta, Opsi chat).
- [ ] Ukuran font sistem 1,3×: tidak ada teks terpotong di kartu kost, dashboard, dan form.
- [ ] Layar 360 dp: tidak ada scroll horizontal tak sengaja; target sentuh ≥ 48 dp.
