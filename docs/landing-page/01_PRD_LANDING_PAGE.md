# Product Requirements Document (PRD) — Landing Page CariKostKita (Pre-Release Hub)

- **Nama Produk**: CariKostKita Pre-Release Web Hub
- **Tipe Proyek**: Web Landing Page & APK Distribution Platform
- **Versi Rilis Awal**: 1.0.0-beta
- **Target Wilayah**: Kota Pekanbaru, Riau (Fokus awal: Kecamatan Bukit Raya, sekitar kampus UIR, UNRI, UIN Suska)
- **Status Dokumen**: Final / Siap Implementasi

---

## 1. Latar Belakang & Masalah

Aplikasi Android **CariKostKita** telah siap untuk pengujian publik tahap awal (*open beta / soft launch*), namun belum dipublikasikan ke Google Play Store karena proses pendaftaran akun developer, review berkas, dan verifikasi memakan waktu.

### Masalah yang Dihadapi:
1. **Distribusi Terbatas**: Mengirimkan file APK secara manual lewat WhatsApp atau Google Drive rawan membuat calon pengguna bingung, tidak percaya keamanan file, atau salah mengunduh versi.
2. **Kekhawatiran Keamanan Pengguna**: Pengguna Android awam sering merasa ragu saat diminta menginstal aplikasi di luar Google Play Store karena adanya peringatan keamanan standar Android (*"Unknown sources"*).
3. **Ketiadaan Informasi Terpusat**: Calon penyewa dan pemilik kost membutuhkan halaman resmi yang menjelaskan apa itu CariKostKita, fitur apa saja yang tersedia, bagaimana alur pendaftaran pemilik kost, serta bagaimana cara mendapatkan bantuan.

### Solusi:
Membangun **Website Landing Page Resmi CariKostKita** yang berfungsi sebagai pusat informasi, etalase fitur aplikasi, panduan langkah demi langkah cara instalasi file APK, serta tombol unduhan langsung file APK yang aman dan terverifikasi.

---

## 2. Tujuan & Sasaran Produk (Goals & Objectives)

1. **Memudahkan Pengunduhan APK Langsung**: Pengguna dapat mengunduh file APK resmi versi terbaru (ukuran file ringan ~6.9 MB) hanya dengan 1-2 klik.
2. **Menumbuhkan Kepercayaan Pengguna**: Memberikan jaminan keamanan (bebas virus/malware, signed APK dari tim pengembang, penjelasan izin aplikasi yang transparan).
3. **Edukasi Instalasi APK**: Menyediakan panduan visual interaktif agar mahasiswa dan perantau dapat melewati proses instalasi Android tanpa kendala teknis.
4. **Onboarding Pemilik Kost**: Menjelaskan proses pengajuan akun pemilik kost dan sistem verifikasi ketat sebelum properti dapat tayang ke publik.
5. **Kanal Masukan & Pengujian (Feedback Loop)**: Menampung laporan bug, masukan fitur, dan pertanyaan pengguna sebelum peluncuran resmi di Google Play Store.

---

## 3. Profil Pengguna Sasaran (Target Audience Persona)

### Persona 1: Mahasiswa / Pencari Kost (Early Adopters)
- **Karakteristik**: Mahasiswa baru atau mahasiswa tingkat akhir di Pekanbaru yang sedang mencari hunian sewa dekat kampus.
- **Kebutuhan**:
  - Mengakses website lewat smartphone (80%+ trafik berasal dari tautan pesan chat/sosial media).
  - Mengunduh aplikasi secara cepat tanpa menghabiskan kuota internet.
  - Memastikan foto kamar, harga bulanan, dan titik Google Maps akurat sebelum mendatangi lokasi.
  - Menghubungi pemilik kost secara langsung via chat aplikasi atau WhatsApp.

### Persona 2: Pemilik Kost (Kost Owners)
- **Karakteristik**: Pemilik rumah kost atau pengelola properti sewa di Pekanbaru.
- **Kebutuhan**:
  - Memahami cara mendaftarkan kost ke aplikasi.
  - Mengetahui bahwa akun pemilik kost melalui verifikasi administratif oleh pengembang/admin demi mencegah penipuan.
  - Mengetahui fitur pengelolaan multi-foto kamar, status ketersediaan kamar, dan kontak penyewa.

---

## 4. Struktur Halaman & Fitur Utama (Page Sections)

