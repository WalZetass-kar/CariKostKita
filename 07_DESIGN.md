# Mobile UI/UX Design System — CariKostKita Mobile v1.0

## 1. Arah Desain (Design Direction)
**Contemporary Soft Card UI & Modern Mobile Property Finder (Material 3)**

Karakteristik:
- **Mobile-First & Thumb-Friendly**: Seluruh interaksi utama dapat dijangkau dengan mudah menggunakan satu tangan (zona jangkauan jempol).
- **Clean & Spacious**: Tata letak rapi, kartu bersudut membulat lembut (*soft rounded*), dan kontras visual yang jelas.
- **Micro-Interactions**: Feedback haptic dan visual saat mengetuk kartu, bookmark favorit, dan navigasi tab.
- **Konsistensi Visual**: Menggunakan warna primer merah marun Pekanbaru yang berkarakter kuat namun elegan.

---

## 2. Palet Warna (Color System)
- **Primary**: `#A30D18` (Merah Marun Pekanbaru - identitas brand CariKostKita)
- **Primary Container / Tint**: `#FDE8EA` (Background aksen lembut chip terpilih)
- **Primary Dark**: `#7A0C12` (Status bar & active pressed states)
- **Secondary / Action**: `#16834B` (Hijau WhatsApp untuk tombol kontak langsung)
- **Background**: `#F8F9FA` (Abu-abu sangat muda, nyaman di mata)
- **Surface**: `#FFFFFF` (Putih murni untuk CardView dan Bottom Sheet)
- **Text Primary**: `#1E293B` (Slate Dark, keterbacaan tinggi)
- **Text Secondary**: `#64748B` (Keterangan lokasi, fasilitas, dan timestamp)
- **Border / Outline**: `#E2E8F0` (Garis pemisah halus)
- **Badge Putra**: `#2563EB` (Biru terang)
- **Badge Putri**: `#DB2777` (Pink/Magenta)
- **Badge Campur**: `#7C3AED` (Ungu)
- **Status Tersedia**: `#16834B` (Hijau)
- **Status Penuh**: `#DC2626` (Merah)

---

## 3. Tipografi Android (Material Typography Scale)
Menggunakan font bawaan sistem Android (Roboto) atau Inter/Plus Jakarta Sans:
- **Title Large**: 22sp, Bold (Nama Kost di halaman detail)
- **Title Medium**: 18sp, Semi-Bold (Header section, judul dialog)
- **Body Large**: 16sp, Medium (Nama kost di kartu ringkasan, harga)
- **Body Medium**: 14sp, Regular (Alamat, deskripsi kost)
- **Label Medium**: 12sp, Semi-Bold (Teks badge tipe kost, status kamar)
- **Caption**: 11sp, Regular (Keterangan legalitas & update waktu)

---

## 4. Komponen Antarmuka Mobile

### A. Bottom Navigation Bar
Menetap di bagian bawah layar dengan 4 menu utama:
1. **Beranda** (Ikon Home): Banner info, kategori kilat, kost terpopuler.
2. **Cari** (Ikon Search): Pencarian cepat & eksplorasi kelurahan Bukit Raya.
3. **Favorit** (Ikon Bookmark): Daftar kost yang disimpan user.
4. **Profil** (Ikon Person): Akun, ubah profil, panduan, logout.

### B. Property Card (`item_kost_card.xml`)
- Dibungkus `MaterialCardView` dengan sudut melengkung `16dp` dan elevasi halus `2dp`.
- **Thumbnail Foto**: Aspect ratio 16:9, scale type `centerCrop`, dilengkapi placeholder shimmer.
- **Badge Tipe**: Mengambang di atas thumbnail (kiri atas) dengan rounded chip.
- **Tombol Favorit**: Mengambang di atas thumbnail (kanan atas) berupa ikon hati/bookmark interaktif.
- **Info Properti**: Nama Kost (bold), Alamat singkat (kelurahan/jalan), Chip fasilitas utama (WiFi, AC, dll).
- **Harga**: Teks tebal ukuran besar di sisi kiri bawah (contoh: `Rp 850.000 / bln`).
- **Status**: Label hijau `Tersedia` atau merah `Penuh`.

