package com.carikostkita.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.carikostkita.data.local.DatabaseHelper;
import com.carikostkita.data.local.dao.ActivityLogDAO;
import com.carikostkita.data.local.dao.ReportDAO;
import com.carikostkita.data.model.KostReport;
import com.carikostkita.data.model.ReportStatus;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportRepository {
    private final ReportDAO reportDAO;
    private final ActivityLogDAO activityLogDAO;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public ReportRepository(Context context) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        this.reportDAO = new ReportDAO(dbHelper);
        this.activityLogDAO = new ActivityLogDAO(dbHelper);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void submitReport(KostReport report, String reporterName, DataCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                long id = reportDAO.insert(report);
                if (id > 0) {
                    activityLogDAO.logAction(
                            report.getIdReporter(),
                            reporterName != null ? reporterName : "Pengguna #" + report.getIdReporter(),
                            "LAPORAN_MASUK",
                            "Pengguna melaporkan kost #" + report.getIdKost() + " (" + report.getKategoriLaporan() + ")",
                            "REPORT",
                            (int) id
                    );
                    mainHandler.post(() -> callback.onSuccess(id));
                } else {
                    mainHandler.post(() -> callback.onError("Gagal mengirim laporan"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getAllReports(DataCallback<List<KostReport>> callback) {
        executorService.execute(() -> {
            try {
                List<KostReport> list = reportDAO.findAllReports();
                mainHandler.post(() -> callback.onSuccess(list));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void updateReportStatus(int idReport, ReportStatus status, String tindakanAdmin, int adminId, String adminName, DataCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                boolean ok = reportDAO.updateStatus(idReport, status, tindakanAdmin);
                if (ok) {
                    activityLogDAO.logAction(
                            adminId,
                            adminName != null ? adminName : "Admin",
                            "MODERASI_LAPORAN",
                            "Laporan #" + idReport + " diubah status menjadi " + status.getDisplayName() + (tindakanAdmin != null && !tindakanAdmin.isEmpty() ? " (" + tindakanAdmin + ")" : ""),
                            "REPORT",
                            idReport
                    );
                }
                mainHandler.post(() -> callback.onSuccess(ok));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void updateReportStatus(int idReport, ReportStatus status, DataCallback<Boolean> callback) {
        updateReportStatus(idReport, status, null, 0, "Admin", callback);
    }

    public void countPendingReports(DataCallback<Integer> callback) {
        getPendingCount(callback);
    }

    public void getPendingCount(DataCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                int count = reportDAO.getPendingReportCount();
                mainHandler.post(() -> callback.onSuccess(count));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}
