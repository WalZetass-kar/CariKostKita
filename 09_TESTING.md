# Testing Plan — CariKostKita Mobile v1.0 (Android Java)

## 1. Testing Scope
- **Mobile Authentication & Session**: Register, login, auto-login via SharedPreferences, logout.
- **Search & Filter**: Keyword query, filter chip, modal bottom sheet.
- **Detail Kost & Media**: ViewPager2 slider foto, deskripsi, chip fasilitas.
- **Android Intent Handlers**: WhatsApp intent dan Google Maps geo intent.
- **Offline & Storage Persistence**: Simpan favorit di SQLite lokal saat offline.
- **Admin Mobile Management**: Form CRUD kost, camera/gallery image picker, toggle status kamar.
- **Lifecycle & Resilience**: Orientasi layar (portrait/landscape), background process killed, jaringan terputus (airplane mode).

---

## 2. Test Cases Matrix

| ID | Fitur | Skenario Uji | Expected Result |
|---|---|---|---|
| TC-01 | Register | Input nama, email baru, password valid | Akun terdaftar, otomatis redirect ke Login/Home |
| TC-02 | Register | Email sudah terdaftar | Menampilkan pesan error validasi di TextInputLayout |
| TC-03 | Login | Email & password benar | Session tersimpan di SharedPreferences, masuk ke Home |
| TC-04 | Login | Password salah | Menampilkan pesan "Email atau password salah" |
| TC-05 | Auto-Login | Buka kembali aplikasi setelah di-kill | Langsung masuk ke Home tanpa login ulang |
| TC-06 | Search | Ketik keyword "Bukit Raya" atau "Garuda" | RecyclerView memuat daftar kost yang sesuai |
| TC-07 | Search | Keyword tidak ditemukan | Tampil ilustrasi Empty State dengan tombol reset |
| TC-08 | Filter | Pilih tipe "PUTRI" & max harga Rp 1.000.000 | Seluruh kartu kost yang tampil memenuhi kriteria |
| TC-09 | Detail Kost | Ketuk salah satu kartu kost | Membuka `DetailKostActivity` dengan data yang presisi |
| TC-10 | Foto Slider | Swipe horizontal foto di halaman detail | Slider berpindah mulus dan dot indicator berganti |
| TC-11 | WhatsApp Intent | Ketuk tombol "Hubungi WhatsApp" | Membuka aplikasi WhatsApp dengan nomor tujuan & format pesan terisi |
| TC-12 | Google Maps Intent | Ketuk tombol "Buka di Maps" | Membuka aplikasi Google Maps pada pin koordinat yang tepat |
| TC-13 | Favorit | Ketuk ikon bookmark di kartu/detail | Ikon aktif, kost tersimpan di database lokal SQLite |
| TC-14 | Favorit Offline | Matikan internet (Airplane Mode), buka tab Favorit | Daftar kost favorit tetap dapat dibaca secara offline |
| TC-15 | Unfavorite | Ketuk kembali ikon bookmark yang aktif | Data dihapus dari daftar favorit lokal |
| TC-16 | Admin CRUD | Tambah kost baru dengan foto dari Galeri | Kost baru tersimpan dan muncul di daftar kost admin |
| TC-17 | Admin Status | Ubah status kost menjadi "PENUH" | Badge status di sisi user berubah menjadi merah (PENUH) |
| TC-18 | Admin Soft Delete | Nonaktifkan kost | Kost berstatus TIDAK_AKTIF tidak muncul di pencarian user |
| TC-19 | Form Validation | Input harga negatif atau format nomor HP tidak valid | Tombol submit diblokir dengan pesan error jelas |
| TC-20 | Screen Rotation | Putar layar smartphone dari portrait ke landscape | State data RecyclerView dan form input tidak hilang |

---

## 3. Jenis Pengujian Android

### A. Unit Testing (JUnit 4)
- Pengujian unit model Java (`Kost`, `User`, `Fasilitas`).
- Pengujian hashing BCrypt password matching.
- Pengujian `FormatUtil` (konversi format mata uang Rupiah dan nomor WhatsApp internasional).

### B. Instrumentation & UI Testing (Espresso)
- Navigasi antar item `BottomNavigationView`.
- Input teks pada `SearchView` dan verifikasi recycler item count.
- Klik tombol login dan verifikasi pergantian Activity.

### C. Device & Hardware Testing
- Pengujian pada berbagai ukuran layar (layar 5.5", 6.1", 6.7").
- Pengujian kompatibilitas OS dari Android 7.0 (Nougat / API 24) hingga Android 14 (API 34).
- Pengujian transisi Intent jika aplikasi WhatsApp atau Google Maps tidak terpasang di perangkat.

---

## 4. Checklist Demo Rilis
- [ ] Database backend MySQL dan SQLite lokal terkonfigurasi.
- [ ] Session login & logout berjalan mulus di SharedPreferences.
- [ ] Pencarian keyword dan Bottom Sheet Filter berfungsi cepat.
- [ ] Foto kost dapat dimuat dengan Glide tanpa memory leak.
- [ ] Intent WhatsApp dan Google Maps dapat dipanggil dengan benar.
- [ ] Fitur Favorit tersimpan di database lokal.
- [ ] Panel Admin mobile dapat mengelola kost dan status kamar.
