# Design System & UI/UX Guidelines — CariKostKita Landing Page

- **Tujuan**: Memastikan tampilan website konsisten dengan identitas visual aplikasi Android CariKostKita, ramah pengguna, berfokus *mobile-first*, serta memenuhi standar aksesibilitas tinggi (*anti-slop*).
- **Status**: Siap Digunakan untuk Implementasi Antarmuka (CSS / Tailwind / Component Framework)

---

## 1. Prinsip Desain (Design Philosophy)

1. **Konsistensi Merek (Brand Continuity)**:
   - Pengunjung yang melihat website dan kemudian menginstal aplikasi Android harus merasakan pengalaman visual yang satu kesatuan tanpa lompatan desain yang canggung.
2. **Kejujuran Visual & Anti-Slop**:
   - Tidak menggunakan ilustrasi vektor 3D generik bergaya korporat yang hampa makna.
   - Menggunakan tangkapan layar asli (*real screenshot*) antarmuka aplikasi CariKostKita di dalam frame smartphone yang realistis.
3. **Mobile-First Responsiveness**:
   - Karena target audiens (mahasiswa) mengakses web mayoritas dari smartphone, desain dititikberatkan pada layar kecil (360px - 430px) terlebih dahulu, baru kemudian dioptimasi untuk tablet dan desktop.
4. **Kontras & Kejelasan Teks (High Readability)**:
   - Hindari warna teks abu-abu pudar. Semua teks body dan headline memiliki rasio kontras minimal 4.5:1 terhadap latar belakang.

---

## 2. Token Warna Resmi (Color Tokens)

Warna diekstraksi langsung dari sistem desain aplikasi Android (`colors.xml`):

| Token Name | Hex Code | Deskripsi & Peruntukan |
| :--- | :--- | :--- |
| `primary` | `#A30D18` | **Crimson Brand**: Warna tombol utama, brand heading, aksen penting |
| `primary-dark` | `#7A0C12` | Warna saat tombol utama di-hover atau di-klik (*active state*) |
| `primary-soft` | `#FBEAEC` | Latar belakang badge, chip filter, dan kartu sorotan fitur |
| `background` | `#F8F8F6` | **Warm Canvas**: Warna latar belakang website (bersih, hangat, tidak menyilaukan) |
| `surface` | `#FFFFFF` | Latar belakang kartu konten, modal, dan navbar |
| `surface-variant`| `#F4F5F7` | Latar belakang seksi selang-seling dan panel sekunder |
| `border` | `#D0D5DD` | Garis pembatas kartu dan tombol outline |
| `border-subtle` | `#E6E8EC` | Garis pembatas tipis antar-elemen list atau accordion |
| `text-primary` | `#18202F` | **Deep Slate**: Warna judul (H1-H3) dan teks utama (kontras tinggi) |
| `text-secondary` | `#344054` | Warna teks paragraf deskripsi dan label form |
| `text-muted` | `#667085` | Warna catatan kaki, timestamp, dan ukuran file |
| `whatsapp-green`| `#16834B` | **Emerald Green**: Tombol aksi WhatsApp dan status ketersediaan kost |
| `whatsapp-soft` | `#EAF7EF` | Latar belakang badge status kost *"TERSEDIA"* |
| `maps-blue` | `#1A73E8` | Tombol dan ikon navigasi Google Maps |

### Konfigurasi Tailwind CSS (`tailwind.config.js`):
```javascript
module.exports = {
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: '#A30D18',
          dark: '#7A0C12',
          soft: '#FBEAEC',
        },
        canvas: '#F8F8F6',
        surface: {
          DEFAULT: '#FFFFFF',
          variant: '#F4F5F7',
        },
        ink: {
          primary: '#18202F',
          secondary: '#344054',
          muted: '#667085',
        },
        action: {
          whatsapp: '#16834B',
          'whatsapp-soft': '#EAF7EF',
          maps: '#1A73E8',
        }
      }
    }
  }
}
```

---

## 3. Tipografi (Typography Hierarchy)

- **Keluarga Font**: *Plus Jakarta Sans* (Pilihan utama) atau *Inter* (Fallback).
- **Alasan**: Font modern sans-serif dengan geometri tegas yang sangat nyaman dibaca pada layar perangkat bergerak.

| Level | Desktop Size / Line-height | Mobile Size / Line-height | Weight | Penggunaan |
| :--- | :--- | :--- | :--- | :--- |
| **Hero Display** | 44px / 1.15 (`leading-tight`) | 32px / 1.2 | ExtraBold (800) | Judul utama seksi Hero |
| **Heading 1 (H1)** | 32px / 1.25 | 26px / 1.3 | Bold (700) | Judul seksi utama |
| **Heading 2 (H2)** | 24px / 1.35 | 20px / 1.35 | SemiBold (600) | Judul kartu & sub-bagian |
| **Heading 3 (H3)** | 18px / 1.4 | 16px / 1.4 | SemiBold (600) | Judul item FAQ & fitur |
| **Body Large** | 18px / 1.6 | 16px / 1.5 | Regular (400) | Paragraf pembuka di Hero |
| **Body Normal** | 16px / 1.6 | 15px / 1.5 | Regular (400) | Teks paragraf utama & FAQ |
| **Caption / Meta** | 13px / 1.4 | 12px / 1.4 | Medium (500) | Ukuran file, versi, catatan kaki |

