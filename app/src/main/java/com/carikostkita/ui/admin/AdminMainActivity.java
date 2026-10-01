package com.carikostkita.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.StatusKost;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.ui.adapter.AdminKostAdapter;
import com.carikostkita.ui.auth.LoginActivity;
import com.carikostkita.ui.main.MainActivity;
import com.carikostkita.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.util.List;

public class AdminMainActivity extends AppCompatActivity implements AdminKostAdapter.OnAdminKostClickListener {

    private TextView tvRoleSubtitle;
    private TextView tvDashboardTitle;
    private TextView tvStatTotal;
    private TextView tvStatTersedia;
    private TextView tvStatPenuh;
    private RecyclerView rvKost;
    private ProgressBar pbLoading;
    private AdminKostAdapter adapter;
    private KostRepository kostRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        kostRepository = new KostRepository(this);
        sessionManager = new SessionManager(this);

        initViews();
        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void initViews() {
        tvRoleSubtitle = findViewById(R.id.tv_admin_role_subtitle);
        tvDashboardTitle = findViewById(R.id.tv_admin_dashboard_title);
        tvStatTotal = findViewById(R.id.tv_stat_total);
        tvStatTersedia = findViewById(R.id.tv_stat_tersedia);
        tvStatPenuh = findViewById(R.id.tv_stat_penuh);
        rvKost = findViewById(R.id.rv_admin_kost);
        pbLoading = findViewById(R.id.pb_admin_loading);
        ImageButton btnLogout = findViewById(R.id.btn_admin_logout);
        MaterialButton btnFasilitas = findViewById(R.id.btn_admin_goto_fasilitas);
        MaterialButton btnUserMode = findViewById(R.id.btn_admin_open_user_app);
        ExtendedFloatingActionButton fabAdd = findViewById(R.id.fab_admin_add_kost);

        if (sessionManager.isDeveloper()) {
            tvRoleSubtitle.setText("Developer / Super Admin");
            tvDashboardTitle.setText("Kontrol Sistem & Properti");
        } else if (sessionManager.isPemilikKost()) {
            tvRoleSubtitle.setText("Pemilik Kost");
            tvDashboardTitle.setText("Dashboard Properti Saya");
        } else {
            tvRoleSubtitle.setText("Pengelola Kost");
            tvDashboardTitle.setText("Dashboard Kost");
        }

        adapter = new AdminKostAdapter(this);
        rvKost.setLayoutManager(new LinearLayoutManager(this));
        rvKost.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Konfirmasi Keluar")
                    .setMessage("Apakah Anda yakin ingin keluar dari akun Anda?")
                    .setPositiveButton("Ya, Keluar", (dialog, which) -> {
                        sessionManager.logout();
                        Toast.makeText(this, "Berhasil keluar", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });

        btnFasilitas.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminFasilitasActivity.class));
        });

        btnUserMode.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
        });

        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminKostFormActivity.class));
        });
    }

    private void loadDashboardData() {
        pbLoading.setVisibility(View.VISIBLE);

        // Load Stats
        kostRepository.getAdminStats(new DataCallback<KostRepository.AdminStats>() {
            @Override
            public void onSuccess(KostRepository.AdminStats stats) {
                tvStatTotal.setText(String.valueOf(stats.totalKost));
                tvStatTersedia.setText(String.valueOf(stats.totalTersedia));
                tvStatPenuh.setText(String.valueOf(stats.totalPenuh));
            }

            @Override
            public void onError(String message) {}
        });

        // Load Kost List
        kostRepository.getAllKostForAdmin(new DataCallback<List<Kost>>() {
            @Override
            public void onSuccess(List<Kost> data) {
                pbLoading.setVisibility(View.GONE);
                adapter.submitList(data);
            }

            @Override
            public void onError(String message) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEditClick(Kost kost) {
        Intent intent = new Intent(this, AdminKostFormActivity.class);
        intent.putExtra("kost_id", kost.getIdKost());
        startActivity(intent);
    }

    @Override
    public void onToggleStatusClick(Kost kost, int position) {
        StatusKost nextStatus;
        if (kost.getStatus() == StatusKost.TERSEDIA) {
            nextStatus = StatusKost.PENUH;
        } else if (kost.getStatus() == StatusKost.PENUH) {
            nextStatus = StatusKost.TIDAK_AKTIF;
        } else {
            nextStatus = StatusKost.TERSEDIA;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Ubah Status Kost")
                .setMessage("Ubah status \"" + kost.getNamaKost() + "\" menjadi " + nextStatus.getDisplayName() + "?")
                .setPositiveButton("Ya, Ubah", (dialog, which) -> {
                    kostRepository.updateStatus(kost.getIdKost(), nextStatus, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean success) {
                            kost.setStatus(nextStatus);
                            adapter.notifyItemChanged(position);
                            Toast.makeText(AdminMainActivity.this, "Status diubah menjadi: " + nextStatus.getDisplayName(), Toast.LENGTH_SHORT).show();
                            loadDashboardData();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
