package com.carikostkita.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.KostReport;
import com.carikostkita.data.model.KostVerificationStatus;
import com.carikostkita.data.model.ReportStatus;
import com.carikostkita.data.model.StatusKost;
import com.carikostkita.data.model.SystemActivityLog;
import com.carikostkita.data.model.User;
import com.carikostkita.data.repository.ActivityLogRepository;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.data.repository.ReportRepository;
import com.carikostkita.data.repository.UserRepository;
import com.carikostkita.ui.adapter.AdminActivityLogAdapter;
import com.carikostkita.ui.adapter.AdminKostAdapter;
import com.carikostkita.ui.adapter.AdminReportAdapter;
import com.carikostkita.ui.adapter.AdminUserAdapter;
import com.carikostkita.ui.adapter.VerificationAdapter;
import com.carikostkita.ui.auth.LoginActivity;
import com.carikostkita.ui.detail.DetailKostActivity;
import com.carikostkita.ui.main.MainActivity;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class AdminMainActivity extends AppCompatActivity implements
        AdminKostAdapter.OnAdminKostClickListener,
        VerificationAdapter.OnVerificationActionListener,
        AdminUserAdapter.OnUserActionListener,
        AdminReportAdapter.OnReportActionListener {

    private enum AdminTab {
        VERIF_PEMILIK,
        VERIF_KOST,
        ALL_KOST,
        USERS,
        REPORTS,
        LOGS
    }

    private AdminTab currentTab = AdminTab.VERIF_PEMILIK;

    // Stat metric views
    private TextView tvStatTotal;
    private TextView tvStatTotalUser;
    private TextView tvStatTotalPemilik;
    private TextView tvStatAntreanVerif;
    private TextView tvStatAntreanKost;
    private TextView tvStatAntreanLaporan;

    // Navigation tab views
    private TextView tabVerifPemilik;
    private TextView tabVerifKost;
    private TextView tabAllKost;
    private TextView tabUsers;
    private TextView tabReports;
    private TextView tabLogs;

    // Dynamic section views
    private TextView tvSectionTitle;
    private TextView tvSectionCount;
    private LinearLayout layoutEmpty;
    private ImageView ivEmptyIcon;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;
    private ProgressBar pbLoading;
    private RecyclerView rvContent;

    // Repositories
    private KostRepository kostRepository;
    private UserRepository userRepository;
    private ReportRepository reportRepository;
    private ActivityLogRepository activityLogRepository;
    private SessionManager sessionManager;

    // Adapters
    private VerificationAdapter verifPemilikAdapter;
    private AdminKostAdapter pendingKostAdapter;
    private AdminKostAdapter allKostAdapter;
    private AdminUserAdapter userAdapter;
    private AdminReportAdapter reportAdapter;
    private AdminActivityLogAdapter logAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        kostRepository = new KostRepository(this);
        userRepository = new UserRepository(this);
        reportRepository = new ReportRepository(this);
        activityLogRepository = new ActivityLogRepository(this);
        sessionManager = new SessionManager(this);

        // Security RBAC: Only ADMIN (Developer) allowed in Developer Control Center!
        if (!sessionManager.isDeveloper()) {
            Toast.makeText(this, "Akses ditolak: Halaman ini hanya untuk Developer / Super Admin", Toast.LENGTH_LONG).show();
            if (sessionManager.isPemilikKost()) {
                startActivity(new Intent(this, PemilikMainActivity.class));
            } else {
                startActivity(new Intent(this, MainActivity.class));
            }
            finish();
            return;
        }

        initViews();
        setupAdapters();
        loadAllStats();
        switchTab(AdminTab.VERIF_PEMILIK);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllStats();
        loadActiveTabData();
    }

    private void initViews() {
        tvStatTotal = findViewById(R.id.tv_stat_total);
        tvStatTotalUser = findViewById(R.id.tv_stat_total_user);
        tvStatTotalPemilik = findViewById(R.id.tv_stat_total_pemilik);
        tvStatAntreanVerif = findViewById(R.id.tv_stat_antrean_verif);
        tvStatAntreanKost = findViewById(R.id.tv_stat_antrean_kost);
        tvStatAntreanLaporan = findViewById(R.id.tv_stat_antrean_laporan);

        tabVerifPemilik = findViewById(R.id.tab_admin_verif_pemilik);
        tabVerifKost = findViewById(R.id.tab_admin_verif_kost);
        tabAllKost = findViewById(R.id.tab_admin_all_kost);
        tabUsers = findViewById(R.id.tab_admin_users);
        tabReports = findViewById(R.id.tab_admin_reports);
        tabLogs = findViewById(R.id.tab_admin_logs);

        tvSectionTitle = findViewById(R.id.tv_admin_section_title);
        tvSectionCount = findViewById(R.id.tv_admin_section_count);
        layoutEmpty = findViewById(R.id.layout_admin_empty);
        ivEmptyIcon = findViewById(R.id.iv_admin_empty_icon);
        tvEmptyTitle = findViewById(R.id.tv_admin_empty_title);
        tvEmptyDesc = findViewById(R.id.tv_admin_empty_desc);
        pbLoading = findViewById(R.id.pb_admin_loading);
        rvContent = findViewById(R.id.rv_admin_content);

        ImageButton btnLogout = findViewById(R.id.btn_admin_logout);
        MaterialButton btnFasilitas = findViewById(R.id.btn_admin_goto_fasilitas);
        MaterialButton btnUserMode = findViewById(R.id.btn_admin_open_user_app);
        ExtendedFloatingActionButton fabAdd = findViewById(R.id.fab_admin_add_kost);

        rvContent.setLayoutManager(new LinearLayoutManager(this));

        // Tab click listeners
        tabVerifPemilik.setOnClickListener(v -> switchTab(AdminTab.VERIF_PEMILIK));
        tabVerifKost.setOnClickListener(v -> switchTab(AdminTab.VERIF_KOST));
        tabAllKost.setOnClickListener(v -> switchTab(AdminTab.ALL_KOST));
        tabUsers.setOnClickListener(v -> switchTab(AdminTab.USERS));
        tabReports.setOnClickListener(v -> switchTab(AdminTab.REPORTS));
        tabLogs.setOnClickListener(v -> switchTab(AdminTab.LOGS));

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

        btnFasilitas.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminFasilitasActivity.class));
        });

        btnUserMode.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
        });

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminKostFormActivity.class);
            intent.putExtra("owner_id", sessionManager.getUserId());
            startActivity(intent);
        });
    }

    private void setupAdapters() {
        verifPemilikAdapter = new VerificationAdapter(this);
        pendingKostAdapter = new AdminKostAdapter(this);
        allKostAdapter = new AdminKostAdapter(this);
        userAdapter = new AdminUserAdapter(this);
        reportAdapter = new AdminReportAdapter(this);
        logAdapter = new AdminActivityLogAdapter();
    }

    private void switchTab(AdminTab tab) {
        currentTab = tab;
        updateTabStyles();
        loadActiveTabData();
    }

    private void updateTabStyles() {
        int activeBg = R.drawable.bg_chip_filter_active;
        int inactiveBg = R.drawable.bg_chip_filter_inactive;
        int activeColor = ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);

        tabVerifPemilik.setBackgroundResource(currentTab == AdminTab.VERIF_PEMILIK ? activeBg : inactiveBg);
        tabVerifPemilik.setTextColor(currentTab == AdminTab.VERIF_PEMILIK ? activeColor : inactiveColor);

        tabVerifKost.setBackgroundResource(currentTab == AdminTab.VERIF_KOST ? activeBg : inactiveBg);
        tabVerifKost.setTextColor(currentTab == AdminTab.VERIF_KOST ? activeColor : inactiveColor);

        tabAllKost.setBackgroundResource(currentTab == AdminTab.ALL_KOST ? activeBg : inactiveBg);
        tabAllKost.setTextColor(currentTab == AdminTab.ALL_KOST ? activeColor : inactiveColor);

        tabUsers.setBackgroundResource(currentTab == AdminTab.USERS ? activeBg : inactiveBg);
        tabUsers.setTextColor(currentTab == AdminTab.USERS ? activeColor : inactiveColor);

        tabReports.setBackgroundResource(currentTab == AdminTab.REPORTS ? activeBg : inactiveBg);
        tabReports.setTextColor(currentTab == AdminTab.REPORTS ? activeColor : inactiveColor);

        tabLogs.setBackgroundResource(currentTab == AdminTab.LOGS ? activeBg : inactiveBg);
        tabLogs.setTextColor(currentTab == AdminTab.LOGS ? activeColor : inactiveColor);
    }

    private void loadAllStats() {
        // Platform Global Kost Stats
        kostRepository.getAdminStats(new DataCallback<KostRepository.AdminStats>() {
            @Override
            public void onSuccess(KostRepository.AdminStats stats) {
                tvStatTotal.setText(String.valueOf(stats.totalKost));
                tvStatAntreanKost.setText(String.valueOf(stats.totalPending));
            }
            @Override
            public void onError(String message) {}
        });

        // Platform User Stats
        userRepository.getUserStats(new DataCallback<UserRepository.UserStats>() {
            @Override
            public void onSuccess(UserRepository.UserStats stats) {
                tvStatTotalUser.setText(String.valueOf(stats.totalPencari));
                tvStatTotalPemilik.setText(String.valueOf(stats.totalPemilik));
                tvStatAntreanVerif.setText(String.valueOf(stats.pendingVerifikasi));
            }
            @Override
            public void onError(String message) {}
        });

        // Platform Reports Stats
        reportRepository.countPendingReports(new DataCallback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                tvStatAntreanLaporan.setText(String.valueOf(count));
            }
            @Override
            public void onError(String message) {}
        });
    }

    private void loadActiveTabData() {
        pbLoading.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        rvContent.setVisibility(View.GONE);

        switch (currentTab) {
            case VERIF_PEMILIK:
                tvSectionTitle.setText("Verifikasi Calon Pemilik Kost");
                rvContent.setAdapter(verifPemilikAdapter);
                userRepository.getPendingVerifications(new DataCallback<List<User>>() {
                    @Override
                    public void onSuccess(List<User> list) {
                        pbLoading.setVisibility(View.GONE);
                        tvSectionCount.setText(list.size() + " antrean");
                        if (list.isEmpty()) {
                            showEmptyState("Tidak Ada Pengajuan Pemilik",
                                    "Semua pengajuan pendaftaran pemilik kost telah diproses oleh admin.");
                        } else {
                            rvContent.setVisibility(View.VISIBLE);
                            verifPemilikAdapter.submitList(list);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        pbLoading.setVisibility(View.GONE);
                        showEmptyState("Gagal Memuat Antrean", message);
                    }
                });
                break;

            case VERIF_KOST:
                tvSectionTitle.setText("Verifikasi Properti Kost");
                rvContent.setAdapter(pendingKostAdapter);
                kostRepository.getPendingKosts(new DataCallback<List<Kost>>() {
                    @Override
                    public void onSuccess(List<Kost> list) {
                        pbLoading.setVisibility(View.GONE);
                        tvSectionCount.setText(list.size() + " properti");
                        if (list.isEmpty()) {
                            showEmptyState("Tidak Ada Antrean Kost",
                                    "Semua properti kost baru atau revisi sudah selesai diverifikasi.");
                        } else {
                            rvContent.setVisibility(View.VISIBLE);
                            pendingKostAdapter.submitList(list);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        pbLoading.setVisibility(View.GONE);
                        showEmptyState("Gagal Memuat Data", message);
                    }
                });
                break;

            case ALL_KOST:
                tvSectionTitle.setText("Monitoring Seluruh Properti Kost");
                rvContent.setAdapter(allKostAdapter);
                kostRepository.getAllKostForAdmin(new DataCallback<List<Kost>>() {
                    @Override
                    public void onSuccess(List<Kost> list) {
                        pbLoading.setVisibility(View.GONE);
                        tvSectionCount.setText(list.size() + " properti");
                        if (list.isEmpty()) {
                            showEmptyState("Belum Ada Properti Kost",
                                    "Belum ada data properti kost yang terdaftar di basis data sistem.");
                        } else {
                            rvContent.setVisibility(View.VISIBLE);
                            allKostAdapter.submitList(list);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        pbLoading.setVisibility(View.GONE);
                        showEmptyState("Gagal Memuat Data", message);
                    }
                });
                break;

            case USERS:
                tvSectionTitle.setText("Manajemen Seluruh Pengguna");
                rvContent.setAdapter(userAdapter);
                userRepository.getAllUsers(new DataCallback<List<User>>() {
                    @Override
                    public void onSuccess(List<User> list) {
                        pbLoading.setVisibility(View.GONE);
                        tvSectionCount.setText(list.size() + " pengguna");
                        if (list.isEmpty()) {
                            showEmptyState("Tidak Ada Pengguna", "Tidak ada akun terdaftar.");
                        } else {
                            rvContent.setVisibility(View.VISIBLE);
                            userAdapter.submitList(list);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        pbLoading.setVisibility(View.GONE);
                        showEmptyState("Gagal Memuat Pengguna", message);
                    }
                });
                break;

            case REPORTS:
                tvSectionTitle.setText("Moderasi Laporan Pengguna");
                rvContent.setAdapter(reportAdapter);
                reportRepository.getAllReports(new DataCallback<List<KostReport>>() {
                    @Override
                    public void onSuccess(List<KostReport> list) {
                        pbLoading.setVisibility(View.GONE);
                        tvSectionCount.setText(list.size() + " laporan");
                        if (list.isEmpty()) {
                            showEmptyState("Belum Ada Laporan",
                                    "Kondisi ekosistem bersih! Tidak ada laporan bermasalah dari pencari kost.");
                        } else {
                            rvContent.setVisibility(View.VISIBLE);
                            reportAdapter.submitList(list);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        pbLoading.setVisibility(View.GONE);
                        showEmptyState("Gagal Memuat Laporan", message);
                    }
                });
                break;

            case LOGS:
                tvSectionTitle.setText("Log Audit Aktivitas Sistem");
                rvContent.setAdapter(logAdapter);
                activityLogRepository.getAllLogs(new DataCallback<List<SystemActivityLog>>() {
                    @Override
                    public void onSuccess(List<SystemActivityLog> list) {
                        pbLoading.setVisibility(View.GONE);
                        tvSectionCount.setText(list.size() + " aktivitas");
                        if (list.isEmpty()) {
                            showEmptyState("Belum Ada Log Aktivitas",
                                    "Aktivitas sistem yang tercatat akan muncul di sini.");
                        } else {
                            rvContent.setVisibility(View.VISIBLE);
                            logAdapter.submitList(list);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        pbLoading.setVisibility(View.GONE);
                        showEmptyState("Gagal Memuat Log", message);
                    }
                });
                break;
        }
    }

    private void showEmptyState(String title, String desc) {
        layoutEmpty.setVisibility(View.VISIBLE);
        rvContent.setVisibility(View.GONE);
        tvEmptyTitle.setText(title);
        tvEmptyDesc.setText(desc);
    }

    // =========================================================================
    // 1. OWNER VERIFICATION ACTIONS
    // =========================================================================
    @Override
    public void onApprove(User user, int position) {
        AppDialogHelper.showConfirm(this,
                "Setujui Pemilik Kost",
                "Verifikasi akun \"" + user.getNama() + "\" (" + user.getEmail() + ") sebagai Pemilik Kost resmi di CariKostKita?",
                "Ya, Setujui",
                () -> {
                    userRepository.approveOwnerVerification(user.getIdUser(), new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean success) {
                            activityLogRepository.logActivity(sessionManager.getUserId(), "APPROVE_OWNER",
                                    "Admin menyetujui akun " + user.getNama() + " (" + user.getEmail() + ") sebagai Pemilik Kost",
                                    "user", user.getIdUser());
                            AppDialogHelper.showSuccess(AdminMainActivity.this,
                                    "Berhasil Disetujui",
                                    "Akun " + user.getNama() + " kini telah terverifikasi sebagai Pemilik Kost.",
                                    () -> {
                                        loadAllStats();
                                        loadActiveTabData();
                                    });
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    @Override
    public void onRequireRevision(User user, int position) {
        AppDialogHelper.showInput(this,
                "Minta Perbaikan / Revisi",
                "Tuliskan catatan perbaikan atau persyaratan yang harus dilengkapi oleh " + user.getNama() + ":",
                "Contoh: Mohon lengkapi nomor WhatsApp aktif dan dokumen kepemilikan.",
                "",
                "Kirim Permintaan",
                revisionNote -> {
                    if (revisionNote.isEmpty()) {
                        Toast.makeText(this, "Catatan perbaikan wajib diisi", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    userRepository.requireRevisionOwnerVerification(user.getIdUser(), revisionNote, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean success) {
                            activityLogRepository.logActivity(sessionManager.getUserId(), "REVISION_OWNER",
                                    "Admin meminta revisi berkas pendaftaran akun " + user.getNama() + ": " + revisionNote,
                                    "user", user.getIdUser());
                            AppDialogHelper.showSuccess(AdminMainActivity.this,
                                    "Permintaan Terkirim",
                                    "Status pengajuan " + user.getNama() + " diubah menjadi Perlu Perbaikan.",
                                    () -> {
                                        loadAllStats();
                                        loadActiveTabData();
                                    });
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    @Override
    public void onReject(User user, int position) {
        AppDialogHelper.showDanger(this,
                "Tolak Pengajuan",
                "Tolak permohonan verifikasi pemilik kost untuk akun \"" + user.getNama() + "\"? Akun akan tetap aktif sebagai Pencari Kost biasa.",
                "Tolak Pengajuan",
                () -> {
                    userRepository.rejectOwnerVerification(user.getIdUser(), "Persyaratan belum lengkap atau data kost tidak valid.", new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean success) {
                            activityLogRepository.logActivity(sessionManager.getUserId(), "REJECT_OWNER",
                                    "Admin menolak pengajuan pemilik kost akun " + user.getNama(),
                                    "user", user.getIdUser());
                            Toast.makeText(AdminMainActivity.this, "Pengajuan pemilik telah ditolak", Toast.LENGTH_SHORT).show();
                            loadAllStats();
                            loadActiveTabData();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    // =========================================================================
    // 2. KOST PROPERTY ACTIONS (Pending Verification & All Kosts)
    // =========================================================================
    @Override
    public void onEditClick(Kost kost) {
        if (currentTab == AdminTab.VERIF_KOST) {
            // For pending verification, show moderator dialog
            showKostModerationDialog(kost);
        } else {
            // For all kosts, open the standard form editor
            Intent intent = new Intent(this, AdminKostFormActivity.class);
            intent.putExtra("kost_id", kost.getIdKost());
            startActivity(intent);
        }
    }

    @Override
    public void onToggleStatusClick(Kost kost, int position) {
        if (currentTab == AdminTab.VERIF_KOST) {
            showKostModerationDialog(kost);
            return;
        }

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
                            activityLogRepository.logActivity(sessionManager.getUserId(), "UPDATE_KOST_STATUS",
                                    "Status kost '" + kost.getNamaKost() + "' diubah menjadi " + nextStatus.getDisplayName(),
                                    "kost", kost.getIdKost());
                            kost.setStatus(nextStatus);
                            allKostAdapter.notifyItemChanged(position);
                            Toast.makeText(AdminMainActivity.this, "Status diubah menjadi: " + nextStatus.getDisplayName(), Toast.LENGTH_SHORT).show();
                            loadAllStats();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    private void showKostModerationDialog(Kost kost) {
        String[] options = new String[] {
                "✓ Setujui & Publikasikan",
                "✎ Minta Perbaikan (Revisi)",
                "✕ Tolak Properti",
                "👁 Buka Detail Kost"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Moderasi: " + kost.getNamaKost())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Approve
                        kostRepository.updateVerificationStatus(kost.getIdKost(), KostVerificationStatus.APPROVED, null, new DataCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                activityLogRepository.logActivity(sessionManager.getUserId(), "APPROVE_KOST",
                                        "Admin menyetujui publikasi properti kost: " + kost.getNamaKost(),
                                        "kost", kost.getIdKost());
                                AppDialogHelper.showSuccess(AdminMainActivity.this, "Kost Disetujui",
                                        "Properti kost \"" + kost.getNamaKost() + "\" kini telah disetujui dan tampil di pencarian publik.",
                                        () -> {
                                            loadAllStats();
                                            loadActiveTabData();
                                        });
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else if (which == 1) {
                        // Request Revision
                        AppDialogHelper.showInput(this,
                                "Minta Perbaikan Kost",
                                "Tuliskan catatan perbaikan data/foto untuk pemilik kost \"" + kost.getNamaKost() + "\":",
                                "Contoh: Mohon perbarui foto kamar mandi dan lengkapi patokan jalan.",
                                "",
                                "Kirim Catatan",
                                revisionNote -> {
                                    if (revisionNote.isEmpty()) {
                                        Toast.makeText(this, "Catatan perbaikan wajib diisi", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    kostRepository.updateVerificationStatus(kost.getIdKost(), KostVerificationStatus.REVISION_REQUIRED, revisionNote, new DataCallback<Boolean>() {
                                        @Override
                                        public void onSuccess(Boolean result) {
                                            activityLogRepository.logActivity(sessionManager.getUserId(), "REVISION_KOST",
                                                    "Admin meminta revisi properti kost: " + kost.getNamaKost() + " - " + revisionNote,
                                                    "kost", kost.getIdKost());
                                            AppDialogHelper.showSuccess(AdminMainActivity.this, "Permintaan Terkirim",
                                                    "Status properti diubah menjadi Perlu Perbaikan dan catatan dikirimkan ke pemilik.",
                                                    () -> {
                                                        loadAllStats();
                                                        loadActiveTabData();
                                                    });
                                        }

                                        @Override
                                        public void onError(String message) {
                                            Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                });
                    } else if (which == 2) {
                        // Reject
                        AppDialogHelper.showDanger(this,
                                "Tolak Properti Kost",
                                "Apakah Anda yakin ingin menolak publikasi properti \"" + kost.getNamaKost() + "\"?",
                                "Tolak",
                                () -> {
                                    kostRepository.updateVerificationStatus(kost.getIdKost(), KostVerificationStatus.REJECTED, "Properti tidak memenuhi standar CariKostKita.", new DataCallback<Boolean>() {
                                        @Override
                                        public void onSuccess(Boolean result) {
                                            activityLogRepository.logActivity(sessionManager.getUserId(), "REJECT_KOST",
                                                    "Admin menolak publikasi kost: " + kost.getNamaKost(),
                                                    "kost", kost.getIdKost());
                                            Toast.makeText(AdminMainActivity.this, "Properti kost ditolak", Toast.LENGTH_SHORT).show();
                                            loadAllStats();
                                            loadActiveTabData();
                                        }

                                        @Override
                                        public void onError(String message) {
                                            Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                });
                    } else if (which == 3) {
                        // Open Detail
                        Intent intent = new Intent(this, DetailKostActivity.class);
                        intent.putExtra("kost_id", kost.getIdKost());
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // =========================================================================
    // 3. USER MANAGEMENT ACTIONS
    // =========================================================================
    @Override
    public void onToggleStatus(User user, int position) {
        boolean currentlyActive = user.isActive();
        if (currentlyActive) {
            AppDialogHelper.showDanger(this,
                    "Tangguhkan Akun",
                    "Apakah Anda yakin ingin menangguhkan akun \"" + user.getNama() + "\" (" + user.getEmail() + ")? Pengguna ini tidak akan dapat login ke sistem CariKostKita.",
                    "Tangguhkan",
                    () -> {
                        userRepository.setUserActiveStatus(user.getIdUser(), false, new DataCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                activityLogRepository.logActivity(sessionManager.getUserId(), "SUSPEND_USER",
                                        "Admin menangguhkan akun " + user.getNama() + " (" + user.getEmail() + ")",
                                        "user", user.getIdUser());
                                user.setActive(false);
                                userAdapter.notifyItemChanged(position);
                                Toast.makeText(AdminMainActivity.this, "Akun berhasil ditangguhkan", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
        } else {
            AppDialogHelper.showConfirm(this,
                    "Aktifkan Kembali Akun",
                    "Pulihkan akses login untuk akun \"" + user.getNama() + "\" (" + user.getEmail() + ")?",
                    "Aktifkan",
                    () -> {
                        userRepository.setUserActiveStatus(user.getIdUser(), true, new DataCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                activityLogRepository.logActivity(sessionManager.getUserId(), "ACTIVATE_USER",
                                        "Admin mengaktifkan kembali akun " + user.getNama() + " (" + user.getEmail() + ")",
                                        "user", user.getIdUser());
                                user.setActive(true);
                                userAdapter.notifyItemChanged(position);
                                Toast.makeText(AdminMainActivity.this, "Akun berhasil diaktifkan kembali", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
        }
    }

    // =========================================================================
    // 4. REPORT MODERATION ACTIONS
    // =========================================================================
    @Override
    public void onResolve(KostReport report, int position) {
        AppDialogHelper.showConfirm(this,
                "Tindaklanjuti Laporan",
                "Laporan untuk \"" + report.getNamaKost() + "\" mengenai '" + report.getAlasan() + "'. Tandai laporan ini telah diselesaikan?",
                "Tandai Selesai",
                () -> {
                    reportRepository.updateReportStatus(report.getIdReport(), ReportStatus.SELESAI, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            activityLogRepository.logActivity(sessionManager.getUserId(), "RESOLVE_REPORT",
                                    "Admin menyelesaikan laporan #" + report.getIdReport() + " untuk kost " + report.getNamaKost(),
                                    "report", report.getIdReport());
                            Toast.makeText(AdminMainActivity.this, "Laporan ditandai selesai", Toast.LENGTH_SHORT).show();
                            loadAllStats();
                            loadActiveTabData();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    @Override
    public void onDismiss(KostReport report, int position) {
        AppDialogHelper.showDanger(this,
                "Tolak / Abaikan Laporan",
                "Tolak laporan dari pelapor " + report.getNamaPelapor() + " karena dianggap tidak valid atau keliru?",
                "Tolak Laporan",
                () -> {
                    reportRepository.updateReportStatus(report.getIdReport(), ReportStatus.DITOLAK, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            activityLogRepository.logActivity(sessionManager.getUserId(), "DISMISS_REPORT",
                                    "Admin menolak laporan #" + report.getIdReport() + " (tidak valid)",
                                    "report", report.getIdReport());
                            Toast.makeText(AdminMainActivity.this, "Laporan ditolak", Toast.LENGTH_SHORT).show();
                            loadAllStats();
                            loadActiveTabData();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }
}
