package com.carikostkita.notifications;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.SupabaseDbService;
import com.carikostkita.data.remote.FastApiProfileService;
import com.carikostkita.data.remote.FastApiOwnerKostService;
import com.carikostkita.data.remote.FastApiUnreadMessagesService;
import com.carikostkita.data.remote.FastApiSurveyService;
import com.carikostkita.data.remote.dto.UserProfileResponse;
import com.carikostkita.data.remote.dto.SurveyDto;
import com.carikostkita.data.remote.dto.SurveyListResponse;
import com.carikostkita.util.SessionManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Pemeriksa berkala (±15 menit, butuh internet) pengganti push server:
 * pesan belum dibaca, perubahan status verifikasi akun/kost, status survei,
 * survei baru untuk pemilik, dan kost favorit yang kembali punya kamar kosong.
 * Status terakhir disimpan di SharedPreferences agar setiap perubahan hanya diberitahukan sekali.
 */
public class NotificationJobService extends JobService {
    private static final int JOB_ID = 4201;
    private static final String PREFS = "notif_state";

    public static void schedule(Context context) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        if (js == null) return;
        for (JobInfo j : js.getAllPendingJobs()) if (j.getId() == JOB_ID) return;
        JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(context, NotificationJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(15 * 60 * 1000L)
                .setPersisted(true)
                .build();
        js.schedule(job);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        new Thread(() -> {
            try {
                check(getApplicationContext());
            } catch (Exception ignored) {}
            jobFinished(params, false);
        }, "notif-check").start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    /** Juga dipanggil saat app dibuka agar status awal tersimpan tanpa memicu notifikasi lama. */
    static void check(Context context) throws Exception {
        SessionManager session = new SessionManager(context);
        if (!session.isLoggedIn()) return;
        String me = session.getUserUid();
        SupabaseDbService db = SupabaseClient.getInstance().createService(SupabaseDbService.class);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean firstRun = !prefs.getBoolean("initialized_" + me, false);
        SharedPreferences.Editor ed = prefs.edit();

        // 1. Pesan belum dibaca
        if (session.isChatNotificationEnabled()) {
            UnreadSummary summary = readUnreadMessages(session, db, me);
            if (summary != null) {
                int last = prefs.getInt("unread_" + me, 0);
                if (!firstRun && summary.count > last && summary.preview != null) {
                    AppNotifications.post(context, 1001, AppNotifications.CHANNEL_CHAT,
                            summary.count == 1 ? "1 pesan baru" : summary.count + " pesan belum dibaca", summary.preview);
                }
                ed.putInt("unread_" + me, summary.count);
            }
        }

        // 2. Status verifikasi akun (pengajuan pemilik)
        String accountVerificationStatus = readAccountVerificationStatus(session, db);
        if (accountVerificationStatus != null) {
            String status = accountVerificationStatus;
            String prev = prefs.getString("ver_" + me, null);
            if (!firstRun && status != null && !status.equals(prev)) {
                if ("APPROVED".equals(status)) {
                    AppNotifications.post(context, 1002, AppNotifications.CHANNEL_STATUS, "Akun pemilik disetujui",
                            "Selamat! Buka aplikasi untuk mulai memasang kost.");
                } else if ("REVISION_REQUIRED".equals(status) || "REJECTED".equals(status)) {
                    AppNotifications.post(context, 1002, AppNotifications.CHANNEL_STATUS, "Pengajuan pemilik perlu dicek",
                            "Tim CariKostKita meninggalkan catatan. Buka Profil untuk melihatnya.");
                }
            }
            if (status != null) ed.putString("ver_" + me, status);
        }

        // 3. Status kost milik pemilik
        if (session.isPemilikKost()) {
            List<JsonObject> kosts = null;
            try {
                kosts = readMyOwnerKosts(session, db);
            } catch (IOException ignored) {}
            if (kosts != null) for (JsonObject k : kosts) {
                String key = "kost_" + str(k, "id");
                String status = str(k, "verification_status");
                String prev = prefs.getString(key, null);
                if (!firstRun && prev != null && status != null && !status.equals(prev)) {
                    String title = "APPROVED".equals(status) ? "Kost disetujui" : "REJECTED".equals(status) ? "Kost ditolak"
                            : "REVISION_REQUIRED".equals(status) ? "Kost perlu perbaikan" : null;
                    if (title != null) {
                        AppNotifications.post(context, key.hashCode(), AppNotifications.CHANNEL_STATUS, title,
                                "\"" + str(k, "name") + "\" — buka Kost Saya untuk detailnya.");
                    }
                }
                if (status != null) ed.putString(key, status);
            }
        }

        // 4. Survei: status berubah (pencari) & permintaan baru (pemilik)
        List<SurveyDto> surveys = null;
        try {
            String token = session.getAccessToken();
            if (token != null && !token.isEmpty()) {
                if (session.isTokenExpired()) token = SupabaseClient.getInstance().refreshTokenBlocking(token);
                if (token != null && !token.isEmpty()) {
                    FastApiSurveyService surveyService = SupabaseClient.getInstance().createSurveyService();
                    retrofit2.Response<SurveyListResponse> response = surveyService
                            .list("Bearer " + token, "all", null, 1, 100).execute();
                    if (response.isSuccessful() && response.body() != null) surveys = response.body().data;
                }
            }
        } catch (Exception ignored) {}
        if (surveys != null) for (SurveyDto s : surveys) {
            String key = "survey_" + s.id;
            String status = s.status;
            String prev = prefs.getString(key, null);
            boolean mineAsSeeker = "seeker".equals(s.viewerRole);
            if (!firstRun && status != null) {
                if (!mineAsSeeker && prev == null && "MENUNGGU".equals(status)) {
                    AppNotifications.post(context, key.hashCode(), AppNotifications.CHANNEL_STATUS, "Permintaan survei baru",
                            "Calon penyewa ingin melihat kost. Konfirmasi jadwalnya di aplikasi.");
                } else if (mineAsSeeker && prev != null && !status.equals(prev)) {
                    String text = "DIKONFIRMASI".equals(status) ? "Pemilik mengonfirmasi jadwal survei kamu."
                            : "DITOLAK".equals(status) ? "Pemilik belum bisa menerima jadwal itu. Coba waktu lain."
                            : "SELESAI".equals(status) ? "Survei selesai. Bagikan ulasanmu untuk membantu pencari lain." : null;
                    if (text != null) AppNotifications.post(context, key.hashCode(), AppNotifications.CHANNEL_STATUS, "Jadwal survei", text);
                }
            }
            if (status != null) ed.putString(key, status);
        }

        // 5. Kost favorit kembali punya kamar kosong
        List<JsonObject> availability = readFavoriteAvailability(session, db, me);
        if (availability != null) {
            for (JsonObject k : availability) {
                String kostId = str(k, "kost_id");
                if (kostId == null || !k.has("is_available") || k.get("is_available").isJsonNull()) continue;
                String key = "favavail_" + kostId;
                boolean available = k.get("is_available").getAsBoolean();
                boolean wasAvailable = prefs.getBoolean(key, available);
                if (!firstRun && available && !wasAvailable) {
                    AppNotifications.post(context, key.hashCode(), AppNotifications.CHANNEL_STATUS, "Ada kamar kosong",
                            "\"" + (str(k, "name") != null ? str(k, "name") : "Kost " + kostId)
                                    + "\" di favoritmu kembali tersedia.");
                }
                ed.putBoolean(key, available);
            }
        }

        ed.putBoolean("initialized_" + me, true);
        ed.apply();
    }

    /** Read through FastAPI first; use the old RLS query only when that route is absent. */
    private static String readAccountVerificationStatus(SessionManager session, SupabaseDbService db) throws IOException {
        String token = session.getAccessToken();
        if (token == null || token.isEmpty()) return null;
        if (session.isTokenExpired()) token = SupabaseClient.getInstance().refreshTokenBlocking(token);
        if (token == null || token.isEmpty()) return null;

        FastApiProfileService profiles = SupabaseClient.getInstance().createProfileService();
        Response<UserProfileResponse> api = profiles.readMyProfile("Bearer " + token).execute();
        if (api.isSuccessful()) {
            return api.body() != null && api.body().data != null
                    ? api.body().data.verificationStatus : null;
        }
        boolean routeMissing = isMissingFastApiRoute(api.code(), api.errorBody());
        if (!routeMissing) {
            close(api.errorBody());
            return null;
        }

        String appUserId = currentAppUserId(db);
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + appUserId);
        Response<List<JsonObject>> legacy = db.select(
                "users", filter, "verification_status", null
        ).execute();
        if (!legacy.isSuccessful() || legacy.body() == null || legacy.body().isEmpty()) {
            close(legacy.errorBody());
            return null;
        }
        return str(legacy.body().get(0), "verification_status");
    }