---

## 4. Sistem Grid, Spacing & Breakpoints

- **Grid Basis**: Kelipatan 4px / 8px (`0.5rem` / `1rem`).
- **Container Maksimum**: `max-w-6xl` (1152px) dengan padding horizontal `px-4 sm:px-6 lg:px-8`.
- **Breakpoints**:
  - `sm`: `640px` (Ponsel landscape / phablet)
  - `md`: `768px` (Tablet)
  - `lg`: `1024px` (Laptop / desktop standar)
  - `xl`: `1280px` (Layar desktop lebar)

---

## 5. Spesifikasi Komponen UI (Component Specifications)

### 5.1 Tombol Utama Download APK (Hero CTA)
- **Tampilan**:
  - Latar belakang: `#A30D18` (`bg-brand`)
  - Warna teks: Putih murni `#FFFFFF`
  - Border radius: `rounded-xl` (12px)
  - Area sentuh: Minimal tinggi 56px pada mobile
  - Elemen di dalam tombol:
    - Ikon Android SVG (Kiri)
    - Teks Dua Baris:
      - Baris 1 (Bold 16px): **Download APK CariKostKita**
      - Baris 2 (Opacity 90% 12px): Versi 1.0.0-beta • ~6.9 MB • Gratis
- **Interaksi**:
  - Hover: Transisi latar ke `#7A0C12` dalam 150ms.
  - Active: Sedikit menyusut (`scale-98`).

### 5.2 Mockup Perangkat Smartphone (Device Mockup)
- Menggunakan bingkai perangkat CSS sederhana tanpa bezel berlebih:
  - Sudut bingkai: `rounded-[2.5rem]`
  - Bayangan: `shadow-2xl shadow-black/10`
  - Border halus: `border-4 border-slate-800`
  - Layar diisi tangkapan layar asli aplikasi CariKostKita (tampilan Beranda dengan kartu kost Pekanbaru).
  - Floating Badge: Menempel di samping mockup dengan tulisan *"Foto Asli & Lokasi Presisi"*.

### 5.3 Kartu Fitur Unggulan (Feature Cards)
- Background: `#FFFFFF`
- Border: `1px solid #E6E8EC`
- Padding: `p-6` (24px)
- Radius: `rounded-2xl`
- Ikon Container: Kotak 48x48px dengan warna `#FBEAEC` dan ikon bergaris merah `#A30D18`.
- Tata letak: Grid 1 kolom di mobile, 2 kolom di tablet, 3-4 kolom di desktop.

### 5.4 Kartu Langkah Instalasi (Step Cards)
- Menampilkan nomor langkah bulat (`1`, `2`, `3`, `4`) dengan latar merah `#A30D18` dan teks putih.
- Menampilkan judul langkah ringkas dan penjelasan detail.
- Disertai contoh visual atau peringatan ramah Android: *"Peringatan 'Unknown Source' adalah prosedur standar Android untuk aplikasi baru sebelum rilis di Play Store."*

### 5.5 Accordion FAQ (Tanya Jawab)
- Desain minimalis tanpa garis tebal.
- Judul pertanyaan menggunakan teks bold dengan ikon chevron yang berputar 180° saat terbuka.
- Jawaban muncul dengan animasi halus tanpa pergeseran layout yang mengejutkan.
- Aksesibel: Mendukung fokus keyboard dan tombol `Enter` untuk membuka/menutup.

### 5.6 Sticky Download Bar (Khusus Tampilan Mobile)
- Saat pengguna scroll melewati seksi Hero di layar smartphone, muncul bar ramping yang menempel di bagian bawah layar (*bottom fixed*):
  - Kiri: Logo mini + Nama "CariKostKita" + Teks "6.9 MB".
  - Kanan: Tombol cepat "Unduh APK".
  - Dilengkapi bayangan atas (*top shadow*) agar terlihat jelas di atas konten.

---

## 6. Checklist Kualitas Visual & Aksesibilitas (Anti-Slop QA)

- [ ] **Tidak ada teks pudar**: Semua teks di atas latar `#F8F8F6` atau putih menggunakan minimal `#344054` atau `#18202F`.
- [ ] **Target sentuh memadai**: Tidak ada tombol interaktif yang lebih kecil dari 44x44px.
- [ ] **Gambar tajam & teroptimasi**: Screenshot aplikasi menggunakan format WebP dengan resolusi 2x untuk layar Retina.
- [ ] **Bebas animasi mengganggu**: Animasi hanya untuk transisi halus (hover, accordion toggle). Tidak ada elemen berputar atau melayang berlebihan.
- [ ] **Dark Mode Handling**: Pada versi pre-release ini, fokuskan pada tema terang (*clean light warm theme*) untuk menjaga keselarasan dengan tema aplikasi CariKostKita saat ini.
