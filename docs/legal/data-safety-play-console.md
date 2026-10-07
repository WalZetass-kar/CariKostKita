# Panduan Isian Data Safety (Google Play Console)

Isi formulir **App content → Data safety** sesuai fitur yang benar-benar ada di versi ini.

| Kategori Play Console | Jenis data | Dikumpulkan | Dibagikan | Opsional? | Tujuan |
| --- | --- | --- | --- | --- | --- |
| Personal info | Name, Email address | Ya | Tidak | Tidak | Account management, App functionality |
| Personal info | Phone number | Ya | Tidak* | Ya | App functionality |
| Personal info | Other info (KTP/selfie pemilik) | Ya | Tidak | Ya (hanya pemilik) | Fraud prevention, security |
| Location | Approximate & precise location | Ya | Tidak | Ya | App functionality (kost terdekat) |
| Messages | Other in-app messages | Ya | Tidak | Ya | App functionality |
| Photos and videos | Photos | Ya | Tidak | Ya | App functionality |
| App activity | App interactions | Ya | Tidak | Tidak | Analytics |
| App info and performance | Crash logs, Diagnostics | Ya | Tidak | Tidak | App functionality (perbaikan bug) |

\* Nomor WhatsApp pemilik ditampilkan ke pengguna lain sebagai bagian fitur, bukan dibagikan ke pihak ketiga.

Jawaban lain:

- **Data dienkripsi saat transit:** Ya (HTTPS ke Supabase).
- **Pengguna dapat meminta penghapusan data:** Ya, lewat menu Hapus Akun di aplikasi. Isi juga URL halaman web penghapusan akun (wajib di Play Console) — buat halaman sederhana yang menjelaskan langkahnya dan email kontak.
- **URL Kebijakan Privasi:** host `kebijakan-privasi.md` (mis. GitHub Pages) lalu isi `privacy_policy_url` di `app/src/main/res/values/strings.xml`.
