# Rekomendasi Tech Stack, Arsitektur & Deployment — CariKostKita Landing Page

- **Tujuan**: Menentukan fondasi teknologi pembuatan website yang cepat, ringan, mudah dirawat, hemat biaya (atau gratis 100%), serta memiliki mekanisme hosting file APK yang stabil dan aman.
- **Status**: Siap untuk Implementasi Teknis

---

## 1. Perbandingan Opsi Tech Stack

| Kriteria | Opsi A: Astro + Tailwind (Rekomendasi Utama) | Opsi B: Next.js (App Router) | Opsi C: HTML5 Murni + Tailwind (Vite) |
| :--- | :--- | :--- | :--- |
| **Kecepatan Buka (TTFB)** | ⚡ Ekstrem (<150ms, Zero JS) | Cepat (membutuhkan hidrasi JS) | ⚡ Sangat Cepat (file statis) |
| **Skor Google Lighthouse** | 100/100 secara default | 90 - 96/100 | 98 - 100/100 |
| **Kemudahan Pemeliharaan** | Komponen terstruktur (`.astro`) | Komponen React (`.tsx`) | 1 file HTML panjang atau modul kecil |
| **Biaya Hosting** | Gratis (Vercel / Cloudflare Pages) | Gratis (Vercel) | Gratis (GitHub Pages / Netlify) |
| **SEO & OpenGraph** | Otomatis di-render saat build | Server-Side Rendering (SSR) | Manual di dalam `<head>` |
| **Kesiapan Menambah Form** | Sangat mudah via API routes/endpoint | Sangat mudah via Server Actions | Butuh backend eksternal |

### Rekomendasi:
Gunakan **Opsi A (Astro + Tailwind CSS)** untuk pengalaman pengembangan modern berbasis komponen dengan performa kecepatan tertinggi di jaringan seluler. Jika ingin solusi instan tanpa instalasi Node/npm berlebih, **Opsi C (HTML5 + Tailwind via Vite)** adalah alternatif tercepat.

---

## 2. Struktur Proyek Website yang Direkomendasikan (Astro)

Jika website dibangun di folder terpisah atau submodule (misalnya folder `web/` atau repository baru `CariKostKita-web`):

```
CariKostKita-web/
├── public/
│   ├── favicon.ico
│   ├── icon.png                      # Ikon aplikasi CariKostKita
│   ├── og-preview.jpg                # Gambar OpenGraph untuk preview WhatsApp/Medsos
│   └── screenshots/
│       ├── home-screen.webp          # Mockup layar beranda
│       ├── detail-screen.webp        # Mockup layar detail kost
│       └── maps-screen.webp          # Mockup integrasi peta
├── src/
│   ├── components/
│   │   ├── Navbar.astro              # Header navigasi & tombol unduh
│   │   ├── Hero.astro                # Headline, CTA APK, QR code, Mockup
│   │   ├── TrustBadges.astro         # Jaminan bebas virus & ukuran ringan
│   │   ├── Features.astro            # 3 Role, GPS Maps, Multi-foto, Chat
│   │   ├── InstallGuide.astro        # 4 langkah panduan pasang APK
│   │   ├── OwnerSection.astro        # Alur verifikasi pemilik kost
│   │   ├── FaqAccordion.astro        # Accordion tanya jawab interaktif
│   │   └── Footer.astro              # Hak cipta & link bantuan
│   ├── layouts/
│   │   └── Layout.astro              # Template HTML utama, SEO & Meta Tag
│   └── pages/
│       └── index.astro               # Halaman utama landing page
├── astro.config.mjs
├── tailwind.config.mjs
└── package.json
```

---

## 3. Strategi Distribusi & Hosting File APK

### 3.1 Opsi Utama: GitHub Releases (Sangat Direkomendasikan)
Menyimpan berkas `.apk` pada fitur **Releases** di repositori GitHub `WalZetass-kar/CariKostKita`.

- **Keunggulan**:
  1. Terhubung langsung dengan hasil build APK proyek Android.
  2. Bandwidth unduhan disediakan gratis oleh GitHub CDN global.
  3. Mendukung riwayat versi (*version tagging*, misalnya `v1.0.0-beta`, `v1.0.1-beta`).
  4. Pengguna mendapatkan URL langsung (*direct download link*):
     ```
     https://github.com/WalZetass-kar/CariKostKita/releases/download/v1.0.0-beta/CariKostKita-v1.0.0-beta.apk
     ```

