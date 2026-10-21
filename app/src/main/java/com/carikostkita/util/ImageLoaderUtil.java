package com.carikostkita.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.carikostkita.BuildConfig;
import java.io.File;

/**
 * Utilitas sentral pemuatan dan normalisasi gambar di CariKostKita Android.
 *
 * Mengatasi masalah:
 * 1. Gambar Excel Import / Base64 Data URI (data:image/...) yang gagal dimuat oleh Glide biasa.
 * 2. Path relatif server (/uploads/...) yang perlu dipadukan dengan API_BASE_URL.
 * 3. Masalah performa & lag/stuttering pada RecyclerView karena pemborosan memori (OOM / GC jank)
 *    dengan cara melakukan downsampling terkontrol (override width/height) dan disk caching optimal.
 */
public final class ImageLoaderUtil {

    private static final String TAG = "ImageLoaderUtil";
    private static final String DEFAULT_BASE_URL = "https://carikostkitafastapi.vercel.app";

    private ImageLoaderUtil() {}

    /**
     * Normalisasi string sumber foto:
     * - Web URL (http://, https://) -> dipertahankan
     * - Base64 Data URI (data:image/...) -> dipertahankan
     * - Local URI (content://, file://) -> dipertahankan
     * - Relative path (/uploads/...) -> diawali dengan API_BASE_URL
     */
    @Nullable
    public static String normalizeUrl(@Nullable String rawUrl) {
        if (rawUrl == null) return null;
        String s = rawUrl.trim();
        if (s.isEmpty()) return null;

        // Base64 data URI
        if (s.startsWith("data:image/")) {
            return s;
        }

        // Web URL atau URI protokol
        if (s.startsWith("http://") || s.startsWith("https://") ||
            s.startsWith("content://") || s.startsWith("file://")) {
            return s;
        }

        // Jika path relatif web/server (misal: /uploads/xxx atau uploads/xxx)
        if (s.startsWith("/") || s.startsWith("uploads/")) {
            String base = BuildConfig.API_BASE_URL;
            if (base == null || base.trim().isEmpty()) {
                base = DEFAULT_BASE_URL;
            }
            base = base.trim();
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            if (!s.startsWith("/")) {
                s = "/" + s;
            }
            return base + s;
        }

        // Jika file lokal yang ada di filesystem Android
        try {
            File f = new File(s);
            if (f.exists()) {
                return s;
            }
        } catch (Exception ignored) {}

        // Fallback jika berupa path relatif tanpa slash
        String base = BuildConfig.API_BASE_URL;
        if (base == null || base.trim().isEmpty()) {
            base = DEFAULT_BASE_URL;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + s;
    }

    /**
     * Memuat foto kartu kost default (ukuran thumbnail kartu: 480x320)
     */
    public static void loadKostThumbnail(@NonNull ImageView imageView, @Nullable String rawSource,
                                        @Nullable Runnable onReady, @Nullable Runnable onFailed) {
        loadKostImage(imageView, rawSource, 480, 320, onReady, onFailed);
    }

    /**
     * Memuat gambar dengan batasan resolusi (targetWidth x targetHeight)
     * untuk mencegah decoding bitmap berukuran raksasa yang menyebabkan lag GC.
     */
    public static void loadKostImage(@NonNull ImageView imageView, @Nullable String rawSource,
                                     int targetWidth, int targetHeight,
                                     @Nullable Runnable onReady, @Nullable Runnable onFailed) {
        Context context = imageView.getContext();
        if (context == null) return;

        String source = normalizeUrl(rawSource);
        if (source == null || source.isEmpty()) {
            imageView.setImageDrawable(null);
            if (onFailed != null) onFailed.run();
            return;
        }

        try {
            // Penanganan Base64 Data URI: data:image/jpeg;base64,.....
            if (source.startsWith("data:image/")) {
                int commaIdx = source.indexOf(",");
                if (commaIdx != -1 && commaIdx < source.length() - 1) {
                    String base64Data = source.substring(commaIdx + 1).trim();
                    byte[] imageBytes = Base64.decode(base64Data, Base64.DEFAULT);

                    Glide.with(context)
                            .asBitmap()
                            .load(imageBytes)
                            .centerCrop()
                            .override(targetWidth, targetHeight)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .listener(new RequestListener<Bitmap>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                    if (onFailed != null) onFailed.run();
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                    if (onReady != null) onReady.run();
                                    return false;
                                }
                            })
                            .into(imageView);
                    return;
                }
            }

            // Penanganan Remote URL atau URI Lokal
            Object loadTarget;
            if (source.startsWith("content://") || source.startsWith("file://")) {
                loadTarget = Uri.parse(source);
            } else if (source.startsWith("http://") || source.startsWith("https://")) {
                loadTarget = source;
            } else {
                File file = new File(source);
                loadTarget = file.exists() ? file : source;
            }

            Glide.with(context)
                    .load(loadTarget)
                    .centerCrop()
                    .override(targetWidth, targetHeight)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .thumbnail(0.15f)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            if (onFailed != null) onFailed.run();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            if (onReady != null) onReady.run();
                            return false;
                        }
                    })
                    .into(imageView);
        } catch (Exception e) {
            Log.e(TAG, "Gagal memuat gambar: " + e.getMessage(), e);
            if (onFailed != null) onFailed.run();
        }
    }

    /**
     * Membebaskan alokasi memori Glide saat view didaur ulang di RecyclerView.
     */
    public static void clear(@Nullable ImageView imageView) {
        if (imageView == null) return;
        try {
            Glide.with(imageView.getContext()).clear(imageView);
        } catch (Exception ignored) {}
    }
}
