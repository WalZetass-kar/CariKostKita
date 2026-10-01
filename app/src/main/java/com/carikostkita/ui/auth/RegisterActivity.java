package com.carikostkita.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.carikostkita.R;
import com.carikostkita.data.model.Role;
import com.carikostkita.data.model.User;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.UserRepository;
import com.carikostkita.ui.admin.AdminMainActivity;
import com.carikostkita.ui.main.MainActivity;
import com.carikostkita.util.FormatUtil;
import com.carikostkita.util.SessionManager;

public class RegisterActivity extends AppCompatActivity {

    private EditText etNama;
    private EditText etPhone;
    private EditText etEmail;
    private EditText etPassword;
    private RadioButton rbRolePencari;
    private RadioButton rbRolePemilik;
    private ProgressBar pbLoading;
    private Button btnRegister;
    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userRepository = new UserRepository(this);
        sessionManager = new SessionManager(this);

        etNama = findViewById(R.id.et_reg_nama);
        etPhone = findViewById(R.id.et_reg_phone);
        etEmail = findViewById(R.id.et_reg_email);
        etPassword = findViewById(R.id.et_reg_password);
        rbRolePencari = findViewById(R.id.rb_role_pencari);
        rbRolePemilik = findViewById(R.id.rb_role_pemilik);
        pbLoading = findViewById(R.id.pb_register);
        btnRegister = findViewById(R.id.btn_register);
        View btnGotoLogin = findViewById(R.id.btn_goto_login);

        btnRegister.setOnClickListener(v -> handleRegister());
        if (btnGotoLogin != null) {
            btnGotoLogin.setOnClickListener(v -> finish());
        }
    }

    private void handleRegister() {
        String nama = etNama.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (nama.isEmpty()) {
            etNama.setError("Nama lengkap tidak boleh kosong");
            etNama.requestFocus();
            return;
        }

        if (email.isEmpty() || !FormatUtil.isValidEmail(email)) {
            etEmail.setError("Format email tidak valid");
            etEmail.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password minimal 6 karakter");
            etPassword.requestFocus();
            return;
        }

        Role selectedRole = rbRolePemilik.isChecked() ? Role.PEMILIK_KOST : Role.USER;

        setLoading(true);
        userRepository.register(nama, email, password, phone, selectedRole, new DataCallback<User>() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                sessionManager.createLoginSession(user);
                Toast.makeText(RegisterActivity.this, "Pendaftaran berhasil! Selamat datang, " + user.getNama(), Toast.LENGTH_SHORT).show();

                if (user.isPemilikKost() || user.isAdmin()) {
                    Intent intent = new Intent(RegisterActivity.this, AdminMainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        pbLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
    }
}