### 3.2 Opsi Mirror / Cadangan: Cloudflare R2 atau Google Drive
- **Mirror 1: Cloudflare R2**:
  - Jika ingin domain unduhan menggunakan nama kustom (misalnya `download.carikostkita.com/app-latest.apk`). Gratis hingga 10 GB penyimpanan dan tanpa biaya transfer data keluar (*zero egress fees*).
- **Mirror 2: Google Drive**:
  - Sebagai tautan cadangan jika ada browser ponsel tertentu yang memblokir unduhan file ekstensi `.apk` dari GitHub.

### 3.3 Verifikasi Integritas File (SHA-256 Checksum)
Di bawah tombol download, sediakan teks kecil atau pop-up:
```
Checksum SHA-256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
```
Hal ini meningkatkan kepercayaan pengguna tingkat lanjut bahwa berkas yang mereka unduh 100% otentik dan tidak dimodifikasi oleh perantara jaringan.

---

## 4. Konfigurasi SEO, Open Graph & Meta Tags

Agar tautan web terlihat profesional saat dibagikan di grup WhatsApp mahasiswa atau media sosial, sisipkan tag meta berikut pada berkas layout HTML:

```html
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  
  <!-- Primary Meta Tags -->
  <title>CariKostKita — Download Aplikasi Pencarian Kost Pekanbaru (APK)</title>
  <meta name="title" content="CariKostKita — Download Aplikasi Pencarian Kost Pekanbaru (APK)" />
  <meta name="description" content="Aplikasi Android ringan untuk mencari kost di Pekanbaru. Foto asli, lokasi Google Maps presisi, dan kontak langsung ke pemilik kost tanpa perantara." />
  <meta name="keywords" content="cari kost pekanbaru, kost uir, kost unri, kost uin suska, sewa kamar pekanbaru, kost bukit raya, apk carikostkita" />
  <meta name="theme-color" content="#A30D18" />

  <!-- Open Graph / Facebook / WhatsApp -->
  <meta property="og:type" content="website" />
  <meta property="og:url" content="https://carikostkita.com/" />
  <meta property="og:title" content="CariKostKita — Download Aplikasi Pencarian Kost Pekanbaru" />
  <meta property="og:description" content="Temukan kamar kost di sekitar kampus Pekanbaru dengan foto asli dan lokasi Google Maps presisi. Download APK v1.0.0-beta gratis (~6.9 MB)." />
  <meta property="og:image" content="https://carikostkita.com/og-preview.jpg" />

  <!-- Twitter -->
  <meta property="twitter:card" content="summary_large_image" />
  <meta property="twitter:url" content="https://carikostkita.com/" />
  <meta property="twitter:title" content="CariKostKita — Download Aplikasi Pencarian Kost Pekanbaru" />
  <meta property="twitter:description" content="Aplikasi Android pencarian kost mahasiswa & perantau di Pekanbaru. Download file APK langsung." />
  <meta property="twitter:image" content="https://carikostkita.com/og-preview.jpg" />

  <!-- Favicon & Touch Icon -->
  <link rel="icon" type="image/png" href="/icon.png" />
  <link rel="apple-touch-icon" href="/icon.png" />
</head>
```

---

## 5. Platform Hosting Website (Pilihan Gratis & Otomatis)

1. **Vercel**:
   - Mendukung integrasi Git otomatis: Setiap kali melakukan `git push` ke repositori, website akan otomatis di-build dan di-deploy dalam waktu kurang dari 30 detik.
   - Menyediakan HTTPS/SSL gratis.
2. **Cloudflare Pages**:
   - Kecepatan jaringan global terbaik di Indonesia dengan data center terdekat (Jakarta).
   - Bebas batas kuota bandwidth.
3. **GitHub Pages**:
   - Cocok jika ingin seluruh kode website berada di dalam repositori yang sama pada branch `gh-pages` atau folder `docs/`.

---

## 6. Pelacakan Metrik & Analitik (Privacy-Friendly Analytics)

Untuk memantau seberapa banyak pengguna yang mengunduh aplikasi tanpa melanggar privasi pengguna:
- **Alat yang Direkomendasikan**: Umami Analytics atau Google Analytics 4 (GA4).
- **Event Kunci yang Dilacak**:
  1. `click_download_apk_hero` (Tombol download di bagian Hero)
  2. `click_download_apk_sticky` (Tombol download di bar melayang mobile)
  3. `view_install_guide` (Pengguna yang membaca seksi panduan)
  4. `click_owner_register` (Pemilik kost yang menekan CTA pendaftaran)
  5. `click_whatsapp_feedback` (Pengguna yang menghubungi tim pengembang untuk bantuan)
