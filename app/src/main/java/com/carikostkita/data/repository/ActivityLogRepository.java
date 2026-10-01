package com.carikostkita.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.carikostkita.data.local.DatabaseHelper;
import com.carikostkita.data.local.dao.ActivityLogDAO;
import com.carikostkita.data.model.SystemActivityLog;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActivityLogRepository {
    private final ActivityLogDAO logDAO;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public ActivityLogRepository(Context context) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        this.logDAO = new ActivityLogDAO(dbHelper);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void log(int idUser, String actorName, String actionType, String description, String targetType, int targetId) {
        executorService.execute(() -> {
            try {
                logDAO.logAction(idUser, actorName, actionType, description, targetType, targetId);
            } catch (Exception ignored) {}
        });
    }

    public void logActivity(int idUser, String actionType, String description, String targetType, int targetId) {
        log(idUser, null, actionType, description, targetType, targetId);
    }

    public void logActivity(int idUser, String actorName, String actionType, String description, String targetType, int targetId) {
        log(idUser, actorName, actionType, description, targetType, targetId);
    }

    public void getAllLogs(DataCallback<List<SystemActivityLog>> callback) {
        executorService.execute(() -> {
            try {
                List<SystemActivityLog> list = logDAO.findAllLogs();
                mainHandler.post(() -> callback.onSuccess(list));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getTotalCount(DataCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                int count = logDAO.getTotalLogCount();
                mainHandler.post(() -> callback.onSuccess(count));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}
