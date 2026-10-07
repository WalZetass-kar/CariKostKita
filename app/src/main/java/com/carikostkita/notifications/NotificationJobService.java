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
import com.carikostkita.util.SessionManager;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            Map<String, String> f = new HashMap<>();
            f.put("or", "(pencari_id.eq." + me + ",owner_id.eq." + me + ")");
            List<JsonObject> chats = db.select("chats", f, "pencari_id,owner_id,unread_pencari,unread_owner,last_message,nama_kost", "last_message_at.desc.nullslast").execute().body();
            int unread = 0;
            String preview = null;
            if (chats != null) for (JsonObject c : chats) {
                boolean seeker = me.equals(str(c, "pencari_id"));
                int n = num(c, seeker ? "unread_pencari" : "unread_owner");
                if (n > 0 && preview == null) preview = (str(c, "nama_kost") != null ? str(c, "nama_kost") + ": " : "") + str(c, "last_message");
                unread += n;
            }
            int last = prefs.getInt("unread_" + me, 0);
            if (!firstRun && unread > last && preview != null) {
                AppNotifications.post(context, 1001, AppNotifications.CHANNEL_CHAT,
                        unread == 1 ? "1 pesan baru" : unread + " pesan belum dibaca", preview);
            }
            ed.putInt("unread_" + me, unread);
        }

        // 2. Status verifikasi akun (pengajuan pemilik)
        Map<String, String> fu = new HashMap<>();
        fu.put("id", "eq." + me);
        List<JsonObject> users = db.select("users", fu, "role,verification_status", null).execute().body();
        if (users != null && !users.isEmpty()) {
            String status = str(users.get(0), "verification_status");
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
            Map<String, String> fk = new HashMap<>();
            fk.put("owner_id", "eq." + me);
            List<JsonObject> kosts = db.select("kosts", fk, "id,nama_kost,verification_status", null).execute().body();
            if (kosts != null) for (JsonObject k : kosts) {
                String key = "kost_" + str(k, "id");
                String status = str(k, "verification_status");
                String prev = prefs.getString(key, null);
                if (!firstRun && prev != null && status != null && !status.equals(prev)) {
                    String title = "APPROVED".equals(status) ? "Kost disetujui" : "REJECTED".equals(status) ? "Kost ditolak"
                            : "REVISION_REQUIRED".equals(status) ? "Kost perlu perbaikan" : null;
                    if (title != null) {
                        AppNotifications.post(context, key.hashCode(), AppNotifications.CHANNEL_STATUS, title,
                                "\"" + str(k, "nama_kost") + "\" — buka Kost Saya untuk detailnya.");
                    }
                }
                if (status != null) ed.putString(key, status);
            }
        }

        // 4. Survei: status berubah (pencari) & permintaan baru (pemilik)
        Map<String, String> fs = new HashMap<>();
        fs.put("or", "(pencari_id.eq." + me + ",owner_id.eq." + me + ")");
        List<JsonObject> surveys = db.select("survey_requests", fs, "id,pencari_id,status,jadwal", null).execute().body();
        if (surveys != null) for (JsonObject s : surveys) {
            String key = "survey_" + str(s, "id");
            String status = str(s, "status");
            String prev = prefs.getString(key, null);
            boolean mineAsSeeker = me.equals(str(s, "pencari_id"));
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
        Map<String, String> ff = new HashMap<>();
        ff.put("user_id", "eq." + me);
        List<JsonObject> favs = db.select("favorites", ff, "kost_id", null).execute().body();
        if (favs != null && !favs.isEmpty()) {
            Set<String> ids = new HashSet<>();
            for (JsonObject f2 : favs) ids.add(str(f2, "kost_id"));
            Map<String, String> fk = new HashMap<>();
            fk.put("id", "in.(" + android.text.TextUtils.join(",", ids) + ")");
            List<JsonObject> kosts = db.select("kosts", fk, "id,nama_kost,status,kamar_tersedia", null).execute().body();
            if (kosts != null) for (JsonObject k : kosts) {
                String key = "favavail_" + str(k, "id");
                boolean available = "TERSEDIA".equals(str(k, "status")) && (k.get("kamar_tersedia") == null
                        || k.get("kamar_tersedia").isJsonNull() || num(k, "kamar_tersedia") > 0);
                boolean wasAvailable = prefs.getBoolean(key, available);
                if (!firstRun && available && !wasAvailable) {
                    AppNotifications.post(context, key.hashCode(), AppNotifications.CHANNEL_STATUS, "Ada kamar kosong",
                            "\"" + str(k, "nama_kost") + "\" di favoritmu kembali punya kamar kosong.");
                }
                ed.putBoolean(key, available);
            }
        }

        ed.putBoolean("initialized_" + me, true);
        ed.apply();
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
