package com.carikostkita.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.carikostkita.data.local.DatabaseHelper;
import com.carikostkita.data.model.Role;
import com.carikostkita.data.model.User;

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

        return db.insert("users", null, values);
    }

    public boolean updateProfile(int idUser, String nama, String noHp, String avatarUrl) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nama", nama);
        if (noHp != null) values.put("no_hp", noHp);
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
        user.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
        return user;
    }
}
