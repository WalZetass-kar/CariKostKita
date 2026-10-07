package com.carikostkita.util;

import android.content.Context;
import android.content.Intent;
import com.carikostkita.ui.auth.LoginActivity;

/**
 * Ajakan masuk untuk tamu, lengkap dengan tombol yang membuka halaman Login.
 * Menggantikan toast "silakan login" yang tidak memberi jalan keluar.
 */
public final class AuthPrompt {

    private AuthPrompt() {}

    /**
     * @return true bila pengguna sudah masuk dan aksi boleh dilanjutkan.
     */
    public static boolean require(Context context, String reason) {
        if (new SessionManager(context).isLoggedIn()) return true;
        AppDialogHelper.showConfirm(context,
                "Masuk Dulu, Yuk",
                reason + "\n\nBelum punya akun? Daftar gratis kurang dari 1 menit.",
                "Masuk / Daftar",
                () -> openLogin(context));
        return false;
    }

    public static void openLogin(Context context) {
        context.startActivity(new Intent(context, LoginActivity.class));
    }
}
