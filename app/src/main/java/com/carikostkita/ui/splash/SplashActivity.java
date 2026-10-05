package com.carikostkita.ui.splash;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.carikostkita.R;
import com.carikostkita.ui.admin.AdminMainActivity;
import com.carikostkita.ui.admin.PemilikMainActivity;
import com.carikostkita.ui.auth.LoginActivity;
import com.carikostkita.ui.main.MainActivity;
import com.carikostkita.util.SessionManager;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionManager session = new SessionManager(this);
            if (!session.isOnboardingCompleted()) {
                startActivity(new Intent(this, com.carikostkita.ui.onboarding.OnboardingActivity.class));
            } else if (session.isLoggedIn()) {
                if (session.isDeveloper()) {
                    startActivity(new Intent(this, AdminMainActivity.class));
                } else if (session.isPemilikKost()) {
                    startActivity(new Intent(this, PemilikMainActivity.class));
                } else {
                    startActivity(new Intent(this, MainActivity.class));
                }
            } else {
                // Onboarding complete, direct to Beranda (guest mode)
                startActivity(new Intent(this, MainActivity.class));
            }
            finish();
        }, 1200);
    }
}
