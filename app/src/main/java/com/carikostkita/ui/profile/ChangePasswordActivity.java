package com.carikostkita.ui.profile;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.carikostkita.R;
import com.carikostkita.data.repository.ActivityLogRepository;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.UserRepository;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ChangePasswordActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private UserRepository userRepository;
    private ActivityLogRepository logRepository;

    private ImageView btnBack;
    private TextInputEditText etOldPassword;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnChangePassword;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        sessionManager = new SessionManager(this);
        userRepository = new UserRepository(this);
        logRepository = new ActivityLogRepository(this);

        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        etOldPassword = findViewById(R.id.et_old_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnChangePassword = findViewById(R.id.btn_change_password);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finishWithAnimation());
        btnChangePassword.setOnClickListener(v -> attemptChangePassword());
    }

    private void attemptChangePassword() {
        String oldPass = etOldPassword.getText() != null ? etOldPassword.getText().toString().trim() : "";
        String newPass = etNewPassword.getText() != null ? etNewPassword.getText().toString().trim() : "";
        String confirmPass = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        if (oldPass.isEmpty()) {
            etOldPassword.setError("Masukkan kata sandi lama Anda");
            etOldPassword.requestFocus();
            return;
        }

        if (newPass.isEmpty()) {
            etNewPassword.setError("Masukkan kata sandi baru");
            etNewPassword.requestFocus();
            return;
        }

        if (newPass.length() < 6) {
            etNewPassword.setError("Kata sandi baru minimal 6 karakter");
            etNewPassword.requestFocus();
            return;
        }

        if (newPass.equals(oldPass)) {
            etNewPassword.setError("Kata sandi baru tidak boleh sama dengan kata sandi lama");
            etNewPassword.requestFocus();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            etConfirmPassword.setError("Konfirmasi kata sandi tidak cocok");
            etConfirmPassword.requestFocus();
            return;
        }

        btnChangePassword.setEnabled(false);
        int userId = sessionManager.getUserId();

        userRepository.changePassword(userId, oldPass, newPass, new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                logRepository.log(userId, sessionManager.getUserName(), "UBAH_PASSWORD",
                        "Pengguna berhasil memperbarui kata sandi akun", sessionManager.getUserRole().name(), userId);

                AppDialogHelper.showSuccessDialog(ChangePasswordActivity.this,
                        "Kata Sandi Berhasil Diperbarui",
                        "Kata sandi akun Anda telah diperbarui demi menjaga keamanan.",
                        () -> finishWithAnimation());
            }

            @Override
            public void onError(String message) {
                btnChangePassword.setEnabled(true);
                AppDialogHelper.showErrorDialog(ChangePasswordActivity.this, "Gagal Mengganti Sandi", message);
            }
        });
    }

    private void finishWithAnimation() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
