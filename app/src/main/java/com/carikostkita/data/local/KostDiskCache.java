package com.carikostkita.data.local;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.carikostkita.data.model.Kost;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cache lokal berbasis file JSON untuk data katalog kost.
 * Memungkinkan aplikasi menampilkan data kost secara instan (0ms skeleton)
 * saat pertama kali dibuka, kemudian memperbarui data di latar belakang.
 */
public final class KostDiskCache {

    private static final String TAG = "KostDiskCache";
    private static final String CACHE_FILE_NAME = "kost_catalog_cache.json";
    private static final Gson gson = new Gson();

    private static final ExecutorService asyncExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "kost-disk-cache-worker");
        t.setDaemon(true);
        return t;
    });
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onLoaded(List<Kost> kosts);
    }

    private KostDiskCache() {}

    /**
     * Memuat cache disk secara asinkron di worker thread agar tidak memblokir UI thread.
     * Hasil dikembalikan ke main thread.
     */
    public static void loadAsync(Context context, Callback callback) {
        if (callback == null) return;
        if (context == null) {
            mainHandler.post(() -> callback.onLoaded(new ArrayList<>()));
            return;
        }
        final Context appCtx = context.getApplicationContext();
        asyncExecutor.execute(() -> {
            List<Kost> list = load(appCtx);
            mainHandler.post(() -> callback.onLoaded(list));
        });
    }

    /**
     * Menyimpan cache disk secara asinkron di worker thread.
     */
    public static void saveAsync(Context context, List<Kost> kosts) {
        if (context == null || kosts == null) return;
        final Context appCtx = context.getApplicationContext();
        final List<Kost> copy = new ArrayList<>(kosts);
        asyncExecutor.execute(() -> save(appCtx, copy));
    }

    public static synchronized void save(Context context, List<Kost> kosts) {
        if (context == null || kosts == null) return;
        try {
            File file = new File(context.getCacheDir(), CACHE_FILE_NAME);
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(kosts, writer);
            }
        } catch (Exception e) {
            Log.w(TAG, "Gagal menyimpan cache disk: " + e.getMessage());
        }
    }

    public static synchronized List<Kost> load(Context context) {
        if (context == null) return new ArrayList<>();
        File file = new File(context.getCacheDir(), CACHE_FILE_NAME);
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<ArrayList<Kost>>(){}.getType();
            List<Kost> list = gson.fromJson(reader, listType);
            return (list != null) ? list : new ArrayList<>();
        } catch (Exception e) {
            Log.w(TAG, "Gagal membaca cache disk: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static synchronized long getLastModified(Context context) {
        if (context == null) return 0L;
        File file = new File(context.getCacheDir(), CACHE_FILE_NAME);
        return file.exists() ? file.lastModified() : 0L;
    }

    public static synchronized boolean isCacheValid(Context context, long maxAgeMillis) {
        if (context == null) return false;
        File file = new File(context.getCacheDir(), CACHE_FILE_NAME);
        if (!file.exists() || file.length() == 0) return false;
        long age = System.currentTimeMillis() - file.lastModified();
        return age >= 0 && age <= maxAgeMillis;
    }

    public static synchronized void clear(Context context) {
        if (context == null) return;
        try {
            File file = new File(context.getCacheDir(), CACHE_FILE_NAME);
            if (file.exists()) file.delete();
        } catch (Exception ignored) {}
    }
}
