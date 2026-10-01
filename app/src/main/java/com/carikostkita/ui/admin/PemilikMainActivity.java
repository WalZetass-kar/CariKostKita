package com.carikostkita.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.util.List;

import androidx.core.content.ContextCompat;
import com.carikostkita.data.model.KostVerificationStatus;
import java.util.ArrayList;

public class PemilikMainActivity extends AppCompatActivity implements AdminKostAdapter.OnAdminKostClickListener {

    private TextView tvWelcomeName;
    private TextView tvStatTotal;
    private TextView tvStatAktif;
    private TextView tvStatPending;
    private TextView tvStatRevisi;
    private TextView tvKostCount;
    private TextView chipAll, chipApproved, chipPending, chipRevision;
    private RecyclerView rvKost;
    private ProgressBar pbLoading;
    private LinearLayout layoutEmpty;
    private AdminKostAdapter adapter;
    private KostRepository kostRepository;
    private SessionManager sessionManager;
    private List<Kost> allOwnerKosts = new ArrayList<>();
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pemilik_main);

        kostRepository = new KostRepository(this);
        sessionManager = new SessionManager(this);

        // Role-Based Access Control: ensure user is verified Pemilik Kost or Admin
        if (!sessionManager.isPemilikKost() && !sessionManager.isDeveloper()) {
            Toast.makeText(this, "Akses ditolak: Hanya pemilik kost terverifikasi yang dapat mengakses halaman ini", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        initViews();
        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void initViews() {
        tvWelcomeName = findViewById(R.id.tv_pemilik_welcome_name);
        tvStatTotal = findViewById(R.id.tv_pemilik_stat_total);
        tvStatAktif = findViewById(R.id.tv_pemilik_stat_aktif);
        tvStatPending = findViewById(R.id.tv_pemilik_stat_pending);
        tvStatRevisi = findViewById(R.id.tv_pemilik_stat_revisi);
        tvKostCount = findViewById(R.id.tv_pemilik_count);
        rvKost = findViewById(R.id.rv_pemilik_kost);
        pbLoading = findViewById(R.id.pb_pemilik_loading);
        layoutEmpty = findViewById(R.id.layout_pemilik_empty);
        ImageButton btnLogout = findViewById(R.id.btn_pemilik_logout);
        MaterialButton btnChats = findViewById(R.id.btn_pemilik_chats);
        MaterialButton btnSwitchUser = findViewById(R.id.btn_pemilik_switch_user);
        ExtendedFloatingActionButton fabAdd = findViewById(R.id.fab_pemilik_add_kost);

        chipAll = findViewById(R.id.chip_pemilik_filter_all);
        chipApproved = findViewById(R.id.chip_pemilik_filter_approved);
        chipPending = findViewById(R.id.chip_pemilik_filter_pending);
        chipRevision = findViewById(R.id.chip_pemilik_filter_revision);

        chipAll.setOnClickListener(v -> applyFilter("ALL"));
        chipApproved.setOnClickListener(v -> applyFilter("APPROVED"));
        chipPending.setOnClickListener(v -> applyFilter("PENDING"));
        chipRevision.setOnClickListener(v -> applyFilter("REVISION"));

        tvWelcomeName.setText(sessionManager.getUserName());

        adapter = new AdminKostAdapter(this);
        rvKost.setLayoutManager(new LinearLayoutManager(this));
        rvKost.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> {
            AppDialogHelper.showLogout(this, () -> {
                sessionManager.logout();
                Toast.makeText(this, "Berhasil keluar dari akun", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });

        btnChats.setOnClickListener(v -> {
            // Open public user app with tab 3 (Chat tab)
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("navigate_tab", 3);
            startActivity(intent);
        });

        btnSwitchUser.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminKostFormActivity.class);
            intent.putExtra("owner_id", sessionManager.getUserId());
            startActivity(intent);
        });
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        updateChipStyles();
        List<Kost> filtered = new ArrayList<>();
        for (Kost k : allOwnerKosts) {
            if ("ALL".equals(filter)) {
                filtered.add(k);
            } else if ("APPROVED".equals(filter) && k.getVerificationStatus() == KostVerificationStatus.APPROVED) {
                filtered.add(k);
            } else if ("PENDING".equals(filter) && k.getVerificationStatus() == KostVerificationStatus.PENDING) {
                filtered.add(k);
            } else if ("REVISION".equals(filter) && k.getVerificationStatus() == KostVerificationStatus.REVISION_REQUIRED) {
                filtered.add(k);
            }
        }
        if (filtered.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvKost.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvKost.setVisibility(View.VISIBLE);
            adapter.submitList(filtered);
        }
        tvKostCount.setText(filtered.size() + " properti");
    }

    private void updateChipStyles() {
        int activeBg = R.drawable.bg_chip_filter_active;
        int inactiveBg = R.drawable.bg_chip_filter_inactive;
        int activeColor = ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);

        chipAll.setBackgroundResource("ALL".equals(currentFilter) ? activeBg : inactiveBg);
        chipAll.setTextColor("ALL".equals(currentFilter) ? activeColor : inactiveColor);
        chipApproved.setBackgroundResource("APPROVED".equals(currentFilter) ? activeBg : inactiveBg);
        chipApproved.setTextColor("APPROVED".equals(currentFilter) ? activeColor : inactiveColor);
        chipPending.setBackgroundResource("PENDING".equals(currentFilter) ? activeBg : inactiveBg);
        chipPending.setTextColor("PENDING".equals(currentFilter) ? activeColor : inactiveColor);
        chipRevision.setBackgroundResource("REVISION".equals(currentFilter) ? activeBg : inactiveBg);
        chipRevision.setTextColor("REVISION".equals(currentFilter) ? activeColor : inactiveColor);
    }

    private void loadDashboardData() {
        int ownerId = sessionManager.getUserId();
        pbLoading.setVisibility(View.VISIBLE);

        // Load Owner Stats
        kostRepository.getPemilikStats(ownerId, new DataCallback<KostRepository.PemilikStats>() {
            @Override
            public void onSuccess(KostRepository.PemilikStats stats) {
                tvStatTotal.setText(String.valueOf(stats.totalKost));
                tvStatAktif.setText(String.valueOf(stats.totalAktif));
                tvStatPending.setText(String.valueOf(stats.totalPending));
                tvStatRevisi.setText(String.valueOf(stats.totalRevisi));
            }

            @Override
            public void onError(String message) {}
        });

        // Load Kost List for this Owner Only
        kostRepository.getKostByPemilik(ownerId, new DataCallback<List<Kost>>() {
            @Override
            public void onSuccess(List<Kost> data) {
                pbLoading.setVisibility(View.GONE);
                allOwnerKosts = data;
                applyFilter(currentFilter);
            }

            @Override
            public void onError(String message) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(PemilikMainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEditClick(Kost kost) {
        Intent intent = new Intent(this, AdminKostFormActivity.class);
        intent.putExtra("kost_id", kost.getIdKost());
        intent.putExtra("owner_id", sessionManager.getUserId());
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

        AppDialogHelper.showConfirm(this,
                "Ubah Status Kost",
                "Ubah status properti \"" + kost.getNamaKost() + "\" menjadi " + nextStatus.getDisplayName() + "?",
                "Ya, Ubah Status",
                () -> {
                    kostRepository.updateStatus(kost.getIdKost(), nextStatus, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean success) {
                            kost.setStatus(nextStatus);
                            adapter.notifyItemChanged(position);
                            Toast.makeText(PemilikMainActivity.this, "Status diubah menjadi: " + nextStatus.getDisplayName(), Toast.LENGTH_SHORT).show();
                            loadDashboardData();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(PemilikMainActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }
}
