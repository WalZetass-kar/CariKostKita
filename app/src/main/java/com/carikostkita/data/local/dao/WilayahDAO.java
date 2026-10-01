package com.carikostkita.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.carikostkita.data.local.DatabaseHelper;
import com.carikostkita.data.model.Wilayah;
import java.util.ArrayList;
import java.util.List;

public class WilayahDAO {
    private final DatabaseHelper dbHelper;

    public WilayahDAO(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public List<Wilayah> findAll() {
        List<Wilayah> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("wilayah", null, null, null, null, null, "kecamatan ASC, kelurahan ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToWilayah(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public Wilayah findById(int idWilayah) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Wilayah w = null;
        Cursor cursor = db.query("wilayah", null, "id_wilayah = ?", new String[]{String.valueOf(idWilayah)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            w = cursorToWilayah(cursor);
            cursor.close();
        }
        return w;
    }

    public long insert(Wilayah wilayah) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("kecamatan", wilayah.getKecamatan());
        values.put("kelurahan", wilayah.getKelurahan());
        values.put("kota", wilayah.getKota() != null ? wilayah.getKota() : "Pekanbaru");
        return db.insert("wilayah", null, values);
    }

    private Wilayah cursorToWilayah(Cursor cursor) {
        return new Wilayah(
                cursor.getInt(cursor.getColumnIndexOrThrow("id_wilayah")),
                cursor.getString(cursor.getColumnIndexOrThrow("kecamatan")),
                cursor.getString(cursor.getColumnIndexOrThrow("kelurahan")),
                cursor.getString(cursor.getColumnIndexOrThrow("kota"))
        );
    }
}
