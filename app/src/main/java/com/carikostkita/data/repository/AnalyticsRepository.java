package com.carikostkita.data.repository;

import android.content.Context;
import com.carikostkita.data.remote.FastApiAnalyticsService;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.model.OwnerKostStat;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Response;

/** Event produk (tanpa data pribadi selain id akun), statistik pemilik, dan funnel admin. */
public class AnalyticsRepository extends BaseRepository {

    private final FastApiAnalyticsService fastApi;

    public static final String SEARCH = "search";
    public static final String VIEW_DETAIL = "view_detail";
    public static final String CHAT_START = "chat_start";
    public static final String SURVEY_REQUEST = "survey_request";
    public static final String FAVORITE_ADD = "favorite_add";

    public AnalyticsRepository(Context context) {
        super(context);
        this.fastApi = SupabaseClient.getInstance().createAnalyticsService();
    }

    /** Catat event tanpa menunggu; kegagalan diabaikan agar tidak mengganggu pengguna. */
    public void log(String event, String kostId) {
        async("logEvent", null, () -> {
            JsonObject body = new JsonObject();
            body.addProperty("event", event);
            if (kostId != null) body.addProperty("kost_id", kostId);
            if (session.isLoggedIn() && session.isTokenExpired()) {
                SupabaseClient.getInstance().refreshTokenBlocking(session.getAccessToken());
            }
            String bearer = session.isLoggedIn() ? "Bearer " + session.getAccessToken() : null;
            Response<okhttp3.ResponseBody> response = fastApi.recordEvent(bearer, body).execute();
            if (response.body() != null) response.body().close();
            if (response.isSuccessful()) return null;
            if (response.code() != 404) {
                if (response.errorBody() != null) response.errorBody().close();
                throw new IllegalStateException("FastAPI analytics rejected the event (HTTP " + response.code() + ").");
            }
            if (response.errorBody() != null) response.errorBody().close();
            // Compatibility with an older API deployment only. Runtime
            // app_events.user_id is bigint, so resolve it through the trusted
            // database bridge instead of sending an Auth UUID or local hash.
            JsonObject legacyBody = body.deepCopy();
            if (session.isLoggedIn()) {
                Response<okhttp3.ResponseBody> identity = db.rpc("current_app_user_id", new HashMap<>()).execute();
                if (!identity.isSuccessful() || identity.body() == null) {
                    if (identity.errorBody() != null) identity.errorBody().close();
                    throw new IllegalStateException("FastAPI belum tersedia dan identitas domain belum bisa diverifikasi.");
                }
                String mappedId = identity.body().string().trim();
                try {
                    long appUserId = Long.parseLong(mappedId);
                    if (appUserId <= 0) throw new NumberFormatException("non-positive identity");
                    legacyBody.addProperty("user_id", appUserId);
                } catch (NumberFormatException invalidMapping) {
                    throw new IllegalStateException("Identity bridge tidak mengembalikan ID numerik yang valid.", invalidMapping);
                }
            }
            db.insert("app_events", legacyBody).execute();
            return null;
        });
    }

    public void ownerKostStats(DataCallback<List<OwnerKostStat>> callback) {
        async("ownerKostStats", callback, () -> {
            List<JsonObject> rows = check("ownerKostStats", db.rpcRows("owner_kost_stats", new HashMap<>()).execute());
            List<OwnerKostStat> list = new ArrayList<>();
            if (rows != null) for (JsonObject o : rows) {
                OwnerKostStat s = new OwnerKostStat();
                s.kostId = str(o, "kost_id");
                s.namaKost = str(o, "nama_kost");
                s.dilihat = integer(o, "dilihat", 0);
                s.disimpan = integer(o, "disimpan", 0);
                s.chat = integer(o, "chat", 0);
                s.survei = integer(o, "survei", 0);
                list.add(s);
            }
            return list;
        });
    }

    /** Jumlah per tahap funnel N hari terakhir, urut dari atas ke bawah. */
    public void adminFunnel(int days, DataCallback<Map<String, Long>> callback) {
        async("adminFunnel", callback, () -> {
            Map<String, Long> raw = new HashMap<>();
            if (session.isLoggedIn() && session.isTokenExpired()) {
                SupabaseClient.getInstance().refreshTokenBlocking(session.getAccessToken());
            }
            String bearer = session.isLoggedIn() ? "Bearer " + session.getAccessToken() : null;
            Response<JsonObject> fastApiResponse = fastApi.adminFunnel(bearer, days).execute();
            if (fastApiResponse.isSuccessful()) {
                if (fastApiResponse.body() == null) throw new IllegalStateException("FastAPI tidak mengembalikan data funnel.");
                com.google.gson.JsonArray rows = fastApiResponse.body().getAsJsonArray("data");
                if (rows == null) throw new IllegalStateException("Format data funnel FastAPI tidak valid.");
                for (com.google.gson.JsonElement item : rows) {
                    if (item.isJsonObject()) {
                        JsonObject row = item.getAsJsonObject();
                        raw.put(str(row, "event"), (long) integer(row, "total", 0));
                    }
                }
            } else if (fastApiResponse.code() == 404) {
                if (fastApiResponse.errorBody() != null) fastApiResponse.errorBody().close();
                Map<String, Object> p = new HashMap<>();
                p.put("days", days);
                List<JsonObject> rows = check("adminFunnel", db.rpcRows("admin_funnel", p).execute());
                if (rows != null) for (JsonObject row : rows) raw.put(str(row, "event"), (long) integer(row, "total", 0));
            } else {
                check("adminFunnel", fastApiResponse);
            }
            Map<String, Long> ordered = new LinkedHashMap<>();
            for (String e : new String[]{SEARCH, VIEW_DETAIL, FAVORITE_ADD, CHAT_START, SURVEY_REQUEST}) {
                ordered.put(e, raw.containsKey(e) ? raw.get(e) : 0L);
            }
            return ordered;
        });
    }

    /** Jumlah laporan crash 7 hari terakhir (untuk kartu kesehatan aplikasi). */
    public void recentCrashCount(DataCallback<Integer> callback) {
        async("crashCount", callback, () -> {
            if (session.isLoggedIn() && session.isTokenExpired()) {
                SupabaseClient.getInstance().refreshTokenBlocking(session.getAccessToken());
            }
            String bearer = session.isLoggedIn() ? "Bearer " + session.getAccessToken() : null;
            Response<JsonObject> fastApiResponse = fastApi.recentCrashCount(bearer, 7).execute();
            if (fastApiResponse.isSuccessful()) {
                if (fastApiResponse.body() == null) throw new IllegalStateException("FastAPI tidak mengembalikan data crash.");
                JsonObject data = fastApiResponse.body().getAsJsonObject("data");
                if (data == null || !data.has("count")) throw new IllegalStateException("Format data crash FastAPI tidak valid.");
                return data.get("count").getAsInt();
            }
            if (fastApiResponse.code() != 404) {
                check("crashCount", fastApiResponse);
                throw new IllegalStateException("FastAPI tidak mengembalikan jumlah crash.");
            }
            if (fastApiResponse.errorBody() != null) fastApiResponse.errorBody().close();
            // Keep old caller-JWT/RLS query only for deployments without this route.
            Map<String, String> f = new HashMap<>();
            String since = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
                    .format(new java.util.Date(System.currentTimeMillis() - 7L * 86400000L));
            f.put("created_at", "gte." + since);
            List<JsonObject> rows = check("crashCount", db.select("crash_reports", f, "id", null).execute());
            return rows != null ? rows.size() : 0;
        });
    }
}
