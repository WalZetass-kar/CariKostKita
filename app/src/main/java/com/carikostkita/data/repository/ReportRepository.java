package com.carikostkita.data.repository;

import com.carikostkita.util.ErrorMessages;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.carikostkita.data.model.KostReport;
import com.carikostkita.data.model.ReportStatus;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.SupabaseDbService;
import com.carikostkita.data.remote.dto.ActivityLogDto;
import com.carikostkita.data.remote.dto.KostDto;
import com.carikostkita.data.remote.dto.ReportDto;
import com.carikostkita.data.remote.dto.ReportUpdateDto;
import com.carikostkita.data.remote.dto.UserDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Response;

public class ReportRepository {
    private final SupabaseDbService dbService;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public ReportRepository(Context context) {
        this.dbService = SupabaseClient.getInstance().createService(SupabaseDbService.class);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void submitReport(KostReport report, String reporterName, DataCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                ReportDto dto = new ReportDto();
                dto.kostId = report.getKostId();
                dto.reporterId = report.getReporterId();
                dto.ownerId = report.getOwnerId();
                dto.kategoriLaporan = report.getKategoriLaporan();
                dto.deskripsi = report.getDeskripsi();
                dto.status = report.getStatus() != null ? report.getStatus().name() : ReportStatus.BARU.name();
                dto.tindakanAdmin = report.getTindakanAdmin();

                Response<List<ReportDto>> res = dbService.insertReport(dto).execute();
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    // Log ke activity_logs
                    ActivityLogDto log = new ActivityLogDto(
                            report.getReporterId(),
                            reporterName != null ? reporterName : "Pengguna",
                            "LAPORAN_MASUK",
                            "Pengguna melaporkan kost #" + report.getKostId() + " (" + report.getKategoriLaporan() + ")",
                            "REPORT",
                            res.body().get(0).id
                    );
                    try {
                        dbService.insertLog(log).execute();
                    } catch (Exception ignored) {}

                    mainHandler.post(() -> callback.onSuccess(1L));
                } else {
                    mainHandler.post(() -> callback.onError("Gagal mengirim laporan ke server"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(ErrorMessages.fromException("Kesalahan", e)));
            }
        });
    }

    public void getAllReports(DataCallback<List<KostReport>> callback) {
        executorService.execute(() -> {
            try {
                Response<List<ReportDto>> res = dbService.getReports(new HashMap<>(), "*", "created_at.desc").execute();
                if (res.isSuccessful() && res.body() != null) {
                    List<KostReport> list = new ArrayList<>();
                    for (ReportDto d : res.body()) {
                        KostReport r = new KostReport();
                        r.setId(d.id);
                        r.setKostId(d.kostId);
                        r.setReporterId(d.reporterId);
                        r.setOwnerId(d.ownerId);
                        r.setKategoriLaporan(d.kategoriLaporan);
                        r.setDeskripsi(d.deskripsi);
                        r.setStatus(ReportStatus.fromString(d.status));
                        r.setTindakanAdmin(d.tindakanAdmin);
                        r.setCreatedAt(d.createdAt);
                        r.setUpdatedAt(d.updatedAt);
                        list.add(r);
                    }

                    // Populate nama kost & nama user
                    populateDisplayNames(list);

                    mainHandler.post(() -> callback.onSuccess(list));
                } else {
                    String msg = ErrorMessages.fromResponse("getReports", res);
                    mainHandler.post(() -> callback.onError(msg));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(ErrorMessages.fromException("Gagal memuat laporan", e)));
            }
        });
    }

    private void populateDisplayNames(List<KostReport> reports) {
        if (reports.isEmpty()) return;
        try {
            Map<String, String> kostNames = new HashMap<>();
            Response<List<KostDto>> kostRes = dbService.getKosts(new HashMap<>(), "id,nama_kost", null).execute();
            if (kostRes.isSuccessful() && kostRes.body() != null) {
                for (KostDto k : kostRes.body()) {
                    kostNames.put(k.id, k.namaKost);
                }
            }

            Map<String, String> userNames = new HashMap<>();
            Response<List<UserDto>> uRes = dbService.getUsers(new HashMap<>(), "id,nama", null).execute();
            if (uRes.isSuccessful() && uRes.body() != null) {
                for (UserDto u : uRes.body()) {
                    userNames.put(u.id, u.nama);
                }
            }

            for (KostReport r : reports) {
                if (kostNames.containsKey(r.getKostId())) {
                    r.setNamaKost(kostNames.get(r.getKostId()));
                }
                if (userNames.containsKey(r.getReporterId())) {
                    r.setNamaReporter(userNames.get(r.getReporterId()));
                }
                if (userNames.containsKey(r.getOwnerId())) {
                    r.setNamaPemilik(userNames.get(r.getOwnerId()));
                }
            }
        } catch (Exception ignored) {}
    }

    public void updateReportStatus(String idReport, ReportStatus status, String tindakanAdmin,
                                   String adminId, String adminName, DataCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                ReportUpdateDto dto = new ReportUpdateDto();
                dto.status = status.name();
                dto.tindakanAdmin = tindakanAdmin;

                Response<List<ReportDto>> res = dbService.updateReport("eq." + idReport, dto).execute();
                if (res.isSuccessful()) {
                    ActivityLogDto log = new ActivityLogDto(
                            adminId,
                            adminName != null ? adminName : "Admin",
                            "MODERASI_LAPORAN",
                            "Laporan #" + idReport + " diubah status menjadi " + status.getDisplayName() + (tindakanAdmin != null && !tindakanAdmin.isEmpty() ? " (" + tindakanAdmin + ")" : ""),
                            "REPORT",
                            idReport
                    );
                    try {
                        dbService.insertLog(log).execute();
                    } catch (Exception ignored) {}

                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    mainHandler.post(() -> callback.onError("Gagal mengupdate laporan di server"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(ErrorMessages.fromException("Kesalahan", e)));
            }
        });
    }

    public void updateReportStatus(int legacyId, ReportStatus status, String tindakanAdmin,
                                   int legacyAdminId, String adminName, DataCallback<Boolean> callback) {
        updateReportStatus(String.valueOf(legacyId), status, tindakanAdmin, String.valueOf(legacyAdminId), adminName, callback);
    }

    public void updateReportStatus(String idReport, ReportStatus status, DataCallback<Boolean> callback) {
        updateReportStatus(idReport, status, null, "", "Admin", callback);
    }

    public void updateReportStatus(int legacyId, ReportStatus status, DataCallback<Boolean> callback) {
        updateReportStatus(String.valueOf(legacyId), status, callback);
    }

    public void countPendingReports(DataCallback<Integer> callback) {
        getPendingCount(callback);
    }

    public void getPendingCount(DataCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                Map<String, String> filters = new HashMap<>();
                filters.put("status", "in.(" + ReportStatus.BARU.name() + "," + ReportStatus.DITINJAU.name() + ")");
                Response<List<ReportDto>> res = dbService.getReports(filters, "id", null).execute();
                if (res.isSuccessful() && res.body() != null) {
                    mainHandler.post(() -> callback.onSuccess(res.body().size()));
                } else {
                    mainHandler.post(() -> callback.onSuccess(0));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(ErrorMessages.fromException("Error", e)));
            }
        });
    }
}
