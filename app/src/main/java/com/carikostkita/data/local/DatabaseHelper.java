package com.carikostkita.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.mindrot.jbcrypt.BCrypt;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "carikostkita_local.db";
    private static final int DATABASE_VERSION = 4;

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
                "bio TEXT, " +
                "verification_status TEXT NOT NULL DEFAULT 'NONE', " +
                "pengajuan_catatan TEXT, " +
                "catatan_revisi TEXT, " +
                "is_active INTEGER NOT NULL DEFAULT 1, " +
                "auth_provider TEXT NOT NULL DEFAULT 'LOCAL', " +
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
                "id_pemilik INTEGER NOT NULL DEFAULT 2, " +
                "nama_kost TEXT NOT NULL, " +
                "alamat TEXT NOT NULL, " +
                "patokan TEXT, " +
                "harga REAL NOT NULL, " +
                "tipe_kost TEXT NOT NULL, " +
                "deskripsi TEXT, " +
                "no_whatsapp TEXT, " +
                "latitude REAL, " +
                "longitude REAL, " +
                "status TEXT NOT NULL DEFAULT 'TERSEDIA', " +
                "verification_status TEXT NOT NULL DEFAULT 'APPROVED', " +
                "catatan_revisi TEXT, " +
                "location_verification TEXT NOT NULL DEFAULT 'VALID', " +
                "provinsi TEXT NOT NULL DEFAULT 'Riau', " +
                "kota TEXT NOT NULL DEFAULT 'Pekanbaru', " +
                "ukuran_kamar TEXT NOT NULL DEFAULT '3x4 m', " +
                "total_kamar INTEGER NOT NULL DEFAULT 10, " +
                "kamar_tersedia INTEGER NOT NULL DEFAULT 3, " +
                "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (id_wilayah) REFERENCES wilayah(id_wilayah), " +
                "FOREIGN KEY (id_pemilik) REFERENCES users(id_user))");

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

        // Table Chat Conversation
        db.execSQL("CREATE TABLE chat_conversation (" +
                "id_conversation INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_kost INTEGER NOT NULL, " +
                "id_pencari INTEGER NOT NULL, " +
                "id_pemilik INTEGER NOT NULL, " +
                "last_message TEXT, " +
                "last_message_time TEXT, " +
                "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE(id_kost, id_pencari, id_pemilik), " +
                "FOREIGN KEY (id_kost) REFERENCES kost(id_kost) ON DELETE CASCADE, " +
                "FOREIGN KEY (id_pencari) REFERENCES users(id_user) ON DELETE CASCADE, " +
                "FOREIGN KEY (id_pemilik) REFERENCES users(id_user) ON DELETE CASCADE)");

        // Table Chat Message
        db.execSQL("CREATE TABLE chat_message (" +
                "id_message INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_conversation INTEGER NOT NULL, " +
                "id_sender INTEGER NOT NULL, " +
                "message TEXT NOT NULL, " +
                "is_read INTEGER NOT NULL DEFAULT 0, " +
                "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (id_conversation) REFERENCES chat_conversation(id_conversation) ON DELETE CASCADE, " +
                "FOREIGN KEY (id_sender) REFERENCES users(id_user) ON DELETE CASCADE)");

        // Table Kost Report (Pelaporan dari Pencari Kost)
        db.execSQL("CREATE TABLE IF NOT EXISTS kost_report (" +
                "id_report INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_kost INTEGER NOT NULL, " +
                "id_reporter INTEGER NOT NULL, " +
                "id_pemilik INTEGER NOT NULL, " +
                "kategori_laporan TEXT NOT NULL, " +
                "deskripsi TEXT NOT NULL, " +
                "status TEXT NOT NULL DEFAULT 'BARU', " +
                "tindakan_admin TEXT, " +
                "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (id_kost) REFERENCES kost(id_kost) ON DELETE CASCADE, " +
                "FOREIGN KEY (id_reporter) REFERENCES users(id_user) ON DELETE CASCADE, " +
                "FOREIGN KEY (id_pemilik) REFERENCES users(id_user) ON DELETE CASCADE)");

        // Table System Activity Log (Audit Trail Sistem)
        db.execSQL("CREATE TABLE IF NOT EXISTS system_activity_log (" +
                "id_log INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_user INTEGER, " +
                "actor_name TEXT NOT NULL, " +
                "action_type TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "target_type TEXT, " +
                "target_id INTEGER, " +
                "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");

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
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE users ADD COLUMN bio TEXT");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE users ADD COLUMN verification_status TEXT NOT NULL DEFAULT 'NONE'");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE users ADD COLUMN pengajuan_catatan TEXT");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE users ADD COLUMN auth_provider TEXT NOT NULL DEFAULT 'LOCAL'");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE kost ADD COLUMN id_pemilik INTEGER NOT NULL DEFAULT 2");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE kost ADD COLUMN patokan TEXT");
            } catch (Exception ignored) {}

            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_conversation (" +
                        "id_conversation INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_kost INTEGER NOT NULL, " +
                        "id_pencari INTEGER NOT NULL, " +
                        "id_pemilik INTEGER NOT NULL, " +
                        "last_message TEXT, " +
                        "last_message_time TEXT, " +
                        "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "UNIQUE(id_kost, idPencari, idPemilik))");
            } catch (Exception ignored) {}

            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_message (" +
                        "id_message INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_conversation INTEGER NOT NULL, " +
                        "id_sender INTEGER NOT NULL, " +
                        "message TEXT NOT NULL, " +
                        "is_read INTEGER NOT NULL DEFAULT 0, " +
                        "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            } catch (Exception ignored) {}
        }

        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE users ADD COLUMN is_active INTEGER NOT NULL DEFAULT 1");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE users ADD COLUMN catatan_revisi TEXT");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE kost ADD COLUMN verification_status TEXT NOT NULL DEFAULT 'APPROVED'");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE kost ADD COLUMN catatan_revisi TEXT");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE kost ADD COLUMN location_verification TEXT NOT NULL DEFAULT 'VALID'");
            } catch (Exception ignored) {}

            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS kost_report (" +
                        "id_report INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_kost INTEGER NOT NULL, " +
                        "id_reporter INTEGER NOT NULL, " +
                        "id_pemilik INTEGER NOT NULL, " +
                        "kategori_laporan TEXT NOT NULL, " +
                        "deskripsi TEXT NOT NULL, " +
                        "status TEXT NOT NULL DEFAULT 'BARU', " +
                        "tindakan_admin TEXT, " +
                        "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            } catch (Exception ignored) {}

            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS system_activity_log (" +
                        "id_log INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_user INTEGER, " +
                        "actor_name TEXT NOT NULL, " +
                        "action_type TEXT NOT NULL, " +
                        "description TEXT NOT NULL, " +
                        "target_type TEXT, " +
                        "target_id INTEGER, " +
                        "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            } catch (Exception ignored) {}
        }
    }

    private void seedData(SQLiteDatabase db) {
        // Seed Default Users: Developer, Pemilik Kost (Verified), Pencari Kost
        String adminPasswordHash = BCrypt.hashpw("admin123", BCrypt.gensalt(10));
        String pemilikPasswordHash = BCrypt.hashpw("pemilik123", BCrypt.gensalt(10));
        String userPasswordHash = BCrypt.hashpw("user123", BCrypt.gensalt(10));

        db.execSQL("INSERT INTO users (nama, email, password, role, no_hp, bio, verification_status, auth_provider, is_active) VALUES " +
                "('Developer CariKostKita', 'admin@carikostkita.com', '" + adminPasswordHash + "', 'ADMIN', '081299990001', 'Pengembang Sistem CariKostKita Pekanbaru', 'APPROVED', 'LOCAL', 1), " +
                "('H. Rahmat (Pemilik Kost)', 'pemilik@carikostkita.com', '" + pemilikPasswordHash + "', 'PEMILIK_KOST', '081234567890', 'Pemilik kost terpercaya di Bukit Raya sejak 2018. Mengutamakan kenyamanan & keamanan mahasiswa.', 'APPROVED', 'LOCAL', 1), " +
                "('Budi Santoso', 'budi@gmail.com', '" + userPasswordHash + "', 'USER', '085211223344', 'Mahasiswa Teknik Sipil UIR semester akhir', 'NONE', 'LOCAL', 1)");

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

        // Seed Kost Realistis (Owned by H. Rahmat, id_user = 2)
        db.execSQL("INSERT INTO kost (id_wilayah, id_pemilik, nama_kost, alamat, patokan, harga, tipe_kost, deskripsi, no_whatsapp, latitude, longitude, status, verification_status, location_verification, provinsi, kota, ukuran_kamar, total_kamar, kamar_tersedia) VALUES " +
                "(1, 2, 'Kost Putri Melati Bukit Raya', 'Jl. Kaharuddin Nasution No. 45, Simpang Tiga', '50 meter dari gerbang utama Kampus UIR', 750000, 'PUTRI', 'Kost putri nyaman, aman, 5 menit ke Kampus UIR. Lengkap kasur, lemari, WiFi cepat, dan penjaga 24 jam.', '6281234567890', 0.4632801, 101.4501234, 'TERSEDIA', 'APPROVED', 'VALID', 'Riau', 'Pekanbaru', '3x4 m', 12, 4), " +
                "(4, 2, 'Kost Putra Garuda Permai', 'Jl. Air Dingin No. 12, Air Dingin', 'Depan Masjid Al-Ikhlas, samping fotokopi', 600000, 'PUTRA', 'Kost putra bersih dan tenang. Lingkungan kondusif untuk belajar. Tersedia parkiran motor luas berpagar aman.', '6282176543210', 0.4591102, 101.4428901, 'TERSEDIA', 'APPROVED', 'VALID', 'Riau', 'Pekanbaru', '3x3 m', 10, 2), " +
                "(2, 2, 'Kost Eksklusif Pelangi', 'Jl. Labuai Indah No. 8, Tangkerang Labuai', 'Dekat Bundaran Labuai, seberang minimarket', 1200000, 'CAMPUR', 'Kost eksklusif ber-AC, kamar mandi dalam, water heater, dapur bersama modern, dan parkiran mobil aman.', '6285298765432', 0.4721405, 101.4589230, 'TERSEDIA', 'APPROVED', 'VALID', 'Riau', 'Pekanbaru', '4x4 m', 8, 3), " +
                "(3, 2, 'Kost Anugerah Bukit Raya', 'Jl. Tengku Bey No. 22, Tangkerang Selatan', 'Gang Mawar 2, belakang RM Ampera', 500000, 'PUTRA', 'Kost hemat nyaman dekat jalan utama dan rumah makan. Fasilitas lengkap kasur dan meja belajar.', '6281311223344', 0.4687500, 101.4498000, 'PENUH', 'APPROVED', 'VALID', 'Riau', 'Pekanbaru', '3x3 m', 6, 0)");

        // Relasi Fasilitas
        db.execSQL("INSERT INTO kost_fasilitas (id_kost, id_fasilitas) VALUES (1, 1), (1, 2), (1, 5), (1, 7), (1, 9)");
        db.execSQL("INSERT INTO kost_fasilitas (id_kost, id_fasilitas) VALUES (2, 1), (2, 2), (2, 7)");
        db.execSQL("INSERT INTO kost_fasilitas (id_kost, id_fasilitas) VALUES (3, 1), (3, 3), (3, 4), (3, 5), (3, 7), (3, 8), (3, 9)");
        db.execSQL("INSERT INTO kost_fasilitas (id_kost, id_fasilitas) VALUES (4, 2), (4, 7)");

        // Seed Sample Chat Conversation between Budi (3) and H. Rahmat (2) on Kost Putri Melati (1)
        db.execSQL("INSERT INTO chat_conversation (id_kost, id_pencari, id_pemilik, last_message, last_message_time) VALUES " +
                "(1, 3, 2, 'Halo Pak, apakah kamar untuk bulan depan masih ada?', '10:30')");

        db.execSQL("INSERT INTO chat_message (id_conversation, id_sender, message, is_read) VALUES " +
                "(1, 3, 'Halo Pak Rahmat, saya tertarik dengan Kost Putri Melati Bukit Raya.', 1), " +
                "(1, 2, 'Halo Budi, ya masih ada 4 kamar kosong di lantai 2.', 1), " +
                "(1, 3, 'Halo Pak, apakah kamar untuk bulan depan masih ada?', 0)");

        // Seed Sample System Activity Logs
        db.execSQL("INSERT INTO system_activity_log (id_user, actor_name, action_type, description, target_type, target_id) VALUES " +
                "(1, 'Developer CariKostKita', 'SISTEM_INIT', 'Sistem CariKostKita v4.0 berhasil diinisialisasi', 'SYSTEM', 1), " +
                "(2, 'H. Rahmat (Pemilik Kost)', 'VERIFIKASI_PEMILIK', 'Akun pemilik terverifikasi resmi oleh Developer', 'USER', 2), " +
                "(2, 'H. Rahmat (Pemilik Kost)', 'PROPERTI_PUBLISH', 'Properti Kost Putri Melati Bukit Raya disetujui & dipublikasikan', 'KOST', 1), " +
                "(3, 'Budi Santoso', 'REGISTRASI', 'Pengguna baru terdaftar di CariKostKita', 'USER', 3)");

        // Seed Sample Report for demo inspection
        db.execSQL("INSERT INTO kost_report (id_kost, id_reporter, id_pemilik, kategori_laporan, deskripsi, status) VALUES " +
                "(4, 3, 2, 'Kost Penuh / Tidak Tersedia', 'Status kost tertulis penuh namun mohon verifikasi ulang ketersediaan kamar aktualnya.', 'BARU')");
    }
}
