package com.carikostkita.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.carikostkita.data.local.DatabaseHelper;
import com.carikostkita.data.model.Fasilitas;
import com.carikostkita.data.model.FotoKost;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.KostFilterCriteria;
import com.carikostkita.data.model.KostVerificationStatus;
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
        return queryKostList("k.status != ? AND k.verification_status = ?",
                new String[]{StatusKost.TIDAK_AKTIF.name(), KostVerificationStatus.APPROVED.name()},
                "k.id_kost DESC");
    }

    public List<Kost> findAllForAdmin() {
        return queryKostList(null, null, "k.id_kost DESC");
    }

    public List<Kost> findPendingKosts() {
        return queryKostList("k.verification_status = ? OR k.verification_status = ?",
                new String[]{KostVerificationStatus.PENDING.name(), KostVerificationStatus.REVISION_REQUIRED.name()},
                "k.id_kost DESC");
    }

    public List<Kost> findByPemilik(int idPemilik) {
        return queryKostList("k.id_pemilik = ?", new String[]{String.valueOf(idPemilik)}, "k.id_kost DESC");
    }

    public boolean updateVerificationStatus(int idKost, KostVerificationStatus status, String catatanRevisi) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("verification_status", status.name());
        if (catatanRevisi != null) {
            values.put("catatan_revisi", catatanRevisi);
        }
        values.put("updated_at", "CURRENT_TIMESTAMP");
        int rows = db.update("kost", values, "id_kost = ?", new String[]{String.valueOf(idKost)});
        return rows > 0;
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
        String selection = "k.status != ? AND k.verification_status = ? AND (LOWER(k.nama_kost) LIKE ? OR LOWER(k.alamat) LIKE ? OR LOWER(w.kelurahan) LIKE ? OR LOWER(w.kecamatan) LIKE ? OR LOWER(k.patokan) LIKE ?)";
        String[] args = new String[]{
                StatusKost.TIDAK_AKTIF.name(),
                KostVerificationStatus.APPROVED.name(),
                pattern, pattern, pattern, pattern, pattern
        };
        return queryKostList(selection, args, "k.id_kost DESC");
    }

    public List<Kost> filter(KostFilterCriteria criteria) {
        if (criteria == null) return findAllActive();

        StringBuilder selection = new StringBuilder("k.status != ? AND k.verification_status = ?");
        List<String> argsList = new ArrayList<>();
        argsList.add(StatusKost.TIDAK_AKTIF.name());
        argsList.add(KostVerificationStatus.APPROVED.name());

        if (criteria.getKeyword() != null && !criteria.getKeyword().trim().isEmpty()) {
            selection.append(" AND (LOWER(k.nama_kost) LIKE ? OR LOWER(k.alamat) LIKE ? OR LOWER(w.kelurahan) LIKE ? OR LOWER(k.patokan) LIKE ?)");
            String p = "%" + criteria.getKeyword().trim().toLowerCase() + "%";
            argsList.add(p);
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

        String orderBy = "k.id_kost DESC";
        if ("TERMURAH".equalsIgnoreCase(criteria.getSortBy())) {
            orderBy = "k.harga ASC, k.id_kost DESC";
        } else if ("TERMAHAL".equalsIgnoreCase(criteria.getSortBy())) {
            orderBy = "k.harga DESC, k.id_kost DESC";
        }

        String[] args = argsList.toArray(new String[0]);
        List<Kost> result = queryKostList(selection.toString(), args, orderBy);

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
            values.put("id_pemilik", kost.getIdPemilik() > 0 ? kost.getIdPemilik() : 2);
            values.put("nama_kost", kost.getNamaKost());
            values.put("alamat", kost.getAlamat());
            values.put("patokan", kost.getPatokan());
            values.put("harga", kost.getHarga());
            values.put("tipe_kost", kost.getTipeKost() != null ? kost.getTipeKost().name() : TipeKost.CAMPUR.name());
            values.put("deskripsi", kost.getDeskripsi());
            values.put("no_whatsapp", kost.getNoWhatsapp());
            values.put("latitude", kost.getLatitude());
            values.put("longitude", kost.getLongitude());
            values.put("status", kost.getStatus() != null ? kost.getStatus().name() : StatusKost.TERSEDIA.name());
            values.put("verification_status", kost.getVerificationStatus() != null ? kost.getVerificationStatus().name() : KostVerificationStatus.PENDING.name());
            values.put("catatan_revisi", kost.getCatatanRevisi());
            values.put("location_verification", kost.getLocationVerification());
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
            if (kost.getIdPemilik() > 0) values.put("id_pemilik", kost.getIdPemilik());
            values.put("nama_kost", kost.getNamaKost());
            values.put("alamat", kost.getAlamat());
            values.put("patokan", kost.getPatokan());
            values.put("harga", kost.getHarga());
            values.put("tipe_kost", kost.getTipeKost().name());
            values.put("deskripsi", kost.getDeskripsi());
            values.put("no_whatsapp", kost.getNoWhatsapp());
            values.put("latitude", kost.getLatitude());
            values.put("longitude", kost.getLongitude());
            values.put("status", kost.getStatus().name());
            values.put("verification_status", kost.getVerificationStatus() != null ? kost.getVerificationStatus().name() : KostVerificationStatus.PENDING.name());
            values.put("catatan_revisi", kost.getCatatanRevisi());
            values.put("location_verification", kost.getLocationVerification());
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
                    fv.put("id_kost", idKostFromEntity(kost));
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

    private int idKostFromEntity(Kost kost) {
        return kost.getIdKost();
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

    public int countByPemilik(int idPemilik) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM kost WHERE id_pemilik = ? AND status != ?",
                new String[]{String.valueOf(idPemilik), StatusKost.TIDAK_AKTIF.name()});
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public int countPendingVerification() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM kost WHERE verification_status = ? OR verification_status = ?",
                new String[]{KostVerificationStatus.PENDING.name(), KostVerificationStatus.REVISION_REQUIRED.name()});
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public int countByPemilikAndVerification(int idPemilik, KostVerificationStatus status) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM kost WHERE id_pemilik = ? AND verification_status = ?",
                new String[]{String.valueOf(idPemilik), status.name()});
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public int countByPemilikAndStatus(int idPemilik, StatusKost status) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM kost WHERE id_pemilik = ? AND status = ? AND verification_status = ?",
                new String[]{String.valueOf(idPemilik), status.name(), KostVerificationStatus.APPROVED.name()});
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    // --- Foto Kost Multiple Photos Management ---
    public long insertFoto(int idKost, String namaFile, String pathFile, boolean isThumbnail) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (isThumbnail) {
            ContentValues resetCv = new ContentValues();
            resetCv.put("is_thumbnail", 0);
            db.update("foto_kost", resetCv, "id_kost = ?", new String[]{String.valueOf(idKost)});
        }
        ContentValues cv = new ContentValues();
        cv.put("id_kost", idKost);
        cv.put("nama_file", namaFile != null ? namaFile : "foto_kost");
        cv.put("path_file", pathFile);
        cv.put("is_thumbnail", isThumbnail ? 1 : 0);
        return db.insert("foto_kost", null, cv);
    }

    public boolean deleteFoto(int idFoto) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete("foto_kost", "id_foto = ?", new String[]{String.valueOf(idFoto)});
        return rows > 0;
    }

    public boolean setThumbnailFoto(int idKost, int idFoto) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues reset = new ContentValues();
            reset.put("is_thumbnail", 0);
            db.update("foto_kost", reset, "id_kost = ?", new String[]{String.valueOf(idKost)});

            ContentValues setThumb = new ContentValues();
            setThumb.put("is_thumbnail", 1);
            int rows = db.update("foto_kost", setThumb, "id_foto = ?", new String[]{String.valueOf(idFoto)});
            db.setTransactionSuccessful();
            return rows > 0;
        } finally {
            db.endTransaction();
        }
    }

    public List<FotoKost> getFotosByKost(int idKost) {
        List<FotoKost> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor curFoto = db.query("foto_kost", null, "id_kost = ?", new String[]{String.valueOf(idKost)}, null, null, "is_thumbnail DESC, id_foto ASC");
        if (curFoto != null) {
            while (curFoto.moveToNext()) {
                list.add(new FotoKost(
                        curFoto.getInt(curFoto.getColumnIndexOrThrow("id_foto")),
                        curFoto.getInt(curFoto.getColumnIndexOrThrow("id_kost")),
                        curFoto.getString(curFoto.getColumnIndexOrThrow("nama_file")),
                        curFoto.getString(curFoto.getColumnIndexOrThrow("path_file")),
                        curFoto.getInt(curFoto.getColumnIndexOrThrow("is_thumbnail")) == 1
                ));
            }
            curFoto.close();
        }
        return list;
    }

    public void replaceKostFotos(int idKost, List<FotoKost> fotos) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("foto_kost", "id_kost = ?", new String[]{String.valueOf(idKost)});
            if (fotos != null) {
                boolean hasThumb = false;
                for (FotoKost f : fotos) {
                    if (f.isThumbnail()) {
                        hasThumb = true;
                        break;
                    }
                }
                for (int i = 0; i < fotos.size(); i++) {
                    FotoKost f = fotos.get(i);
                    ContentValues cv = new ContentValues();
                    cv.put("id_kost", idKost);
                    cv.put("nama_file", f.getNamaFile() != null ? f.getNamaFile() : "foto_" + i);
                    cv.put("path_file", f.getPathFile());
                    // If no explicit thumbnail set, default first photo as thumbnail
                    boolean isThumb = f.isThumbnail() || (!hasThumb && i == 0);
                    cv.put("is_thumbnail", isThumb ? 1 : 0);
                    db.insert("foto_kost", null, cv);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private List<Kost> queryKostList(String selection, String[] args, String orderBy) {
        List<Kost> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT k.*, w.kecamatan, w.kelurahan FROM kost k " +
                "LEFT JOIN wilayah w ON k.id_wilayah = w.id_wilayah " +
                (selection != null ? "WHERE " + selection : "") +
                (orderBy != null ? " ORDER BY " + orderBy : " ORDER BY k.id_kost DESC");

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

        int colPemilik = cursor.getColumnIndex("id_pemilik");
        if (colPemilik != -1 && !cursor.isNull(colPemilik)) k.setIdPemilik(cursor.getInt(colPemilik));

        int colPatokan = cursor.getColumnIndex("patokan");
        if (colPatokan != -1 && !cursor.isNull(colPatokan)) k.setPatokan(cursor.getString(colPatokan));

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

        int colVerif = cursor.getColumnIndex("verification_status");
        if (colVerif != -1 && !cursor.isNull(colVerif)) {
            k.setVerificationStatus(KostVerificationStatus.fromString(cursor.getString(colVerif)));
        }

        int colCat = cursor.getColumnIndex("catatan_revisi");
        if (colCat != -1 && !cursor.isNull(colCat)) {
            k.setCatatanRevisi(cursor.getString(colCat));
        }

        int colLoc = cursor.getColumnIndex("location_verification");
        if (colLoc != -1 && !cursor.isNull(colLoc)) {
            k.setLocationVerification(cursor.getString(colLoc));
        }

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
        kost.setListFoto(getFotosByKost(kost.getIdKost()));
        if (!kost.getListFoto().isEmpty()) {
            for (FotoKost fk : kost.getListFoto()) {
                if (fk.isThumbnail()) {
                    kost.setThumbnailPath(fk.getPathFile());
                    break;
                }
            }
            if (kost.getThumbnailPath() == null || kost.getThumbnailPath().isEmpty()) {
                kost.setThumbnailPath(kost.getListFoto().get(0).getPathFile());
            }
        }
    }
}
