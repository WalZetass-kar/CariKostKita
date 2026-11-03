package com.carikostkita.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.carikostkita.data.model.RoommatePost;
import com.carikostkita.data.remote.FastApiKostService;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.dto.RoommateListResponse;
import com.carikostkita.data.remote.dto.RoommateResponse;
import com.carikostkita.util.ErrorMessages;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Response;

/**
 * Repository handling Roommate Matchmaker operations.
 */
public class RoommateRepository {

    private static final String TAG = "RoommateRepo";

    private final FastApiKostService kostService;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public RoommateRepository(Context context) {
        this.kostService = SupabaseClient.getInstance().createKostService();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void getRoommates(String campus, String gender, Integer maxBudget, String query,
                             DataCallback<List<RoommatePost>> callback) {
        executor.execute(() -> {
            try {
                String cleanCampus = (campus == null || campus.equalsIgnoreCase("Semua")) ? null : campus;
                String cleanGender = (gender == null || gender.equalsIgnoreCase("Semua")) ? null : gender;
                Response<RoommateListResponse> res = kostService.listRoommates(
                        cleanCampus, cleanGender, maxBudget, query, 1, 50).execute();

                if (res.isSuccessful() && res.body() != null && res.body().data != null) {
                    List<RoommatePost> list = res.body().data;
                    mainHandler.post(() -> callback.onSuccess(list));
                } else {
                    mainHandler.post(() -> callback.onError("Gagal memuat teman sekamar."));
                }
            } catch (Exception e) {
                Log.e(TAG, "getRoommates error", e);
                mainHandler.post(() -> callback.onError(ErrorMessages.fromException("getRoommates", e)));
            }
        });
    }

    public void createRoommatePost(RoommatePost post, DataCallback<RoommatePost> callback) {
        executor.execute(() -> {
            try {
                Response<RoommateResponse> res = kostService.createRoommate(post).execute();
                if (res.isSuccessful() && res.body() != null && res.body().data != null) {
                    RoommatePost created = res.body().data;
                    mainHandler.post(() -> callback.onSuccess(created));
                } else {
                    mainHandler.post(() -> callback.onError("Gagal menerbitkan postingan teman sekamar."));
                }
            } catch (Exception e) {
                Log.e(TAG, "createRoommate error", e);
                mainHandler.post(() -> callback.onError(ErrorMessages.fromException("createRoommate", e)));
            }
        });
    }
}
