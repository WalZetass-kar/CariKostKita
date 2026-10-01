package com.carikostkita.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.carikostkita.data.local.DatabaseHelper;
import com.carikostkita.data.model.Role;
import com.carikostkita.data.model.User;
import com.carikostkita.data.model.VerificationStatus;
import org.mindrot.jbcrypt.BCrypt;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private final DatabaseHelper dbHelper;

    public UserDAO(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public User findByEmail(String email) {
        if (email == null) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        User user = null;
        Cursor cursor = db.query(
                "users",
                null,
                "LOWER(email) = ?",
                new String[]{email.trim().toLowerCase()},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            user = cursorToUser(cursor);
            cursor.close();
        }
        return user;
    }

    public User findById(int idUser) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        User user = null;
        Cursor cursor = db.query(
                "users",
                null,
                "id_user = ?",
                new String[]{String.valueOf(idUser)},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            user = cursorToUser(cursor);
            cursor.close();
        }
        return user;
    }

    public long insert(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nama", user.getNama());
        values.put("email", user.getEmail().trim().toLowerCase());
        values.put("password", user.getPassword());
        values.put("role", user.getRole() != null ? user.getRole().name() : Role.USER.name());
        values.put("no_hp", user.getNoHp());
        values.put("avatar_url", user.getAvatarUrl());
        values.put("bio", user.getBio());
        values.put("verification_status", user.getVerificationStatus() != null ? user.getVerificationStatus().name() : VerificationStatus.NONE.name());
        values.put("pengajuan_catatan", user.getPengajuanCatatan());
        values.put("catatan_revisi", user.getCatatanRevisi());
        values.put("is_active", user.isActive() ? 1 : 0);
        values.put("auth_provider", user.getAuthProvider() != null ? user.getAuthProvider() : "LOCAL");

        return db.insert("users", null, values);
    }

    public boolean updateProfile(int idUser, String nama, String noHp, String bio, String avatarUrl) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nama", nama);
        if (noHp != null) values.put("no_hp", noHp);
        if (bio != null) values.put("bio", bio);
        if (avatarUrl != null) values.put("avatar_url", avatarUrl);
        int rows = db.update("users", values, "id_user = ?", new String[]{String.valueOf(idUser)});
        return rows > 0;
    }

    public boolean updatePassword(int idUser, String newHashedPassword) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("password", newHashedPassword);
        int rows = db.update("users", values, "id_user = ?", new String[]{String.valueOf(idUser)});
        return rows > 0;
    }

    public boolean submitOwnerVerification(int idUser, String catatan) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("verification_status", VerificationStatus.PENDING.name());
        values.put("pengajuan_catatan", catatan);
        int rows = db.update("users", values, "id_user = ?", new String[]{String.valueOf(idUser)});
        return rows > 0;
    }

    public boolean approveOwnerVerification(int idUser) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("role", Role.PEMILIK_KOST.name());
        values.put("verification_status", VerificationStatus.APPROVED.name());
        values.put("catatan_revisi", "");
        int rows = db.update("users", values, "id_user = ?", new String[]{String.valueOf(idUser)});
        return rows > 0;
    }

    public boolean rejectOwnerVerification(int idUser, String alasan) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("role", Role.USER.name());
        values.put("verification_status", VerificationStatus.REJECTED.name());
        values.put("pengajuan_catatan", alasan);
        int rows = db.update("users", values, "id_user = ?", new String[]{String.valueOf(idUser)});
        return rows > 0;
    }

    public boolean requireRevisionOwnerVerification(int idUser, String catatanRevisi) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("role", Role.USER.name());
        values.put("verification_status", VerificationStatus.REVISION_REQUIRED.name());
        values.put("catatan_revisi", catatanRevisi);
        int rows = db.update("users", values, "id_user = ?", new String[]{String.valueOf(idUser)});
        return rows > 0;
    }

    public boolean setUserActiveStatus(int idUser, boolean isActive) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_active", isActive ? 1 : 0);
        int rows = db.update("users", values, "id_user = ?", new String[]{String.valueOf(idUser)});
        return rows > 0;
    }

    public List<User> getPendingVerifications() {
        List<User> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "users",
                null,
                "verification_status = ? OR verification_status = ?",
                new String[]{VerificationStatus.PENDING.name(), VerificationStatus.REVISION_REQUIRED.name()},
                null, null, "id_user DESC"
        );
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToUser(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public List<User> findPendingVerifications() {
        return getPendingVerifications();
    }

    public List<User> findAllUsers() {
        List<User> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "users",
                null,
                null,
                null,
                null, null, "id_user ASC"
        );
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToUser(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public int getTotalUserCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM users", null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public int getTotalOwnerCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM users WHERE role = ?", new String[]{Role.PEMILIK_KOST.name()});
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public User findOrCreateGoogleUser(String email, String nama, String avatarUrl) {
        User existing = findByEmail(email);
        if (existing != null) {
            return existing;
        }
        User newUser = new User();
        newUser.setEmail(email.trim().toLowerCase());
        newUser.setNama(nama != null && !nama.isEmpty() ? nama : "User Google");
        newUser.setPassword(BCrypt.hashpw("GoogleOAuth2026!", BCrypt.gensalt(10)));
        newUser.setRole(Role.USER);
        newUser.setVerificationStatus(VerificationStatus.NONE);
        newUser.setActive(true);
        newUser.setAuthProvider("GOOGLE");
        newUser.setAvatarUrl(avatarUrl != null ? avatarUrl : "avatar_google");
        long id = insert(newUser);
        newUser.setIdUser((int) id);
        return newUser;
    }

    private User cursorToUser(Cursor cursor) {
        User user = new User();
        user.setIdUser(cursor.getInt(cursor.getColumnIndexOrThrow("id_user")));
        user.setNama(cursor.getString(cursor.getColumnIndexOrThrow("nama")));
        user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("email")));
        user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow("password")));
        user.setRole(Role.fromString(cursor.getString(cursor.getColumnIndexOrThrow("role"))));

        int noHpIdx = cursor.getColumnIndex("no_hp");
        if (noHpIdx != -1) user.setNoHp(cursor.getString(noHpIdx));

        int avatarIdx = cursor.getColumnIndex("avatar_url");
        if (avatarIdx != -1) user.setAvatarUrl(cursor.getString(avatarIdx));

        int bioIdx = cursor.getColumnIndex("bio");
        if (bioIdx != -1) user.setBio(cursor.getString(bioIdx));

        int verifIdx = cursor.getColumnIndex("verification_status");
        if (verifIdx != -1) user.setVerificationStatus(VerificationStatus.fromString(cursor.getString(verifIdx)));

        int catIdx = cursor.getColumnIndex("pengajuan_catatan");
        if (catIdx != -1) user.setPengajuanCatatan(cursor.getString(catIdx));

        int revIdx = cursor.getColumnIndex("catatan_revisi");
        if (revIdx != -1) user.setCatatanRevisi(cursor.getString(revIdx));

        int actIdx = cursor.getColumnIndex("is_active");
        if (actIdx != -1) user.setActive(cursor.getInt(actIdx) == 1);

        int authIdx = cursor.getColumnIndex("auth_provider");
        if (authIdx != -1) user.setAuthProvider(cursor.getString(authIdx));

        user.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
        return user;
    }
}
