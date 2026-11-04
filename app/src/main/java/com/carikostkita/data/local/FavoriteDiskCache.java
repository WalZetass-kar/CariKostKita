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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cache persisten offline-first untuk kost favorit pengguna.
 * Memastikan kost yang disukai seketika tersimpan di storage internal perangkat
 * dan langsung tampil di Tab Favorit tanpa tertunda latensi jaringan atau issue backend.
 */
public final class FavoriteDiskCache {

    private static final String TAG = "FavoriteDiskCache";
    private static final String CACHE_FILE_NAME = "favorite_kosts_persistent.json";
    private static final Gson gson = new Gson();

    private FavoriteDiskCache() {}

    public static synchronized List<Kost> loadFavorites(Context context) {
        if (context == null) return new ArrayList<>();
        File file = new File(context.getApplicationContext().getFilesDir(), CACHE_FILE_NAME);
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<ArrayList<Kost>>(){}.getType();
            List<Kost> list = gson.fromJson(reader, listType);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            Log.w(TAG, "Gagal membaca favorite disk cache: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static synchronized void saveFavorites(Context context, List<Kost> favorites) {
        if (context == null || favorites == null) return;
        try {
            File file = new File(context.getApplicationContext().getFilesDir(), CACHE_FILE_NAME);
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(favorites, writer);
            }
        } catch (Exception e) {
            Log.w(TAG, "Gagal menyimpan favorite disk cache: " + e.getMessage());
        }
    }

    public static synchronized boolean isFavorite(Context context, String kostId) {
        if (context == null || kostId == null || kostId.isEmpty()) return false;
        List<Kost> list = loadFavorites(context);
        for (Kost k : list) {
            if (k != null && kostId.equals(k.getIdKost())) return true;
        }
        return false;
    }

    public static synchronized Set<String> getFavoriteIds(Context context) {
        Set<String> ids = new HashSet<>();
        if (context == null) return ids;
        List<Kost> list = loadFavorites(context);
        for (Kost k : list) {
            if (k != null && k.getIdKost() != null) {
                ids.add(k.getIdKost());
            }
        }
        return ids;
    }

    public static synchronized void addFavorite(Context context, Kost kost) {
        if (context == null || kost == null || kost.getIdKost() == null || kost.getIdKost().isEmpty()) return;
        List<Kost> list = loadFavorites(context);
        boolean exists = false;
        for (int i = 0; i < list.size(); i++) {
            Kost k = list.get(i);
            if (k != null && kost.getIdKost().equals(k.getIdKost())) {
                exists = true;
                kost.setFavorite(true);
                list.set(i, kost);
                break;
            }
        }
        if (!exists) {
            kost.setFavorite(true);
            list.add(0, kost);
        }
        saveFavorites(context, list);
    }

    public static synchronized void removeFavorite(Context context, String kostId) {
        if (context == null || kostId == null || kostId.isEmpty()) return;
        List<Kost> list = loadFavorites(context);
        boolean removed = list.removeIf(k -> k != null && kostId.equals(k.getIdKost()));
        if (removed) {
            saveFavorites(context, list);
        }
    }

    public static synchronized void clearFavorites(Context context) {
        if (context == null) return;
        File file = new File(context.getApplicationContext().getFilesDir(), CACHE_FILE_NAME);
        if (file.exists()) {
            file.delete();
        }
    }
}
