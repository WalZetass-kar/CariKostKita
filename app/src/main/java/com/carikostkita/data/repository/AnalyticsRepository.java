package com.carikostkita.data.repository;

import android.content.Context;
import com.carikostkita.data.model.OwnerKostStat;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Event produk (tanpa data pribadi selain id akun), statistik pemilik, dan funnel admin. */
public class AnalyticsRepository extends BaseRepository {

    public static final String SEARCH = "search";
    public static final String VIEW_DETAIL = "view_detail";
    public static final String CHAT_START = "chat_start";
    public static final String SURVEY_REQUEST = "survey_request";
    public static final String FAVORITE_ADD = "favorite_add";

    public AnalyticsRepository(Context context) {
        super(context);
    }

    /** Catat event tanpa menunggu; kegagalan diabaikan agar tidak mengganggu pengguna. */
    public void log(String event, String kostId) {
        async("logEvent", null, () -> {
            JsonObject body = new JsonObject();
            body.addProperty("event", event);
            if (kostId != null) body.addProperty("kost_id", kostId);
            if (session.isLoggedIn()) body.addProperty("user_id", session.getUserUid());
            db.insert("app_events", body).execute();
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
            Map<String, Object> p = new HashMap<>();
            p.put("days", days);
            List<JsonObject> rows = check("adminFunnel", db.rpcRows("admin_funnel", p).execute());
            Map<String, Long> raw = new HashMap<>();
            if (rows != null) for (JsonObject o : rows) raw.put(str(o, "event"), (long) integer(o, "total", 0));
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
            Map<String, String> f = new HashMap<>();
            String since = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
                    .format(new java.util.Date(System.currentTimeMillis() - 7L * 86400000L));
            f.put("created_at", "gte." + since);
            List<JsonObject> rows = check("crashCount", db.select("crash_reports", f, "id", null).execute());
            return rows != null ? rows.size() : 0;
        });
    }
}
