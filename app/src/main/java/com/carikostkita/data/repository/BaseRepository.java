package com.carikostkita.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.SupabaseDbService;
import com.carikostkita.util.ErrorMessages;
import com.carikostkita.util.SessionManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Response;

/** Dasar repository fitur baru: executor latar, handler UI, dan pembaca JSON aman-null. */
abstract class BaseRepository {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);

    protected final Context context;
    protected final SupabaseDbService db;
    protected final SessionManager session;
    protected final Handler main = new Handler(Looper.getMainLooper());

    BaseRepository(Context context) {
        this.context = context.getApplicationContext();
        this.db = SupabaseClient.getInstance().createService(SupabaseDbService.class);
        this.session = new SessionManager(context);
    }

    protected interface Work<T> {
        T run() throws Exception;
    }

    /** Jalankan di latar; error jaringan/server diterjemahkan jadi pesan ramah. */
    protected <T> void async(String tag, DataCallback<T> callback, Work<T> work) {
        EXECUTOR.execute(() -> {
            try {
                T result = work.run();
                if (callback != null) main.post(() -> callback.onSuccess(result));
            } catch (HttpFailure f) {
                if (callback != null) main.post(() -> callback.onError(f.getMessage()));
            } catch (Exception e) {
                String msg = ErrorMessages.fromException(tag, e);
                if (callback != null) main.post(() -> callback.onError(msg));
            }
        });
    }

    /** Lempar pesan ramah bila respons tidak sukses. */
    protected static <T> T check(String tag, Response<T> res) throws HttpFailure {
        if (!res.isSuccessful()) {
            String body = "";
            try {
                if (res.errorBody() != null) body = res.errorBody().string();
            } catch (Exception ignored) {}
            if (body.contains("rate limit")) throw new HttpFailure("Terlalu sering. Tunggu sebentar lalu coba lagi.");
            throw new HttpFailure(ErrorMessages.fromCode(tag, res.code(), body));
        }
        return res.body();
    }

    static final class HttpFailure extends Exception {
        HttpFailure(String message) {
            super(message);
        }
    }

    protected static String str(JsonObject o, String key) {
        JsonElement e = o != null ? o.get(key) : null;
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    protected static int integer(JsonObject o, String key, int fallback) {
        JsonElement e = o != null ? o.get(key) : null;
        try {
            return e == null || e.isJsonNull() ? fallback : e.getAsInt();
        } catch (Exception ex) {
            return fallback;
        }
    }

    protected static double dbl(JsonObject o, String key, double fallback) {
        JsonElement e = o != null ? o.get(key) : null;
        try {
            return e == null || e.isJsonNull() ? fallback : e.getAsDouble();
        } catch (Exception ex) {
            return fallback;
        }
    }
}
