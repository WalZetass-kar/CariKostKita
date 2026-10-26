package com.carikostkita.data.local;

import android.content.Context;
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

/**
 * Cache lokal berbasis file JSON untuk data katalog kost.
 * Memungkinkan aplikasi menampilkan data kost secara instan (0ms skeleton)
 * saat pertama kali dibuka, kemudian memperbarui data di latar belakang.
 */
public final class KostDiskCache {

    private static final String TAG = "KostDiskCache";
    private static final String CACHE_FILE_NAME = "kost_catalog_cache.json";
    private static final Gson gson = new Gson();

    private KostDiskCache() {}

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

    public static synchronized void clear(Context context) {
        if (context == null) return;
        try {
            File file = new File(context.getCacheDir(), CACHE_FILE_NAME);
            if (file.exists()) file.delete();
        } catch (Exception ignored) {}
    }
}
