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

        // Muat background visual halaman login (Jembatan Siak Pekanbaru)
        ImageView ivLoginBg = findViewById(R.id.iv_login_bg);
        if (ivLoginBg != null) {
            String bgUrl = "https://lh3.googleusercontent.com/gps-cs-s/AHRPTWlVcBc3cLykeucw8kEFM9FuTxjHkD71cxOKLKHnI1itd9bN_4wAtoXVi_mmwDc7D4cXZyog5ZPQ3mCwQY0zylh1icrDlzf7llZnVB1H8OKYGVJT5IOPsOb6lwCN0NQv5ZPL2Sfmbg=s680-w680-h510";
            Glide.with(this)
                    .load(bgUrl)
                    .centerCrop()
                    .placeholder(R.drawable.bg_login_pekanbaru)
                    .error(R.drawable.bg_login_pekanbaru)
                    .into(ivLoginBg);
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
        String password = etPassword.getText().toString().trim();

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
                if (errorDesc.contains("exchanging code")) {
                    AppDialogHelper.showError(this, "Konfigurasi Google Belum Lengkap",
                            "Client Secret di Dashboard Supabase belum sesuai dengan Web Client ID. Pastikan Client Secret dari Web Client dimasukkan ke Supabase Dashboard -> Authentication -> Providers -> Google.");
                } else {
                    AppDialogHelper.showError(this, "Login Google Dibatalkan", errorDesc);
                }
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
                    AppDialogHelper.showError(this, "Gagal Membuka Google Sign-In", e.getMessage());
                }
            });
        } catch (Exception e) {
            AppDialogHelper.showError(this, "Gagal Membuka Google Sign-In", e.getMessage());
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
                    "Tidak dapat menghubungkan akun Google: " + e.getMessage());
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

    private void loginWithGoogleProfile(GoogleSignInAccount account) {
        userRepository.loginWithGoogle(
                account.getEmail(),
                account.getDisplayName(),
                account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "",
                new DataCallback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        setLoading(false);
                        proceedToDashboard(user);
                    }

                    @Override
                    public void onError(String errorMsg) {
                        setLoading(false);
                        AppDialogHelper.showError(LoginActivity.this, "Gagal Masuk Google", errorMsg);
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
                        "Tidak dapat membuka browser untuk login Google. Silakan masuk menggunakan Email & Password.");
            }
        }
    }

    private void showForgotPasswordDialog() {
        AppDialogHelper.showInfo(this, "Lupa Password?",
                "Untuk memulihkan akun Anda, silakan hubungi Customer Support via WhatsApp admin di Pekanbaru atau masuk langsung menggunakan akun Google yang terdaftar.");
    }

    private void proceedToDashboard(User user) {
        Toast.makeText(this, "Selamat datang, " + user.getNama(), Toast.LENGTH_SHORT).show();
        Intent intent;
        if (user.getRole() == Role.ADMIN) {
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
        if (btnLoginGoogle != null) {
            btnLoginGoogle.setEnabled(!isLoading);
        }
    }
}
