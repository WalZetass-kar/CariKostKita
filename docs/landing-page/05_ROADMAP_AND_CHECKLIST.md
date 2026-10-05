# Roadmap & Checklist Peluncuran — CariKostKita Landing Page & Pre-Release

- **Tujuan**: Memberikan panduan kerja terstruktur (*step-by-step actionable checklist*) mulai dari persiapan aset aplikasi Android, perakitan website landing page, uji coba unduhan di berbagai merek smartphone, hingga transisi ke Google Play Store.
- **Status**: Panduan Operasional Siap Dijalankan

---

## 1. Alur Kerja Menyeluruh (Roadmap Diagram)

```
┌─────────────────────────────────────────────────────────────┐
│ FASE 1: Persiapan APK & Aset Visual                         │
│ • Build APK Release/Beta resmi                              │
│ • Ambil screenshot aplikasi asli & buat mockup              │
│ • Hitung hash SHA-256 berkas APK                            │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ FASE 2: Pembuatan Website Landing Page                      │
│ • Implementasi komponen UI sesuai 02_DESIGN_SYSTEM.md       │
│ • Penataan teks sesuai 03_COPYWRITING_GUIDE.md              │
│ • Penyesuaian responsif layar smartphone                    │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ FASE 3: Pengujian Unduh & Pasang di Berbagai HP Android     │
│ • Uji instalasi di Xiaomi (HyperOS), Samsung, Oppo, Vivo   │
│ • Uji link via in-app browser WhatsApp/Instagram            │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ FASE 4: Deployment & Distribusi Publik (Beta Testing)        │
│ • Deploy web ke Vercel / Cloudflare Pages                   │
│ • Unggah file APK ke GitHub Releases                        │
│ • Sebar tautan ke grup mahasiswa & pemilik kost Pekanbaru   │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ FASE 5: Transisi ke Google Play Store                       │
│ • Ganti tombol utama menjadi badge "Get it on Google Play"  │
│ • Pertahankan opsi APK direct download sebagai alternatif   │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Checklist Detail per Fase

### Fase 1: Persiapan APK & Aset Visual
- [ ] **Generate Build APK Bersih**:
  - Pastikan APK terbaru sudah mencakup fitur 3 role lengkap (Pencari Kost, Pemilik Kost, Admin/Developer), multi-foto, koordinat Maps, dan in-app chat.
  - Periksa ukuran file APK agar tetap ramping (~6.9 MB hingga <10 MB).
- [ ] **Tentukan Penamaan Berkas**:
  - Format standar: `CariKostKita-v1.0.0-beta.apk`.
- [ ] **Hitung Checksum SHA-256**:
  - Jalankan di terminal:
    ```bash
    sha256sum app/build/outputs/apk/debug/app-debug.apk
    ```
  - Catat hasilnya untuk dicantumkan di website sebagai verifikasi integritas file.
- [ ] **Ambil Screenshot Beresolusi Tinggi**:
  - Halaman Beranda (Daftar kost terpopuler & banner Pekanbaru).
  - Halaman Detail Kost (Galeri multi-foto, badge status, tombol WhatsApp & Maps).
  - Halaman Kelola Kost Pemilik (Status verifikasi & multi-foto upload).
  - Halaman Alamat & Peta Presisi.
- [ ] **Buat Gambar Pratinjau Sosial (OpenGraph)**:
  - Rasio 1200 x 630 piksel berisi logo CariKostKita, mockup smartphone, dan teks *"Cari Kost di Pekanbaru Jadi Lebih Praktis"*.

---

### Fase 2: Pembuatan Website Landing Page
- [ ] **Inisialisasi Proyek Web**:
  - Mengikuti salah satu opsi dari `04_TECH_STACK_AND_DEPLOYMENT.md` (misalnya Astro + Tailwind atau HTML5 + Tailwind).
- [ ] **Penerapan Sistem Desain**:
  - Masukkan token warna resmi (`#A30D18`, `#F8F8F6`, `#18202F`, `#16834B`).
  - Atur tipografi *Plus Jakarta Sans* atau *Inter*.
