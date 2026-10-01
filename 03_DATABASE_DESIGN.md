# Database Design — CariKostKita Mobile v1.0

Aplikasi mobile CariKostKita menggunakan dual-tier data architecture:
1. **Server Database (MySQL `carikostkita_db`)**: Single source of truth untuk seluruh data kost, akun user/admin, wilayah, dan fasilitas.
2. **Local Client Database (Android SQLite / Room Database `carikostkita_local.db`)**: Caching data katalog untuk performa cepat dan bookmark favorit offline.

---

## 1. Skema Server Database (MySQL `carikostkita_db`)

### Entity Relasional
- `users`: Data akun mobile (user pencari kost & admin pengelola).
- `wilayah`: Data geografis wilayah (Kota Pekanbaru, Kecamatan Bukit Raya, beserta kelurahannya).
- `kost`: Data utama properti kost (nama, alamat, harga, tipe, ketersediaan, kontak WhatsApp, koordinat GPS).
- `fasilitas`: Master data fasilitas (WiFi, AC, Parkir, dll).
- `kost_fasilitas`: Relasi Many-to-Many antara kost dan fasilitas.
- `foto_kost`: Daftar URL/path foto kamar dan thumbnail.
- `favorit`: Relasi bookmark pengguna terhadap kost tertentu.

### DDL MySQL Server
```sql
CREATE DATABASE IF NOT EXISTS carikostkita_db;
USE carikostkita_db;

CREATE TABLE users (
    id_user INT AUTO_INCREMENT PRIMARY KEY,
    nama VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE wilayah (
    id_wilayah INT AUTO_INCREMENT PRIMARY KEY,
    kecamatan VARCHAR(100) NOT NULL,
    kelurahan VARCHAR(100) NOT NULL,
    kota VARCHAR(100) NOT NULL DEFAULT 'Pekanbaru',
    UNIQUE KEY uk_wilayah (kecamatan, kelurahan, kota)
);

CREATE TABLE kost (
    id_kost INT AUTO_INCREMENT PRIMARY KEY,
    id_wilayah INT NOT NULL,
    nama_kost VARCHAR(150) NOT NULL,
    alamat TEXT NOT NULL,
    harga DECIMAL(12,2) NOT NULL,
    tipe_kost ENUM('PUTRA','PUTRI','CAMPUR') NOT NULL,
    deskripsi TEXT,
    no_whatsapp VARCHAR(20),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    status ENUM('TERSEDIA','PENUH','TIDAK_AKTIF') NOT NULL DEFAULT 'TERSEDIA',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_kost_wilayah
        FOREIGN KEY (id_wilayah) REFERENCES wilayah(id_wilayah)
);

CREATE TABLE fasilitas (
    id_fasilitas INT AUTO_INCREMENT PRIMARY KEY,
    nama_fasilitas VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE kost_fasilitas (
    id_kost INT NOT NULL,
    id_fasilitas INT NOT NULL,
    PRIMARY KEY (id_kost, id_fasilitas),
    CONSTRAINT fk_kf_kost
        FOREIGN KEY (id_kost) REFERENCES kost(id_kost) ON DELETE CASCADE,
    CONSTRAINT fk_kf_fasilitas
        FOREIGN KEY (id_fasilitas) REFERENCES fasilitas(id_fasilitas) ON DELETE CASCADE
);

CREATE TABLE foto_kost (
    id_foto INT AUTO_INCREMENT PRIMARY KEY,
    id_kost INT NOT NULL,
    nama_file VARCHAR(255) NOT NULL,
    path_file VARCHAR(500) NOT NULL,
    is_thumbnail BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_foto_kost
        FOREIGN KEY (id_kost) REFERENCES kost(id_kost) ON DELETE CASCADE
);

CREATE TABLE favorit (
    id_favorit INT AUTO_INCREMENT PRIMARY KEY,
    id_user INT NOT NULL,
    id_kost INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_favorit (id_user, id_kost),
    CONSTRAINT fk_favorit_user
        FOREIGN KEY (id_user) REFERENCES users(id_user) ON DELETE CASCADE,
    CONSTRAINT fk_favorit_kost
        FOREIGN KEY (id_kost) REFERENCES kost(id_kost) ON DELETE CASCADE
);

-- Indeks Performa untuk Query Mobile
CREATE INDEX idx_kost_wilayah ON kost(id_wilayah);
CREATE INDEX idx_kost_harga ON kost(harga);
CREATE INDEX idx_kost_tipe ON kost(tipe_kost);
CREATE INDEX idx_kost_status ON kost(status);
CREATE INDEX idx_kost_nama ON kost(nama_kost);
```

---

## 2. Skema Database Lokal Android (SQLite / Room)

Pada sisi perangkat Android, SQLite/Room Database mengelola entitas lokal agar aplikasi tetap responsif meskipun jaringan lambat atau terputus:

```text
Table: local_favorites
- id_favorit: INTEGER (Primary Key AutoGenerate)
- id_kost: INTEGER (Indexed, Unique)
- nama_kost: TEXT
- harga: REAL
- tipe_kost: TEXT
- kelurahan: TEXT
- kecamatan: TEXT
- thumbnail_url: TEXT
- no_whatsapp: TEXT
- saved_at: INTEGER (Timestamp)

Table: cached_kost
- id_kost: INTEGER (Primary Key)
- id_wilayah: INTEGER
- nama_kost: TEXT
- alamat: TEXT
- harga: REAL
- tipe_kost: TEXT
- status: TEXT
- thumbnail_url: TEXT
- last_updated: INTEGER
```

### Konfigurasi SharedPreferences (Android Session)
Menyimpan state kredensial user lokal:
- `KEY_IS_LOGGED_IN`: boolean
- `KEY_USER_ID`: int
- `KEY_USER_NAME`: string
- `KEY_USER_EMAIL`: string
- `KEY_USER_ROLE`: string (`USER` / `ADMIN`)
- `KEY_AUTH_TOKEN`: string (opsional/JWT token)

---

## 3. Aturan Integritas Data Mobile
- Validasi form (nomor WhatsApp diawali kode negara/08, harga angka positif, koordinat latitude & longitude valid) dilakukan di client Android sebelum dikirim.
- Operasi delete kost oleh Admin menggunakan Soft Delete (status `TIDAK_AKTIF`) agar relasi data histori favorit di perangkat user tidak mengalami inkonsistensi.
- Foto yang diunggah dari kamera/galeri smartphone dikompresi (format WebP/JPEG) di sisi client sebelum disimpan ke storage server.