Landing page dirancang sebagai **Single-Page Application (SPA)** yang responsif, terbagi ke dalam 9 seksi strategis:

```
┌────────────────────────────────────────────────────────┐
│ 1. Header / Navbar (Brand, Menu, CTA Download)        │
├────────────────────────────────────────────────────────┤
│ 2. Hero Section (Headline, CTA APK, QR Code, Mockup)   │
├────────────────────────────────────────────────────────┤
│ 3. Security & Trust Badges (Aman, Ringan, Transparan)  │
├────────────────────────────────────────────────────────┤
│ 4. Fitur Unggulan Aplikasi (3 Role, Maps, Chat, Galeri)│
├────────────────────────────────────────────────────────┤
│ 5. Panduan Instalasi APK Android (Langkah 1 - 4)       │
├────────────────────────────────────────────────────────┤
│ 6. Khusus Pemilik Kost (Alur Verifikasi Properti)      │
├────────────────────────────────────────────────────────┤
│ 7. Kanal Umpan Balik & Pelaporan Kendala (Feedback)    │
├────────────────────────────────────────────────────────┤
│ 8. FAQ (Pertanyaan yang Sering Diajukan)               │
├────────────────────────────────────────────────────────┤
│ 9. Footer (Info Tim, Tautan Repositori, Dukungan WA)   │
└────────────────────────────────────────────────────────┘
```

### Rincian Setiap Seksi:

### 4.1 Header / Navbar
- Logo resmi CariKostKita + teks brand.
- Navigasi cepat: *Fitur*, *Panduan Instalasi*, *Untuk Pemilik*, *FAQ*.
- Tombol CTA sekunder: *"Download APK (v1.0)"*.
- Sticky / fixed saat di-scroll, dengan latar belakang sedikit blur (*backdrop blur*).

### 4.2 Hero Section (Titik Fokus Utama)
- **Headline Utama**: Menyampaikan solusi pencarian kost di Pekanbaru tanpa berputar-putar survei manual.
- **Sub-headline**: Ringkasan aplikasi Android ringan, foto asli, lokasi Maps presisi, dan kontak pemilik terverifikasi.
- **Call-to-Action (CTA) Utama**:
  - Tombol unduh file `.apk` dengan badge versi `v1.0.0-beta` dan ukuran file `6.9 MB`.
  - Tombol sekunder: *"Lihat Panduan Pasang"*.
- **Desktop QR Code**: Jika dibuka di laptop/PC, menampilkan QR Code agar pengguna bisa memindai langsung menggunakan kamera smartphone mereka.
- **Visual Mockup**: Mockup perangkat smartphone Android yang menampilkan antarmuka asli Beranda dan Detail Kost CariKostKita.

### 4.3 Security & Trust Badges
- Menampilkan 3 kartu keyakinan keamanan:
  1. **Bebas Malware & Terverifikasi**: Dibangun langsung dari source code resmi tanpa adware tersembunyi.
  2. **Ukuran Ringan**: Hanya ~6.9 MB, hemat penyimpanan HP dan hemat kuota data.
  3. **Izin Standar Android**: Hanya membutuhkan izin lokasi (Maps) dan penyimpanan foto (untuk upload pemilik).

### 4.4 Showcase Fitur Unggulan (Core Features)
Menyoroti 4 pilar fungsional yang sudah diimplementasikan di aplikasi:
1. **Sistem 3 Role Jelas**:
   - Pencari Kost (eksplorasi, filter, favorit, chat).
   - Pemilik Kost (kelola kost, cek kamar kosong, multi-foto).
   - Developer/Admin (verifikasi akun dan kurasi listing).
2. **Detail Lokasi & Navigasi Maps Presisi**:
   - Alamat lengkap hingga kelurahan, kecamatan, patokan jalan, koordinat latitude/longitude, dan tombol *"Buka di Google Maps"*.
3. **Galeri Multi-Foto Kamar**:
   - Foto cover utama dan sudut kamar yang jelas agar calon penyewa mendapat gambaran nyata.
4. **Komunikasi Langsung & Sistem Lapor**:
   - Fitur chat interaktif dan pintasan WhatsApp langsung, serta formulir lapor jika ada kost yang tidak sesuai.