### C. Filter Modal Bottom Sheet (`bottom_sheet_filter.xml`)
Muncul dari bawah saat tombol filter ditekan:
- Slider / Radio pilihan rentang harga (Di bawah 500rb, 500rb - 1jt, 1jt - 1.5jt, > 1.5jt).
- Chip pilihan tipe kost (Semua, Putra, Putri, Campur).
- Checkbox fasilitas wajib (WiFi, AC, Kamar Mandi Dalam, Parkir Motor/Mobil).
- Tombol aksi ganda: "Reset" (Outlined) dan "Terapkan Filter" (Primary Filled).

### D. Detail Kost Sticky Action Bar
Pada bagian bawah `DetailKostActivity`:
- Terdiri dari 2 tombol aksi utama yang selalu terlihat saat pengguna scroll konten:
  1. **Buka di Maps** (Outlined Button berikon lokasi).
  2. **Hubungi WhatsApp** (Filled Button warna hijau `#16834B` berikon chat).

---

## 5. Daftar Layar Pengguna (User Screens)
1. **Splash Screen**: Logo CariKostKita, animasi loading, auto check login.
2. **Login Activity**: Form email & password, toggle visibility password, tombol masuk & daftar.
3. **Register Activity**: Form registrasi nama, email, nomor HP, password.
4. **Home Screen (Fragment)**:
   - Header greeting pengguna & lokasi saat ini (Pekanbaru).
   - Search bar tiruan yang mengarahkan ke tab Cari.
   - Quick Filter Chips: "Semua", "Kost Putri", "Kost Putra", "Dekat Kampus".
   - RecyclerView Kost Rekomendasi (Horizontal & Vertical).
5. **Search Screen (Fragment)**:
   - Active Search Input dengan tombol clear & tombol filter.
   - Active filter indicators (chip terpilih).
   - RecyclerView hasil pencarian + Pull to Refresh.
6. **Detail Kost Activity**:
   - `ViewPager2` slider foto + dot indicator.
   - Header nama kost, badge tipe, dan tombol share.
   - Rincian harga sewa & deposit.
   - Grid fasilitas berikon.
   - Deskripsi lengkap (dengan opsi "Baca Selengkapnya").
   - Alamat & peta preview kecil.
   - Sticky bottom action (WhatsApp & Maps).
7. **Favorite Screen (Fragment)**:
   - Tab bookmark tersimpan.
   - Empty state visual jika belum ada kost favorit.
8. **Profile Screen (Fragment)**:
   - Avatar pengguna, nama, dan email.
   - Menu Pengaturan, Pusat Bantuan, Kebijakan Privasi, dan Keluar.

---

## 6. Layar Pengelola (Admin Screens)
1. **Admin Main Activity**: Ringkasan metrik statistik (Total Kost, Kamar Tersedia, Kamar Penuh) dan FAB (Floating Action Button) Tambah Kost.
2. **Admin Kost Form Activity**: Input nama, alamat, kelurahan, koordinat maps picker, tipe, harga sewa, nomor WhatsApp, dan pilihan fasilitas.
3. **Admin Photo Manager**: Grid foto kost yang dapat ditambah melalui Camera Intent atau Gallery Picker Android.
4. **Admin Fasilitas Activity**: Form input dan daftar master fasilitas.

---

## 7. Prinsip Aksesibilitas & Responsivitas Mobile
- **Ukuran Sentuh Minimum**: Semua tombol, chip, dan ikon interaktif memiliki target sentuh minimal `48 x 48 dp`.
- **Dukungan Orientasi & Densitas**: Tata letak fleksibel terhadap berbagai ukuran layar smartphone (HD, FHD, QHD) dari 5 inci hingga 6.7 inci.
- **Feedback Langsung**: Menampilkan `Snackbar` informatif saat operasi berhasil atau gagal, bukan `Toast` singkat yang mudah terlewat.
