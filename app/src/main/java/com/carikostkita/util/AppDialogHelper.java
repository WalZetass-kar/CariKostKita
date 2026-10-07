package com.carikostkita.util;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.carikostkita.R;
import com.google.android.material.button.MaterialButton;

public class AppDialogHelper {

    public enum DialogType {
        INFO,
        CONFIRM,
        SUCCESS,
        ERROR,
        DANGER,
        LOGOUT
    }

    public interface OnDialogActionListener {
        void onAction();
    }

    public interface OnInputConfirmListener {
        void onConfirm(String text);
    }

    public static void showInput(Context context, String title, String message, String hint, String initialText, String positiveText, OnInputConfirmListener onConfirm) {
        showInput(context, title, message, hint, initialText, positiveText,
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, onConfirm);
    }

    /** Dialog isian dengan tipe input tertentu (mis. email atau kata sandi). */
    public static void showInput(Context context, String title, String message, String hint, String initialText, String positiveText, int inputType, OnInputConfirmListener onConfirm) {
        boolean isPassword = (inputType & android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0;
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_app_modal, null);
        dialog.setContentView(view);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.88);
            if (params.width > (int) (380 * context.getResources().getDisplayMetrics().density)) {
                params.width = (int) (380 * context.getResources().getDisplayMetrics().density);
            }
            window.setAttributes(params);
        }

        FrameLayout layoutBadge = view.findViewById(R.id.layout_dialog_badge);
        ImageView ivIcon = view.findViewById(R.id.iv_dialog_icon);
        TextView tvTitle = view.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = view.findViewById(R.id.tv_dialog_message);
        EditText etInput = view.findViewById(R.id.et_dialog_input);
        MaterialButton btnNegative = view.findViewById(R.id.btn_dialog_negative);
        MaterialButton btnPositive = view.findViewById(R.id.btn_dialog_positive);

        tvTitle.setText(title);
        tvMessage.setText(message);

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setShape(GradientDrawable.OVAL);
        badgeBg.setColor(Color.parseColor("#FEF3C7"));
        layoutBadge.setBackground(badgeBg);
        ivIcon.setImageResource(R.drawable.ic_edit);
        ivIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#D97706")));

        btnPositive.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary)));
        btnPositive.setText(positiveText != null ? positiveText : "Kirim");

        if (etInput != null) {
            etInput.setVisibility(View.VISIBLE);
            etInput.setInputType(inputType);
            if (hint != null) etInput.setHint(hint);
            if (initialText != null) {
                etInput.setText(initialText);
                etInput.setSelection(initialText.length());
            }
        }

        btnNegative.setText("Batal");
        btnNegative.setOnClickListener(v -> dialog.dismiss());
        btnPositive.setOnClickListener(v -> {
            String raw = etInput != null ? etInput.getText().toString() : "";
            String input = isPassword ? raw : raw.trim();
            dialog.dismiss();
            if (onConfirm != null) {
                onConfirm.onConfirm(input);
            }
        });

        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public static void showInfo(Context context, String title, String message) {
        show(context, DialogType.INFO, title, message, "Mengerti", null, null, null);
    }

    public static void showInfoDialog(Context context, String title, String message) {
        showInfo(context, title, message);
    }

    public static void showSuccess(Context context, String title, String message) {
        showSuccess(context, title, message, null);
    }

    public static void showSuccess(Context context, String title, String message, Runnable onDismiss) {
        show(context, DialogType.SUCCESS, title, message, "OK", null, () -> {
            if (onDismiss != null) onDismiss.run();
        }, null);
    }

    public static void showSuccessDialog(Context context, String title, String message) {
        showSuccess(context, title, message, null);
    }

    public static void showSuccessDialog(Context context, String title, String message, Runnable onDismiss) {
        showSuccess(context, title, message, onDismiss);
    }

    public static void showError(Context context, String title, String message) {
        show(context, DialogType.ERROR, title, message, "Tutup", null, null, null);
    }

    public static void showErrorDialog(Context context, String title, String message) {
        showError(context, title, message);
    }

    public static void showConfirm(Context context, String title, String message, String positiveText, Runnable onConfirm) {
        show(context, DialogType.CONFIRM, title, message, positiveText, "Batal", () -> {
            if (onConfirm != null) onConfirm.run();
        }, null);
    }

    public static void showConfirmationDialog(Context context, String title, String message, Runnable onConfirm) {
        showConfirm(context, title, message, "Ya, Lanjutkan", onConfirm);
    }

    public static void showDanger(Context context, String title, String message, String positiveText, Runnable onConfirm) {
        show(context, DialogType.DANGER, title, message, positiveText, "Batal", () -> {
            if (onConfirm != null) onConfirm.run();
        }, null);
    }

    public static void showLogout(Context context, Runnable onConfirm) {
        show(context, DialogType.LOGOUT, "Konfirmasi Keluar", "Apakah Anda yakin ingin keluar dari sesi akun Anda saat ini?", "Ya, Keluar", "Batal", () -> {
            if (onConfirm != null) onConfirm.run();
        }, null);
    }

    public static void showLogoutDialog(Context context, Runnable onConfirm) {
        showLogout(context, onConfirm);
    }

    public static Dialog show(Context context, DialogType type, String title, String message,
                              String positiveText, String negativeText,
                              OnDialogActionListener onPositive, OnDialogActionListener onNegative) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_app_modal, null);
        dialog.setContentView(view);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.88);
            if (params.width > (int) (380 * context.getResources().getDisplayMetrics().density)) {
                params.width = (int) (380 * context.getResources().getDisplayMetrics().density);
            }
            window.setAttributes(params);
        }

        FrameLayout layoutBadge = view.findViewById(R.id.layout_dialog_badge);
        ImageView ivIcon = view.findViewById(R.id.iv_dialog_icon);
        TextView tvTitle = view.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = view.findViewById(R.id.tv_dialog_message);
        MaterialButton btnNegative = view.findViewById(R.id.btn_dialog_negative);
        MaterialButton btnPositive = view.findViewById(R.id.btn_dialog_positive);

        tvTitle.setText(title);
        tvMessage.setText(message);

        // Styling based on DialogType
        int badgeBgColor;
        int iconColor;
        int iconRes;
        int positiveBtnColor;

        switch (type) {
            case SUCCESS:
                badgeBgColor = Color.parseColor("#E8F5E9");
                iconColor = ContextCompat.getColor(context, R.color.action_whatsapp);
                iconRes = R.drawable.ic_check;
                positiveBtnColor = ContextCompat.getColor(context, R.color.action_whatsapp);
                break;
            case ERROR:
                badgeBgColor = Color.parseColor("#FEE2E2");
                iconColor = Color.parseColor("#DC2626");
                iconRes = R.drawable.ic_warning;
                positiveBtnColor = Color.parseColor("#DC2626");
                break;
            case DANGER:
                badgeBgColor = Color.parseColor("#FEE2E2");
                iconColor = Color.parseColor("#DC2626");
                iconRes = R.drawable.ic_delete;
                positiveBtnColor = Color.parseColor("#DC2626");
                break;
            case LOGOUT:
                badgeBgColor = Color.parseColor("#FEE2E2");
                iconColor = Color.parseColor("#DC2626");
                iconRes = R.drawable.ic_logout;
                positiveBtnColor = Color.parseColor("#DC2626");
                break;
            case CONFIRM:
                badgeBgColor = Color.parseColor("#FFFBEB");
                iconColor = Color.parseColor("#D97706");
                iconRes = R.drawable.ic_info;
                positiveBtnColor = ContextCompat.getColor(context, R.color.primary);
                break;
            case INFO:
            default:
                badgeBgColor = ContextCompat.getColor(context, R.color.primary_soft);
                iconColor = ContextCompat.getColor(context, R.color.primary);
                iconRes = R.drawable.ic_info;
                positiveBtnColor = ContextCompat.getColor(context, R.color.primary);
                break;
        }

        // Apply Badge Background
        GradientDrawable badgeDrawable = new GradientDrawable();
        badgeDrawable.setShape(GradientDrawable.OVAL);
        badgeDrawable.setColor(badgeBgColor);
        layoutBadge.setBackground(badgeDrawable);

        ivIcon.setImageResource(iconRes);
        ivIcon.setImageTintList(ColorStateList.valueOf(iconColor));

        btnPositive.setText(positiveText != null ? positiveText : "OK");
        btnPositive.setBackgroundTintList(ColorStateList.valueOf(positiveBtnColor));

        if (negativeText != null && !negativeText.isEmpty()) {
            btnNegative.setVisibility(View.VISIBLE);
            btnNegative.setText(negativeText);
            btnNegative.setOnClickListener(v -> {
                dialog.dismiss();
                if (onNegative != null) onNegative.onAction();
            });
        } else {
            btnNegative.setVisibility(View.GONE);
        }

        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            if (onPositive != null) onPositive.onAction();
        });

        dialog.show();
        return dialog;
    }
}