    private static final class UnreadSummary {
        final int count;
        final String preview;

        UnreadSummary(int count, String preview) {
            this.count = count;
            this.preview = preview;
        }
    }

    /** FastAPI first; keep the old unread-counter query only for deployments without this route. */
    private static UnreadSummary readUnreadMessages(SessionManager session, SupabaseDbService db, String me) {
        String token = session.getAccessToken();
        if (token == null || token.isEmpty()) return null;
        try {
            if (session.isTokenExpired()) token = SupabaseClient.getInstance().refreshTokenBlocking(token);
            if (token == null || token.isEmpty()) return null;

            FastApiUnreadMessagesService service = SupabaseClient.getInstance().createUnreadMessagesService();
            Response<JsonObject> response = service.readMyUnreadMessages("Bearer " + token).execute();
            if (response.isSuccessful() && response.body() != null) {
                JsonObject data = response.body().has("data") && response.body().get("data").isJsonObject()
                        ? response.body().getAsJsonObject("data") : null;
                if (data == null || !data.has("unread_count") || data.get("unread_count").isJsonNull()) return null;
                int count = Math.max(0, data.get("unread_count").getAsInt());
                JsonObject latest = data.has("latest_message") && data.get("latest_message").isJsonObject()
                        ? data.getAsJsonObject("latest_message") : null;
                return new UnreadSummary(count, latest == null ? null : str(latest, "message"));
            }

            boolean routeMissing = isMissingFastApiRoute(response.code(), response.errorBody());
            if (!routeMissing) {
                close(response.errorBody());
                return null;
            }
        } catch (Exception ignored) {
            return null;
        }

        try {
            Map<String, String> filters = new HashMap<>();
            filters.put("or", "(pencari_id.eq." + me + ",owner_id.eq." + me + ")");
            Response<List<JsonObject>> legacy = db.select(
                    "chats", filters,
                    "pencari_id,owner_id,unread_pencari,unread_owner,last_message,nama_kost",
                    "last_message_at.desc.nullslast"
            ).execute();
            if (!legacy.isSuccessful() || legacy.body() == null) {
                close(legacy.errorBody());
                return null;
            }
            int unread = 0;
            String preview = null;
            for (JsonObject chat : legacy.body()) {
                boolean seeker = me.equals(str(chat, "pencari_id"));
                int count = num(chat, seeker ? "unread_pencari" : "unread_owner");
                if (count > 0 && preview == null) {
                    preview = (str(chat, "nama_kost") != null ? str(chat, "nama_kost") + ": " : "") + str(chat, "last_message");
                }
                unread += count;
            }
            return new UnreadSummary(unread, preview);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Read favorite availability from FastAPI; retain the legacy query only on a generic 404. */
    private static List<JsonObject> readFavoriteAvailability(SessionManager session, SupabaseDbService db, String me) {
        String token = session.getAccessToken();
        if (token == null || token.isEmpty()) return null;
        try {
            if (session.isTokenExpired()) token = SupabaseClient.getInstance().refreshTokenBlocking(token);
            if (token == null || token.isEmpty()) return null;
            Response<JsonObject> response = SupabaseClient.getInstance().createFavoriteService()
                    .listFavoriteAvailability("Bearer " + token).execute();
            if (response.isSuccessful() && response.body() != null) {
                JsonObject body = response.body();
                if (!body.has("data") || !body.get("data").isJsonArray()) return null;
                List<JsonObject> result = new java.util.ArrayList<>();
                for (com.google.gson.JsonElement element : body.getAsJsonArray("data")) {
                    if (element.isJsonObject()) result.add(element.getAsJsonObject());
                }
                return result;
            }
            boolean routeMissing = isMissingFastApiRoute(response.code(), response.errorBody());
            if (!routeMissing) {
                close(response.errorBody());
                return null;
            }
        } catch (Exception ignored) {
            return null;
        }

        try {
            String appUserId = currentAppUserId(db);
            Map<String, String> favoriteFilter = new HashMap<>();
            favoriteFilter.put("user_id", "eq." + appUserId);
            Response<List<JsonObject>> favoriteResponse = db.select("favorites", favoriteFilter, "kost_id", null).execute();
            if (!favoriteResponse.isSuccessful() || favoriteResponse.body() == null) {
                close(favoriteResponse.errorBody());
                return null;
            }
            Set<String> ids = new HashSet<>();
            for (JsonObject favorite : favoriteResponse.body()) {
                String id = str(favorite, "kost_id");
                try {
                    long numericId = Long.parseLong(id);
                    if (numericId > 0) ids.add(Long.toString(numericId));
                } catch (Exception ignored) {}
            }
            if (ids.isEmpty()) return java.util.Collections.emptyList();

            Map<String, String> kostFilter = new HashMap<>();
            kostFilter.put("id", "in.(" + android.text.TextUtils.join(",", ids) + ")");
            Response<List<JsonObject>> kostResponse = db.select(
                    "kosts", kostFilter, "id,nama_kost,status,kamar_tersedia", null
            ).execute();
            if (!kostResponse.isSuccessful() || kostResponse.body() == null) {
                close(kostResponse.errorBody());
                return null;
            }
            List<JsonObject> result = new java.util.ArrayList<>();
            for (JsonObject kost : kostResponse.body()) {
                String id = str(kost, "id");
                if (id == null) continue;
                boolean available = "TERSEDIA".equals(str(kost, "status")) && (kost.get("kamar_tersedia") == null
                        || kost.get("kamar_tersedia").isJsonNull() || num(kost, "kamar_tersedia") > 0);
                JsonObject item = new JsonObject();
                item.addProperty("kost_id", id);
                item.addProperty("name", str(kost, "nama_kost"));
                item.addProperty("is_available", available);
                result.add(item);
            }
            return result;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isMissingFastApiRoute(int statusCode, ResponseBody errorBody) {
        if (statusCode != 404 || errorBody == null) return false;
        try {
            JsonElement root = JsonParser.parseString(errorBody.string());
            if (!root.isJsonObject() || !root.getAsJsonObject().has("detail")) return false;
            JsonElement detail = root.getAsJsonObject().get("detail");
            return detail.isJsonPrimitive() && "Not Found".equals(detail.getAsString());
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String currentAppUserId(SupabaseDbService db) throws IOException {
        Response<ResponseBody> response = db.rpc("current_app_user_id", new HashMap<>()).execute();
        if (!response.isSuccessful() || response.body() == null) {
            close(response.body());
            close(response.errorBody());
            throw new IOException("Identity bridge is unavailable for the legacy profile fallback.");
        }
        String rawId = response.body().string().trim();
        try {
            long appUserId = Long.parseLong(rawId);
            if (appUserId <= 0) throw new NumberFormatException("non-positive app user ID");
            return Long.toString(appUserId);
        } catch (NumberFormatException invalidId) {
            throw new IOException("Identity bridge did not return a valid numeric user ID.", invalidId);
        }
    }

    /** Fetch all listing-status pages; direct PostgREST is retained only for a missing route. */
    private static List<JsonObject> readMyOwnerKosts(SessionManager session, SupabaseDbService db) throws IOException {
        String token = session.getAccessToken();
        if (token == null || token.isEmpty()) return null;
        if (session.isTokenExpired()) token = SupabaseClient.getInstance().refreshTokenBlocking(token);
        if (token == null || token.isEmpty()) return null;

        FastApiOwnerKostService service = SupabaseClient.getInstance().createOwnerKostService();
        List<JsonObject> listings = new java.util.ArrayList<>();
        for (int page = 1; page <= 20; page++) {
            Response<JsonObject> response = service
                    .listMyOwnerKosts("Bearer " + token, page, 100).execute();
            if (response.isSuccessful() && response.body() != null) {
                JsonObject body = response.body();
                if (body.has("data") && body.get("data").isJsonArray()) {
                    for (com.google.gson.JsonElement element : body.getAsJsonArray("data")) {
                        if (element.isJsonObject()) listings.add(element.getAsJsonObject());
                    }
                }
                JsonObject pagination = body.has("pagination") && body.get("pagination").isJsonObject()
                        ? body.getAsJsonObject("pagination") : null;
                if (pagination == null || !pagination.has("has_more")
                        || !pagination.get("has_more").getAsBoolean()) return listings;
                continue;
            }

            boolean routeMissing = page == 1 && isMissingFastApiRoute(response.code(), response.errorBody());
            if (!routeMissing) {
                close(response.errorBody());
                return null;
            }
            Map<String, String> filters = new HashMap<>();
            filters.put("owner_id", "eq." + currentAppUserId(db));
            filters.put("limit", "2000");
            Response<List<JsonObject>> legacy = db.select(
                    "kosts", filters, "id,name,status,verification_status,created_at", "created_at.desc"
            ).execute();
            if (legacy.isSuccessful()) return legacy.body();
            close(legacy.errorBody());
            return null;
        }
        throw new IOException("Owner listing status exceeded the safe pagination limit.");
    }

    private static void close(ResponseBody body) {
        if (body != null) body.close();
    }

    /** Jalankan pemeriksaan sekali di latar (mis. saat app dibuka). */
    public static void runNow(Context context) {
        Context app = context.getApplicationContext();
        new Thread(() -> {
            try {
                check(app);
            } catch (Exception ignored) {}
        }, "notif-now").start();
    }

    private static String str(JsonObject o, String k) {
        return o.get(k) == null || o.get(k).isJsonNull() ? null : o.get(k).getAsString();
    }

    private static int num(JsonObject o, String k) {
        try {
            return o.get(k) == null || o.get(k).isJsonNull() ? 0 : o.get(k).getAsInt();
        } catch (Exception e) {
            return 0;
        }
    }
}
