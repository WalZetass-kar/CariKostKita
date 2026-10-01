package com.carikostkita.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.carikostkita.data.local.DatabaseHelper;
import com.carikostkita.data.model.Fasilitas;
import com.carikostkita.data.model.FotoKost;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.KostFilterCriteria;
import com.carikostkita.data.model.StatusKost;
import com.carikostkita.data.model.TipeKost;
import java.util.ArrayList;
import java.util.List;

public class KostDAO {
    private final DatabaseHelper dbHelper;

    public KostDAO(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public List<Kost> findAllActive() {
        return queryKostList("k.status != ?", new String[]{StatusKost.TIDAK_AKTIF.name()});
    }

    public List<Kost> findAllForAdmin() {
        return queryKostList(null, null);
    }

    public Kost findById(int idKost) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT k.*, w.kecamatan, w.kelurahan FROM kost k " +
                "LEFT JOIN wilayah w ON k.id_wilayah = w.id_wilayah " +
                "WHERE k.id_kost = ?";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idKost)});
        Kost kost = null;
        if (cursor != null && cursor.moveToFirst()) {
            kost = cursorToKost(cursor);
            cursor.close();
        }
        if (kost != null) {
            populateFasilitasAndFoto(kost);
        }
        return kost;
    }

    public List<Kost> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAllActive();
        }
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        String selection = "k.status != ? AND (LOWER(k.nama_kost) LIKE ? OR LOWER(k.alamat) LIKE ? OR LOWER(w.kelurahan) LIKE ? OR LOWER(w.kecamatan) LIKE ?)";
        String[] args = new String[]{
                StatusKost.TIDAK_AKTIF.name(),
                pattern, pattern, pattern, pattern
        };
        return queryKostList(selection, args);
    }

    public List<Kost> filter(KostFilterCriteria criteria) {
        if (criteria == null) return findAllActive();

        StringBuilder selection = new StringBuilder("k.status != ?");
        List<String> argsList = new ArrayList<>();
        argsList.add(StatusKost.TIDAK_AKTIF.name());

        if (criteria.getKeyword() != null && !criteria.getKeyword().trim().isEmpty()) {
            selection.append(" AND (LOWER(k.nama_kost) LIKE ? OR LOWER(k.alamat) LIKE ? OR LOWER(w.kelurahan) LIKE ?)");
            String p = "%" + criteria.getKeyword().trim().toLowerCase() + "%";
            argsList.add(p);
            argsList.add(p);
            argsList.add(p);
        }

        if (criteria.getTipeKost() != null) {
            selection.append(" AND k.tipe_kost = ?");
            argsList.add(criteria.getTipeKost().name());
        }

        if (criteria.getMaxHarga() != null && criteria.getMaxHarga() > 0) {
            selection.append(" AND k.harga <= ?");
            argsList.add(String.valueOf(criteria.getMaxHarga()));
        }

        if (criteria.getMinHarga() != null && criteria.getMinHarga() > 0) {
            selection.append(" AND k.harga >= ?");
            argsList.add(String.valueOf(criteria.getMinHarga()));
        }

        if (criteria.getIdWilayah() != null && criteria.getIdWilayah() > 0) {
            selection.append(" AND k.id_wilayah = ?");
            argsList.add(String.valueOf(criteria.getIdWilayah()));
        }

        String[] args = argsList.toArray(new String[0]);
        List<Kost> result = queryKostList(selection.toString(), args);

        // Filter fasilitas jika dipilih
        if (criteria.getFasilitasIds() != null && !criteria.getFasilitasIds().isEmpty()) {
            List<Kost> filteredByFasilitas = new ArrayList<>();
            for (Kost kost : result) {
                populateFasilitasAndFoto(kost);
                boolean hasAll = true;
                List<Integer> existingIds = new ArrayList<>();
                for (Fasilitas f : kost.getListFasilitas()) {
                    existingIds.add(f.getIdFasilitas());
                }
                for (Integer reqId : criteria.getFasilitasIds()) {
                    if (!existingIds.contains(reqId)) {
                        hasAll = false;
                        break;
                    }
                }
                if (hasAll) {
                    filteredByFasilitas.add(kost);
                }
            }
            return filteredByFasilitas;
        }

        return result;
    }

    public long insert(Kost kost, List<Integer> fasilitasIds) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("id_wilayah", kost.getIdWilayah());
            values.put("nama_kost", kost.getNamaKost());
            values.put("alamat", kost.getAlamat());
            values.put("harga", kost.getHarga());
            values.put("tipe_kost", kost.getTipeKost() != null ? kost.getTipeKost().name() : TipeKost.CAMPUR.name());
            values.put("deskripsi", kost.getDeskripsi());
            values.put("no_whatsapp", kost.getNoWhatsapp());
            values.put("latitude", kost.getLatitude());
            values.put("status", kost.getStatus() != null ? kost.getStatus().name() : StatusKost.TERSEDIA.name());
            values.put("provinsi", kost.getProvinsi());
            values.put("kota", kost.getKota());
            values.put("ukuran_kamar", kost.getUkuranKamar());
            values.put("total_kamar", kost.getTotalKamar());
            values.put("kamar_tersedia", kost.getKamarTersedia());

            long idKost = db.insert("kost", null, values);
            if (idKost != -1 && fasilitasIds != null) {
                for (int idFasilitas : fasilitasIds) {
                    ContentValues fv = new ContentValues();
                    fv.put("id_kost", idKost);
                    fv.put("id_fasilitas", idFasilitas);
                    db.insert("kost_fasilitas", null, fv);
                }
            }
            db.setTransactionSuccessful();
            return idKost;
        } finally {
            db.endTransaction();
        }
    }

    public boolean update(Kost kost, List<Integer> fasilitasIds) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("id_wilayah", kost.getIdWilayah());
            values.put("nama_kost", kost.getNamaKost());
            values.put("alamat", kost.getAlamat());
            values.put("harga", kost.getHarga());
            values.put("tipe_kost", kost.getTipeKost().name());
            values.put("deskripsi", kost.getDeskripsi());
            values.put("no_whatsapp", kost.getNoWhatsapp());
            values.put("latitude", kost.getLatitude());
            values.put("longitude", kost.getLongitude());
            values.put("status", kost.getStatus().name());
            values.put("provinsi", kost.getProvinsi());
            values.put("kota", kost.getKota());
            values.put("ukuran_kamar", kost.getUkuranKamar());
            values.put("total_kamar", kost.getTotalKamar());
            values.put("kamar_tersedia", kost.getKamarTersedia());

            int rows = db.update("kost", values, "id_kost = ?", new String[]{String.valueOf(kost.getIdKost())});

            if (fasilitasIds != null) {
                db.delete("kost_fasilitas", "id_kost = ?", new String[]{String.valueOf(kost.getIdKost())});
                for (int idF : fasilitasIds) {
                    ContentValues fv = new ContentValues();
                    fv.put("id_kost", kost.getIdKost());
                    fv.put("id_fasilitas", idF);
                    db.insert("kost_fasilitas", null, fv);
                }
            }
            db.setTransactionSuccessful();
            return rows > 0;
        } finally {
            db.endTransaction();
        }
    }

    public boolean updateStatus(int idKost, StatusKost status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status.name());
        int rows = db.update("kost", values, "id_kost = ?", new String[]{String.valueOf(idKost)});
        return rows > 0;
    }

    public int countByStatus(StatusKost status) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM kost WHERE status = ?", new String[]{status.name()});
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public int countTotal() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM kost WHERE status != ?", new String[]{StatusKost.TIDAK_AKTIF.name()});
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    private List<Kost> queryKostList(String selection, String[] args) {
        List<Kost> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT k.*, w.kecamatan, w.kelurahan FROM kost k " +
                "LEFT JOIN wilayah w ON k.id_wilayah = w.id_wilayah " +
                (selection != null ? "WHERE " + selection : "") +
                " ORDER BY k.id_kost DESC";

        Cursor cursor = db.rawQuery(sql, args);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Kost kost = cursorToKost(cursor);
                populateFasilitasAndFoto(kost);
                list.add(kost);
            }
            cursor.close();
        }
        return list;
    }

    private Kost cursorToKost(Cursor cursor) {
        Kost k = new Kost();
        k.setIdKost(cursor.getInt(cursor.getColumnIndexOrThrow("id_kost")));
        k.setIdWilayah(cursor.getInt(cursor.getColumnIndexOrThrow("id_wilayah")));
        k.setNamaKost(cursor.getString(cursor.getColumnIndexOrThrow("nama_kost")));
        k.setAlamat(cursor.getString(cursor.getColumnIndexOrThrow("alamat")));
        k.setHarga(cursor.getDouble(cursor.getColumnIndexOrThrow("harga")));
        k.setTipeKost(TipeKost.fromString(cursor.getString(cursor.getColumnIndexOrThrow("tipe_kost"))));
        k.setDeskripsi(cursor.getString(cursor.getColumnIndexOrThrow("deskripsi")));
        k.setNoWhatsapp(cursor.getString(cursor.getColumnIndexOrThrow("no_whatsapp")));
        k.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")));
        k.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")));
        k.setStatus(StatusKost.fromString(cursor.getString(cursor.getColumnIndexOrThrow("status"))));
        k.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
        k.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow("updated_at")));

        int colKel = cursor.getColumnIndex("kelurahan");
        if (colKel != -1 && !cursor.isNull(colKel)) k.setKelurahan(cursor.getString(colKel));
        int colKec = cursor.getColumnIndex("kecamatan");
        if (colKec != -1 && !cursor.isNull(colKec)) k.setKecamatan(cursor.getString(colKec));

        int colProv = cursor.getColumnIndex("provinsi");
        if (colProv != -1 && !cursor.isNull(colProv)) k.setProvinsi(cursor.getString(colProv));
        int colKota = cursor.getColumnIndex("kota");
        if (colKota != -1 && !cursor.isNull(colKota)) k.setKota(cursor.getString(colKota));
        int colUkuran = cursor.getColumnIndex("ukuran_kamar");
        if (colUkuran != -1 && !cursor.isNull(colUkuran)) k.setUkuranKamar(cursor.getString(colUkuran));
        int colTotal = cursor.getColumnIndex("total_kamar");
        if (colTotal != -1 && !cursor.isNull(colTotal)) k.setTotalKamar(cursor.getInt(colTotal));
        int colTersedia = cursor.getColumnIndex("kamar_tersedia");
        if (colTersedia != -1 && !cursor.isNull(colTersedia)) k.setKamarTersedia(cursor.getInt(colTersedia));

        return k;
    }

    private void populateFasilitasAndFoto(Kost kost) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Populate Fasilitas
        String sqlF = "SELECT f.id_fasilitas, f.nama_fasilitas FROM fasilitas f " +
                "INNER JOIN kost_fasilitas kf ON f.id_fasilitas = kf.id_fasilitas " +
                "WHERE kf.id_kost = ?";
        Cursor curF = db.rawQuery(sqlF, new String[]{String.valueOf(kost.getIdKost())});
        List<Fasilitas> fList = new ArrayList<>();
        if (curF != null) {
            while (curF.moveToNext()) {
                fList.add(new Fasilitas(
                        curF.getInt(curF.getColumnIndexOrThrow("id_fasilitas")),
                        curF.getString(curF.getColumnIndexOrThrow("nama_fasilitas"))
                ));
            }
            curF.close();
        }
        kost.setListFasilitas(fList);

        // Populate Foto
        Cursor curFoto = db.query("foto_kost", null, "id_kost = ?", new String[]{String.valueOf(kost.getIdKost())}, null, null, "is_thumbnail DESC");
        List<FotoKost> fotoList = new ArrayList<>();
        if (curFoto != null) {
            while (curFoto.moveToNext()) {
                FotoKost fk = new FotoKost(
                        curFoto.getInt(curFoto.getColumnIndexOrThrow("id_foto")),
                        curFoto.getInt(curFoto.getColumnIndexOrThrow("id_kost")),
                        curFoto.getString(curFoto.getColumnIndexOrThrow("nama_file")),
                        curFoto.getString(curFoto.getColumnIndexOrThrow("path_file")),
                        curFoto.getInt(curFoto.getColumnIndexOrThrow("is_thumbnail")) == 1
                );
                fotoList.add(fk);
                if (fk.isThumbnail() && (kost.getThumbnailPath() == null || kost.getThumbnailPath().isEmpty())) {
                    kost.setThumbnailPath(fk.getPathFile());
                }
            }
            curFoto.close();
        }
        kost.setListFoto(fotoList);
    }
}
