package com.carikostkita.util;

import android.util.Log;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import retrofit2.Response;

/**
 * Menerjemahkan kegagalan jaringan/server menjadi pesan yang dapat dipahami pengguna.
 * Detail teknis hanya ditulis ke Logcat, tidak pernah ditampilkan di UI.
 */
public final class ErrorMessages {

    private static final String TAG = "CariKostError";

    public static final String OFFLINE = "Kamu sedang offline. Periksa koneksi internet lalu coba lagi.";
    public static final String TIMEOUT = "Koneksi terlalu lambat. Coba lagi beberapa saat.";
    public static final String SESSION_EXPIRED = "Sesi kamu sudah berakhir. Silakan masuk lagi.";
    public static final String FORBIDDEN = "Kamu tidak memiliki izin untuk melakukan aksi ini.";
    public static final String SERVER = "Server sedang bermasalah. Coba lagi beberapa saat.";
    public static final String GENERIC = "Terjadi gangguan. Coba lagi beberapa saat.";

    private ErrorMessages() {}

    /** Pesan untuk exception (biasanya masalah jaringan). */
    public static String fromException(String context, Throwable e) {
        Log.e(TAG, context + ": " + (e != null ? e.getMessage() : "null"), e);
        if (e instanceof UnknownHostException) return OFFLINE;
        if (e instanceof SocketTimeoutException) return TIMEOUT;
        if (e instanceof IOException) return OFFLINE;
        return GENERIC;
    }

    /** Pesan untuk respons HTTP yang tidak sukses. */
    public static String fromResponse(String context, Response<?> response) {
        int code = response != null ? response.code() : -1;
        String body = "";
        try {
            if (response != null && response.errorBody() != null) {
                body = response.errorBody().string();
            }
        } catch (Exception ignored) {}
        return fromCode(context, code, body);
    }

    public static String fromCode(String context, int code, String errorBody) {
        Log.e(TAG, context + ": HTTP " + code + " " + errorBody);
        if (errorBody != null && errorBody.contains("42501")) return FORBIDDEN;
        if (code == 401) return SESSION_EXPIRED;
        if (code == 403) return FORBIDDEN;
        if (code == 408) return TIMEOUT;
        if (code == 413) return "Ukuran file terlalu besar. Gunakan foto yang lebih kecil.";
        if (code == 429) return "Terlalu banyak percobaan. Tunggu sebentar lalu coba lagi.";
        if (code >= 500) return SERVER;
        return GENERIC;
    }

    /** True bila pesan menandakan pengguna sedang offline (untuk ikon state error). */
    public static boolean isOffline(String message) {
        return OFFLINE.equals(message) || TIMEOUT.equals(message);
    }
}
