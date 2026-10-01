package com.carikostkita.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.carikostkita.R;
import com.carikostkita.data.model.Role;
import com.carikostkita.data.model.User;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.UserRepository;
import com.carikostkita.ui.main.MainActivity;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.FormatUtil;
import com.carikostkita.util.SessionManager;

public class RegisterActivity extends AppCompatActivity {

    private EditText etNama;
    private EditText etPhone;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etCatatanKost;
    private RadioGroup rgRoleSelection;
    private RadioButton rbRolePencari;
    private RadioButton rbRolePemilik;
    private LinearLayout layoutVerifInfo;
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
        etCatatanKost = findViewById(R.id.et_reg_catatan_kost);
        rgRoleSelection = findViewById(R.id.rg_role_selection);
        rbRolePencari = findViewById(R.id.rb_role_pencari);
        rbRolePemilik = findViewById(R.id.rb_role_pemilik);
        layoutVerifInfo = findViewById(R.id.layout_verif_info_box);
        pbLoading = findViewById(R.id.pb_register);
        btnRegister = findViewById(R.id.btn_register);
        View btnGotoLogin = findViewById(R.id.btn_goto_login);

        rgRoleSelection.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_role_pemilik) {
                layoutVerifInfo.setVisibility(View.VISIBLE);
            } else {
                layoutVerifInfo.setVisibility(View.GONE);
            }
        });

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
        String catatan = etCatatanKost != null ? etCatatanKost.getText().toString().trim() : "";
        boolean isRequestOwner = rbRolePemilik.isChecked();

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

        setLoading(true);
        userRepository.register(nama, email, password, phone, Role.USER, isRequestOwner, catatan, new DataCallback<User>() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                sessionManager.createLoginSession(user);

                if (isRequestOwner) {
                    AppDialogHelper.showSuccess(RegisterActivity.this,
                            "Pendaftaran Berhasil!",
                            "Akun Anda telah aktif sebagai Pencari Kost. Pengajuan sebagai Pemilik Kost telah kami terima dan sedang dalam antrean verifikasi Admin (1x24 jam).",
                            () -> {
                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            });
                } else {
                    Toast.makeText(RegisterActivity.this, "Pendaftaran berhasil! Selamat datang, " + user.getNama(), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                AppDialogHelper.showError(RegisterActivity.this, "Gagal Mendaftar", message);
            }
        });
    }

    private void setLoading(boolean isLoading) {
        pbLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
    }
}