- [ ] **Rakit 9 Seksi Komponen**:
  - [x] Navbar dengan logo dan tombol CTA.
  - [x] Hero Section dengan mockup smartphone dan tombol download utama.
  - [x] Seksi Trust & Security Badges (bebas malware & ringan).
  - [x] Seksi Fitur Unggulan (3 Role, Maps presisi, multi-foto, direct chat).
  - [x] Seksi Panduan Langkah Instalasi APK (4 langkah terstruktur).
  - [x] Seksi Khusus Pemilik Kost (alur verifikasi data).
  - [x] Seksi Feedback & Kontak WhatsApp Pengembang.
  - [x] Seksi FAQ Accordion.
  - [x] Footer dengan hak cipta dan tautan pendukung.
- [ ] **Fitur Sticky Download Bar di Mobile**:
  - Muncul di layar bagian bawah saat pengguna scroll ke bawah pada tampilan ponsel.

---

### Fase 3: Pengujian Kompatibilitas Perangkat & Browser

| Jenis Pengujian | Target Perangkat / Browser | Kriteria Keberhasilan | Status |
| :--- | :--- | :--- | :--- |
| **Download APK Langsung** | Google Chrome Mobile | Berkas `.apk` langsung terunduh tanpa redirect rusak | [ ] |
| **In-App Browser** | WhatsApp In-App Browser | Web tampil sempurna, unduhan file berjalan normal | [ ] |
| **In-App Browser** | Instagram In-App Browser | Tautan unduh tidak terblokir (atau diarahkan buka di Chrome) | [ ] |
| **Instalasi Android (Xiaomi)** | HyperOS / MIUI | Muncul dialog izin sumber tidak dikenal, instalasi lancar | [ ] |
| **Instalasi Android (Samsung)**| One UI | Dialog "Instal aplikasi tidak dikenal" jelas, terpasang sukses | [ ] |
| **Instalasi Android (Oppo/Vivo)**| ColorOS / FuntouchOS | Verifikasi paket aman, aplikasi berhasil dibuka | [ ] |
| **Kecepatan Web** | Google PageSpeed Insights | Skor Mobile minimal 90+, Skor Desktop 98-100 | [ ] |

---

### Fase 4: Deployment & Distribusi Publik (Beta Testing)
- [ ] **Unggah APK ke GitHub Releases**:
  - Buka GitHub repository `WalZetass-kar/CariKostKita`.
  - Buat release baru dengan tag `v1.0.0-beta`.
  - Lampirkan file `CariKostKita-v1.0.0-beta.apk` dan sertakan changelog fitur.
- [ ] **Deploy Website**:
  - Sambungkan repositori website ke Vercel atau Cloudflare Pages.
  - Pasang custom domain (misalnya `carikostkita.com` atau subdomain sementara `beta.carikostkita.com` atau domain gratis `.vercel.app`).
- [ ] **Siapkan Kanal Umpan Balik (Feedback Loop)**:
  - Buat Google Form evaluasi singkat (3-5 pertanyaan):
    1. Apakah Anda berhasil menginstal aplikasi tanpa kesulitan?
    2. Merk dan tipe smartphone apa yang Anda gunakan?
    3. Apakah pencarian kost dan peta lokasi berjalan lancar?
    4. Apa fitur yang menurut Anda paling perlu ditambahkan?
  - Siapkan tautan WhatsApp Admin untuk penanganan kendala cepat.
- [ ] **Mulai Distribusi Tautan**:
  - Bagikan tautan website ke perwakilan mahasiswa kampus (UIR, UNRI, UIN Suska) dan paguyuban pemilik kost setempat.

---

### Fase 5: Transisi ke Google Play Store (Public Launch)
- [ ] **Pendaftaran Akun Pengembang Google Play Console**:
  - Selesaikan registrasi identitas dan verifikasi organisasi/perorangan.
- [ ] **Generate Android App Bundle (.aab)**:
  - Build berkas `.aab` bertanda tangan kunci rilis (*release keystore*).
- [ ] **Pembaruan Landing Page Setelah Play Store Aktif**:
  - Ganti tombol unduh utama menjadi gambar badge resmi **"Temukan di Google Play" (Get it on Google Play)**.
  - Sediakan tautan sekunder kecil di bawahnya: *"Atau unduh file APK langsung (v1.0)"* bagi pengguna gawai yang tidak mendukung Google Play Services.
  - Pindahkan seksi panduan instalasi APK ke halaman bantuan sekunder (*Help Center*).
  - Tampilkan ulasan positif dari pengguna beta sebagai testimoni nyata.
