# Use Case — CariKostKita Mobile v1.0

## 1. Aktor Sistem
- **USER (Pencari Kost)**: Mengakses katalog kost dari smartphone, melakukan pencarian, menerapkan filter, menyimpan favorit, melihat rute di Google Maps, dan menghubungi pemilik via WhatsApp.
- **ADMIN (Pengelola Kost)**: Mengelola katalog kost, mengunggah foto via kamera/galeri smartphone, mengelola data wilayah dan fasilitas melalui antarmuka mobile admin.

---

## 2. Use Case USER (Mobile)
- **UC-01 Registrasi Akun Mobile**: Mendaftar akun baru dengan nama, email, dan password.
- **UC-02 Login Akun**: Masuk akun dan menyimpan session di `SharedPreferences`.
- **UC-03 Auto-login & Logout**: Otomatis login jika session aktif; logout membersihkan session lokal.
- **UC-04 Jelajah Beranda (Home)**: Melihat banner, shortcut kategori cepat (Putra/Putri/Campur), dan list rekomendasi kost.
- **UC-05 Pencarian Kost (Search)**: Mengetik keyword nama/jalan/wilayah pada search bar.
- **UC-06 Filter Kost (Bottom Sheet)**: Memilih filter harga slider/range, tipe, dan fasilitas via modal bottom sheet.
- **UC-07 Melihat Detail Kost**: Membuka halaman detail dengan galeri foto swipeable dan badge ketersediaan.
- **UC-08 Swipe Galeri Foto**: Melihat foto kamar dan fasilitas dalam ukuran layar penuh.
- **UC-09 Buka Navigasi Rute (Google Maps Intent)**: Tombol "Lihat Peta" yang langsung meluncurkan aplikasi Google Maps menuju koordinat kost.
- **UC-10 Hubungi Pemilik (WhatsApp Intent)**: Tombol "Chat Pemilik" yang langsung membuka obrolan WhatsApp dengan template pesan otomatis.
- **UC-11 Tambah & Hapus Favorit (Bookmark)**: Menekan ikon love/bookmark untuk toggle simpan ke daftar favorit (tersedia offline).
- **UC-12 Kelola Profil Pengguna**: Melihat ringkasan akun dan ubah preferensi.

---

## 3. Use Case ADMIN (Mobile)
- **UC-13 Login Admin Mobile**: Otentikasi dengan credential ber-role `ADMIN`.
- **UC-14 Dashboard Statistik Mobile**: Melihat total kost, kamar tersedia, kamar penuh, dan jumlah fasilitas.
- **UC-15 Tambah Kost Baru**: Mengisi data form properti kost dengan validasi mobile.
- **UC-16 Ambil & Unggah Foto Kost**: Memilih foto dari Galeri Android atau mengambil langsung melalui Kamera smartphone.
- **UC-17 Edit Data Kost**: Memperbarui harga, deskripsi, alamat, kontak WhatsApp, atau koordinat lokasi.
- **UC-18 Nonaktifkan / Ubah Status Kost**: Mengubah status kost menjadi `TERSEDIA`, `PENUH`, atau `TIDAK_AKTIF` (soft delete).
- **UC-19 Kelola Fasilitas**: Menambah atau mengedit master fasilitas kost.
- **UC-20 Kelola Wilayah**: Mengelola kelurahan di Kecamatan Bukit Raya.
- **UC-21 Monitoring Daftar Kost**: Melihat list seluruh kost dengan indikator status jelas.
- **UC-22 Logout Admin**: Keluar dari dashboard admin.

---

## 4. Alur Interaksi Kritis Mobile

### UC-05 & UC-06 Pencarian & Filter Kost
1. Pengguna membuka tab *Cari* pada Bottom Navigation.
2. Pengguna mengetik keyword atau menekan tombol filter untuk menampilkan *Filter Bottom Sheet*.
3. Pengguna mengatur range harga, memilih chip tipe kost, dan mencentang fasilitas yang diinginkan.
4. Pengguna menekan tombol "Terapkan Filter".
5. Controller/ViewModel melakukan query terfilter di background thread.
6. RecyclerView diperbarui dengan hasil yang cocok. Jika tidak ditemukan, tampilkan ilustrasi *Empty State*.

### UC-09 Buka Google Maps Intent
1. Pengguna membuka `DetailKostActivity`.
2. Pengguna menekan tombol "Buka di Maps".
3. Aplikasi membuat Intent: `android.intent.action.VIEW` dengan Uri `geo:latitude,longitude?q=latitude,longitude(Nama Kost)`.
4. Sistem Android membuka aplikasi Google Maps yang terinstal dan menandai titik lokasi kost secara akurat.

### UC-10 Hubungi Pemilik via WhatsApp Intent
1. Pada `DetailKostActivity`, pengguna menekan tombol "Hubungi Pemilik".
2. Aplikasi memformat nomor telepon (memastikan diawali `62`) dan menyusun template pesan URL-encoded.
3. Aplikasi membuat Intent dengan Uri `https://wa.me/62xxxxxxxxxx?text=Halo%20saya%20tertarik%20dengan%20kost%20...`.
4. Sistem Android membuka aplikasi WhatsApp langsung ke obrolan pemilik kost.

### UC-11 Toggle Favorit
1. Pengguna mengetuk ikon bookmark pada kartu kost atau detail kost.
2. Jika pengguna belum login, tampilkan dialog konfirmasi untuk login terlebih dahulu.
3. Jika sudah login, simpan relasi ke database lokal SQLite dan kirim update ke server.
4. Ikon berubah warna menjadi aktif secara instan (responsif feedback).

---

## 5. Mobile Business Rules
- Kost dengan status `TIDAK_AKTIF` tidak pernah dimunculkan di pencarian atau beranda User.
- Nomor WhatsApp harus divalidasi memiliki format nomor seluler Indonesia yang valid (minimal 10 digit).
- Koordinat lokasi latitude dan longitude harus berupa format desimal valid.
- Semua aksi background (network/database) wajib menampilkan indikator loading (ProgressBar/Shimmer) dan menangani error koneksi internet tanpa membuat aplikasi berhenti (force close).
