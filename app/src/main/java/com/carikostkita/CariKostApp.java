package com.carikostkita;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.util.SessionManager;

public class CariKostApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Sesuai Design Reference Resmi: CariKostKita beroperasi dalam Mode Terang (Warm Light)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Inisialisasi Supabase Client & Session
        SupabaseClient.getInstance();
        new SessionManager(this);

        // Inisialisasi OSMDroid OpenStreetMap
        try {
            org.osmdroid.config.Configuration.getInstance().setUserAgentValue(getPackageName());
            org.osmdroid.config.Configuration.getInstance().load(this, getSharedPreferences("osmdroid_prefs", MODE_PRIVATE));
        } catch (Exception ignored) {}
    }
}
