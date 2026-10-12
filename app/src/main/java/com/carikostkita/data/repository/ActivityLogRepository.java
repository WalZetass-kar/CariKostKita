package com.carikostkita.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.carikostkita.data.model.SystemActivityLog;
import com.carikostkita.data.remote.FastApiActivityLogService;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.SupabaseDbService;
import com.carikostkita.data.remote.dto.ActivityLogCreateRequest;
import com.carikostkita.data.remote.dto.ActivityLogDto;
import com.carikostkita.data.remote.dto.ActivityLogListResponse;
import com.carikostkita.util.SessionManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class ActivityLogRepository {
    private final SupabaseDbService legacyDbService;
    private final FastApiActivityLogService activityLogService;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final SessionManager sessionManager;

    public ActivityLogRepository(Context context) {
        this.sessionManager = new SessionManager(context);
        this.legacyDbService = SupabaseClient.getInstance().createService(SupabaseDbService.class);
        this.activityLogService = SupabaseClient.getInstance().createActivityLogService();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    private String bearerToken() {
        String token = sessionManager.getAccessToken();
        if (token != null && !token.isEmpty() && sessionManager.isTokenExpired()) {
            token = SupabaseClient.getInstance().refreshTokenBlocking(token);
        }
        return token == null || token.isEmpty() ? null : "Bearer " + token;
    }

    /** Resolve the domain bigint via the trusted RPC; never use the Auth UUID/hash as a row ID. */
    private String legacyActorId() throws IOException {
        Response<ResponseBody> response = legacyDbService.rpc("current_app_user_id", new HashMap<>()).execute();
        if (!response.isSuccessful() || response.body() == null) {
            close(response.body());
            close(response.errorBody());
            throw new IOException("Identity bridge is unavailable for the legacy activity-log fallback.");
        }
        String value = response.body().string().trim();
        try {
            long appUserId = Long.parseLong(value);
            if (appUserId <= 0) throw new NumberFormatException("non-positive user ID");
            return Long.toString(appUserId);
        } catch (NumberFormatException invalidMapping) {
            throw new IOException("Identity bridge did not return a valid numeric user ID.", invalidMapping);
        }
    }

    public void log(String ignoredUserId, String actorName, String actionType,
                    String description, String targetType, String targetId) {
        executorService.execute(() -> {
            String bearer = bearerToken();
            if (bearer == null) return;
            try {
                Response<ResponseBody> response = activityLogService.createMyLog(
                        bearer,
                        new ActivityLogCreateRequest(actionType, description, targetType, targetId, actorName)
                ).execute();
                close(response.body());
                close(response.errorBody());
                // Compatibility only when this FastAPI route is not deployed yet.
                if (response.code() == 404) {
                    legacyDbService.insertLog(new ActivityLogDto(
                            legacyActorId(), actorName, actionType, description, targetType, targetId
                    )).execute();
                }
            } catch (Exception ignored) {
                // Logs are best-effort and must not interrupt the user's action.
            }
        });
    }

    public void log(int legacyIdUser, String actorName, String actionType, String description,
                    String targetType, int legacyTargetId) {
        log(sessionManager.getUserUid(), actorName, actionType, description, targetType, String.valueOf(legacyTargetId));
    }

    public void logActivity(String idUser, String actionType, String description, String targetType, String targetId) {
        log(idUser, null, actionType, description, targetType, targetId);
    }

    public void logActivity(int legacyIdUser, String actionType, String description, String targetType, int legacyTargetId) {
        log(sessionManager.getUserUid(), null, actionType, description, targetType, String.valueOf(legacyTargetId));
    }

    public void logActivity(int legacyIdUser, String actionType, String description, String targetType, String targetId) {
        log(sessionManager.getUserUid(), null, actionType, description, targetType, targetId);
    }

    public void logActivity(String idUser, String actorName, String actionType, String description, String targetType, String targetId) {
        log(idUser, actorName, actionType, description, targetType, targetId);
    }

    public void logActivity(int legacyIdUser, String actorName, String actionType, String description, String targetType, String targetId) {
        log(sessionManager.getUserUid(), actorName, actionType, description, targetType, targetId);
    }

    public void logActivity(int legacyIdUser, String actorName, String actionType, String description, String targetType, int legacyTargetId) {
        log(sessionManager.getUserUid(), actorName, actionType, description, targetType, String.valueOf(legacyTargetId));
    }

    public void getAllLogs(DataCallback<List<SystemActivityLog>> callback) {
        executorService.execute(() -> {
            String bearer = bearerToken();
            if (bearer == null) {
                mainHandler.post(() -> callback.onError("Sesi berakhir. Silakan login kembali."));
                return;
            }
            try {
                Response<ActivityLogListResponse> response = activityLogService.listAdminLogs(bearer, 1, 500).execute();
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    postLogs(callback, response.body().data);
                    return;
                }
                if (response.code() != 404) {
                    String message = "Gagal memuat log aktivitas (HTTP " + response.code() + ").";
                    mainHandler.post(() -> callback.onError(message));
                    return;
                }
                // Legacy route remains as a deployment-compatibility fallback and still obeys Supabase RLS.
                Response<List<ActivityLogDto>> legacy = legacyDbService.getLogs("*", "created_at.desc").execute();
                if (legacy.isSuccessful() && legacy.body() != null) postLogs(callback, legacy.body());
                else mainHandler.post(() -> callback.onError("Gagal memuat log aktivitas dari sumber lama."));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Gagal memuat log aktivitas: " + e.getMessage()));
            }
        });
    }

    public void getTotalCount(DataCallback<Integer> callback) {
        executorService.execute(() -> {
            String bearer = bearerToken();
            if (bearer == null) {
                mainHandler.post(() -> callback.onError("Sesi berakhir. Silakan login kembali."));
                return;
            }
            try {
                Response<ActivityLogListResponse> response = activityLogService.listAdminLogs(bearer, 1, 1).execute();
                if (response.isSuccessful() && response.body() != null && response.body().pagination != null
                        && response.body().pagination.count != null) {
                    int count = response.body().pagination.count;
                    mainHandler.post(() -> callback.onSuccess(count));
                    return;
                }
                if (response.code() != 404) {
                    String message = "Gagal menghitung log aktivitas (HTTP " + response.code() + ").";
                    mainHandler.post(() -> callback.onError(message));
                    return;
                }
                Response<List<ActivityLogDto>> legacy = legacyDbService.getLogs("id", null).execute();
                if (legacy.isSuccessful() && legacy.body() != null) {
                    int count = legacy.body().size();
                    mainHandler.post(() -> callback.onSuccess(count));
                } else mainHandler.post(() -> callback.onError("Gagal menghitung log aktivitas dari sumber lama."));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    private void postLogs(DataCallback<List<SystemActivityLog>> callback, List<ActivityLogDto> dtos) {
        List<SystemActivityLog> list = new ArrayList<>();
        for (ActivityLogDto dto : dtos) {
            SystemActivityLog item = new SystemActivityLog(
                    dto.userId, dto.actorName, dto.actionType, dto.description, dto.targetType, dto.targetId
            );
            item.setIdLog(dto.id);
            item.setCreatedAt(dto.createdAt);
            list.add(item);
        }
        mainHandler.post(() -> callback.onSuccess(list));
    }

    private static void close(ResponseBody body) {
        if (body != null) body.close();
    }
}
