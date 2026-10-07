package com.carikostkita.ui.splash;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.carikostkita.R;
import com.carikostkita.data.model.User;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.UserRepository;
import com.carikostkita.ui.admin.AdminMainActivity;
import com.carikostkita.ui.admin.PemilikMainActivity;
import com.carikostkita.ui.main.MainActivity;
import com.carikostkita.ui.onboarding.OnboardingActivity;
import com.carikostkita.util.ErrorMessages;
import com.carikostkita.util.SessionManager;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    /** Splash minimal agar logo tidak berkedip, dan batas tunggu server agar tidak terasa lambat. */
    private static final long MIN_SPLASH_MS = 900;
    private static final long MAX_WAIT_MS = 2500;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean routed = false;
    private long startedAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        startedAt = System.currentTimeMillis();
        playIntroAnimation();

        SessionManager session = new SessionManager(this);
        if (!session.isOnboardingCompleted()) {
            routeAfterMinimum(() -> startActivity(new Intent(this, OnboardingActivity.class)));
            return;
        }
        if (!session.isLoggedIn()) {
            routeAfterMinimum(() -> startActivity(new Intent(this, MainActivity.class)));
            return;
        }

        // Ambil peran & status akun terbaru (bisa berubah oleh developer sejak terakhir dibuka)
        handler.postDelayed(() -> routeAfterMinimum(this::openHomeForRole), MAX_WAIT_MS);
        new UserRepository(this).refreshCurrentUser(new DataCallback<User>() {
            @Override
            public void onSuccess(User data) {
                routeAfterMinimum(SplashActivity.this::openHomeForRole);
            }

            @Override
            public void onError(String message) {
                if (!new SessionManager(SplashActivity.this).isLoggedIn()) {
                    // Akun dinonaktifkan / dihapus / sesi berakhir
                    Toast.makeText(SplashActivity.this, message, Toast.LENGTH_LONG).show();
                } else if (!ErrorMessages.isOffline(message)) {
                    android.util.Log.w("SplashActivity", "Sinkron profil gagal: " + message);
                }
                routeAfterMinimum(SplashActivity.this::openHomeForRole);
            }
        });
    }

    /** Animasi masuk ≤500ms; titik pemuatan berdenyut selama sinkron profil. */
    private void playIntroAnimation() {
        android.view.animation.Interpolator ease = new androidx.interpolator.view.animation.FastOutSlowInInterpolator();
        View logo = findViewById(R.id.frame_splash_logo);
        View title = findViewById(R.id.tv_splash_title);
        View accent = findViewById(R.id.view_splash_accent);
        View subtitle = findViewById(R.id.tv_splash_subtitle);
        View skyline = findViewById(R.id.iv_splash_skyline);
        View ornament = findViewById(R.id.iv_splash_ornament);
        View dots = findViewById(R.id.layout_splash_dots);
        float lift = 12 * getResources().getDisplayMetrics().density;

        if (logo != null) logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(420).setInterpolator(ease).start();
        if (ornament != null) ornament.animate().alpha(1f).setDuration(500).start();
        if (skyline != null) {
            skyline.setTranslationY(lift);
            skyline.animate().alpha(1f).translationY(0f).setStartDelay(80).setDuration(460).setInterpolator(ease).start();
        }
        View[] texts = {title, accent, subtitle};
        for (int i = 0; i < texts.length; i++) {
            View t = texts[i];
            if (t == null) continue;
            t.setTranslationY(lift / 2);
            t.animate().alpha(1f).translationY(0f).setStartDelay(160 + i * 60L).setDuration(340).setInterpolator(ease).start();
        }
        if (dots != null) {
            dots.animate().alpha(1f).setStartDelay(420).setDuration(200).start();
            int[] ids = {R.id.dot_splash_1, R.id.dot_splash_2, R.id.dot_splash_3};
            for (int i = 0; i < ids.length; i++) {
                View dot = findViewById(ids[i]);
                if (dot == null) continue;
                android.animation.ObjectAnimator pulse = android.animation.ObjectAnimator.ofFloat(dot, View.ALPHA, 1f, 0.25f);
                pulse.setDuration(480);
                pulse.setStartDelay(i * 160L);
                pulse.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                pulse.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                pulse.start();
            }
        }
    }

    private void openHomeForRole() {
        SessionManager session = new SessionManager(this);
        Class<?> target = MainActivity.class;
        if (session.isLoggedIn()) {
            if (session.isDeveloper()) target = AdminMainActivity.class;
            else if (session.isPemilikKost()) target = PemilikMainActivity.class;
        }
        startActivity(new Intent(this, target));
    }

    private void routeAfterMinimum(Runnable navigation) {
        long remaining = MIN_SPLASH_MS - (System.currentTimeMillis() - startedAt);
        handler.postDelayed(() -> {
            if (routed || isFinishing()) return;
            routed = true;
            navigation.run();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, Math.max(0, remaining));
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
