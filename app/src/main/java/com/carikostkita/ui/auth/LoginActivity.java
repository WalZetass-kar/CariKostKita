package com.carikostkita.ui.auth;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.carikostkita.R;
import com.carikostkita.data.model.User;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.UserRepository;
import com.carikostkita.ui.admin.AdminMainActivity;
import com.carikostkita.ui.admin.PemilikMainActivity;
import com.carikostkita.ui.main.MainActivity;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.SessionManager;
import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private ProgressBar pbLoading;
    private Button btnLogin;
    private View btnLoginGoogle;
    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userRepository = new UserRepository(this);
        sessionManager = new SessionManager(this);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        pbLoading = findViewById(R.id.pb_login);
        btnLogin = findViewById(R.id.btn_login);
        btnLoginGoogle = findViewById(R.id.btn_login_google);
        View btnGotoRegister = findViewById(R.id.btn_goto_register);
        View btnDemoAdmin = findViewById(R.id.btn_demo_admin);
        View btnDemoPemilik = findViewById(R.id.btn_demo_pemilik);
        View btnDemoUser = findViewById(R.id.btn_demo_user);

        btnLogin.setOnClickListener(v -> handleLogin());

        if (btnLoginGoogle != null) {
            btnLoginGoogle.setOnClickListener(v -> showGoogleAccountPicker());
        }

        if (btnGotoRegister != null) {
            btnGotoRegister.setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            });
        }

        if (btnDemoAdmin != null) {
            btnDemoAdmin.setOnClickListener(v -> {
                etEmail.setText("admin@carikostkita.com");
                etPassword.setText("admin123");
            });
        }

        if (btnDemoPemilik != null) {
            btnDemoPemilik.setOnClickListener(v -> {
                etEmail.setText("pemilik@carikostkita.com");
                etPassword.setText("pemilik123");
            });
        }

        if (btnDemoUser != null) {
            btnDemoUser.setOnClickListener(v -> {
                etEmail.setText("budi@gmail.com");
                etPassword.setText("user123");
            });
        }
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email tidak boleh kosong");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password tidak boleh kosong");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);
        userRepository.login(email, password, new DataCallback<User>() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                sessionManager.createLoginSession(user);
                proceedToDashboard(user);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                AppDialogHelper.showError(LoginActivity.this, "Gagal Masuk", message);
            }
        });
    }

    private void showGoogleAccountPicker() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_app_modal, null);
        dialog.setContentView(view);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88);
            window.setAttributes(params);
        }

        TextView tvTitle = view.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = view.findViewById(R.id.tv_dialog_message);
        MaterialButton btnNegative = view.findViewById(R.id.btn_dialog_negative);
        MaterialButton btnPositive = view.findViewById(R.id.btn_dialog_positive);

        tvTitle.setText("Masuk dengan Akun Google");
        tvMessage.setText("Gunakan akun Google aktif Anda untuk masuk langsung secara aman ke CariKostKita sebagai Pencari Kost.");
        btnNegative.setText("Batal");
        btnPositive.setText("Pilih Budi (Google)");

        btnNegative.setOnClickListener(v -> dialog.dismiss());
        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            loginGoogleUser("budi.santoso@gmail.com", "Budi Santoso", "https://api.dicebear.com/7.x/avataaars/png?seed=budi");
        });

        dialog.show();
    }

    private void loginGoogleUser(String email, String name, String avatar) {
        setLoading(true);
        userRepository.loginWithGoogle(email, name, avatar, new DataCallback<User>() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                sessionManager.createLoginSession(user);
                Toast.makeText(LoginActivity.this, "Berhasil masuk dengan Google!", Toast.LENGTH_SHORT).show();
                proceedToDashboard(user);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                AppDialogHelper.showError(LoginActivity.this, "Gagal Login Google", message);
            }
        });
    }

    private void proceedToDashboard(User user) {
        Toast.makeText(LoginActivity.this, "Selamat datang, " + user.getNama(), Toast.LENGTH_SHORT).show();

        Intent intent;
        if (user.isDeveloper()) {
            intent = new Intent(LoginActivity.this, AdminMainActivity.class);
        } else if (user.isPemilikKost()) {
            intent = new Intent(LoginActivity.this, PemilikMainActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, MainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        pbLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        if (btnLoginGoogle != null) btnLoginGoogle.setEnabled(!isLoading);
    }
}
