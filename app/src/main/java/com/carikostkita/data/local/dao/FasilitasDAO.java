package com.carikostkita.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.carikostkita.data.local.DatabaseHelper;
import com.carikostkita.data.model.Fasilitas;
import java.util.ArrayList;
import java.util.List;

public class FasilitasDAO {
    private final DatabaseHelper dbHelper;

    public FasilitasDAO(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public List<Fasilitas> findAll() {
        List<Fasilitas> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("fasilitas", null, null, null, null, null, "nama_fasilitas ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(new Fasilitas(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id_fasilitas")),
                        cursor.getString(cursor.getColumnIndexOrThrow("nama_fasilitas"))
                ));
            }
            cursor.close();
        }
        return list;
    }

    public List<Fasilitas> findByKostId(int idKost) {
        List<Fasilitas> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT f.id_fasilitas, f.nama_fasilitas FROM fasilitas f " +
                "INNER JOIN kost_fasilitas kf ON f.id_fasilitas = kf.id_fasilitas " +
                "WHERE kf.id_kost = ? ORDER BY f.nama_fasilitas ASC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idKost)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(new Fasilitas(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id_fasilitas")),
                        cursor.getString(cursor.getColumnIndexOrThrow("nama_fasilitas"))
                ));
            }
            cursor.close();
        }
        return list;
    }

    public long insert(String namaFasilitas) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nama_fasilitas", namaFasilitas);
        return db.insert("fasilitas", null, values);
    }
}
