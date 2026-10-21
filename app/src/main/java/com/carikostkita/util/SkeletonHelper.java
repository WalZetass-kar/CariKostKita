package com.carikostkita.util;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * Utilitas pengatur durasi tampilan Skeleton Loading.
 * Dioptimalkan agar langsung menampilkan konten riil segera setelah data selesai dimuat
 * tanpa penundaan atau jeda waktu buatan.
 */
public final class SkeletonHelper {

    public static final long MIN_SKELETON_DURATION_MS = 0L;

    private SkeletonHelper() {
        // private constructor
    }

    /**
     * Menandai waktu mulai fetch data / menampilkan skeleton.
     */
    public static long markStart() {
        return SystemClock.elapsedRealtime();
    }

    /**
     * Langsung eksekusi aksi penyelesaian (menutup skeleton & menampilkan konten riil)
     * segera setelah data selesai dimuat tanpa delay.
     *
     * @param startTime Waktu mulai dari {@link #markStart()}
     * @param onComplete Runnable yang menampilkan konten asli & menyembunyikan skeleton
     */
    public static void complete(long startTime, Runnable onComplete) {
        if (onComplete == null) return;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onComplete.run();
        } else {
            new Handler(Looper.getMainLooper()).post(onComplete);
        }
    }

    /**
     * Versi dengan Handler spesifik.
     */
    public static void complete(long startTime, Handler handler, Runnable onComplete) {
        if (onComplete == null) return;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onComplete.run();
        } else if (handler != null) {
            handler.post(onComplete);
        } else {
            new Handler(Looper.getMainLooper()).post(onComplete);
        }
    }
}
