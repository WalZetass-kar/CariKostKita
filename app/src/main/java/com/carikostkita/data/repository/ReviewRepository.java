package com.carikostkita.data.repository;

import android.content.Context;
import com.carikostkita.data.model.Review;
import com.carikostkita.data.model.SurveyRequest;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Ulasan & rating. Hanya pencari dengan survei SELESAI yang boleh menulis (dijaga RLS). */
public class ReviewRepository extends BaseRepository {

    public ReviewRepository(Context context) {
        super(context);
    }

    public void list(String kostId, DataCallback<List<Review>> callback) {
        async("listReviews", callback, () -> {
            Map<String, String> f = new HashMap<>();
            f.put("kost_id", "eq." + kostId);
            List<JsonObject> rows = check("listReviews", db.select("reviews", f, "*", "created_at.desc").execute());
            List<Review> list = new ArrayList<>();
            if (rows != null) for (JsonObject o : rows) {
                Review r = new Review();
                r.id = str(o, "id");
                r.kostId = str(o, "kost_id");
                r.userId = str(o, "user_id");
                r.namaUser = str(o, "nama_user");
                r.rating = integer(o, "rating", 0);
                r.komentar = str(o, "komentar");
                r.createdAt = str(o, "created_at");
                list.add(r);
            }
            return list;
        });
    }

    /** Boleh menulis ulasan bila survei pencari untuk kost ini sudah SELESAI. */
    public void canReview(String kostId, DataCallback<Boolean> callback) {
        async("canReview", callback, () -> {
            if (!session.isLoggedIn()) return false;
            Map<String, String> f = new HashMap<>();
            f.put("pencari_id", "eq." + session.getUserUid());
            f.put("kost_id", "eq." + kostId);
            f.put("status", "eq." + SurveyRequest.SELESAI);
            List<JsonObject> rows = check("canReview", db.select("survey_requests", f, "id", null).execute());
            return rows != null && !rows.isEmpty();
        });
    }

    /** Tulis atau perbarui ulasan milik sendiri (upsert per kost). */
    public void submit(String kostId, int rating, String komentar, DataCallback<Boolean> callback) {
        async("submitReview", callback, () -> {
            Map<String, String> f = new HashMap<>();
            f.put("kost_id", "eq." + kostId);
            f.put("user_id", "eq." + session.getUserUid());
            List<JsonObject> existing = check("findReview", db.select("reviews", f, "id", null).execute());
            JsonObject body = new JsonObject();
            body.addProperty("rating", rating);
            body.addProperty("komentar", komentar);
            body.addProperty("nama_user", session.getUserName());
            if (existing != null && !existing.isEmpty()) {
                check("updateReview", db.patch("reviews", f, body).execute());
            } else {
                body.addProperty("kost_id", kostId);
                body.addProperty("user_id", session.getUserUid());
                check("insertReview", db.insert("reviews", body).execute());
            }
            return true;
        });
    }
}
