package com.carikostkita.util;

import android.content.Context;
import android.os.Build;
import com.carikostkita.BuildConfig;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.SupabaseDbService;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Pelapor crash ringan tanpa layanan pihak ketiga: crash disimpan ke file saat terjadi,
 * lalu dikirim ke tabel crash_reports saat aplikasi dibuka berikutnya.
 */
public final class CrashReporter {
    private static final String FILE = "pending_crash.txt";

    private CrashReporter() {}

    public static void install(Context context) {
        Context app = context.getApplicationContext();
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                StringWriter sw = new StringWriter();
                error.printStackTrace(new PrintWriter(sw));
                String trace = sw.toString();
                if (trace.length() > 18000) trace = trace.substring(0, 18000);
                try (FileOutputStream out = new FileOutputStream(new File(app.getFilesDir(), FILE))) {
                    out.write((error.getClass().getName() + ": " + error.getMessage() + "\n" + trace).getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {}
            if (previous != null) previous.uncaughtException(thread, error);
        });
        uploadPending(app);
    }

    private static void uploadPending(Context app) {
        File file = new File(app.getFilesDir(), FILE);
        if (!file.exists()) return;
        new Thread(() -> {
            try {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                int nl = content.indexOf('\n');
                JsonObject body = new JsonObject();
                body.addProperty("message", nl > 0 ? content.substring(0, Math.min(nl, 500)) : content);
                body.addProperty("stacktrace", content);
                body.addProperty("app_version", BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
                body.addProperty("device", Build.MANUFACTURER + " " + Build.MODEL);
                body.addProperty("android_version", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
                SessionManager session = new SessionManager(app);
                if (session.isLoggedIn()) body.addProperty("user_id", session.getUserUid());
                SupabaseDbService db = SupabaseClient.getInstance().createService(SupabaseDbService.class);
                if (db.insert("crash_reports", body).execute().isSuccessful()) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            } catch (Exception ignored) {}
        }, "crash-upload").start();
    }
}
