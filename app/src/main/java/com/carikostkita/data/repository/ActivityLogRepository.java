package com.carikostkita.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.carikostkita.data.model.SystemActivityLog;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.SupabaseDbService;
import com.carikostkita.data.remote.dto.ActivityLogDto;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Response;

public class ActivityLogRepository {
    private final SupabaseDbService dbService;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final com.carikostkita.util.SessionManager sessionManager;

    public ActivityLogRepository(Context context) {
        this.sessionManager = new com.carikostkita.util.SessionManager(context);
        this.dbService = SupabaseClient.getInstance().createService(SupabaseDbService.class);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void log(String idUser, String actorName, String actionType, String description, String targetType, String targetId) {
        executorService.execute(() -> {
            try {
                ActivityLogDto dto = new ActivityLogDto(idUser, actorName, actionType, description, targetType, targetId);
                dbService.insertLog(dto).execute();
            } catch (Exception ignored) {}
        });
    }

    public void log(int legacyIdUser, String actorName, String actionType, String description, String targetType, int legacyTargetId) {
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
            try {
                Response<List<ActivityLogDto>> res = dbService.getLogs("*", "created_at.desc").execute();
                if (res.isSuccessful() && res.body() != null) {
                    List<SystemActivityLog> list = new ArrayList<>();
                    for (ActivityLogDto d : res.body()) {
                        SystemActivityLog item = new SystemActivityLog(
                                d.userId,
                                d.actorName,
                                d.actionType,
                                d.description,
                                d.targetType,
                                d.targetId
                        );
                        item.setIdLog(d.id);
                        item.setCreatedAt(d.createdAt);
                        list.add(item);
                    }
                    mainHandler.post(() -> callback.onSuccess(list));
                } else {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Gagal memuat log: " + e.getMessage()));
            }
        });
    }

    public void getTotalCount(DataCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                Response<List<ActivityLogDto>> res = dbService.getLogs("id", null).execute();
                if (res.isSuccessful() && res.body() != null) {
                    mainHandler.post(() -> callback.onSuccess(res.body().size()));
                } else {
                    mainHandler.post(() -> callback.onSuccess(0));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }
}
