package com.carikostkita.ui.auth;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import com.bumptech.glide.Glide;
import com.carikostkita.BuildConfig;
import com.carikostkita.R;
import com.carikostkita.data.model.Role;
import com.carikostkita.data.model.User;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.UserRepository;
import com.carikostkita.ui.admin.AdminMainActivity;
import com.carikostkita.ui.admin.PemilikMainActivity;
import com.carikostkita.ui.main.MainActivity;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.SessionManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_IN = 9001;
    private static final String GOOGLE_WEB_CLIENT_ID = "328759693822-gc05nlhkdpd4cp54t0miqm8u3mf6lqtk.apps.googleusercontent.com";

    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private EditText etEmail;
    private EditText etPassword;
    private ProgressBar pbLoading;
    private Button btnLogin;
    private View btnLoginGoogle;
    private UserRepository userRepository;
    private SessionManager sessionManager;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userRepository = new UserRepository(this);
        sessionManager = new SessionManager(this);

        TextView heroTitle = findViewById(R.id.tv_auth_hero_title);
        TextView heroSub = findViewById(R.id.tv_auth_hero_subtitle);
        if (heroTitle != null) heroTitle.setText("Selamat datang\nkembali");
        if (heroSub != null) heroSub.setText("Lanjutkan mencari kost terverifikasi\ndan ngobrol langsung dengan pemiliknya.");
        View hero = findViewById(R.id.auth_hero_root);
        if (hero != null) hero.setClipToOutline(true);

        View btnGuest = findViewById(R.id.btn_continue_guest);
        if (btnGuest != null) {
            btnGuest.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // Periksa callback OAuth via deep link saat activity pertama kali dibuat
        handleDeepLink(getIntent());

        // Configure Google Sign-In SDK dengan Web Client ID Supabase
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(GOOGLE_WEB_CLIENT_ID)
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        pbLoading = findViewById(R.id.pb_login);
        btnLogin = findViewById(R.id.btn_login);
        btnLoginGoogle = findViewById(R.id.btn_login_google);
        View tvForgotPassword = findViewById(R.id.tv_forgot_password);
        View btnGotoRegister = findViewById(R.id.btn_goto_register);

        btnLogin.setOnClickListener(v -> handleLogin());
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                handleLogin();
                return true;
            }
            return false;
        });

        if (btnLoginGoogle != null) {
            btnLoginGoogle.setOnClickListener(v -> launchGoogleSignIn());
        }

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        }

        if (btnGotoRegister != null) {
            btnGotoRegister.setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            });
        }
    }

    private void clearErrors() {
        if (tilEmail != null) tilEmail.setError(null);
        if (tilPassword != null) tilPassword.setError(null);
    }

    private void handleLogin() {
        clearErrors();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty()) {
            if (tilEmail != null) {
                tilEmail.setError("Email tidak boleh kosong");
            } else {
                etEmail.setError("Email tidak boleh kosong");
            }
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            if (tilPassword != null) {
                tilPassword.setError("Password tidak boleh kosong");
            } else {
                etPassword.setError("Password tidak boleh kosong");
            }
            etPassword.requestFocus();
            return;
        }

        setLoading(true);
        userRepository.login(email, password, new DataCallback<User>() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                proceedToDashboard(user);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                AppDialogHelper.showError(LoginActivity.this, "Gagal Masuk", message);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    private void handleDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) return;
        Uri uri = intent.getData();
        if ("carikostkita".equals(uri.getScheme()) && "reset-callback".equals(uri.getHost())) {
            handlePasswordRecovery(uri);
            return;
        }
        if ("carikostkita".equals(uri.getScheme()) && "login-callback".equals(uri.getHost())) {
            Map<String, String> params = new HashMap<>();
            if (uri.getFragment() != null && !uri.getFragment().isEmpty()) {
                params.putAll(parseFragmentParams(uri.getFragment()));
            }
            if (uri.getQuery() != null && !uri.getQuery().isEmpty()) {
                for (String key : uri.getQueryParameterNames()) {
                    params.put(key, uri.getQueryParameter(key));
                }
            }

            String accessToken = params.get("access_token");
            String refreshToken = params.get("refresh_token");
            String expiresInStr = params.get("expires_in");
            long expiresIn = 3600;
            try {
                if (expiresInStr != null) expiresIn = Long.parseLong(expiresInStr);
            } catch (Exception ignored) {}

            if (accessToken != null && !accessToken.isEmpty()) {
                setLoading(true);
                userRepository.handleOAuthCallback(accessToken, refreshToken, expiresIn, new DataCallback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        setLoading(false);
                        proceedToDashboard(user);
                    }

                    @Override
                    public void onError(String message) {
                        setLoading(false);
                        AppDialogHelper.showError(LoginActivity.this, "Gagal Masuk Google", message);
                    }
                });
            } else if (params.containsKey("error_description")) {
                String errorDesc = params.get("error_description");
                android.util.Log.e("LoginActivity", "OAuth error: " + errorDesc);
                AppDialogHelper.showError(this, "Login Google Gagal",
                        "Masuk dengan Google sedang tidak bisa dipakai. Silakan masuk memakai email dan kata sandi.");
            } else if (params.containsKey("error")) {
                AppDialogHelper.showError(this, "Login Google Gagal", params.get("error"));
            }
        }
    }

    private Map<String, String> parseFragmentParams(String fragment) {
        Map<String, String> map = new HashMap<>();
        String[] pairs = fragment.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try {
                    map.put(kv[0], URLDecoder.decode(kv[1], "UTF-8"));
                } catch (Exception e) {
                    map.put(kv[0], kv[1]);
                }
            }
        }
        return map;
    }

    private boolean isRetryingBasicGoogle = false;

    private void launchGoogleSignIn() {
        isRetryingBasicGoogle = false;
        try {
            // Sign out first to ensure account picker is shown every time
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                try {
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
                } catch (Exception e) {
                    AppDialogHelper.showError(this, "Google Sign-In Tidak Tersedia", "Pastikan Layanan Google Play aktif, atau masuk memakai email dan kata sandi.");
                }
            });
        } catch (Exception e) {
            AppDialogHelper.showError(this, "Google Sign-In Tidak Tersedia", "Pastikan Layanan Google Play aktif, atau masuk memakai email dan kata sandi.");
        }
    }

    private void launchBasicGoogleSignIn() {
        try {
            GoogleSignInOptions basicGso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestProfile()
                    .build();
            GoogleSignInClient basicClient = GoogleSignIn.getClient(this, basicGso);
            basicClient.signOut().addOnCompleteListener(this, task -> {
                try {
                    Intent signInIntent = basicClient.getSignInIntent();
                    startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
                } catch (Exception ex) {
                    AppDialogHelper.showError(this, "Google Sign-In Tidak Tersedia", "Pastikan Layanan Google Play aktif, atau masuk memakai email dan kata sandi.");
                }
            });
        } catch (Exception e) {
            AppDialogHelper.showError(this, "Google Sign-In Tidak Tersedia", "Pastikan Layanan Google Play aktif, atau masuk memakai email dan kata sandi.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
        }
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                isRetryingBasicGoogle = false;
                loginGoogleAccount(account);
            }
        } catch (ApiException e) {
            int statusCode = e.getStatusCode();
            if (statusCode == 12501) return;
            if (statusCode == 10 && !isRetryingBasicGoogle) {
                // Jika requestIdToken gagal (mismatch Web Client ID), coba dengan Google profile dasar
                isRetryingBasicGoogle = true;
                launchBasicGoogleSignIn();
                return;
            }
            AppDialogHelper.showError(this, "Gagal Masuk Google (" + statusCode + ")",
                    "Akun Google belum bisa dihubungkan. Coba lagi, atau masuk memakai email dan kata sandi.");
        }
    }

    private void loginGoogleAccount(GoogleSignInAccount account) {
        setLoading(true);
        String idToken = account.getIdToken();
        if (idToken != null && !idToken.isEmpty()) {
            userRepository.loginWithGoogle(idToken, new DataCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    setLoading(false);
                    proceedToDashboard(user);
                }

                @Override
                public void onError(String message) {
                    // Fallback mulus menggunakan email dan nama Google tanpa buka browser!
                    loginWithGoogleProfile(account);
                }
            });
        } else {
            loginWithGoogleProfile(account);
        }
    }

    /**
     * Dipanggil saat Google tidak memberikan ID token. Tanpa token yang diverifikasi server
     * kita tidak bisa membuktikan pemilik email, jadi pengguna diarahkan ke login email.
     */
    private void loginWithGoogleProfile(GoogleSignInAccount account) {
        setLoading(false);
        AppDialogHelper.showError(this, "Gagal Masuk Google", "Masuk dengan Google belum tersedia di perangkat ini. Silakan masuk memakai email dan kata sandi.");
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
                        "Tidak dapat membuka browser untuk login Google. Silakan masuk menggunakan Email & Password.");
            }
        }
    }

    private void showForgotPasswordDialog() {
        String prefill = etEmail != null ? etEmail.getText().toString().trim() : "";
        AppDialogHelper.showInput(this, "Lupa Kata Sandi?",
                "Masukkan email akunmu. Kami akan mengirim tautan untuk membuat kata sandi baru.",
                "nama@email.com", prefill, "Kirim Tautan",
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                email -> {
                    if (!com.carikostkita.util.FormatUtil.isValidEmail(email)) {
                        AppDialogHelper.showError(this, "Email Tidak Valid", "Periksa kembali penulisan email kamu (contoh: nama@email.com).");
                        return;
                    }
                    setLoading(true);
                    userRepository.sendPasswordReset(email, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean data) {
                            setLoading(false);
                            AppDialogHelper.showSuccessDialog(LoginActivity.this, "Cek Email Kamu",
                                    "Jika " + email + " terdaftar, tautan reset kata sandi sudah dikirim. Buka tautan itu dari HP ini untuk membuat kata sandi baru.");
                        }

                        @Override
                        public void onError(String message) {
                            setLoading(false);
                            AppDialogHelper.showError(LoginActivity.this, "Gagal Mengirim Email", message);
                        }
                    });
                });
    }

    /** Tautan reset dari email: carikostkita://reset-callback#access_token=...&type=recovery */
    private void handlePasswordRecovery(Uri uri) {
        Map<String, String> params = new HashMap<>();
        if (uri.getFragment() != null) params.putAll(parseFragmentParams(uri.getFragment()));
        String accessToken = params.get("access_token");
        String refreshToken = params.get("refresh_token");
        if (accessToken == null || accessToken.isEmpty()) {
            AppDialogHelper.showError(this, "Tautan Tidak Valid",
                    "Tautan reset sudah kedaluwarsa atau sudah dipakai. Minta tautan baru lewat \"Lupa kata sandi\".");
            return;
        }
        long expiresIn = 3600;
        try {
            if (params.get("expires_in") != null) expiresIn = Long.parseLong(params.get("expires_in"));
        } catch (Exception ignored) {}
        final long exp = expiresIn;
        AppDialogHelper.showInput(this, "Buat Kata Sandi Baru", "Minimal 6 karakter.",
                "Kata sandi baru", "", "Simpan",
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD,
                newPassword -> {
                    setLoading(true);
                    userRepository.setPasswordFromRecovery(accessToken, refreshToken, exp, newPassword, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean data) {
                            setLoading(false);
                            AppDialogHelper.showSuccessDialog(LoginActivity.this, "Kata Sandi Diperbarui",
                                    "Silakan masuk memakai kata sandi baru kamu.");
                        }

                        @Override
                        public void onError(String message) {
                            setLoading(false);
                            AppDialogHelper.showError(LoginActivity.this, "Gagal Menyimpan", message);
                        }
                    });
                });
    }

    private void proceedToDashboard(User user) {
        Toast.makeText(this, "Selamat datang, " + user.getNama(), Toast.LENGTH_SHORT).show();
        Intent intent;
        if (user.getRole() != null && user.getRole().isDeveloper()) {
            intent = new Intent(this, AdminMainActivity.class);
        } else if (user.getRole() == Role.PEMILIK_KOST) {
            intent = new Intent(this, PemilikMainActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        pbLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        btnLogin.setText(isLoading ? "" : "Masuk");
        if (btnLoginGoogle != null) {
            btnLoginGoogle.setEnabled(!isLoading);
        }
    }
}
