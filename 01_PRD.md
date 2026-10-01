# Product Requirements Document (PRD) — CariKostKita Mobile v1.0

## 1. Informasi Produk
- Nama: CariKostKita
- Versi: 1.0 (Mobile)
- Platform: Android (Mobile Smartphone)
- Bahasa Pemrograman: Java (Android SDK)
- Database: SQLite / Room (Local Cache & Offline Favorites) + MySQL (Backend Data Source)
- Arsitektur: Android MVVM / MVC + Repository Pattern
- Wilayah Operasional: Kota Pekanbaru
- Fokus Awal: Kecamatan Bukit Raya
- Target Pengguna: Mahasiswa, pekerja perantau, dan pencari tempat tinggal sementara di Pekanbaru

## 2. Tujuan
CariKostKita Mobile memudahkan pencari kost menemukan hunian sewa secara praktis langsung dari smartphone mereka, lengkap dengan foto kamar, harga transparan, filter fasilitas, navigasi lokasi via Google Maps, serta direct contact ke pemilik melalui WhatsApp.

## 3. Fitur Pengguna (User)
- **Splash & Onboarding**: Pengenalan cepat keunggulan aplikasi.
- **Autentikasi Mobile**: Registrasi, login, logout, dan remember session berbasis SharedPreferences.
- **Bottom Navigation**: Navigasi intuitif (Home, Eksplor/Cari, Favorit, Akun/Profil).
- **Beranda (Home)**: Banner promo/rekomendasi, shortcut kategori tipe kost (Putra, Putri, Campur), dan daftar kost terpopuler/terbaru di Bukit Raya.
- **Pencarian Cepat**: Input pencarian real-time berdasarkan nama kost, jalan, kelurahan, dan kecamatan.
- **Filter Modal/Bottom Sheet**: Filter harga maksimum, rentang harga, tipe kost, fasilitas wajib (AC, WiFi, Parkir, dll), dan status ketersediaan kamar.
- **Detail Kost**: Galeri foto (swipeable image slider), badge tipe & harga sewa per bulan, daftar fasilitas berikon, alamat lengkap & deskripsi.
- **Direct Location Action**: Tombol interaktif untuk membuka titik koordinat kost langsung pada aplikasi Google Maps bawaan smartphone.
- **Direct Contact Action**: Tombol floating/sticky untuk langsung membuka obrolan WhatsApp dengan pemilik kost (WhatsApp Intent).
- **Favorit (Bookmark)**: Simpan kost favorit ke penyimpanan lokal untuk diakses kapan saja secara cepat.

## 4. Fitur Pengelola (Admin Mobile)
- **Login Admin**: Akses khusus untuk akun dengan role ADMIN.
- **Dashboard Statistik Mobile**: Ringkasan jumlah kost aktif, kost penuh, jumlah fasilitas, dan total pengguna.
- **Manajemen Kost (CRUD)**: Form tambah dan edit data kost responsif untuk layar smartphone.
- **Pengambilan & Upload Foto**: Ambil foto kost langsung melalui kamera smartphone atau galeri Android.
- **Manajemen Fasilitas & Wilayah**: Tambah/edit data fasilitas dan kelurahan di Kecamatan Bukit Raya.
- **Status Switch**: Toggle ketersediaan kamar (TERSEDIA / PENUH / TIDAK_AKTIF) dengan cepat.

## 5. Batasan v1.0 Mobile
- Belum menyediakan in-app payment gateway (pembayaran tetap dilakukan langsung antara penyewa dan pemilik kost).
- Belum menyediakan sistem in-app instant messaging (komunikasi langsung dialihkan ke WhatsApp via Android Intent).
- Tidak mencakup sistem booking/reservasi unit kamar berbayar di dalam aplikasi.
- Belum mencakup sistem ulasan/rating bintang bertingkat.
- Belum mencakup live tracking posisi pengguna secara real-time.
- Fokus wilayah operasional terbatas di Kota Pekanbaru (prioritas Bukit Raya).

## 6. MVP (Minimum Viable Product)
- Autentikasi User & Admin
- Beranda Kost dengan RecyclerView & CardView
- Pencarian & Filter Kost
- Detail Kost & Image Slider
- Integrasi WhatsApp Intent & Google Maps Intent
- Fitur Favorit
- Panel Admin Mobile CRUD Kost, Foto, dan Fasilitas
- Koneksi data terstruktur dengan MySQL & Room/SQLite

## 7. Acceptance Criteria
1. Pengguna dapat mendaftar dan masuk ke akun dengan validasi input yang aman di layar Android.
2. Pengguna dapat menjelajahi daftar kost, mencari dengan keyword, serta memfilter berdasarkan tipe/harga.
3. Halaman detail menampilkan info lengkap dan saat tombol WhatsApp/Maps ditekan, aplikasi Android membuka aplikasi WhatsApp dan Google Maps dengan parameter yang tepat.
4. Pengguna dapat menandai kost favorit dan status tersimpan secara konsisten.
5. Admin dapat mengelola data kost dan upload foto dari galeri/kamera smartphone.
6. Aplikasi berjalan mulus di perangkat Android (Android 7.0 / API 24 ke atas) tanpa crash atau lag.
