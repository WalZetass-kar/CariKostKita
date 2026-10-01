# System Design — CariKostKita Mobile v1.0

## 1. Tujuan
Dokumen ini merinci arsitektur teknis aplikasi mobile Android **CariKostKita** berbasis bahasa pemrograman **Java**.

## 2. Arsitektur Aplikasi Android
Menggunakan pola **Android MVC / MVVM + Repository Pattern**:

```text
[UI Layer: Activity / Fragment / XML Layout]
                │
                ▼
[Controller / ViewModel / Presenter Layer]
                │
                ▼
[Repository Layer (Single Source of Truth)]
         │                              │
         ▼ (Lokal)                      ▼ (Remote/Network)
[Room / SQLite DAO]             [Retrofit / Network Service]
         │                              │
         ▼                              ▼
  (SQLite DB Lokal)              (MySQL Server API)
```

### A. UI Layer (View)
- Terdiri dari **Activity**, **Fragment**, **RecyclerView Adapter**, dan **XML Layouts**.
- Menggunakan **ViewBinding** untuk manipulasi tampilan yang aman dari NullPointerException.
- Mengimplementasikan komponen Material 3: `MaterialCardView`, `BottomNavigationView`, `BottomSheetDialog`, `ExtendedFloatingActionButton`, dan `TextInputLayout`.

### B. Controller / ViewModel Layer
- Mengelola state layar (Loading, Success, Empty, Error).
- Mengontrol alur navigasi antar activity/fragment menggunakan `Intent` dan `FragmentManager`.
- Menghubungkan interaksi pengguna dengan Repository Layer di background thread (menggunakan `ExecutorService` / `AsyncTask` modern / `HandlerThread`).

### C. Repository Layer
- Mengabstraksi sumber data (Lokal SQLite/Room vs Remote MySQL Server).
- Mengatur strategi caching (misal: data Kost Favorit disimpan lokal di SQLite agar dapat diakses saat offline).

### D. Data Source Layer
- **Local DAO (SQLite / Room)**: Menyimpan session user, cache daftar kost, dan list favorit lokal.
- **Remote Data Source (MySQL REST API / Network Connector)**: Menangani sinkronisasi data master kost, autentikasi, serta mutasi data admin ke database terpusat `carikostkita_db`.

## 3. Komponen & Modul Sistem Mobile
1. **Auth & Session Module**: `LoginActivity`, `RegisterActivity`, dan `SessionManager` (menggunakan `SharedPreferences`).
2. **Main Navigation**: `MainActivity` dengan `BottomNavigationView` yang memuat:
   - `HomeFragment`: Banner, quick category chips, daftar kost rekomendasi.
   - `SearchFragment`: Search bar, filter bottom sheet, `RecyclerView` hasil pencarian.
   - `FavoriteFragment`: Daftar kost favorit yang tersimpan.
   - `ProfileFragment`: Info profil pengguna, ganti password, informasi aplikasi, tombol logout.
3. **Kost Detail Module**: `DetailKostActivity` dengan Image Slider (`ViewPager2`), chip fasilitas, alamat, serta action buttons.
4. **External Intent Handlers**:
   - `MapsIntentHelper`: Membuka aplikasi Google Maps via URI intent `geo:lat,lng?q=lat,lng(NamaKost)`.
   - `WhatsAppIntentHelper`: Membuka chat WhatsApp langsung via URI intent `https://wa.me/{no_whatsapp}?text={pesan}`.
5. **Admin Mobile Module**:
   - `AdminMainActivity`: Dashboard ringkasan & manajemen cepat.
   - `AdminKostFormActivity`: Form input kost baru atau edit kost eksisting.
   - `PhotoPickerHelper`: Mengambil gambar dari galeri Android (`ActivityResultLauncher`) atau kamera.
   - `AdminFasilitasActivity`: Manajemen daftar fasilitas.

## 4. Alur Autentikasi & Session Mobile
1. User membuka aplikasi → `SplashActivity` memeriksa `SessionManager`.
2. Jika session valid:
   - Role `USER` → Arahkan langsung ke `MainActivity`.
   - Role `ADMIN` → Arahkan langsung ke `AdminMainActivity`.
3. Jika belum login → Arahkan ke `LoginActivity`.
4. Setelah login berhasil → Simpan `id_user`, `nama`, `email`, dan `role` ke dalam `SharedPreferences`.

## 5. Alur Pencarian & Filter Mobile
1. User mengetik di `SearchView` atau memilih chip kategori di `SearchFragment`.
2. User dapat menekan ikon filter untuk memunculkan `FilterBottomSheetDialog` (filter harga, tipe, ketersediaan).
3. Controller/ViewModel memproses parameter pencarian di background thread.
4. UI menampilkan state shimmer/loading, lalu mengupdate `KostAdapter` pada `RecyclerView`. Jika data kosong, tampilkan `EmptyStateView`.

## 6. Alur Favorit (Offline-First)
1. User menekan ikon bookmark pada kartu kost atau halaman detail.
2. `FavoriteRepository` memeriksa session login user.
3. Toggle status favorit disimpan ke database lokal SQLite dan dikirim ke server.
4. Ikon berubah secara reaktif dan daftar di `FavoriteFragment` diperbarui.

## 7. Aturan Rekayasa Android
- **No Network on Main Thread**: Operasi jaringan atau database wajib dijalankan di worker thread (`Executors.newSingleThreadExecutor()`).
- **Memory Leak Prevention**: Gunakan listener/callback dengan lifecycle-aware components. Hindari menyimpan reference static Context/Activity.
- **PreparedStatement & Sanitasi**: Semua query lokal atau remote tetap menggunakan prepared statement / parameter binding.
- **Image Optimization**: Gunakan Glide untuk lazy loading, caching thumbnail disk & memory, dan mencegah OutOfMemoryError (OOM) saat memuat gambar kost.
