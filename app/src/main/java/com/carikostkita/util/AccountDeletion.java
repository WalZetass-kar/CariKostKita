package com.carikostkita.util;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.SafetyRepository;
import com.carikostkita.ui.auth.LoginActivity;

/** Alur hapus akun permanen dengan konfirmasi ketik "HAPUS" (kebijakan Google Play). */
public final class AccountDeletion {

    private AccountDeletion() {}

    public static void confirm(Activity activity) {
        SessionManager session = new SessionManager(activity);
        String extra = session.isPemilikKost() ? " Semua kost milikmu juga akan dihapus dari pencarian." : "";
        AppDialogHelper.showInput(activity, "Hapus Akun Permanen",
                "Profil, favorit, dan riwayat akunmu akan dihapus dan tidak bisa dikembalikan." + extra
                        + "\n\nKetik HAPUS untuk melanjutkan.",
                "HAPUS", "", "Hapus Akun",
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS,
                text -> {
                    if (!"HAPUS".equals(text.trim())) {
                        Toast.makeText(activity, "Ketik HAPUS dengan huruf besar untuk mengonfirmasi", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new SafetyRepository(activity).deleteMyAccount(new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean ok) {
                            Toast.makeText(activity, "Akunmu sudah dihapus", Toast.LENGTH_LONG).show();
                            Intent i = new Intent(activity, LoginActivity.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            activity.startActivity(i);
                            activity.finish();
                        }

                        @Override
                        public void onError(String message) {
                            AppDialogHelper.showError(activity, "Gagal Menghapus Akun", message);
                        }
                    });
                });
    }
}
