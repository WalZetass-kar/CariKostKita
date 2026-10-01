# Implementation Guide — CariKostKita Mobile v1.0 (Android Java)

## 1. Platform & Environment Stack
- **OS Platform**: Android (Minimum SDK: API 24 / Android 7.0, Target SDK: API 34 / Android 14)
- **Language**: Java 17 (dengan `coreLibraryDesugaring` untuk fitur Java modern)
- **Build System**: Gradle dengan Android Gradle Plugin (AGP)
- **IDE**: Android Studio
- **Backend / Database Central**: MySQL 8.x / MariaDB (`carikostkita_db`) via REST API Service / Direct Connector Service
- **Local Persistence**: Android SQLite (`SQLiteOpenHelper`) / Room Database

---

## 2. Gradle Dependencies Minimum (`app/build.gradle`)

```groovy
dependencies {
    // AndroidX & Material UI
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.viewpager2:viewpager2:1.0.0'
    implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'

    // Image Loading & Caching
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'

    // Networking & JSON Parsing (MySQL API Integration)
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'

    // Security & Password Hashing
    implementation 'org.mindrot:jbcrypt:0.4'

    // Local Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

---

## 3. Android Permissions (`AndroidManifest.xml`)

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

    <!-- Query paket eksternal (WhatsApp & Maps) untuk Android 11+ -->
    <queries>
        <package android:name="com.whatsapp" />
        <package android:name="com.google.android.apps.maps" />
        <intent>
            <action android:name="android.intent.action.VIEW" />
            <data android:scheme="geo" />
        </intent>
        <intent>
            <action android:name="android.intent.action.VIEW" />
            <data android:scheme="https" />
        </intent>
    </queries>
</manifest>
```

---

## 4. Keamanan & Konkurensi Android (Threading Rules)

### A. Larangan Operasi Berat di Main (UI) Thread
Akses database lokal (SQLite), request jaringan (Retrofit), dan komputasi berat (BCrypt hash) **dilarang keras** berjalan di Main Thread untuk menghindari *Application Not Responding (ANR)*.

Gunakan thread executor standar:
```java
ExecutorService executor = Executors.newSingleThreadExecutor();
Handler mainHandler = new Handler(Looper.getMainLooper());

executor.execute(() -> {
    // 1. Operasi Database / Jaringan di Background Thread
    List<Kost> result = kostRepository.fetchData();
    
    // 2. Post Hasil Kembali ke UI Thread
    mainHandler.post(() -> {
        kostAdapter.submitList(result);
    });
});
```

### B. Keamanan Kredensial & SQL Injection
- Semua query SQLite lokal wajib menggunakan parameter binding (`selectionArgs = new String[]{...}`) atau Room `@Query` berparameter.
- Password pengguna di-hash menggunakan BCrypt sebelum dikirim atau disimpan:
  ```java
  String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
  boolean isMatched = BCrypt.checkpw(inputPassword, storedHash);
  ```

---

## 5. Implementasi Intent Eksternal

### A. WhatsApp Direct Chat
```java
public static void openWhatsApp(Context context, String phoneNumber, String message) {
    String formattedPhone = phoneNumber.replaceAll("[^0-9]", "");
    if (formattedPhone.startsWith("0")) {
        formattedPhone = "62" + formattedPhone.substring(1);
    }
    String url = "https://wa.me/" + formattedPhone + "?text=" + Uri.encode(message);
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    context.startActivity(intent);
}
```

### B. Google Maps Location Intent
```java
public static void openGoogleMaps(Context context, double latitude, double longitude, String label) {
    Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude + "(" + Uri.encode(label) + ")");
    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
    mapIntent.setPackage("com.google.android.apps.maps");
    if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
        context.startActivity(mapIntent);
    } else {
        // Fallback ke browser jika aplikasi Google Maps tidak terinstal
        Uri browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude);
        context.startActivity(new Intent(Intent.ACTION_VIEW, browserUri));
    }
}
```

---

## 6. Urutan Tahap Pengembangan (Development Order)
1. **Konfigurasi Project**: Setup Gradle, dependencies Material 3, Glide, Retrofit, dan permissions.
2. **Domain Model & Entities**: Buat kelas POJO (`User`, `Kost`, `Wilayah`, `Fasilitas`, dll).
3. **Penyimpanan Lokal**: Implementasi `DatabaseHelper` (SQLite) & `SessionManager` (`SharedPreferences`).
4. **Networking / Repository**: Bangun `ApiClient`, `KostRepository`, dan `UserRepository`.
5. **Autentikasi UI**: `SplashActivity`, `LoginActivity`, `RegisterActivity`.
6. **Main Navigation**: `MainActivity` dengan `BottomNavigationView` dan 4 Fragment.
7. **Home & Katalog**: `HomeFragment` dengan horizontal chip dan vertical `KostAdapter`.
8. **Pencarian & Filter**: `SearchFragment` dan `FilterBottomSheetDialogFragment`.
9. **Detail Kost**: `DetailKostActivity` lengkap dengan slider foto `ViewPager2`, WhatsApp intent, dan Maps intent.
10. **Bookmark Favorit**: Integrasi simpan favorit lokal pada `FavoriteFragment`.
11. **Modul Admin Mobile**: `AdminMainActivity`, `AdminKostFormActivity`, dan Camera/Gallery Photo Picker.
12. **Validasi & Polish**: Cek memory leak, loading state shimmer, error handler offline, dan UI testing.

---

## 7. Definition of Done (DoD) Mobile
Fitur dianggap selesai jika:
- Berjalan stabil pada emulator atau perangkat Android fisik (API 24+).
- Validasi input form berjalan dengan baik tanpa crash.
- Transisi intent ke aplikasi WhatsApp dan Google Maps berjalan mulus.
- Tidak ada operasi jaringan/database yang memicu ANR di UI thread.
- Tampilan responsif di berbagai resolusi layar smartphone.
