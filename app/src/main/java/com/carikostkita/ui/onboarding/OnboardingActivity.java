package com.carikostkita.ui.onboarding;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.carikostkita.R;
import com.carikostkita.ui.admin.AdminMainActivity;
import com.carikostkita.ui.admin.PemilikMainActivity;
import com.carikostkita.ui.auth.LoginActivity;
import com.carikostkita.ui.main.MainActivity;
import com.carikostkita.util.SessionManager;
import com.google.android.material.button.MaterialButton;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private View dot1, dot2, dot3;
    private MaterialButton btnAction;
    private TextView btnSkip;
    private SessionManager sessionManager;
    private Dialog locationDialog;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                finishOnboardingAndNavigate();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        sessionManager = new SessionManager(this);

        viewPager = findViewById(R.id.vp_onboarding);
        dot1 = findViewById(R.id.dot_1);
        dot2 = findViewById(R.id.dot_2);
        dot3 = findViewById(R.id.dot_3);
        btnAction = findViewById(R.id.btn_onboard_action);
        btnSkip = findViewById(R.id.btn_onboard_skip);

        OnboardingPagerAdapter adapter = new OnboardingPagerAdapter();
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
            }
        });

        btnAction.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < 2) {
                viewPager.setCurrentItem(current + 1, true);
            } else {
                showLocationPermissionDialog();
            }
        });

        btnSkip.setOnClickListener(v -> showLocationPermissionDialog());
    }

    private void updateIndicators(int position) {
        // Reset all dots to inactive oval (8dp x 8dp)
        ViewGroup.LayoutParams p1 = dot1.getLayoutParams();
        p1.width = dpToPx(8);
        dot1.setLayoutParams(p1);
        dot1.setBackgroundResource(R.drawable.bg_dot_inactive);

        ViewGroup.LayoutParams p2 = dot2.getLayoutParams();
        p2.width = dpToPx(8);
        dot2.setLayoutParams(p2);
        dot2.setBackgroundResource(R.drawable.bg_dot_inactive);

        ViewGroup.LayoutParams p3 = dot3.getLayoutParams();
        p3.width = dpToPx(8);
        dot3.setLayoutParams(p3);
        dot3.setBackgroundResource(R.drawable.bg_dot_inactive);

        // Highlight active dot as wide pill (24dp x 8dp)
        View activeDot = (position == 0) ? dot1 : (position == 1) ? dot2 : dot3;
        ViewGroup.LayoutParams activeP = activeDot.getLayoutParams();
        activeP.width = dpToPx(24);
        activeDot.setLayoutParams(activeP);
        activeDot.setBackgroundResource(R.drawable.bg_dot_active);

        if (position == 2) {
            btnAction.setText("Mulai Cari Kost");
            btnSkip.setVisibility(View.INVISIBLE);
        } else {
            btnAction.setText("Lanjut");
            btnSkip.setVisibility(View.VISIBLE);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void showLocationPermissionDialog() {
        if (locationDialog != null && locationDialog.isShowing()) {
            return;
        }

        locationDialog = new Dialog(this);
        locationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        locationDialog.setContentView(R.layout.dialog_onboarding_location);
        if (locationDialog.getWindow() != null) {
            locationDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            locationDialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        locationDialog.setCancelable(false);

        View btnAllow = locationDialog.findViewById(R.id.btn_dialog_allow_location);
        View btnSkipLoc = locationDialog.findViewById(R.id.btn_dialog_skip_location);

        if (btnAllow != null) {
            btnAllow.setOnClickListener(v -> {
                locationDialog.dismiss();
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            });
        }

        if (btnSkipLoc != null) {
            btnSkipLoc.setOnClickListener(v -> {
                locationDialog.dismiss();
                finishOnboardingAndNavigate();
            });
        }

        locationDialog.show();
    }

    private void finishOnboardingAndNavigate() {
        sessionManager.setOnboardingCompleted(true);
        sessionManager.setFirstTimeLaunch(false);

        Intent intent;
        if (sessionManager.isLoggedIn()) {
            if (sessionManager.isDeveloper()) {
                intent = new Intent(this, AdminMainActivity.class);
            } else if (sessionManager.isPemilikKost()) {
                intent = new Intent(this, PemilikMainActivity.class);
            } else {
                intent = new Intent(this, MainActivity.class);
            }
        } else {
            // Beranda pencari langsung terbuka untuk user baru yang belum login
            intent = new Intent(this, MainActivity.class);
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // Inner Adapter for Onboarding Slides
    private static class OnboardingPagerAdapter extends RecyclerView.Adapter<OnboardingPagerAdapter.ViewHolder> {

        private final int[] images = {
                R.drawable.il_onboarding_house,
                R.drawable.il_onboarding_map,
                R.drawable.il_onboarding_chat
        };

        private final String[] titles = {
                "Temukan Kost yang Cocok",
                "Cari Kost Terdekat",
                "Chat Pemilik Kost"
        };

        private final String[] descs = {
                "Pilih hunian kost impian dengan filter lengkap, fasilitas detail, dan informasi terpercaya.",
                "Jelajahi kost strategis di sekitar kampus dan tempat kerja langsung melalui peta interaktif.",
                "Hubungi langsung pemilik kost via WhatsApp atau chat aplikasi untuk memastikan ketersediaan kamar."
        };

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_page, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.ivImage.setImageResource(images[position]);
            holder.tvTitle.setText(titles[position]);
            holder.tvDesc.setText(descs[position]);
        }

        @Override
        public int getItemCount() {
            return titles.length;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivImage;
            TextView tvTitle, tvDesc;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivImage = itemView.findViewById(R.id.iv_onboard_image);
                tvTitle = itemView.findViewById(R.id.tv_onboard_title);
                tvDesc = itemView.findViewById(R.id.tv_onboard_desc);
            }
        }
    }
}
