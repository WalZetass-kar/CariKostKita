package com.carikostkita.data.repository;

import com.carikostkita.util.ErrorMessages;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.carikostkita.data.model.KostReport;
import com.carikostkita.data.model.ReportStatus;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.SupabaseDbService;
import com.carikostkita.data.remote.FastApiReportService;
import com.carikostkita.data.remote.FastApiAdminReportService;
import com.carikostkita.data.remote.dto.KostDto;
import com.carikostkita.data.remote.dto.ReportDto;
import com.carikostkita.data.remote.dto.ReportUpdateDto;
import com.carikostkita.data.remote.dto.UserDto;
import com.carikostkita.data.remote.dto.ReportCreateRequest;
import com.carikostkita.data.remote.dto.ReportResponse;
import com.carikostkita.data.remote.dto.AdminReportDto;
import com.carikostkita.data.remote.dto.AdminReportListResponse;
import com.carikostkita.util.SessionManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class ReportRepository {
    private final SupabaseDbService dbService;
    private final FastApiReportService reportService;
    private final FastApiAdminReportService adminReportService;
    private final ActivityLogRepository activityLogRepository;
    private final SessionManager sessionManager;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public ReportRepository(Context context) {
        this.dbService = SupabaseClient.getInstance().createService(SupabaseDbService.class);
        this.reportService = SupabaseClient.getInstance().createReportService();
        this.adminReportService = SupabaseClient.getInstance().createAdminReportService();
        this.activityLogRepository = new ActivityLogRepository(context);
        this.sessionManager = new SessionManager(context);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void submitReport(KostReport report, String reporterName, DataCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                String token = sessionManager.getAccessToken();
                if (token == null || token.isEmpty()) {
                    mainHandler.post(() -> callback.onError(ErrorMessages.SESSION_EXPIRED));
                    return;
                }
                if (sessionManager.isTokenExpired()) {
                    token = SupabaseClient.getInstance().refreshTokenBlocking(token);
                }
                if (token == null || token.isEmpty()) {
                    mainHandler.post(() -> callback.onError(ErrorMessages.SESSION_EXPIRED));
                    return;
                }
                long kostId = Long.parseLong(report.getKostId());
                ReportCreateRequest request = new ReportCreateRequest(
                        kostId, report.getKategoriLaporan(), report.getDeskripsi());
                Response<ReportResponse> res = reportService
                        .createReport("Bearer " + token, request).execute();
                if (res.isSuccessful() && res.body() != null && res.body().data != null) {
                    activityLogRepository.logActivity(
                            sessionManager.getUserUid(), reporterName != null ? reporterName : "Pengguna",
                            "LAPORAN_MASUK",
                            "Pengguna melaporkan kost #" + report.getKostId() + " (" + report.getKategoriLaporan() + ")",
                            "REPORT", String.valueOf(res.body().data.id));

                    long reportId = res.body().data.id;
                    mainHandler.post(() -> callback.onSuccess(reportId));
                } else {
                    mainHandler.post(() -> callback.onError(ErrorMessages.fromResponse("createReport", res)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(ErrorMessages.fromException("Kesalahan", e)));
            }
        });
    }

    public void getAllReports(DataCallback<List<KostReport>> callback) {
        executorService.execute(() -> {
            try {
                String token = sessionManager.getAccessToken();
                if (token != null && !token.isEmpty() && sessionManager.isTokenExpired()) {
                    token = SupabaseClient.getInstance().refreshTokenBlocking(token);
                }
                if (token != null && !token.isEmpty()) {
                    try {
                        List<KostReport> reports = new ArrayList<>();
                        boolean apiSupported = true;
                        boolean hasMore = false;
                        for (int page = 1; page <= 100; page++) {
                            Response<AdminReportListResponse> apiRes = adminReportService
                                    .listReports("Bearer " + token, page, 100).execute();
                            if (apiRes.isSuccessful() && apiRes.body() != null && apiRes.body().data != null) {
                                for (AdminReportDto dto : apiRes.body().data) {
                                    reports.add(mapAdminReport(dto));
                                }
                                hasMore = apiRes.body().pagination != null && apiRes.body().pagination.hasMore;
                                if (!hasMore) break;
                                continue;
                            }
                            // Only an absent FastAPI route is a deployment-compatibility signal.
                            // 503 means the API route exists but its database dependency failed;
                            // falling through would hide that failure and write via another path.
                            boolean canFallback = page == 1 && apiRes.code() == 404;
                            String error = ErrorMessages.fromResponse("listAdminReports", apiRes);
                            if (apiRes.errorBody() != null) apiRes.errorBody().close();
                            if (canFallback) {
                                apiSupported = false;
                                break;
                            }
                            mainHandler.post(() -> callback.onError(error));
                            return;
                        }
                        if (apiSupported && hasMore) {
                            mainHandler.post(() -> callback.onError("Antrean laporan terlalu besar untuk dimuat sekaligus."));
                            return;
                        }
                        if (apiSupported) {
                            mainHandler.post(() -> callback.onSuccess(reports));
                            return;
                        }
                    } catch (java.io.IOException apiUnavailable) {
                        // Keep the old RLS-scoped query until the FastAPI queue is deployed.
                    }
                }

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

    private KostReport mapAdminReport(AdminReportDto dto) {
        KostReport report = new KostReport();
        report.setId(dto.id);
        report.setKostId(dto.kostId);
        report.setReporterId(dto.reporterId);
        report.setKategoriLaporan(dto.reason);
        report.setDeskripsi(dto.description);
        report.setStatus(ReportStatus.fromString(dto.status));
        report.setTindakanAdmin(dto.adminAction);
        report.setCreatedAt(dto.createdAt);
        report.setUpdatedAt(dto.updatedAt);
        if (dto.kostName != null) report.setNamaKost(dto.kostName);
        if (dto.reporterName != null) report.setNamaReporter(dto.reporterName);
        return report;
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
                String token = sessionManager.getAccessToken();
                if (token != null && !token.isEmpty() && sessionManager.isTokenExpired()) {
                    token = SupabaseClient.getInstance().refreshTokenBlocking(token);
                }
                if (token != null && !token.isEmpty()) {
                    try {
                        Response<ResponseBody> apiRes = adminReportService.reviewReport(
                                "Bearer " + token,
                                idReport,
                                new com.carikostkita.data.remote.dto.ReportModerationRequest(
                                        status.name(), tindakanAdmin)
                        ).execute();
                        if (apiRes.isSuccessful()) {
                            closeBody(apiRes.body());
                            logModeration(adminId, adminName, idReport, status, tindakanAdmin);
                            mainHandler.post(() -> callback.onSuccess(true));
                            return;
                        }
                        // Do not bypass API/database failures via the legacy write path.
                        if (apiRes.code() != 404) {
                            String apiError = ErrorMessages.fromResponse("reviewReport", apiRes);
                            closeBody(apiRes.errorBody());
                            mainHandler.post(() -> callback.onError(apiError));
                            return;
                        }
                        closeBody(apiRes.errorBody());
                    } catch (java.io.IOException apiUnavailable) {
                        // The request may have reached FastAPI before the connection failed.
                        // Do not retry a moderation mutation through a second write path.
                        mainHandler.post(() -> callback.onError(
                                ErrorMessages.fromException("reviewReport", apiUnavailable)));
                        return;
                    }
                }

                ReportUpdateDto dto = new ReportUpdateDto();
                dto.status = status.name();
                // tindakan_admin is not a verified production column. Only the
                // status-only legacy write is retained as a temporary RLS fallback.

                Response<List<ReportDto>> res = dbService.updateReport("eq." + idReport, dto).execute();
                if (res.isSuccessful()) {
                    logModeration(adminId, adminName, idReport, status, tindakanAdmin);
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    mainHandler.post(() -> callback.onError("Gagal mengupdate laporan di server"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(ErrorMessages.fromException("Kesalahan", e)));
            }
        });
    }

    private void logModeration(String adminId, String adminName, String idReport,
                               ReportStatus status, String action) {
        activityLogRepository.logActivity(
                sessionManager.getUserUid(), adminName != null ? adminName : "Admin",
                "MODERASI_LAPORAN",
                "Laporan #" + idReport + " diubah status menjadi " + status.getDisplayName()
                        + (action != null && !action.isEmpty() ? " (" + action + ")" : ""),
                "REPORT", idReport);
    }

    private void closeBody(ResponseBody body) {
        if (body != null) body.close();
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
                String token = sessionManager.getAccessToken();
                if (token != null && !token.isEmpty() && sessionManager.isTokenExpired()) {
                    token = SupabaseClient.getInstance().refreshTokenBlocking(token);
                }
                if (token == null || token.isEmpty()) {
                    mainHandler.post(() -> callback.onError(ErrorMessages.SESSION_EXPIRED));
                    return;
                }

                Response<com.carikostkita.data.remote.dto.PendingReportCountResponse> apiRes =
                        adminReportService.readPendingCount("Bearer " + token).execute();
                if (apiRes.isSuccessful() && apiRes.body() != null && apiRes.body().pendingCount != null) {
                    mainHandler.post(() -> callback.onSuccess(apiRes.body().pendingCount));
                    return;
                }
                if (apiRes.code() != 404) {
                    String error = ErrorMessages.fromResponse("getPendingReportCount", apiRes);
                    if (apiRes.errorBody() != null) apiRes.errorBody().close();
                    mainHandler.post(() -> callback.onError(error));
                    return;
                }
                if (apiRes.errorBody() != null) apiRes.errorBody().close();

                // Compatibility for deployments that do not yet expose this read-only count route.
                Map<String, String> filters = new HashMap<>();
                filters.put("status", "in.(" + ReportStatus.BARU.name() + "," + ReportStatus.DITINJAU.name() + ")");
                Response<List<ReportDto>> res = dbService.getReports(filters, "id", null).execute();
                if (res.isSuccessful() && res.body() != null) {
                    mainHandler.post(() -> callback.onSuccess(res.body().size()));
                } else {
                    String error = ErrorMessages.fromResponse("getPendingReportCount", res);
                    mainHandler.post(() -> callback.onError(error));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(ErrorMessages.fromException("Error", e)));
            }
        });
    }
}
