package com.carikostkita.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.carikostkita.R;
import java.io.File;

/**
 * Single source of truth helper untuk pemuatan foto profil / avatar pengguna
 * di seluruh aplikasi CariKostKita (Beranda, Profil, Edit Profil, Drawer).
 *
 * Menerapkan circleCrop secara konsisten dan mencegah fallback botak
 * jika pengguna sudah memiliki foto tersimpan.
 */
public class UserAvatarHelper {

    /**
     * Memuat avatar pengguna ke ImageView secara konsisten dengan circle crop.
     * Menggunakan URL/path yang tersimpan sebagai sumber utama avatar.
     * Fallback ke avatar default hanya jika user memang belum memiliki foto.
     */
    public static void loadAvatar(ImageView imageView, String avatarSource) {
        if (imageView == null) return;
        Context context = imageView.getContext();

        if (avatarSource != null && !avatarSource.trim().isEmpty()) {
            String trimmed = avatarSource.trim();

            // 1. Cek apakah berupa URL remote atau URI lokal (file:, content://, /)
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://") ||
                trimmed.startsWith("content://") || trimmed.startsWith("file:") ||
                trimmed.startsWith("/")) {

                Object loadTarget = trimmed;
                if (trimmed.startsWith("file:") || trimmed.startsWith("content://")) {
                    try {
                        loadTarget = Uri.parse(trimmed);
                    } catch (Exception ignored) {}
                } else if (trimmed.startsWith("/")) {
                    loadTarget = new File(trimmed);
                }

                Glide.with(context)
                        .load(loadTarget)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_avatar_male)
                        .error(R.drawable.ic_avatar_male)
                        .into(imageView);
                return;
            }

            // 2. Cek apakah ada file lokal di folder internal /avatars/
            File localAvatarFile = new File(context.getFilesDir(), "avatars/" + trimmed);
            if (localAvatarFile.exists()) {
                Glide.with(context)
                        .load(localAvatarFile)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .placeholder(R.drawable.ic_avatar_male)
                        .error(R.drawable.ic_avatar_male)
                        .into(imageView);
                return;
            }

            // 3. Cek preset avatar legacy jika belum ada foto
            if ("avatar_female".equalsIgnoreCase(trimmed)) {
                Glide.with(context).load(R.drawable.ic_avatar_female).circleCrop().into(imageView);
                return;
            } else if ("avatar_owner".equalsIgnoreCase(trimmed)) {
                Glide.with(context).load(R.drawable.ic_avatar_owner).circleCrop().into(imageView);
                return;
            }
        }

        // Fallback default avatar jika pengguna belum mengatur foto
        Glide.with(context)
                .load(R.drawable.ic_avatar_male)
                .circleCrop()
                .into(imageView);
    }
}
