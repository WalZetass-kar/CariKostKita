package com.carikostkita.data.repository;

import android.content.Context;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Blokir & laporkan pengguna, serta penghapusan akun sendiri. */
public class SafetyRepository extends BaseRepository {

    public static final String[] USER_REPORT_REASONS = {
            "Penipuan / minta transfer di luar kesepakatan",
            "Pelecehan atau kata-kata kasar",
            "Spam atau promosi",
            "Mengaku sebagai orang lain",
            "Lainnya"
    };

    public SafetyRepository(Context context) {
        super(context);
    }

    /** True bila salah satu pihak memblokir pihak lain. */
    public void isBlockedWith(String otherId, DataCallback<Boolean> callback) {
        async("isBlocked", callback, () -> {
            String me = session.getUserUid();
            Map<String, String> f = new HashMap<>();
            f.put("or", "(and(blocker_id.eq." + me + ",blocked_id.eq." + otherId + "),and(blocker_id.eq." + otherId + ",blocked_id.eq." + me + "))");
            List<JsonObject> rows = check("isBlocked", db.select("user_blocks", f, "blocker_id", null).execute());
            return rows != null && !rows.isEmpty();
        });
    }

    /** True bila SAYA yang memblokir (hanya saya yang bisa membuka blokir). */
    public void isBlockedByMe(String otherId, DataCallback<Boolean> callback) {
        async("isBlockedByMe", callback, () -> {
            Map<String, String> f = new HashMap<>();
            f.put("blocker_id", "eq." + session.getUserUid());
            f.put("blocked_id", "eq." + otherId);
            List<JsonObject> rows = check("isBlockedByMe", db.select("user_blocks", f, "blocker_id", null).execute());
            return rows != null && !rows.isEmpty();
        });
    }

    public void block(String otherId, DataCallback<Boolean> callback) {
        async("block", callback, () -> {
            JsonObject body = new JsonObject();
            body.addProperty("blocker_id", session.getUserUid());
            body.addProperty("blocked_id", otherId);
            check("block", db.insert("user_blocks", body).execute());
            return true;
        });
    }

    public void unblock(String otherId, DataCallback<Boolean> callback) {
        async("unblock", callback, () -> {
            Map<String, String> f = new HashMap<>();
            f.put("blocker_id", "eq." + session.getUserUid());
            f.put("blocked_id", "eq." + otherId);
            check("unblock", db.remove("user_blocks", f).execute());
            return true;
        });
    }

    public void reportUser(String reportedId, String chatId, String reason, String detail, DataCallback<Boolean> callback) {
        async("reportUser", callback, () -> {
            JsonObject body = new JsonObject();
            body.addProperty("reporter_id", session.getUserUid());
            body.addProperty("reported_id", reportedId);
            if (chatId != null) body.addProperty("chat_id", chatId);
            body.addProperty("alasan", reason);
            if (detail != null && !detail.isEmpty()) body.addProperty("detail", detail);
            check("reportUser", db.insert("user_reports", body).execute());
            return true;
        });
    }

    /** Hapus akun & semua data milik akun (fungsi SQL delete_my_account). */
    public void deleteMyAccount(DataCallback<Boolean> callback) {
        async("deleteMyAccount", callback, () -> {
            check("deleteMyAccount", db.rpc("delete_my_account", new HashMap<>()).execute());
            session.logout();
            return true;
        });
    }
}
