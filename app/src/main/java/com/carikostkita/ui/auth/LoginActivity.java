package com.carikostkita.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.carikostkita.R;
import com.carikostkita.data.model.User;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.UserRepository;
import com.carikostkita.ui.admin.AdminMainActivity;
import com.carikostkita.ui.main.MainActivity;
import com.carikostkita.util.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private ProgressBar pbLoading;
    private Button btnLogin;
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
        View btnGotoRegister = findViewById(R.id.btn_goto_register);
        View btnDemoAdmin = findViewById(R.id.btn_demo_admin);
        View btnDemoPemilik = findViewById(R.id.btn_demo_pemilik);
        View btnDemoUser = findViewById(R.id.btn_demo_user);

        btnLogin.setOnClickListener(v -> handleLogin());

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
                Toast.makeText(LoginActivity.this, "Selamat datang, " + user.getNama(), Toast.LENGTH_SHORT).show();

                if (user.isAdmin() || user.isPemilikKost()) {
                    startActivity(new Intent(LoginActivity.this, AdminMainActivity.class));
                } else {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                }
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        pbLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
    }
}
