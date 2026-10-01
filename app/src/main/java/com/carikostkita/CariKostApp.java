package com.carikostkita;

import android.app.Application;
import com.carikostkita.data.local.DatabaseHelper;

public class CariKostApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Inisialisasi Database SQLite Lokal
        DatabaseHelper.getInstance(this).getWritableDatabase();
    }
}
