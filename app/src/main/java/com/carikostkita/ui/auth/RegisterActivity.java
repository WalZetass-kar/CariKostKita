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
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import com.carikostkita.BuildConfig;
import com.carikostkita.R;
import com.carikostkita.data.model.Role;
import com.carikostkita.data.model.User;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.UserRepository;
import com.carikostkita.ui.main.MainActivity;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.FormatUtil;
import com.carikostkita.util.SessionManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_UP = 9002;
    private static final String GOOGLE_WEB_CLIENT_ID = "328759693822-gc05nlhkdpd4cp54t0miqm8u3mf6lqtk.apps.googleusercontent.com";

    private TextInputLayout tilNama;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPhone;
    private TextInputLayout tilPassword;
    private TextInputLayout tilConfirmPassword;

    private EditText etNama;
    private EditText etEmail;
    private EditText etPhone;
    private EditText etPassword;
    private EditText etConfirmPassword;

    private ProgressBar pbLoading;
    private Button btnRegister;
    private View btnRegisterGoogle;
    private UserRepository userRepository;
    private SessionManager sessionManager;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userRepository = new UserRepository(this);
        sessionManager = new SessionManager(this);

        // Configure Google Sign-In SDK dengan Web Client ID Supabase
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(GOOGLE_WEB_CLIENT_ID)
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        tilNama = findViewById(R.id.til_reg_nama);
        tilEmail = findViewById(R.id.til_reg_email);
        tilPhone = findViewById(R.id.til_reg_phone);
        tilPassword = findViewById(R.id.til_reg_password);
        tilConfirmPassword = findViewById(R.id.til_reg_confirm_password);

        etNama = findViewById(R.id.et_reg_nama);
        etEmail = findViewById(R.id.et_reg_email);
        etPhone = findViewById(R.id.et_reg_phone);
        etPassword = findViewById(R.id.et_reg_password);
        etConfirmPassword = findViewById(R.id.et_reg_confirm_password);

        pbLoading = findViewById(R.id.pb_register);
        btnRegister = findViewById(R.id.btn_register);
        btnRegisterGoogle = findViewById(R.id.btn_register_google);
        View btnGotoLogin = findViewById(R.id.btn_goto_login);

        btnRegister.setOnClickListener(v -> handleRegister());

        if (btnRegisterGoogle != null) {
            btnRegisterGoogle.setOnClickListener(v -> launchGoogleSignUp());
        }

        if (btnGotoLogin != null) {
            btnGotoLogin.setOnClickListener(v -> finish());
        }
    }

    private void clearErrors() {
        if (tilNama != null) tilNama.setError(null);
        if (tilEmail != null) tilEmail.setError(null);
        if (tilPhone != null) tilPhone.setError(null);
        if (tilPassword != null) tilPassword.setError(null);
        if (tilConfirmPassword != null) tilConfirmPassword.setError(null);
    }

    private void handleRegister() {
        clearErrors();

        String nama = etNama.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword != null ? etConfirmPassword.getText().toString().trim() : "";

        boolean hasError = false;

        if (nama.isEmpty()) {
            if (tilNama != null) tilNama.setError("Nama lengkap tidak boleh kosong");
            hasError = true;
        }

        if (email.isEmpty()) {
            if (tilEmail != null) tilEmail.setError("Email tidak boleh kosong");
            hasError = true;
        } else if (!FormatUtil.isValidEmail(email)) {
            if (tilEmail != null) tilEmail.setError("Format email tidak valid (contoh: user@email.com)");
            hasError = true;
        }

        if (password.isEmpty()) {
            if (tilPassword != null) tilPassword.setError("Password tidak boleh kosong");
            hasError = true;
        } else if (password.length() < 6) {
            if (tilPassword != null) tilPassword.setError("Password minimal 6 karakter");
            hasError = true;
        }

        if (etConfirmPassword != null) {
            if (confirmPassword.isEmpty()) {
                if (tilConfirmPassword != null) tilConfirmPassword.setError("Konfirmasi password tidak boleh kosong");
                hasError = true;
            } else if (!password.equals(confirmPassword)) {
                if (tilConfirmPassword != null) tilConfirmPassword.setError("Konfirmasi kata sandi tidak cocok");
                hasError = true;
            }
        }

        if (hasError) return;

        setLoading(true);
        // Aturan Keamanan Role: Registrasi mandiri baru SELALU ROLE_USER (Pencari Kost).
        userRepository.register(nama, email, password, phone, Role.USER, false, "", new DataCallback<User>() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                sessionManager.createLoginSession(user);

                Toast.makeText(RegisterActivity.this, "Pendaftaran berhasil! Selamat datang, " + user.getNama(), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                AppDialogHelper.showError(RegisterActivity.this, "Gagal Mendaftar", message);
            }
        });
    }

    private boolean isRetryingBasicGoogle = false;

    private void launchGoogleSignUp() {
        isRetryingBasicGoogle = false;
        try {
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                try {
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    startActivityForResult(signInIntent, RC_GOOGLE_SIGN_UP);
                } catch (Exception e) {
                    AppDialogHelper.showError(this, "Gagal Membuka Google Sign-In", e.getMessage());
                }
            });
        } catch (Exception e) {
            AppDialogHelper.showError(this, "Gagal Membuka Google Sign-In", e.getMessage());
        }
    }

    private void launchBasicGoogleSignUp() {
        try {
            GoogleSignInOptions basicGso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestProfile()
                    .build();
            GoogleSignInClient basicClient = GoogleSignIn.getClient(this, basicGso);
            basicClient.signOut().addOnCompleteListener(this, task -> {
                try {
                    Intent signInIntent = basicClient.getSignInIntent();
                    startActivityForResult(signInIntent, RC_GOOGLE_SIGN_UP);
                } catch (Exception ex) {
                    AppDialogHelper.showError(this, "Gagal Membuka Google Sign-In", ex.getMessage());
                }
            });
        } catch (Exception e) {
            AppDialogHelper.showError(this, "Gagal Membuka Google Sign-In", e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_UP) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignUpResult(task);
        }
    }

    private void handleGoogleSignUpResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                isRetryingBasicGoogle = false;
                registerGoogleAccount(account);
            }
        } catch (ApiException e) {
            int statusCode = e.getStatusCode();
            if (statusCode == 12501) return;
            if (statusCode == 10 && !isRetryingBasicGoogle) {
                isRetryingBasicGoogle = true;
                launchBasicGoogleSignUp();
                return;
            }
            AppDialogHelper.showError(this, "Gagal Masuk Google (" + statusCode + ")",
                    "Tidak dapat menghubungkan akun Google: " + e.getMessage());
        }
    }

    private void registerGoogleAccount(GoogleSignInAccount account) {
        setLoading(true);
        String idToken = account.getIdToken();
        if (idToken != null && !idToken.isEmpty()) {
            userRepository.loginWithGoogle(idToken, new DataCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    setLoading(false);
                    Toast.makeText(RegisterActivity.this, "Pendaftaran Google berhasil! Selamat datang, " + user.getNama(), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String message) {
                    // Fallback ke profil Google
                    registerWithGoogleProfile(account);
                }
            });
        } else {
            registerWithGoogleProfile(account);
        }
    }

    private void registerWithGoogleProfile(GoogleSignInAccount account) {
        userRepository.loginWithGoogle(
                account.getEmail(),
                account.getDisplayName(),
                account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "",
                new DataCallback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        setLoading(false);
                        Toast.makeText(RegisterActivity.this, "Pendaftaran Google berhasil! Selamat datang, " + user.getNama(), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(String errorMsg) {
                        setLoading(false);
                        AppDialogHelper.showError(RegisterActivity.this, "Gagal Daftar Google", errorMsg);
                    }
                }
        );
    }

    private void launchCustomTabOAuth() {
        try {
            String oauthUrl = BuildConfig.SUPABASE_URL + "/auth/v1/authorize?provider=google&redirect_to=carikostkita://login-callback";
            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build();
            customTabsIntent.launchUrl(this, Uri.parse(oauthUrl));
        } catch (Exception e) {
            try {
                String oauthUrl = BuildConfig.SUPABASE_URL + "/auth/v1/authorize?provider=google&redirect_to=carikostkita://login-callback";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(oauthUrl));
                startActivity(browserIntent);
            } catch (Exception ex) {
                AppDialogHelper.showError(this, "Gagal Membuka Browser",
                        "Tidak dapat membuka browser untuk pendaftaran Google. Silakan daftar menggunakan formulir Email & Password.");
            }
        }
    }

    private void setLoading(boolean isLoading) {
        pbLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
        if (btnRegisterGoogle != null) {
            btnRegisterGoogle.setEnabled(!isLoading);
        }
    }
}
