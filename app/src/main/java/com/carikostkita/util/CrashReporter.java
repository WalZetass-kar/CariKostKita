package com.carikostkita.util;

import android.content.Context;
import android.os.Build;
import com.carikostkita.BuildConfig;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.SupabaseDbService;
import com.carikostkita.data.remote.FastApiCrashReportService;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import retrofit2.Response;

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
                String message = nl > 0 ? content.substring(0, Math.min(nl, 1000)) : content;
                JsonObject body = new JsonObject();
                body.addProperty("error_message", message);
                body.addProperty("stack_trace", content.length() > 20000 ? content.substring(0, 20000) : content);
                body.addProperty("app_version", BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
                body.addProperty("platform", "android");
                JsonObject devInfo = new JsonObject();
                devInfo.addProperty("manufacturer", Build.MANUFACTURER != null ? Build.MANUFACTURER : "Unknown");
                devInfo.addProperty("model", Build.MODEL != null ? Build.MODEL : "Unknown");
                devInfo.addProperty("os_version", Build.VERSION.RELEASE != null ? Build.VERSION.RELEASE : "Unknown");
                body.add("device_info", devInfo);

                SessionManager session = new SessionManager(app);
                if (session.isLoggedIn()) {
                    String token = session.getAccessToken();
                    if (session.isTokenExpired()) token = SupabaseClient.getInstance().refreshTokenBlocking(token);
                    if (token != null && !token.isEmpty()) {
                        FastApiCrashReportService fastApi = SupabaseClient.getInstance().createCrashReportService();
                        Response<okhttp3.ResponseBody> response = fastApi.submit("Bearer " + token, body).execute();
                        try {
                            if (response.isSuccessful()) {
                                file.delete();
                                return;
                            }
                        } finally {
                            if (response.body() != null) response.body().close();
                            if (response.errorBody() != null) response.errorBody().close();
                        }
                    }
                }

                // Public endpoint fallback
                try {
                    FastApiCrashReportService fastApi = SupabaseClient.getInstance().createCrashReportService();
                    Response<okhttp3.ResponseBody> pubResp = fastApi.submitPublic(body).execute();
                    try {
                        if (pubResp.isSuccessful()) {
                            file.delete();
                            return;
                        }
                    } finally {
                        if (pubResp.body() != null) pubResp.body().close();
                        if (pubResp.errorBody() != null) pubResp.errorBody().close();
                    }
                } catch (Exception ignored) {}

                // PostgREST fallback
                SupabaseDbService db = SupabaseClient.getInstance().createService(SupabaseDbService.class);
                if (db.insert("crash_reports", body).execute().isSuccessful()) {
                    file.delete();
                }
            } catch (Exception ignored) {}
        }, "crash-upload").start();
    }
}
