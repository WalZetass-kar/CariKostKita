package com.carikostkita.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.mindrot.jbcrypt.BCrypt;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "carikostkita_local.db";
    private static final int DATABASE_VERSION = 2;

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Table Users
        db.execSQL("CREATE TABLE users (" +
                "id_user INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nama TEXT NOT NULL, " +
                "email TEXT NOT NULL UNIQUE, " +
                "password TEXT NOT NULL, " +
                "role TEXT NOT NULL DEFAULT 'USER', " +
                "no_hp TEXT, " +
                "avatar_url TEXT, " +
                "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");

        // Table Wilayah
        db.execSQL("CREATE TABLE wilayah (" +
                "id_wilayah INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "kecamatan TEXT NOT NULL, " +
                "kelurahan TEXT NOT NULL, " +
                "kota TEXT NOT NULL DEFAULT 'Pekanbaru', " +
                "provinsi TEXT NOT NULL DEFAULT 'Riau', " +
                "UNIQUE(kecamatan, kelurahan, kota, provinsi))");

        // Table Fasilitas
        db.execSQL("CREATE TABLE fasilitas (" +
                "id_fasilitas INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nama_fasilitas TEXT NOT NULL UNIQUE)");

        // Table Kost
        db.execSQL("CREATE TABLE kost (" +
                "id_kost INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_wilayah INTEGER NOT NULL, " +
                "nama_kost TEXT NOT NULL, " +
                "alamat TEXT NOT NULL, " +
                "harga REAL NOT NULL, " +
                "tipe_kost TEXT NOT NULL, " +
                "deskripsi TEXT, " +
                "no_whatsapp TEXT, " +
                "latitude REAL, " +
                "longitude REAL, " +
                "status TEXT NOT NULL DEFAULT 'TERSEDIA', " +
                "provinsi TEXT NOT NULL DEFAULT 'Riau', " +
                "kota TEXT NOT NULL DEFAULT 'Pekanbaru', " +
                "ukuran_kamar TEXT NOT NULL DEFAULT '3x4 m', " +
                "total_kamar INTEGER NOT NULL DEFAULT 10, " +
                "kamar_tersedia INTEGER NOT NULL DEFAULT 3, " +
                "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (id_wilayah) REFERENCES wilayah(id_wilayah))");

        // Table Kost_Fasilitas
        db.execSQL("CREATE TABLE kost_fasilitas (" +
                "id_kost INTEGER NOT NULL, " +
                "id_fasilitas INTEGER NOT NULL, " +
                "PRIMARY KEY (id_kost, id_fasilitas), " +
                "FOREIGN KEY (id_kost) REFERENCES kost(id_kost) ON DELETE CASCADE, " +
                "FOREIGN KEY (id_fasilitas) REFERENCES fasilitas(id_fasilitas) ON DELETE CASCADE)");

        // Table Foto_Kost
        db.execSQL("CREATE TABLE foto_kost (" +
                "id_foto INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_kost INTEGER NOT NULL, " +
                "nama_file TEXT NOT NULL, " +
                "path_file TEXT NOT NULL, " +
                "is_thumbnail INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY (id_kost) REFERENCES kost(id_kost) ON DELETE CASCADE)");

        // Table Favorit
        db.execSQL("CREATE TABLE favorit (" +
                "id_favorit INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_user INTEGER NOT NULL, " +
                "id_kost INTEGER NOT NULL, " +
                "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE(id_user, id_kost), " +
                "FOREIGN KEY (id_user) REFERENCES users(id_user) ON DELETE CASCADE, " +
                "FOREIGN KEY (id_kost) REFERENCES kost(id_kost) ON DELETE CASCADE)");

        seedData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE kost ADD COLUMN provinsi TEXT NOT NULL DEFAULT 'Riau'");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE kost ADD COLUMN kota TEXT NOT NULL DEFAULT 'Pekanbaru'");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE kost ADD COLUMN ukuran_kamar TEXT NOT NULL DEFAULT '3x4 m'");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE kost ADD COLUMN total_kamar INTEGER NOT NULL DEFAULT 10");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE kost ADD COLUMN kamar_tersedia INTEGER NOT NULL DEFAULT 3");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE users ADD COLUMN no_hp TEXT");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE users ADD COLUMN avatar_url TEXT");
            } catch (Exception ignored) {}
        }
    }

    private void seedData(SQLiteDatabase db) {
        // Seed Default Users: Developer, Pemilik Kost, Pencari Kost
        String adminPasswordHash = BCrypt.hashpw("admin123", BCrypt.gensalt(10));
        String pemilikPasswordHash = BCrypt.hashpw("pemilik123", BCrypt.gensalt(10));
        String userPasswordHash = BCrypt.hashpw("user123", BCrypt.gensalt(10));

        db.execSQL("INSERT INTO users (nama, email, password, role, no_hp) VALUES " +
                "('Developer CariKostKita', 'admin@carikostkita.com', '" + adminPasswordHash + "', 'ADMIN', '081299990001'), " +
                "('H. Rahmat (Pemilik Kost)', 'pemilik@carikostkita.com', '" + pemilikPasswordHash + "', 'PEMILIK_KOST', '081234567890'), " +
                "('Budi Santoso', 'budi@gmail.com', '" + userPasswordHash + "', 'USER', '085211223344')");

        // Seed Wilayah
        db.execSQL("INSERT INTO wilayah (kecamatan, kelurahan, kota, provinsi) VALUES " +
                "('Bukit Raya', 'Simpang Tiga', 'Pekanbaru', 'Riau'), " +
                "('Bukit Raya', 'Tangkerang Labuai', 'Pekanbaru', 'Riau'), " +
                "('Bukit Raya', 'Tangkerang Selatan', 'Pekanbaru', 'Riau'), " +
                "('Bukit Raya', 'Air Dingin', 'Pekanbaru', 'Riau'), " +
                "('Tampan', 'Simpang Baru', 'Pekanbaru', 'Riau'), " +
                "('Marpoyan Damai', 'Sidomulyo Timur', 'Pekanbaru', 'Riau')");

        // Seed Fasilitas Standar
        db.execSQL("INSERT INTO fasilitas (nama_fasilitas) VALUES " +
                "('WiFi'), ('Parkir Motor'), ('Parkir Mobil'), ('AC'), " +
                "('Kamar Mandi Dalam'), ('Kamar Mandi Luar'), ('Kasur & Lemari'), " +
                "('Dapur Bersama'), ('CCTV 24 Jam'), ('Listrik Termasuk')");

        // Seed Kost Realistis
        db.execSQL("INSERT INTO kost (id_wilayah, nama_kost, alamat, harga, tipe_kost, deskripsi, no_whatsapp, latitude, longitude, status, provinsi, kota, ukuran_kamar, total_kamar, kamar_tersedia) VALUES " +
                "(1, 'Kost Putri Melati Bukit Raya', 'Jl. Kaharuddin Nasution No. 45, Simpang Tiga', 750000, 'PUTRI', 'Kost putri nyaman, aman, 5 menit ke Kampus UIR. Lengkap kasur, lemari, WiFi cepat, dan penjaga 24 jam.', '6281234567890', 0.4632801, 101.4501234, 'TERSEDIA', 'Riau', 'Pekanbaru', '3x4 m', 12, 4), " +
                "(4, 'Kost Putra Garuda Permai', 'Jl. Air Dingin No. 12, Air Dingin', 600000, 'PUTRA', 'Kost putra bersih dan tenang. Lingkungan kondusif untuk belajar. Tersedia parkiran motor luas berpagar aman.', '6282176543210', 0.4591102, 101.4428901, 'TERSEDIA', 'Riau', 'Pekanbaru', '3x3 m', 10, 2), " +
                "(2, 'Kost Eksklusif Pelangi', 'Jl. Labuai Indah No. 8, Tangkerang Labuai', 1200000, 'CAMPUR', 'Kost eksklusif ber-AC, kamar mandi dalam, water heater, dapur bersama modern, dan parkiran mobil aman.', '6285298765432', 0.4721405, 101.4589230, 'TERSEDIA', 'Riau', 'Pekanbaru', '4x4 m', 8, 3), " +
                "(3, 'Kost Anugerah Bukit Raya', 'Jl. Tengku Bey No. 22, Tangkerang Selatan', 500000, 'PUTRA', 'Kost hemat nyaman dekat jalan utama dan rumah makan. Fasilitas lengkap kasur dan meja belajar.', '6281311223344', 0.4687500, 101.4498000, 'PENUH', 'Riau', 'Pekanbaru', '3x3 m', 6, 0)");

        // Relasi Fasilitas Kost 1
        db.execSQL("INSERT INTO kost_fasilitas (id_kost, id_fasilitas) VALUES (1, 1), (1, 2), (1, 5), (1, 7), (1, 9)");
        // Relasi Fasilitas Kost 2
        db.execSQL("INSERT INTO kost_fasilitas (id_kost, id_fasilitas) VALUES (2, 1), (2, 2), (2, 7)");
        // Relasi Fasilitas Kost 3
        db.execSQL("INSERT INTO kost_fasilitas (id_kost, id_fasilitas) VALUES (3, 1), (3, 3), (3, 4), (3, 5), (3, 7), (3, 8), (3, 9)");
        // Relasi Fasilitas Kost 4
        db.execSQL("INSERT INTO kost_fasilitas (id_kost, id_fasilitas) VALUES (4, 2), (4, 7)");
    }
}