### 4.5 Panduan Instalasi APK Android (Interactive Step-by-Step)
Menjelaskan proses instalasi APK tanpa menimbulkan kepanikan pengguna:
- **Langkah 1**: Tekan tombol unduh APK dan tunggu hingga selesai.
- **Langkah 2**: Buka file unduhan dari panel notifikasi atau folder *Download*.
- **Langkah 3**: Jika muncul pesan keamanan Android, pilih *Pengaturan / Settings* lalu aktifkan opsi *Izinkan dari sumber ini*.
- **Langkah 4**: Tekan tombol *Install*, tunggu beberapa detik, lalu aplikasi siap digunakan.

### 4.6 Khusus Pemilik Kost (Owner Onboarding Section)
- Menjelaskan alur menjadi pemilik kost terverifikasi:
  1. Unduh dan buat akun di aplikasi CariKostKita.
  2. Masuk ke menu profil dan pilih *"Daftar sebagai Pemilik Kost"*.
  3. Lengkapi formulir pengajuan identitas dan data properti.
  4. Tunggu proses review dan verifikasi oleh tim Developer/Admin.
  5. Setelah berstatus *Pemilik Terverifikasi*, dashboard pemilik langsung aktif.

### 4.7 Feedback & Bug Report Channel
- Menyediakan tombol cepat untuk bergabung ke grup uji coba WhatsApp atau mengisi formulir Google Form umpan balik jika menemukan kendala.

### 4.8 FAQ (Frequently Asked Questions)
Komponen accordion interaktif menjawab:
- Mengapa aplikasi belum tersedia di Google Play Store?
- Apakah menginstal APK dari luar Play Store aman?
- Perangkat Android apa saja yang didukung? (Android 8.0 Oreo hingga Android 14+).
- Apakah pencarian kost di aplikasi ini dipungut biaya?
- Bagaimana jika ada data kost yang sudah tidak aktif atau salah?

### 4.9 Footer
- Hak Cipta © 2026 CariKostKita.
- Dibuat untuk mahasiswa dan masyarakat Pekanbaru.
- Tautan ke GitHub Repository, Kebijakan Privasi Sederhana, dan Kontak Dukungan.

---

## 5. Kebutuhan Non-Fungsional (Non-Functional Requirements)

1. **Performa & Kecepatan**:
   - Google PageSpeed / Lighthouse Score minimal 95+ (Performance, Accessibility, Best Practices, SEO).
   - Waktu buka awal (*Time to Interactive*) di bawah 1.5 detik pada jaringan seluler 4G.
2. **Responsivitas Layar (Mobile-First)**:
   - Tampilan sempurna pada resolusi mobile (360px - 430px), tablet (768px), dan desktop (1024px+).
   - Komponen tombol CTA memiliki area sentuh (*tap target*) minimal 48x48 piksel.
3. **Aksesibilitas (A11y)**:
   - Rasio kontras teks minimal 4.5:1 untuk teks normal dan 3:1 untuk teks besar sesuai panduan WCAG AA.
   - Semua elemen interaktif dapat diakses melalui navigasi keyboard (Tab, Enter, Space).
4. **Optimasi Sosial Media (Open Graph & Meta Tags)**:
   - Dilengkapi kartu pratinjau (*rich preview card*) untuk WhatsApp, Telegram, Facebook, dan Twitter saat tautan web dibagikan.

---

## 6. Metrik Keberhasilan (Success Metrics)

1. **Rasio Konversi Unduhan (Download Conversion Rate)**:
   - Minimal 35% pengunjung unik yang membuka web menekan tombol unduh APK.
2. **Tingkat Keberhasilan Instalasi**:
   - Meminimalkan pertanyaan kendala instalasi berkat panduan langkah yang jelas di seksi 5.
3. **Pendaftaran Pemilik Kost Baru**:
   - Terkumpulnya pengajuan kost baru dari pemilik di sekitar area Bukit Raya selama masa pre-release.
4. **Masukan Pengguna (Feedback Submission)**:
   - Mendapatkan masukan pengujian fungsional sebelum rilis Play Store.

---

## 7. Rencana Transisi ke Google Play Store

Ketika aplikasi telah siap dan disetujui di Google Play Store:
1. Tombol utama *"Download APK"* akan digantikan dengan badge resmi *"Get it on Google Play"*.
2. Tombol APK tetap dipertahankan sebagai opsi sekunder (*"Unduh file APK langsung"* untuk perangkat yang tidak memiliki Google Play Services).
3. Seksi panduan instalasi APK dipindahkan ke halaman bantuan sekunder.
