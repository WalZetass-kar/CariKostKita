package com.carikostkita.util;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.carikostkita.R;
import com.carikostkita.data.model.Role;
import com.carikostkita.ui.admin.AdminMainActivity;
import com.carikostkita.ui.admin.PemilikMainActivity;
import com.carikostkita.ui.main.MainActivity;

public class RoleSwitcherDialog {

    public static void show(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        SessionManager session = new SessionManager(activity);
        if (!session.isLoggedIn()) {
            AppDialogHelper.showInfo(activity, "Belum Masuk", "Silakan login terlebih dahulu untuk beralih mode.");
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_role_switcher, null);
        dialog.setContentView(view);

        View itemPencari = view.findViewById(R.id.item_switch_pencari);
        View itemPemilik = view.findViewById(R.id.item_switch_pemilik);
        View itemAdmin = view.findViewById(R.id.item_switch_admin);
        TextView tvSubtitle = view.findViewById(R.id.tv_switch_subtitle);

        if (tvSubtitle != null) {
            tvSubtitle.setText("Akun: " + session.getUserName() + " (" + session.getUserRole().getDisplayName() + ")");
        }

        // Mode Pencari selalu tersedia untuk semua akun
        if (itemPencari != null) {
            itemPencari.setOnClickListener(v -> {
                dialog.dismiss();
                if (!(activity instanceof MainActivity)) {
                    Intent intent = new Intent(activity, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    activity.startActivity(intent);
                    activity.finish();
                }
            });
        }

        // Mode Pemilik: untuk pemilik kost terverifikasi atau developer/admin
        boolean canAccessOwner = session.isPemilikKost() || session.isDeveloper();
        if (itemPemilik != null) {
            if (canAccessOwner) {
                itemPemilik.setVisibility(View.VISIBLE);
                itemPemilik.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (!(activity instanceof PemilikMainActivity)) {
                        Intent intent = new Intent(activity, PemilikMainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        activity.startActivity(intent);
                        activity.finish();
                    }
                });
            } else {
                itemPemilik.setVisibility(View.GONE);
            }
        }

        // Mode Developer/Admin: khusus super admin & developer
        boolean canAccessAdmin = session.isDeveloper();
        if (itemAdmin != null) {
            if (canAccessAdmin) {
                itemAdmin.setVisibility(View.VISIBLE);
                itemAdmin.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (!(activity instanceof AdminMainActivity)) {
                        Intent intent = new Intent(activity, AdminMainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        activity.startActivity(intent);
                        activity.finish();
                    }
                });
            } else {
                itemAdmin.setVisibility(View.GONE);
            }
        }

        dialog.show();
    }
}
