package com.carikostkita.data.repository;

import android.content.Context;
import com.carikostkita.data.model.SurveyRequest;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Jadwal survei: pencari mengajukan, pemilik mengonfirmasi/menolak/menandai selesai. */
public class SurveyRepository extends BaseRepository {

    private static final String SELECT_SIMPLE = "*";

    public SurveyRepository(Context context) {
        super(context);
    }

    public void request(String kostId, String ownerId, String jadwalIso, String catatan, DataCallback<SurveyRequest> callback) {
        async("requestSurvey", callback, () -> {
            JsonObject body = new JsonObject();
            body.addProperty("kost_id", kostId);
            body.addProperty("owner_id", ownerId);
            body.addProperty("pencari_id", session.getUserUid());
            body.addProperty("jadwal", jadwalIso);
            if (catatan != null && !catatan.isEmpty()) body.addProperty("catatan", catatan);
            List<JsonObject> rows = check("requestSurvey", db.insert("survey_requests", body).execute());
            new AnalyticsRepository(context).log(AnalyticsRepository.SURVEY_REQUEST, kostId);
            return rows != null && !rows.isEmpty() ? map(rows.get(0)) : null;
        });
    }

    /** Survei milik pencari yang login (asSeeker) atau masuk ke pemilik (!asSeeker). */
    public void list(boolean asSeeker, DataCallback<List<SurveyRequest>> callback) {
        async("listSurveys", callback, () -> {
            Map<String, String> f = new HashMap<>();
            f.put(asSeeker ? "pencari_id" : "owner_id", "eq." + session.getUserUid());
            List<JsonObject> rows = check("listSurveys", db.select("survey_requests", f, SELECT_SIMPLE, "jadwal.desc").execute());
            List<SurveyRequest> list = new ArrayList<>();
            if (rows != null) for (JsonObject o : rows) list.add(map(o));
            fillNames(list);
            return list;
        });
    }

    /** Survei aktif pencari untuk satu kost (untuk tombol di halaman detail). */
    public void findMineForKost(String kostId, DataCallback<SurveyRequest> callback) {
        async("findSurvey", callback, () -> {
            Map<String, String> f = new HashMap<>();
            f.put("pencari_id", "eq." + session.getUserUid());
            f.put("kost_id", "eq." + kostId);
            List<JsonObject> rows = check("findSurvey", db.select("survey_requests", f, SELECT_SIMPLE, "created_at.desc").execute());
            return rows != null && !rows.isEmpty() ? map(rows.get(0)) : null;
        });
    }

    public void updateStatus(String id, String status, String alasan, DataCallback<Boolean> callback) {
        async("updateSurvey", callback, () -> {
            Map<String, String> f = new HashMap<>();
            f.put("id", "eq." + id);
            JsonObject body = new JsonObject();
            body.addProperty("status", status);
            if (alasan != null) body.addProperty("alasan", alasan);
            check("updateSurvey", db.patch("survey_requests", f, body).execute());
            return true;
        });
    }

    /** Nama kost & nama pencari diambil terpisah agar tidak bergantung pada nama foreign key. */
    private void fillNames(List<SurveyRequest> list) {
        if (list.isEmpty()) return;
        try {
            List<String> kostIds = new ArrayList<>(), userIds = new ArrayList<>();
            for (SurveyRequest s : list) {
                if (!kostIds.contains(s.kostId)) kostIds.add(s.kostId);
                if (!userIds.contains(s.pencariId)) userIds.add(s.pencariId);
            }
            Map<String, String> fk = new HashMap<>();
            fk.put("id", "in.(" + android.text.TextUtils.join(",", kostIds) + ")");
            List<JsonObject> kosts = db.select("kosts", fk, "id,nama_kost", null).execute().body();
            Map<String, String> fu = new HashMap<>();
            fu.put("id", "in.(" + android.text.TextUtils.join(",", userIds) + ")");
            List<JsonObject> users = db.select("users", fu, "id,nama", null).execute().body();
            for (SurveyRequest s : list) {
                if (kosts != null) for (JsonObject k : kosts) if (s.kostId.equals(str(k, "id"))) s.namaKost = str(k, "nama_kost");
                if (users != null) for (JsonObject u : users) if (s.pencariId.equals(str(u, "id"))) s.namaPencari = str(u, "nama");
            }
        } catch (Exception ignored) {}
    }

    private static SurveyRequest map(JsonObject o) {
        SurveyRequest s = new SurveyRequest();
        s.id = str(o, "id");
        s.kostId = str(o, "kost_id");
        s.pencariId = str(o, "pencari_id");
        s.ownerId = str(o, "owner_id");
        s.jadwal = str(o, "jadwal");
        s.catatan = str(o, "catatan");
        s.status = str(o, "status");
        s.alasan = str(o, "alasan");
        s.createdAt = str(o, "created_at");
        return s;
    }
}
