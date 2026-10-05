package com.carikostkita.util;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * Utilitas pengatur durasi minimum tampilan Skeleton Loading.
 * Sesuai panduan desain sistem CariKostKita:
 * Skeleton wajib tampil minimal 200ms agar mencegah layar berkedip (content flashing/layout jump).
 */
public final class SkeletonHelper {

    public static final long MIN_SKELETON_DURATION_MS = 200L;

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
     * Memastikan aksi penyelesaian (menutup skeleton & menampilkan konten riil)
     * hanya dieksekusi setelah durasi minimal 200ms terpenuhi.
     *
     * @param startTime Waktu mulai dari {@link #markStart()}
     * @param onComplete Runnable yang menampilkan konten asli & menyembunyikan skeleton
     */
    public static void complete(long startTime, Runnable onComplete) {
        if (onComplete == null) return;
        long elapsed = SystemClock.elapsedRealtime() - startTime;
        long remaining = MIN_SKELETON_DURATION_MS - elapsed;

        if (remaining > 0) {
            new Handler(Looper.getMainLooper()).postDelayed(onComplete, remaining);
        } else {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                onComplete.run();
            } else {
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
        }
    }

    /**
     * Versi dengan Handler spesifik.
     */
    public static void complete(long startTime, Handler handler, Runnable onComplete) {
        if (onComplete == null) return;
        if (handler == null) {
            complete(startTime, onComplete);
            return;
        }

        long elapsed = SystemClock.elapsedRealtime() - startTime;
        long remaining = MIN_SKELETON_DURATION_MS - elapsed;

        if (remaining > 0) {
            handler.postDelayed(onComplete, remaining);
        } else {
            handler.post(onComplete);
        }
    }
}
