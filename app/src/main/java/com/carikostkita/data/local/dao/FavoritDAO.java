package com.carikostkita.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.carikostkita.data.local.DatabaseHelper;
import com.carikostkita.data.model.Kost;
import java.util.ArrayList;
import java.util.List;

public class FavoritDAO {
    private final DatabaseHelper dbHelper;
    private final KostDAO kostDAO;

    public FavoritDAO(DatabaseHelper dbHelper, KostDAO kostDAO) {
        this.dbHelper = dbHelper;
        this.kostDAO = kostDAO;
    }

    public boolean isFavorite(int idUser, int idKost) {
        if (idUser <= 0 || idKost <= 0) return false;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "favorit",
                new String[]{"id_favorit"},
                "id_user = ? AND id_kost = ?",
                new String[]{String.valueOf(idUser), String.valueOf(idKost)},
                null, null, null
        );
        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) cursor.close();
        return exists;
    }

    public boolean toggleFavorite(int idUser, int idKost) {
        if (idUser <= 0 || idKost <= 0) return false;
        if (isFavorite(idUser, idKost)) {
            removeFavorite(idUser, idKost);
            return false; // Now unfavorited
        } else {
            addFavorite(idUser, idKost);
            return true; // Now favorited
        }
    }

    public long addFavorite(int idUser, int idKost) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id_user", idUser);
        values.put("id_kost", idKost);
        return db.insertWithOnConflict("favorit", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public int removeFavorite(int idUser, int idKost) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete("favorit", "id_user = ? AND id_kost = ?", new String[]{String.valueOf(idUser), String.valueOf(idKost)});
    }

    public List<Kost> findFavoritesByUserId(int idUser) {
        List<Kost> list = new ArrayList<>();
        if (idUser <= 0) return list;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT id_kost FROM favorit WHERE id_user = ? ORDER BY id_favorit DESC";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idUser)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int idKost = cursor.getInt(0);
                Kost kost = kostDAO.findById(idKost);
                if (kost != null) {
                    kost.setFavorite(true);
                    list.add(kost);
                }
            }
            cursor.close();
        }
        return list;
    }
}
