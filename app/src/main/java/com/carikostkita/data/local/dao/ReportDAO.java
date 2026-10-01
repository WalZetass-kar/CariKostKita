package com.carikostkita.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.carikostkita.data.local.DatabaseHelper;
import com.carikostkita.data.model.KostReport;
import com.carikostkita.data.model.ReportStatus;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {
    private final DatabaseHelper dbHelper;

    public ReportDAO(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insert(KostReport report) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id_kost", report.getIdKost());
        values.put("id_reporter", report.getIdReporter());
        values.put("id_pemilik", report.getIdPemilik());
        values.put("kategori_laporan", report.getKategoriLaporan());
        values.put("deskripsi", report.getDeskripsi());
        values.put("status", report.getStatus() != null ? report.getStatus().name() : ReportStatus.BARU.name());
        values.put("tindakan_admin", report.getTindakanAdmin());
        return db.insert("kost_report", null, values);
    }

    public List<KostReport> findAllReports() {
        List<KostReport> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT r.*, k.nama_kost, u1.nama AS reporter_name, u2.nama AS pemilik_name " +
                "FROM kost_report r " +
                "LEFT JOIN kost k ON r.id_kost = k.id_kost " +
                "LEFT JOIN users u1 ON r.id_reporter = u1.id_user " +
                "LEFT JOIN users u2 ON r.id_pemilik = u2.id_user " +
                "ORDER BY r.id_report DESC";
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToReport(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public KostReport findById(int idReport) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT r.*, k.nama_kost, u1.nama AS reporter_name, u2.nama AS pemilik_name " +
                "FROM kost_report r " +
                "LEFT JOIN kost k ON r.id_kost = k.id_kost " +
                "LEFT JOIN users u1 ON r.id_reporter = u1.id_user " +
                "LEFT JOIN users u2 ON r.id_pemilik = u2.id_user " +
                "WHERE r.id_report = ?";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idReport)});
        KostReport report = null;
        if (cursor != null && cursor.moveToFirst()) {
            report = cursorToReport(cursor);
            cursor.close();
        }
        return report;
    }

    public boolean updateStatus(int idReport, ReportStatus status, String tindakanAdmin) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status.name());
        if (tindakanAdmin != null) {
            values.put("tindakan_admin", tindakanAdmin);
        }
        values.put("updated_at", "CURRENT_TIMESTAMP");
        int rows = db.update("kost_report", values, "id_report = ?", new String[]{String.valueOf(idReport)});
        return rows > 0;
    }

    public int getPendingReportCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM kost_report WHERE status = ? OR status = ?",
                new String[]{ReportStatus.BARU.name(), ReportStatus.DITINJAU.name()});
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    private KostReport cursorToReport(Cursor cursor) {
        KostReport r = new KostReport();
        r.setIdReport(cursor.getInt(cursor.getColumnIndexOrThrow("id_report")));
        r.setIdKost(cursor.getInt(cursor.getColumnIndexOrThrow("id_kost")));
        r.setIdReporter(cursor.getInt(cursor.getColumnIndexOrThrow("id_reporter")));
        r.setIdPemilik(cursor.getInt(cursor.getColumnIndexOrThrow("id_pemilik")));
        r.setKategoriLaporan(cursor.getString(cursor.getColumnIndexOrThrow("kategori_laporan")));
        r.setDeskripsi(cursor.getString(cursor.getColumnIndexOrThrow("deskripsi")));
        r.setStatus(ReportStatus.fromString(cursor.getString(cursor.getColumnIndexOrThrow("status"))));
        r.setTindakanAdmin(cursor.getString(cursor.getColumnIndexOrThrow("tindakan_admin")));
        r.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
        r.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow("updated_at")));

        int colKost = cursor.getColumnIndex("nama_kost");
        if (colKost != -1 && !cursor.isNull(colKost)) {
            r.setNamaKost(cursor.getString(colKost));
        }

        int colReporter = cursor.getColumnIndex("reporter_name");
        if (colReporter != -1 && !cursor.isNull(colReporter)) {
            r.setNamaReporter(cursor.getString(colReporter));
        }

        int colPemilik = cursor.getColumnIndex("pemilik_name");
        if (colPemilik != -1 && !cursor.isNull(colPemilik)) {
            r.setNamaPemilik(cursor.getString(colPemilik));
        }

        return r;
    }
}
