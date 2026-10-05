package com.carikostkita.ui.admin;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;
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
import com.carikostkita.ui.adapter.AdminOwnerAdapter;
import com.carikostkita.ui.adapter.AdminReportAdapter;
import com.carikostkita.ui.adapter.AdminUserAdapter;
import com.carikostkita.ui.adapter.VerificationAdapter;
import com.carikostkita.ui.auth.LoginActivity;
import com.carikostkita.ui.detail.DetailKostActivity;
import com.carikostkita.ui.main.MainActivity;
import com.carikostkita.ui.view.ModernLineChartView;
import com.carikostkita.data.remote.dto.KostDto;
import com.carikostkita.data.remote.dto.UserDto;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.FormatUtil;
import com.carikostkita.util.SessionManager;
import com.carikostkita.util.UserAvatarHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.List;

public class AdminMainActivity extends AppCompatActivity implements
        AdminKostAdapter.OnAdminKostClickListener,
        VerificationAdapter.OnVerificationActionListener,
        AdminUserAdapter.OnUserActionListener,
        AdminReportAdapter.OnReportActionListener,
        AdminOwnerAdapter.OnOwnerActionListener {

    private enum TabAdmin {
        DASHBOARD,
        VERIFIKASI,
        DATA,
        LAPORAN,
        PROFIL
    }

    private TabAdmin currentTab = TabAdmin.DASHBOARD;
    private boolean isVerifPemilikActive = true; // true: Pemilik, false: Kost
    private boolean isDataKostActive = true; // true: Kost, false: Users

    // Drawer & Header Views
    private DrawerLayout drawerLayout;
    private TextView tvHeaderSub;
    private TextView tvHeaderTitle;
    private ProgressBar pbLoading;

    // Tab Containers
    private View containerDashboard;
    private View containerVerifikasi;
    private View containerData;
    private View containerLaporan;
    private View containerProfil;
    private ExtendedFloatingActionButton fabAddKost;

    // Dashboard Metric Views (6 Indikator Ekosistem)
    private TextView tvStatTotal;
    private TextView tvStatTotalUser;
    private TextView tvStatTotalPemilik;
    private TextView tvStatKostAktif;
    private TextView tvStatAntreanKost;
    private TextView tvStatKostTerverifikasi;
    private TextView tvStatAntreanVerif;
    private TextView tvStatAntreanLaporan;

    private View cardStatTotalKost;
    private View cardStatTotalPencari;
    private View cardStatTotalPemilik;
    private View cardStatKostAktif;
    private View cardStatKostPending;
    private View cardStatKostTerverifikasi;

    // Historical Analytics Line Chart Views
    private TextView chipAdminChart7d, chipAdminChart30d, chipAdminChart3m, chipAdminChart1y;
    private TextView tvAdminChartFocusTitle;
    private ModernLineChartView chartAdminGrowth;

    public enum AdminChartMetric {
        TOTAL_KOST, TOTAL_PENCARI, TOTAL_PEMILIK, KOST_AKTIF, KOST_PENDING, KOST_VERIFIKASI
    }
    private AdminChartMetric currentAdminChartMetric = AdminChartMetric.TOTAL_KOST;
    private int currentAdminChartDays = 7;
    private final List<KostDto> cachedAdminKosts = new ArrayList<>();
    private final List<UserDto> cachedAdminUsers = new ArrayList<>();

    // Rich Dashboard Cards Views
    private TextView tvAdminHeroKosts;
    private TextView tvAdminHeroUsers;
    private TextView tvAdminHeroPending;
    private View cardAdminQueueAlert;
    private TextView tvAdminQueueAlertSub;
    private TextView tvAdminRecentLog1Title, tvAdminRecentLog1Time;
    private TextView tvAdminRecentLog2Title, tvAdminRecentLog2Time;

    // Verifikasi Tab Views
    private TextView chipVerifPemilik, chipVerifKost;
    private TextView tvVerifTitle, tvVerifCount;
    private LinearLayout layoutVerifEmpty;
    private TextView tvVerifEmptyTitle, tvVerifEmptyDesc;
    private RecyclerView rvVerif;

    // Data Master Tab Views
    private TextView chipDataKost, chipDataUsers, chipDataOwners;
    private int dataMasterSubTab = 0; // 0 = Kost, 1 = Users, 2 = Owners
    private TextView tvDataTitle, tvDataCount;
    private LinearLayout layoutDataEmpty;
    private RecyclerView rvData;

    // Laporan Tab Views
    private LinearLayout layoutLaporanEmpty;
    private RecyclerView rvReports;

    // Profil Tab Views
    private TextView tvAdminProfileName;
    private TextView tvAdminProfileEmail;
    private ImageView ivAdminProfileAvatar;
    private View itemAdminChangePassword;
    private RecyclerView rvLogs;

    // Badges & Gesture
    private TextView badgeDrawerVerif;
    private View badgeNavVerif;
    private GestureDetector swipeGestureDetector;

    // Bottom Navigation Views
    private final View[] navTabs = new View[5];
    private final LinearLayout[] navPills = new LinearLayout[5];
    private final ImageView[] navIcons = new ImageView[5];
    private final TextView[] navLabels = new TextView[5];

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
    private AdminOwnerAdapter ownerAdapter;
    private AdminReportAdapter reportAdapter;
    private AdminActivityLogAdapter logAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        kostRepository = new KostRepository(this);
        userRepository = new UserRepository(this);
        reportRepository = new ReportRepository(this);
        activityLogRepository = new ActivityLogRepository(this);

        // Strict Role-Based Access Control: Hanya Developer / Admin
        if (!sessionManager.isLoggedIn() || !sessionManager.isDeveloper()) {
            Toast.makeText(this, "Akses ditolak: Halaman ini khusus Developer / Super Admin", Toast.LENGTH_SHORT).show();
            if (sessionManager.isPemilikKost()) {
                startActivity(new Intent(this, PemilikMainActivity.class));
            } else {
                startActivity(new Intent(this, MainActivity.class));
            }
            finish();
            return;
        }

        setContentView(R.layout.activity_admin_main);

        initViews();
        setupBottomNav();
        setupAdapters();
        loadAllStats();
        switchTab(TabAdmin.DASHBOARD);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sessionManager.isLoggedIn() || !sessionManager.isDeveloper()) {
            finish();
            return;
        }
        updateDrawerProfile();
        populateProfileData();
        loadAllStats();
        refreshCurrentTabContent();
    }

    private void initViews() {
        tvHeaderSub = findViewById(R.id.tv_admin_header_sub);
        tvHeaderTitle = findViewById(R.id.tv_admin_header_title);
        pbLoading = findViewById(R.id.pb_admin_loading);
        
        drawerLayout = findViewById(R.id.drawer_admin_layout);
        ImageButton btnMenu = findViewById(R.id.btn_admin_menu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
        setupDrawer();

        containerDashboard = findViewById(R.id.container_admin_dashboard);
        containerVerifikasi = findViewById(R.id.container_admin_verifikasi);
        containerData = findViewById(R.id.container_admin_data);
        containerLaporan = findViewById(R.id.container_admin_laporan);
        containerProfil = findViewById(R.id.container_admin_profil);
        fabAddKost = findViewById(R.id.fab_admin_add_kost);

        // Dashboard Metrics (6 Indikator Ekosistem)
        tvStatTotal = findViewById(R.id.tv_stat_total);
        tvStatTotalUser = findViewById(R.id.tv_stat_total_user);
        tvStatTotalPemilik = findViewById(R.id.tv_stat_total_pemilik);
        tvStatKostAktif = findViewById(R.id.tv_stat_kost_aktif);
        tvStatAntreanKost = findViewById(R.id.tv_stat_antrean_kost);
        tvStatKostTerverifikasi = findViewById(R.id.tv_stat_kost_terverifikasi);
        tvStatAntreanVerif = findViewById(R.id.tv_stat_antrean_verif);
        tvStatAntreanLaporan = findViewById(R.id.tv_stat_antrean_laporan);

        cardStatTotalKost = findViewById(R.id.card_stat_total_kost);
        cardStatTotalPencari = findViewById(R.id.card_stat_total_pencari);
        cardStatTotalPemilik = findViewById(R.id.card_stat_total_pemilik);
        cardStatKostAktif = findViewById(R.id.card_stat_kost_aktif);
        cardStatKostPending = findViewById(R.id.card_stat_kost_pending);
        cardStatKostTerverifikasi = findViewById(R.id.card_stat_kost_terverifikasi);

        chipAdminChart7d = findViewById(R.id.chip_admin_chart_7d);
        chipAdminChart30d = findViewById(R.id.chip_admin_chart_30d);
        chipAdminChart3m = findViewById(R.id.chip_admin_chart_3m);
        chipAdminChart1y = findViewById(R.id.chip_admin_chart_1y);
        tvAdminChartFocusTitle = findViewById(R.id.tv_admin_chart_focus_title);
        chartAdminGrowth = findViewById(R.id.chart_admin_growth);

        setupChartAndMetricInteractions();

        // Rich Dashboard Views
        tvAdminHeroKosts = findViewById(R.id.tv_admin_hero_kosts);
        tvAdminHeroUsers = findViewById(R.id.tv_admin_hero_users);
        tvAdminHeroPending = findViewById(R.id.tv_admin_hero_pending);
        cardAdminQueueAlert = findViewById(R.id.card_admin_queue_alert);
        tvAdminQueueAlertSub = findViewById(R.id.tv_admin_queue_alert_sub);
        View btnAdminHeroVerif = findViewById(R.id.btn_admin_hero_verif);
        View btnQuickVerif = findViewById(R.id.btn_admin_quick_verif);
        View btnGotoFasilitas = findViewById(R.id.btn_admin_goto_fasilitas);
        View btnQuickAddKost = findViewById(R.id.btn_admin_quick_add_kost);
        View btnQuickData = findViewById(R.id.btn_admin_quick_data);
        tvAdminRecentLog1Title = findViewById(R.id.tv_admin_recent_log_1_title);
        tvAdminRecentLog1Time = findViewById(R.id.tv_admin_recent_log_1_time);
        tvAdminRecentLog2Title = findViewById(R.id.tv_admin_recent_log_2_title);
        tvAdminRecentLog2Time = findViewById(R.id.tv_admin_recent_log_2_time);
        View btnViewAllLogs = findViewById(R.id.btn_admin_view_all_logs);

        if (btnAdminHeroVerif != null) {
            btnAdminHeroVerif.setOnClickListener(v -> switchTab(TabAdmin.VERIFIKASI));
        }
        if (btnQuickVerif != null) {
            btnQuickVerif.setOnClickListener(v -> switchTab(TabAdmin.VERIFIKASI));
        }
        if (btnGotoFasilitas != null) {
            btnGotoFasilitas.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminFasilitasActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
        if (btnQuickAddKost != null) {
            btnQuickAddKost.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminKostFormActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
        if (btnQuickData != null) {
            btnQuickData.setOnClickListener(v -> switchTab(TabAdmin.DATA));
        }
        if (btnViewAllLogs != null) {
            btnViewAllLogs.setOnClickListener(v -> switchTab(TabAdmin.PROFIL));
        }

        // Verifikasi Tab
        chipVerifPemilik = findViewById(R.id.chip_verif_sub_pemilik);
        chipVerifKost = findViewById(R.id.chip_verif_sub_kost);
        tvVerifTitle = findViewById(R.id.tv_verif_title);
        tvVerifCount = findViewById(R.id.tv_verif_count);
        layoutVerifEmpty = findViewById(R.id.layout_verif_empty);
        tvVerifEmptyTitle = findViewById(R.id.tv_verif_empty_title);
        tvVerifEmptyDesc = findViewById(R.id.tv_verif_empty_desc);
        rvVerif = findViewById(R.id.rv_admin_verif);

        // Data Tab
        chipDataKost = findViewById(R.id.chip_data_sub_kost);
        chipDataUsers = findViewById(R.id.chip_data_sub_users);
        chipDataOwners = findViewById(R.id.chip_data_sub_owners);
        tvDataTitle = findViewById(R.id.tv_data_title);
        tvDataCount = findViewById(R.id.tv_data_count);
        layoutDataEmpty = findViewById(R.id.layout_data_empty);
        rvData = findViewById(R.id.rv_admin_data);

        // Laporan Tab
        layoutLaporanEmpty = findViewById(R.id.layout_laporan_empty);
        rvReports = findViewById(R.id.rv_admin_reports);

        // Profil Tab
        tvAdminProfileName = findViewById(R.id.tv_admin_profile_name);
        tvAdminProfileEmail = findViewById(R.id.tv_admin_profile_email);
        itemAdminChangePassword = findViewById(R.id.item_admin_change_password);
        rvLogs = findViewById(R.id.rv_admin_logs);


        // Verifikasi Sub-Toggles
        chipVerifPemilik.setOnClickListener(v -> {
            isVerifPemilikActive = true;
            updateVerifChips();
            loadVerifikasiData();
        });
        chipVerifKost.setOnClickListener(v -> {
            isVerifPemilikActive = false;
            updateVerifChips();
            loadVerifikasiData();
        });

        // Data Sub-Toggles
        chipDataKost.setOnClickListener(v -> {
            dataMasterSubTab = 0;
            updateDataChips();
            loadDataMasterContent();
        });
        chipDataUsers.setOnClickListener(v -> {
            dataMasterSubTab = 1;
            updateDataChips();
            loadDataMasterContent();
        });
        chipDataOwners.setOnClickListener(v -> {
            dataMasterSubTab = 2;
            updateDataChips();
            loadDataMasterContent();
        });

        // FAB Tambah Kost
        fabAddKost.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminKostFormActivity.class);
            intent.putExtra("owner_id", sessionManager.getUserId());
            startActivity(intent);
        });

        itemAdminChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        View itemAdminEditProfile = findViewById(R.id.item_admin_edit_profile);
        if (itemAdminEditProfile != null) {
            itemAdminEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, com.carikostkita.ui.profile.EditProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }

        ivAdminProfileAvatar = findViewById(R.id.iv_admin_profile_avatar);
        badgeDrawerVerif = findViewById(R.id.badge_drawer_admin_verif);

        View itemAdminProfileLogs = findViewById(R.id.item_admin_profile_logs);
        if (itemAdminProfileLogs != null) {
            itemAdminProfileLogs.setOnClickListener(v -> {
                if (rvLogs != null) {
                    rvLogs.getParent().requestChildFocus(rvLogs, rvLogs);
                }
            });
        }

        setupSwipeNavigation();
    }

    private void setupDrawer() {
        if (drawerLayout == null) return;
        updateDrawerProfile();

        View itemDash = findViewById(R.id.item_drawer_admin_dashboard);
        View itemVerif = findViewById(R.id.item_drawer_admin_verifikasi);
        View itemKost = findViewById(R.id.item_drawer_admin_kost);
        View itemLaporan = findViewById(R.id.item_drawer_admin_laporan);
        View itemFasilitas = findViewById(R.id.item_drawer_admin_fasilitas);
        View itemAddKost = findViewById(R.id.item_drawer_admin_add_kost);
        View itemEditProf = findViewById(R.id.item_drawer_admin_edit_profile);
        View itemChangePass = findViewById(R.id.item_drawer_admin_change_password);
        View btnLogoutDrawer = findViewById(R.id.btn_drawer_admin_logout);

        if (itemDash != null) {
            itemDash.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                switchTab(TabAdmin.DASHBOARD);
            });
        }
        if (itemVerif != null) {
            itemVerif.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                switchTab(TabAdmin.VERIFIKASI);
            });
        }
        if (itemKost != null) {
            itemKost.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                switchTab(TabAdmin.DATA);
                isDataKostActive = true;
                updateDataChips();
                loadDataMasterContent();
            });
        }
        if (itemLaporan != null) {
            itemLaporan.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                switchTab(TabAdmin.LAPORAN);
            });
        }
        if (itemFasilitas != null) {
            itemFasilitas.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, AdminFasilitasActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
        if (itemAddKost != null) {
            itemAddKost.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(this, AdminKostFormActivity.class);
                intent.putExtra("owner_id", sessionManager.getUserId());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
        if (itemEditProf != null) {
            itemEditProf.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(this, com.carikostkita.ui.profile.EditProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
        if (itemChangePass != null) {
            itemChangePass.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                showChangePasswordDialog();
            });
        }
        if (btnLogoutDrawer != null) {
            btnLogoutDrawer.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                performLogout();
            });
        }
    }

    private void updateDrawerProfile() {
        ImageView ivDrawerAvatar = findViewById(R.id.iv_admin_drawer_avatar);
        TextView tvDrawerName = findViewById(R.id.tv_admin_drawer_name);
        TextView tvDrawerEmail = findViewById(R.id.tv_admin_drawer_email);

        if (tvDrawerName != null) tvDrawerName.setText(sessionManager.getUserName());
        if (tvDrawerEmail != null) tvDrawerEmail.setText(sessionManager.getUserEmail());
        if (ivDrawerAvatar != null) {
            UserAvatarHelper.loadAvatar(ivDrawerAvatar, sessionManager.getUserAvatar());
        }
    }

    private void animateContainerIn(View view) {
        if (view == null) return;
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setTranslationY(14f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();
    }

    private long backPressedTime = 0;
    private Toast backToast;

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - backPressedTime < 2000) {
            if (backToast != null) {
                backToast.cancel();
            }
            finish();
        } else {
            backPressedTime = currentTime;
            if (backToast != null) {
                backToast.cancel();
            }
            backToast = Toast.makeText(this, "Tekan kembali sekali lagi untuk keluar", Toast.LENGTH_SHORT);
            backToast.show();
        }
    }

    private void setupBottomNav() {
        View bottomNav = findViewById(R.id.bottom_nav_admin);
        if (bottomNav == null) return;

        navTabs[0] = bottomNav.findViewById(R.id.tab_admin_nav_dashboard);
        navTabs[1] = bottomNav.findViewById(R.id.tab_admin_nav_verif);
        navTabs[2] = bottomNav.findViewById(R.id.tab_admin_nav_data);
        navTabs[3] = bottomNav.findViewById(R.id.tab_admin_nav_laporan);
        navTabs[4] = bottomNav.findViewById(R.id.tab_admin_nav_profil);

        navPills[0] = bottomNav.findViewById(R.id.pill_admin_dashboard);
        navPills[1] = bottomNav.findViewById(R.id.pill_admin_verif);
        navPills[2] = bottomNav.findViewById(R.id.pill_admin_data);
        navPills[3] = bottomNav.findViewById(R.id.pill_admin_laporan);
        navPills[4] = bottomNav.findViewById(R.id.pill_admin_profil);

        navIcons[0] = bottomNav.findViewById(R.id.iv_admin_nav_dashboard);
        navIcons[1] = bottomNav.findViewById(R.id.iv_admin_nav_verif);
        navIcons[2] = bottomNav.findViewById(R.id.iv_admin_nav_data);
        navIcons[3] = bottomNav.findViewById(R.id.iv_admin_nav_laporan);
        navIcons[4] = bottomNav.findViewById(R.id.iv_admin_nav_profil);

        navLabels[0] = bottomNav.findViewById(R.id.tv_admin_nav_dashboard);
        navLabels[1] = bottomNav.findViewById(R.id.tv_admin_nav_verif);
        navLabels[2] = bottomNav.findViewById(R.id.tv_admin_nav_data);
        navLabels[3] = bottomNav.findViewById(R.id.tv_admin_nav_laporan);
        navLabels[4] = bottomNav.findViewById(R.id.tv_admin_nav_profil);

        badgeNavVerif = bottomNav.findViewById(R.id.badge_unread_admin_verif);

        navTabs[0].setOnClickListener(v -> switchTab(TabAdmin.DASHBOARD));
        navTabs[1].setOnClickListener(v -> switchTab(TabAdmin.VERIFIKASI));
        navTabs[2].setOnClickListener(v -> switchTab(TabAdmin.DATA));
        navTabs[3].setOnClickListener(v -> switchTab(TabAdmin.LAPORAN));
        navTabs[4].setOnClickListener(v -> switchTab(TabAdmin.PROFIL));
    }

    private void updateVerifBadges(int pendingCount) {
        if (badgeDrawerVerif != null) {
            if (pendingCount > 0) {
                badgeDrawerVerif.setText(String.valueOf(pendingCount));
                badgeDrawerVerif.setVisibility(View.VISIBLE);
            } else {
                badgeDrawerVerif.setVisibility(View.GONE);
            }
        }
        if (badgeNavVerif != null) {
            badgeNavVerif.setVisibility(pendingCount > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void setupSwipeNavigation() {
        swipeGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_MIN_DISTANCE = 120;
            private static final int SWIPE_THRESHOLD_VELOCITY = 200;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    return false;
                }
                // Avoid conflict with drawer drag gesture from left edge
                if (e1.getX() < 40) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                // Predominantly horizontal swipe
                if (Math.abs(diffX) > Math.abs(diffY) * 2 && Math.abs(diffX) > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    if (diffX < 0) {
                        navigateToNextTab();
                        return true;
                    } else {
                        navigateToPrevTab();
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void navigateToNextTab() {
        int nextIndex = currentTab.ordinal() + 1;
        if (nextIndex < TabAdmin.values().length) {
            switchTab(TabAdmin.values()[nextIndex]);
        }
    }

    private void navigateToPrevTab() {
        int prevIndex = currentTab.ordinal() - 1;
        if (prevIndex >= 0) {
            switchTab(TabAdmin.values()[prevIndex]);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (swipeGestureDetector != null) {
            swipeGestureDetector.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void setupAdapters() {
        verifPemilikAdapter = new VerificationAdapter(this);
        pendingKostAdapter = new AdminKostAdapter(this);
        rvVerif.setLayoutManager(new LinearLayoutManager(this));

        allKostAdapter = new AdminKostAdapter(this);
        userAdapter = new AdminUserAdapter(this);
        ownerAdapter = new AdminOwnerAdapter(this);
        rvData.setLayoutManager(new LinearLayoutManager(this));

        reportAdapter = new AdminReportAdapter(this);
        rvReports.setLayoutManager(new LinearLayoutManager(this));
        rvReports.setAdapter(reportAdapter);

        logAdapter = new AdminActivityLogAdapter();
        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        rvLogs.setAdapter(logAdapter);
    }

    private void switchTab(TabAdmin tab) {
        currentTab = tab;
        int tabIndex = tab.ordinal();

        containerDashboard.setVisibility(View.GONE);
        containerVerifikasi.setVisibility(View.GONE);
        containerData.setVisibility(View.GONE);
        containerLaporan.setVisibility(View.GONE);
        containerProfil.setVisibility(View.GONE);
        fabAddKost.setVisibility(View.GONE);

        View bottomNav = findViewById(R.id.bottom_nav_admin);
        if (bottomNav != null) {
            ViewGroup navContainer = bottomNav.findViewById(R.id.nav_admin_container);
            if (navContainer != null) {
                AutoTransition transition = new AutoTransition();
                transition.setDuration(250);
                transition.setInterpolator(new FastOutSlowInInterpolator());
                TransitionManager.beginDelayedTransition(navContainer, transition);
            }
        }

        int colorSecondary = ContextCompat.getColor(this, R.color.text_secondary);

        for (int i = 0; i < 5; i++) {
            if (i == tabIndex) {
                if (navTabs[i] != null) {
                    navTabs[i].setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.8f));
                }
                if (navPills[i] != null) navPills[i].setBackgroundResource(R.drawable.bg_nav_pill_active);
                if (navIcons[i] != null) navIcons[i].setImageTintList(ColorStateList.valueOf(Color.WHITE));
                if (navLabels[i] != null) {
                    navLabels[i].setVisibility(View.VISIBLE);
                    navLabels[i].setTextColor(Color.WHITE);
                }
            } else {
                if (navTabs[i] != null) {
                    navTabs[i].setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.8f));
                }
                if (navPills[i] != null) navPills[i].setBackground(null);
                if (navIcons[i] != null) navIcons[i].setImageTintList(ColorStateList.valueOf(colorSecondary));
                if (navLabels[i] != null) {
                    navLabels[i].setVisibility(View.GONE);
                }
            }
        }

        switch (tab) {
            case DASHBOARD:
                tvHeaderSub.setText("Developer / Super Admin");
                tvHeaderTitle.setText("Pusat Kontrol Sistem");
                animateContainerIn(containerDashboard);
                loadAllStats();
                break;

            case VERIFIKASI:
                tvHeaderSub.setText("Moderasi Persetujuan");
                tvHeaderTitle.setText("Verifikasi Masuk");
                animateContainerIn(containerVerifikasi);
                updateVerifChips();
                loadVerifikasiData();
                break;

            case DATA:
                tvHeaderSub.setText("Manajemen Entitas");
                tvHeaderTitle.setText("Data Master");
                animateContainerIn(containerData);
                if (isDataKostActive) fabAddKost.setVisibility(View.VISIBLE);
                updateDataChips();
                loadDataMasterContent();
                break;

            case LAPORAN:
                tvHeaderSub.setText("Keamanan Ekosistem");
                tvHeaderTitle.setText("Laporan Pengguna");
                animateContainerIn(containerLaporan);
                loadReportsData();
                break;

            case PROFIL:
                tvHeaderSub.setText("Akun Administratif");
                tvHeaderTitle.setText("Profil Super Admin");
                animateContainerIn(containerProfil);
                populateProfileData();
                loadAuditLogs();
                break;
        }
    }

    private void refreshCurrentTabContent() {
        switch (currentTab) {
            case DASHBOARD:
                loadAllStats();
                break;
            case VERIFIKASI:
                loadVerifikasiData();
                break;
            case DATA:
                loadDataMasterContent();
                break;
            case LAPORAN:
                loadReportsData();
                break;
            case PROFIL:
                loadAuditLogs();
                break;
        }
    }

    private void setupChartAndMetricInteractions() {
        if (cardStatTotalKost != null) cardStatTotalKost.setOnClickListener(v -> onMetricClicked(AdminChartMetric.TOTAL_KOST));
        if (cardStatTotalPencari != null) cardStatTotalPencari.setOnClickListener(v -> onMetricClicked(AdminChartMetric.TOTAL_PENCARI));
        if (cardStatTotalPemilik != null) cardStatTotalPemilik.setOnClickListener(v -> onMetricClicked(AdminChartMetric.TOTAL_PEMILIK));
        if (cardStatKostAktif != null) cardStatKostAktif.setOnClickListener(v -> onMetricClicked(AdminChartMetric.KOST_AKTIF));
        if (cardStatKostPending != null) cardStatKostPending.setOnClickListener(v -> onMetricClicked(AdminChartMetric.KOST_PENDING));
        if (cardStatKostTerverifikasi != null) cardStatKostTerverifikasi.setOnClickListener(v -> onMetricClicked(AdminChartMetric.KOST_VERIFIKASI));

        if (chipAdminChart7d != null) chipAdminChart7d.setOnClickListener(v -> setChartPeriod(7));
        if (chipAdminChart30d != null) chipAdminChart30d.setOnClickListener(v -> setChartPeriod(30));
        if (chipAdminChart3m != null) chipAdminChart3m.setOnClickListener(v -> setChartPeriod(90));
        if (chipAdminChart1y != null) chipAdminChart1y.setOnClickListener(v -> setChartPeriod(365));

        setChartPeriod(7);
        highlightSelectedMetricCard();
    }

    private void onMetricClicked(AdminChartMetric metric) {
        currentAdminChartMetric = metric;
        highlightSelectedMetricCard();
        updateGrowthChart();
        showMetricDetail(metric);
    }

    private void highlightSelectedMetricCard() {
        View[] cards = {cardStatTotalKost, cardStatTotalPencari, cardStatTotalPemilik, cardStatKostAktif, cardStatKostPending, cardStatKostTerverifikasi};
        AdminChartMetric[] metrics = {AdminChartMetric.TOTAL_KOST, AdminChartMetric.TOTAL_PENCARI, AdminChartMetric.TOTAL_PEMILIK, AdminChartMetric.KOST_AKTIF, AdminChartMetric.KOST_PENDING, AdminChartMetric.KOST_VERIFIKASI};

        int strokeColorPrimary = ContextCompat.getColor(this, R.color.primary);
        int strokeColorDefault = ContextCompat.getColor(this, R.color.border);

        for (int i = 0; i < cards.length; i++) {
            if (cards[i] instanceof com.google.android.material.card.MaterialCardView) {
                com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) cards[i];
                boolean isSelected = (metrics[i] == currentAdminChartMetric);
                card.setStrokeColor(isSelected ? strokeColorPrimary : strokeColorDefault);
                card.setStrokeWidth(isSelected ? 4 : 2);
            }
        }
    }

    private void setChartPeriod(int days) {
        currentAdminChartDays = days;
        int activeBg = R.drawable.bg_chip_filter_active;
        int inactiveBg = R.drawable.bg_chip_filter_inactive;
        int activeColor = ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);

        if (chipAdminChart7d != null) {
            chipAdminChart7d.setBackgroundResource(days == 7 ? activeBg : inactiveBg);
            chipAdminChart7d.setTextColor(days == 7 ? activeColor : inactiveColor);
            chipAdminChart7d.setTypeface(null, days == 7 ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (chipAdminChart30d != null) {
            chipAdminChart30d.setBackgroundResource(days == 30 ? activeBg : inactiveBg);
            chipAdminChart30d.setTextColor(days == 30 ? activeColor : inactiveColor);
            chipAdminChart30d.setTypeface(null, days == 30 ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (chipAdminChart3m != null) {
            chipAdminChart3m.setBackgroundResource(days == 90 ? activeBg : inactiveBg);
            chipAdminChart3m.setTextColor(days == 90 ? activeColor : inactiveColor);
            chipAdminChart3m.setTypeface(null, days == 90 ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (chipAdminChart1y != null) {
            chipAdminChart1y.setBackgroundResource(days == 365 ? activeBg : inactiveBg);
            chipAdminChart1y.setTextColor(days == 365 ? activeColor : inactiveColor);
            chipAdminChart1y.setTypeface(null, days == 365 ? Typeface.BOLD : Typeface.NORMAL);
        }

        updateGrowthChart();
    }

    private long parseIsoDate(String dateStr) {
        if (dateStr == null) return 0L;
        try {
            if (dateStr.length() >= 10) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date d = sdf.parse(dateStr.substring(0, 10));
                if (d != null) return d.getTime();
            }
        } catch (Exception ignored) {}
        return 0L;
    }

    private void updateGrowthChart() {
        if (chartAdminGrowth == null) return;

        String metricTitle;
        switch (currentAdminChartMetric) {
            case TOTAL_PENCARI: metricTitle = "Total Pencari Kost"; break;
            case TOTAL_PEMILIK: metricTitle = "Total Pemilik Kost"; break;
            case KOST_AKTIF: metricTitle = "Kost Aktif (Tersedia)"; break;
            case KOST_PENDING: metricTitle = "Kost Menunggu Moderasi"; break;
            case KOST_VERIFIKASI: metricTitle = "Kost Terverifikasi"; break;
            default: metricTitle = "Total Kost Terdaftar"; break;
        }

        String periodTitle = (currentAdminChartDays == 7 ? "7 Hari Terakhir" :
                              currentAdminChartDays == 30 ? "30 Hari Terakhir" :
                              currentAdminChartDays == 90 ? "3 Bulan Terakhir" : "1 Tahun Terakhir");

        if (tvAdminChartFocusTitle != null) {
            tvAdminChartFocusTitle.setText("Menampilkan: Tren " + metricTitle + " (" + periodTitle + ")");
        }

        List<ModernLineChartView.DataPoint> points = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        long now = cal.getTimeInMillis();
        long startTime = now - ((long) currentAdminChartDays * 24L * 60L * 60L * 1000L);

        int numBuckets = (currentAdminChartDays == 7 ? 7 : (currentAdminChartDays == 30 ? 7 : (currentAdminChartDays == 90 ? 7 : 12)));
        long intervalStep = (now - startTime) / Math.max(1, numBuckets - 1);

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());

        for (int i = 0; i < numBuckets; i++) {
            long bucketTime = (i == numBuckets - 1) ? now : (startTime + (i * intervalStep));
            String label = (currentAdminChartDays == 365) ? monthFormat.format(new Date(bucketTime)) : dayFormat.format(new Date(bucketTime));

            int count = 0;
            if (currentAdminChartMetric == AdminChartMetric.TOTAL_PENCARI) {
                for (UserDto u : cachedAdminUsers) {
                    if ("pencari".equalsIgnoreCase(u.role)) {
                        long t = parseIsoDate(u.createdAt);
                        if (t <= bucketTime) count++;
                    }
                }
            } else if (currentAdminChartMetric == AdminChartMetric.TOTAL_PEMILIK) {
                for (UserDto u : cachedAdminUsers) {
                    if ("pemilik".equalsIgnoreCase(u.role)) {
                        long t = parseIsoDate(u.createdAt);
                        if (t <= bucketTime) count++;
                    }
                }
            } else if (currentAdminChartMetric == AdminChartMetric.KOST_AKTIF) {
                for (KostDto k : cachedAdminKosts) {
                    if ("TERSEDIA".equalsIgnoreCase(k.status)) {
                        long t = parseIsoDate(k.createdAt);
                        if (t <= bucketTime) count++;
                    }
                }
            } else if (currentAdminChartMetric == AdminChartMetric.KOST_PENDING) {
                for (KostDto k : cachedAdminKosts) {
                    if (k.verificationStatus == null || "PENDING".equalsIgnoreCase(k.verificationStatus)) {
                        long t = parseIsoDate(k.createdAt);
                        if (t <= bucketTime) count++;
                    }
                }
            } else if (currentAdminChartMetric == AdminChartMetric.KOST_VERIFIKASI) {
                for (KostDto k : cachedAdminKosts) {
                    if ("APPROVED".equalsIgnoreCase(k.verificationStatus)) {
                        long t = parseIsoDate(k.createdAt);
                        if (t <= bucketTime) count++;
                    }
                }
            } else {
                for (KostDto k : cachedAdminKosts) {
                    long t = parseIsoDate(k.createdAt);
                    if (t <= bucketTime) count++;
                }
            }

            points.add(new ModernLineChartView.DataPoint(label, count));
        }

        chartAdminGrowth.setData(points);
    }

    private void showMetricDetail(AdminChartMetric metric) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_metric_detail, null);
        dialog.setContentView(view);

        ImageView ivIcon = view.findViewById(R.id.iv_detail_icon);
        TextView tvTitle = view.findViewById(R.id.tv_detail_title);
        TextView tvCategory = view.findViewById(R.id.tv_detail_category);
        TextView tvValue = view.findViewById(R.id.tv_detail_value);
        TextView tvDesc = view.findViewById(R.id.tv_detail_description);
        TextView tvBreakdown1 = view.findViewById(R.id.tv_detail_breakdown_1);
        TextView tvBreakdown2 = view.findViewById(R.id.tv_detail_breakdown_2);
        MaterialButton btnAction = view.findViewById(R.id.btn_detail_action);
        MaterialButton btnClose = view.findViewById(R.id.btn_detail_close);

        tvCategory.setText("Indikator Ekosistem Database Asli");

        switch (metric) {
            case TOTAL_KOST:
                ivIcon.setImageResource(R.drawable.ic_nav_home);
                tvTitle.setText("Total Properti Kost");
                tvValue.setText(String.valueOf(cachedAdminKosts.size()));
                tvDesc.setText("Jumlah akumulatif seluruh properti kost yang terdaftar dalam basis data PostgreSQL Supabase.");
                int aktif = 0, pending = 0;
                for (KostDto k : cachedAdminKosts) {
                    if ("TERSEDIA".equalsIgnoreCase(k.status)) aktif++;
                    if (k.verificationStatus == null || "PENDING".equalsIgnoreCase(k.verificationStatus)) pending++;
                }
                tvBreakdown1.setText("• Status: " + aktif + " Aktif Tersedia, " + (cachedAdminKosts.size() - aktif) + " Penuh/Lainnya");
                tvBreakdown2.setText("• Moderasi: " + pending + " Pending Verifikasi Publikasi");
                btnAction.setText("Buka Data Kost");
                btnAction.setOnClickListener(v -> {
                    dialog.dismiss();
                    switchTab(TabAdmin.DATA);
                    isDataKostActive = true;
                    updateDataChips();
                    loadDataMasterContent();
                });
                break;

            case TOTAL_PENCARI:
                ivIcon.setImageResource(R.drawable.ic_nav_profile);
                tvTitle.setText("Total Akun Pencari");
                int countPencari = 0;
                for (UserDto u : cachedAdminUsers) {
                    if ("pencari".equalsIgnoreCase(u.role)) countPencari++;
                }
                tvValue.setText(String.valueOf(countPencari));
                tvDesc.setText("Jumlah pengguna terdaftar dengan peran Pencari Kost yang aktif mencari hunian.");
                tvBreakdown1.setText("• Akun Terverifikasi: Berhak chat langsung dengan pemilik kost");
                tvBreakdown2.setText("• Keamanan: Dilindungi autentikasi email & kata sandi terenkripsi");
                btnAction.setText("Buka Manajemen Pengguna");
                btnAction.setOnClickListener(v -> {
                    dialog.dismiss();
                    switchTab(TabAdmin.DATA);
                    isDataKostActive = false;
                    updateDataChips();
                    loadDataMasterContent();
                });
                break;

            case TOTAL_PEMILIK:
                ivIcon.setImageResource(R.drawable.ic_avatar_owner);
                tvTitle.setText("Total Akun Pemilik");
                int countPemilik = 0;
                for (UserDto u : cachedAdminUsers) {
                    if ("pemilik".equalsIgnoreCase(u.role)) countPemilik++;
                }
                tvValue.setText(String.valueOf(countPemilik));
                tvDesc.setText("Mitra pemilik properti kost yang mengelola dan mempublikasikan kamar di CariKostKita.");
                tvBreakdown1.setText("• Hak Akses: Dashboard Mitra Pemilik, Manajemen Properti, dan Chat");
                tvBreakdown2.setText("• Verifikasi Identitas: Nomor kontak & legalitas akun terdaftar");
                btnAction.setText("Buka Manajemen Akun");
                btnAction.setOnClickListener(v -> {
                    dialog.dismiss();
                    switchTab(TabAdmin.DATA);
                    isDataKostActive = false;
                    updateDataChips();
                    loadDataMasterContent();
                });
                break;

            case KOST_AKTIF:
                ivIcon.setImageResource(R.drawable.ic_sparkle);
                tvTitle.setText("Properti Kost Aktif");
                int countAktif = 0;
                for (KostDto k : cachedAdminKosts) {
                    if ("TERSEDIA".equalsIgnoreCase(k.status)) countAktif++;
                }
                tvValue.setText(String.valueOf(countAktif));
                tvDesc.setText("Properti kost yang saat ini berstatus 'Tersedia' dan siap menerima calon penyewa kost.");
                tvBreakdown1.setText("• Visibilitas: Tampil di Map Search & Halaman Beranda");
                tvBreakdown2.setText("• Ketersediaan Kamar: Kamar kosong siap dipesan");
                btnAction.setText("Kelola di Data Master");
                btnAction.setOnClickListener(v -> {
                    dialog.dismiss();
                    switchTab(TabAdmin.DATA);
                    isDataKostActive = true;
                    updateDataChips();
                    loadDataMasterContent();
                });
                break;

            case KOST_PENDING:
                ivIcon.setImageResource(R.drawable.ic_activity);
                tvTitle.setText("Kost Menunggu Verifikasi");
                int countPend = 0;
                for (KostDto k : cachedAdminKosts) {
                    if (k.verificationStatus == null || "PENDING".equalsIgnoreCase(k.verificationStatus)) countPend++;
                }
                tvValue.setText(String.valueOf(countPend));
                tvDesc.setText("Antrean listing properti baru atau pembaruan yang memerlukan persetujuan dari Super Admin.");
                tvBreakdown1.setText("• Perlindungan Pengguna: Mencegah penipuan dan data kost palsu");
                tvBreakdown2.setText("• Aksi Diperlukan: Setujui, Minta Revisi, atau Tolak");
                btnAction.setText("Buka Antrean Moderasi");
                btnAction.setOnClickListener(v -> {
                    dialog.dismiss();
                    switchTab(TabAdmin.VERIFIKASI);
                    isVerifPemilikActive = false;
                    updateVerifChips();
                    loadVerifikasiData();
                });
                break;

            case KOST_VERIFIKASI:
                ivIcon.setImageResource(R.drawable.ic_verified);
                tvTitle.setText("Kost Terverifikasi");
                int countVerif = 0;
                for (KostDto k : cachedAdminKosts) {
                    if ("APPROVED".equalsIgnoreCase(k.verificationStatus)) countVerif++;
                }
                tvValue.setText(String.valueOf(countVerif));
                tvDesc.setText("Properti kost yang telah lolos uji kelayakan, alamat valid, dan telah diberi lencana terverifikasi.");
                tvBreakdown1.setText("• Prioritas: Mendapatkan lencana resmi dan posisi utama pencarian");
                tvBreakdown2.setText("• Kepercayaan: Terkonfirmasi kepemilikan dan foto aslinya");
                btnAction.setText("Lihat Properti");
                btnAction.setOnClickListener(v -> {
                    dialog.dismiss();
                    switchTab(TabAdmin.DATA);
                    isDataKostActive = true;
                    updateDataChips();
                    loadDataMasterContent();
                });
                break;
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadAllStats() {
        kostRepository.getAdminStats(new DataCallback<KostRepository.AdminStats>() {
            @Override
            public void onSuccess(KostRepository.AdminStats stats) {
                if (tvStatTotal != null) tvStatTotal.setText(String.valueOf(stats.totalKost));
                if (tvStatKostAktif != null) tvStatKostAktif.setText(String.valueOf(stats.kostAktif));
                if (tvStatAntreanKost != null) tvStatAntreanKost.setText(String.valueOf(stats.totalPending));
                if (tvStatKostTerverifikasi != null) tvStatKostTerverifikasi.setText(String.valueOf(stats.kostTerverifikasi));
                if (tvAdminHeroKosts != null) {
                    tvAdminHeroKosts.setText(String.valueOf(stats.totalKost));
                }
                cachedAdminKosts.clear();
                if (stats.kostList != null) {
                    cachedAdminKosts.addAll(stats.kostList);
                }
                updateGrowthChart();
                updateDashboardModerationAlert();
            }
            @Override
            public void onError(String message) {}
        });

        userRepository.getUserStats(new DataCallback<UserRepository.UserStats>() {
            @Override
            public void onSuccess(UserRepository.UserStats stats) {
                if (tvStatTotalUser != null) tvStatTotalUser.setText(String.valueOf(stats.totalPencari));
                if (tvStatTotalPemilik != null) tvStatTotalPemilik.setText(String.valueOf(stats.totalPemilik));
                if (tvStatAntreanVerif != null) tvStatAntreanVerif.setText(String.valueOf(stats.pendingVerifikasi));
                updateVerifBadges(stats.pendingVerifikasi);
                if (tvAdminHeroUsers != null) {
                    int totalAccounts = stats.totalPencari + stats.totalPemilik;
                    tvAdminHeroUsers.setText(String.valueOf(totalAccounts));
                }
                cachedAdminUsers.clear();
                if (stats.userList != null) {
                    cachedAdminUsers.addAll(stats.userList);
                }
                updateGrowthChart();
                updateDashboardModerationAlert();
            }
            @Override
            public void onError(String message) {}
        });

        reportRepository.countPendingReports(new DataCallback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                if (tvStatAntreanLaporan != null) tvStatAntreanLaporan.setText(String.valueOf(count));
                updateDashboardModerationAlert();
            }
            @Override
            public void onError(String message) {}
        });

        loadDashboardRecentLogs();
    }

    private void updateDashboardModerationAlert() {
        try {
            int pendingVerif = Integer.parseInt(tvStatAntreanVerif.getText().toString());
            int pendingKost = Integer.parseInt(tvStatAntreanKost.getText().toString());
            int pendingLaporan = Integer.parseInt(tvStatAntreanLaporan.getText().toString());
            int totalPending = pendingVerif + pendingKost;

            if (tvAdminHeroPending != null) {
                tvAdminHeroPending.setText(String.valueOf(totalPending));
            }

            if (cardAdminQueueAlert != null) {
                if (totalPending > 0 || pendingLaporan > 0) {
                    cardAdminQueueAlert.setVisibility(View.VISIBLE);
                    if (tvAdminQueueAlertSub != null) {
                        tvAdminQueueAlertSub.setText("Ada " + pendingVerif + " pemilik dan " + pendingKost + " listing kost menunggu verifikasi.");
                    }
                } else {
                    cardAdminQueueAlert.setVisibility(View.GONE);
                }
            }
        } catch (Exception ignored) {}
    }

    private void loadDashboardRecentLogs() {
        if (activityLogRepository == null) return;
        activityLogRepository.getAllLogs(new DataCallback<List<SystemActivityLog>>() {
            @Override
            public void onSuccess(List<SystemActivityLog> logs) {
                if (logs != null && !logs.isEmpty()) {
                    if (tvAdminRecentLog1Title != null && logs.size() > 0) {
                        SystemActivityLog log1 = logs.get(0);
                        tvAdminRecentLog1Title.setText(log1.getActionType() + ": " + log1.getDescription());
                        if (tvAdminRecentLog1Time != null) {
                            tvAdminRecentLog1Time.setText(log1.getCreatedAt() != null ? log1.getCreatedAt() : "Baru saja");
                        }
                    }
                    if (tvAdminRecentLog2Title != null && logs.size() > 1) {
                        SystemActivityLog log2 = logs.get(1);
                        tvAdminRecentLog2Title.setText(log2.getActionType() + ": " + log2.getDescription());
                        if (tvAdminRecentLog2Time != null) {
                            tvAdminRecentLog2Time.setText(log2.getCreatedAt() != null ? log2.getCreatedAt() : "Hari ini");
                        }
                    }
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void updateVerifChips() {
        int activeBg = R.drawable.bg_chip_filter_active;
        int inactiveBg = R.drawable.bg_chip_filter_inactive;
        int activeColor = ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);

        chipVerifPemilik.setBackgroundResource(isVerifPemilikActive ? activeBg : inactiveBg);
        chipVerifPemilik.setTextColor(isVerifPemilikActive ? activeColor : inactiveColor);

        chipVerifKost.setBackgroundResource(!isVerifPemilikActive ? activeBg : inactiveBg);
        chipVerifKost.setTextColor(!isVerifPemilikActive ? activeColor : inactiveColor);
    }

    private void loadVerifikasiData() {
        pbLoading.setVisibility(View.VISIBLE);
        layoutVerifEmpty.setVisibility(View.GONE);
        rvVerif.setVisibility(View.GONE);

        if (isVerifPemilikActive) {
            tvVerifTitle.setText("Antrean Calon Pemilik Kost");
            rvVerif.setAdapter(verifPemilikAdapter);

            userRepository.getPendingVerifications(new DataCallback<List<User>>() {
                @Override
                public void onSuccess(List<User> list) {
                    pbLoading.setVisibility(View.GONE);
                    tvVerifCount.setText(list.size() + " antrean");
                    updateVerifBadges(list.size());
                    if (list.isEmpty()) {
                        layoutVerifEmpty.setVisibility(View.VISIBLE);
                        tvVerifEmptyTitle.setText("Tidak Ada Pengajuan Pemilik");
                        tvVerifEmptyDesc.setText("Semua pendaftaran calon pemilik kost telah selesai ditinjau.");
                    } else {
                        rvVerif.setVisibility(View.VISIBLE);
                        verifPemilikAdapter.submitList(list);
                    }
                }

                @Override
                public void onError(String message) {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            tvVerifTitle.setText("Antrean Properti Kost Baru/Revisi");
            rvVerif.setAdapter(pendingKostAdapter);

            kostRepository.getPendingKosts(new DataCallback<List<Kost>>() {
                @Override
                public void onSuccess(List<Kost> list) {
                    pbLoading.setVisibility(View.GONE);
                    tvVerifCount.setText(list.size() + " properti");
                    if (list.isEmpty()) {
                        layoutVerifEmpty.setVisibility(View.VISIBLE);
                        tvVerifEmptyTitle.setText("Tidak Ada Antrean Kost");
                        tvVerifEmptyDesc.setText("Semua data properti kost baru atau revisi sudah selesai diverifikasi.");
                    } else {
                        rvVerif.setVisibility(View.VISIBLE);
                        pendingKostAdapter.submitList(list);
                    }
                }

                @Override
                public void onError(String message) {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateDataChips() {
        int activeBg = R.drawable.bg_chip_filter_active;
        int inactiveBg = R.drawable.bg_chip_filter_inactive;
        int activeColor = ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);

        chipDataKost.setBackgroundResource(dataMasterSubTab == 0 ? activeBg : inactiveBg);
        chipDataKost.setTextColor(dataMasterSubTab == 0 ? activeColor : inactiveColor);

        chipDataUsers.setBackgroundResource(dataMasterSubTab == 1 ? activeBg : inactiveBg);
        chipDataUsers.setTextColor(dataMasterSubTab == 1 ? activeColor : inactiveColor);

        if (chipDataOwners != null) {
            chipDataOwners.setBackgroundResource(dataMasterSubTab == 2 ? activeBg : inactiveBg);
            chipDataOwners.setTextColor(dataMasterSubTab == 2 ? activeColor : inactiveColor);
        }

        fabAddKost.setVisibility(dataMasterSubTab == 0 ? View.VISIBLE : View.GONE);
    }

    private void loadDataMasterContent() {
        pbLoading.setVisibility(View.VISIBLE);
        layoutDataEmpty.setVisibility(View.GONE);
        rvData.setVisibility(View.GONE);

        if (dataMasterSubTab == 0) {
            tvDataTitle.setText("Monitoring Seluruh Properti Kost");
            rvData.setAdapter(allKostAdapter);

            kostRepository.getAllKostForAdmin(new DataCallback<List<Kost>>() {
                @Override
                public void onSuccess(List<Kost> list) {
                    pbLoading.setVisibility(View.GONE);
                    tvDataCount.setText(list.size() + " properti");
                    if (list.isEmpty()) {
                        layoutDataEmpty.setVisibility(View.VISIBLE);
                    } else {
                        rvData.setVisibility(View.VISIBLE);
                        allKostAdapter.submitList(list);
                    }
                }

                @Override
                public void onError(String message) {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        } else if (dataMasterSubTab == 1) {
            tvDataTitle.setText("Manajemen Seluruh Akun Pengguna");
            rvData.setAdapter(userAdapter);

            userRepository.getAllUsers(new DataCallback<List<User>>() {
                @Override
                public void onSuccess(List<User> list) {
                    pbLoading.setVisibility(View.GONE);
                    tvDataCount.setText(list.size() + " pengguna");
                    if (list.isEmpty()) {
                        layoutDataEmpty.setVisibility(View.VISIBLE);
                    } else {
                        rvData.setVisibility(View.VISIBLE);
                        userAdapter.submitList(list);
                    }
                }

                @Override
                public void onError(String message) {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            tvDataTitle.setText("Daftar Mitra Pemilik Kost");
            rvData.setAdapter(ownerAdapter);

            userRepository.getOwners(new DataCallback<List<User>>() {
                @Override
                public void onSuccess(List<User> list) {
                    pbLoading.setVisibility(View.GONE);
                    tvDataCount.setText(list.size() + " pemilik");
                    if (list.isEmpty()) {
                        layoutDataEmpty.setVisibility(View.VISIBLE);
                    } else {
                        rvData.setVisibility(View.VISIBLE);
                        ownerAdapter.submitList(list);
                    }
                }

                @Override
                public void onError(String message) {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadReportsData() {
        pbLoading.setVisibility(View.VISIBLE);
        layoutLaporanEmpty.setVisibility(View.GONE);
        rvReports.setVisibility(View.GONE);

        reportRepository.getAllReports(new DataCallback<List<KostReport>>() {
            @Override
            public void onSuccess(List<KostReport> list) {
                pbLoading.setVisibility(View.GONE);
                if (list == null || list.isEmpty()) {
                    layoutLaporanEmpty.setVisibility(View.VISIBLE);
                } else {
                    rvReports.setVisibility(View.VISIBLE);
                    reportAdapter.submitList(list);
                }
            }

            @Override
            public void onError(String message) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateProfileData() {
        if (tvAdminProfileName != null) tvAdminProfileName.setText(sessionManager.getUserName());
        if (tvAdminProfileEmail != null) tvAdminProfileEmail.setText(sessionManager.getUserEmail());
        if (ivAdminProfileAvatar != null) {
            UserAvatarHelper.loadAvatar(ivAdminProfileAvatar, sessionManager.getUserAvatar());
        }
    }

    private void loadAuditLogs() {
        activityLogRepository.getAllLogs(new DataCallback<List<SystemActivityLog>>() {
            @Override
            public void onSuccess(List<SystemActivityLog> list) {
                if (list != null) {
                    logAdapter.submitList(list);
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    // =========================================================================
    // MODERASI AKSI PEMILIK KOST
    // =========================================================================
    @Override
    public void onApprove(User user, int position) {
        AppDialogHelper.showConfirm(this,
                "Setujui Pemilik Kost",
                "Verifikasi akun \"" + user.getNama() + "\" (" + user.getEmail() + ") sebagai Pemilik Kost resmi di CariKostKita?",
                "Ya, Setujui",
                () -> {
                    verifPemilikAdapter.setProcessing(user.getUid(), true);
                    userRepository.approveOwnerVerification(user.getUid(), new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean success) {
                            verifPemilikAdapter.setProcessing(user.getUid(), false);
                            verifPemilikAdapter.removeItem(user);
                            int currentCount = verifPemilikAdapter.getItemCount();
                            tvVerifCount.setText(currentCount + " antrean");
                            updateVerifBadges(currentCount);
                            if (currentCount == 0) {
                                layoutVerifEmpty.setVisibility(View.VISIBLE);
                                tvVerifEmptyTitle.setText("Tidak Ada Pengajuan Pemilik");
                                tvVerifEmptyDesc.setText("Semua pendaftaran calon pemilik kost telah selesai ditinjau.");
                                rvVerif.setVisibility(View.GONE);
                            }

                            activityLogRepository.logActivity(sessionManager.getUserUid(), "APPROVE_OWNER",
                                    "Admin menyetujui akun " + user.getNama() + " (" + user.getEmail() + ") sebagai Pemilik Kost",
                                    "user", user.getUid());
                            AppDialogHelper.showSuccess(AdminMainActivity.this,
                                    "Berhasil Disetujui",
                                    "Akun " + user.getNama() + " kini telah terverifikasi sebagai Pemilik Kost.",
                                    () -> {
                                        loadAllStats();
                                        loadVerifikasiData();
                                    });
                        }

                        @Override
                        public void onError(String message) {
                            verifPemilikAdapter.setProcessing(user.getUid(), false);
                            AppDialogHelper.showError(AdminMainActivity.this, "Gagal Menyetujui", message);
                        }
                    });
                });
    }

    @Override
    public void onRequireRevision(User user, int position) {
        AppDialogHelper.showInput(this,
                "Minta Perbaikan / Revisi",
                "Tuliskan catatan perbaikan yang harus dilengkapi oleh " + user.getNama() + ":",
                "Contoh: Mohon lengkapi nomor WhatsApp aktif dan patokan kost.",
                "",
                "Kirim Permintaan",
                revisionNote -> {
                    if (revisionNote.isEmpty()) {
                        Toast.makeText(this, "Catatan perbaikan wajib diisi", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    verifPemilikAdapter.setProcessing(user.getUid(), true);
                    userRepository.requireRevisionOwnerVerification(user.getUid(), revisionNote, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean success) {
                            verifPemilikAdapter.setProcessing(user.getUid(), false);
                            verifPemilikAdapter.removeItem(user);
                            int currentCount = verifPemilikAdapter.getItemCount();
                            tvVerifCount.setText(currentCount + " antrean");
                            updateVerifBadges(currentCount);
                            if (currentCount == 0) {
                                layoutVerifEmpty.setVisibility(View.VISIBLE);
                                tvVerifEmptyTitle.setText("Tidak Ada Pengajuan Pemilik");
                                tvVerifEmptyDesc.setText("Semua pendaftaran calon pemilik kost telah selesai ditinjau.");
                                rvVerif.setVisibility(View.GONE);
                            }

                            activityLogRepository.logActivity(sessionManager.getUserUid(), "REVISION_OWNER",
                                    "Admin meminta revisi pendaftaran akun " + user.getNama() + ": " + revisionNote,
                                    "user", user.getUid());
                            AppDialogHelper.showSuccess(AdminMainActivity.this,
                                    "Permintaan Terkirim",
                                    "Status pengajuan " + user.getNama() + " diubah menjadi Perlu Perbaikan.",
                                    () -> {
                                        loadAllStats();
                                        loadVerifikasiData();
                                    });
                        }

                        @Override
                        public void onError(String message) {
                            verifPemilikAdapter.setProcessing(user.getUid(), false);
                            AppDialogHelper.showError(AdminMainActivity.this, "Gagal Mengirim Revisi", message);
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
                    verifPemilikAdapter.setProcessing(user.getUid(), true);
                    userRepository.rejectOwnerVerification(user.getUid(), "Persyaratan belum lengkap atau data kost tidak valid.", new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean success) {
                            verifPemilikAdapter.setProcessing(user.getUid(), false);
                            verifPemilikAdapter.removeItem(user);
                            int currentCount = verifPemilikAdapter.getItemCount();
                            tvVerifCount.setText(currentCount + " antrean");
                            updateVerifBadges(currentCount);
                            if (currentCount == 0) {
                                layoutVerifEmpty.setVisibility(View.VISIBLE);
                                tvVerifEmptyTitle.setText("Tidak Ada Pengajuan Pemilik");
                                tvVerifEmptyDesc.setText("Semua pendaftaran calon pemilik kost telah selesai ditinjau.");
                                rvVerif.setVisibility(View.GONE);
                            }

                            activityLogRepository.logActivity(sessionManager.getUserUid(), "REJECT_OWNER",
                                    "Admin menolak pengajuan pemilik kost akun " + user.getNama(),
                                    "user", user.getUid());
                            Toast.makeText(AdminMainActivity.this, "Pengajuan pemilik telah ditolak", Toast.LENGTH_SHORT).show();
                            loadAllStats();
                            loadVerifikasiData();
                        }

                        @Override
                        public void onError(String message) {
                            verifPemilikAdapter.setProcessing(user.getUid(), false);
                            AppDialogHelper.showError(AdminMainActivity.this, "Gagal Menolak Pengajuan", message);
                        }
                    });
                });
    }

    // =========================================================================
    // MODERASI PROPERTI KOST (PENDING & ALL)
    // =========================================================================
    @Override
    public void onEditClick(Kost kost) {
        if (currentTab == TabAdmin.VERIFIKASI && !isVerifPemilikActive) {
            showKostModerationDialog(kost);
        } else {
            Intent intent = new Intent(this, AdminKostFormActivity.class);
            intent.putExtra("kost_id", kost.getIdKost());
            startActivity(intent);
        }
    }

    @Override
    public void onToggleStatusClick(Kost kost, int position) {
        if (currentTab == TabAdmin.VERIFIKASI && !isVerifPemilikActive) {
            showKostModerationDialog(kost);
            return;
        }

        StatusKost nextStatus = (kost.getStatus() == StatusKost.TERSEDIA) ? StatusKost.PENUH : StatusKost.TERSEDIA;
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
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_moderation_kost, null);
        dialog.setContentView(view);

        TextView tvName = view.findViewById(R.id.tv_dialog_kost_name);
        TextView tvDesc = view.findViewById(R.id.tv_dialog_kost_desc);
        View btnApprove = view.findViewById(R.id.btn_action_approve);
        View btnRevision = view.findViewById(R.id.btn_action_revision);
        View btnReject = view.findViewById(R.id.btn_action_reject);
        View btnPreview = view.findViewById(R.id.btn_action_preview);
        View btnCancel = view.findViewById(R.id.btn_action_cancel);

        tvName.setText(kost.getNamaKost());
        tvDesc.setText((kost.getAlamat() != null ? kost.getAlamat() : "") + " • " + FormatUtil.formatRupiah(kost.getHarga()));

        btnApprove.setOnClickListener(v -> {
            dialog.dismiss();
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
                                loadVerifikasiData();
                            });
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnRevision.setOnClickListener(v -> {
            dialog.dismiss();
            AppDialogHelper.showInput(this,
                    "Minta Perbaikan Kost",
                    "Tuliskan catatan perbaikan data/foto untuk pemilik kost \"" + kost.getNamaKost() + "\":",
                    "Contoh: Mohon lengkapi foto kamar mandi dan patokan jalan.",
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
                                            loadVerifikasiData();
                                        });
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
        });

        btnReject.setOnClickListener(v -> {
            dialog.dismiss();
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
                                loadVerifikasiData();
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
        });

        btnPreview.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, DetailKostActivity.class);
            intent.putExtra("kost_id", kost.getIdKost());
            startActivity(intent);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // =========================================================================
    // MODERASI PENGGUNA
    // =========================================================================
    @Override
    public void onToggleStatus(User user, int position) {
        boolean currentlyActive = user.isActive();
        if (currentlyActive) {
            AppDialogHelper.showDanger(this,
                    "Tangguhkan Akun",
                    "Tangguhkan akun \"" + user.getNama() + "\" (" + user.getEmail() + ")? Pengguna tidak akan dapat masuk ke sistem.",
                    "Tangguhkan",
                    () -> {
                        userRepository.setUserActiveStatus(user.getUid(), false, new DataCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                activityLogRepository.logActivity(sessionManager.getUserUid(), "SUSPEND_USER",
                                        "Admin menangguhkan akun " + user.getNama() + " (" + user.getEmail() + ")",
                                        "user", user.getUid());
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
                        userRepository.setUserActiveStatus(user.getUid(), true, new DataCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                activityLogRepository.logActivity(sessionManager.getUserUid(), "ACTIVATE_USER",
                                        "Admin mengaktifkan kembali akun " + user.getNama() + " (" + user.getEmail() + ")",
                                        "user", user.getUid());
                                user.setActive(true);
                                userAdapter.notifyItemChanged(position);
                                Toast.makeText(AdminMainActivity.this, "Akun berhasil diaktifkan", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
        }
    }

    @Override
    public void onDeleteUser(User user, int position) {
        if (user == null) return;
        if (user.getRole() == com.carikostkita.data.model.Role.ADMIN) {
            Toast.makeText(this, "Akun Developer tidak dapat dihapus", Toast.LENGTH_SHORT).show();
            return;
        }
        if (user.getUid() != null && user.getUid().equals(sessionManager.getUserUid())) {
            Toast.makeText(this, "Tidak dapat menghapus akun Anda sendiri saat sedang login", Toast.LENGTH_SHORT).show();
            return;
        }

        AppDialogHelper.showDanger(this,
                "Hapus Akun Pengguna",
                "Apakah Anda yakin ingin menghapus akun \"" + user.getNama() + "\" (" + user.getEmail() + ") secara permanen? Data yang berkaitan dengan akun ini akan dihapus dari sistem.",
                "Ya, Hapus Akun",
                () -> {
                    userRepository.deleteUser(user.getUid(), new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            activityLogRepository.logActivity(sessionManager.getUserUid(), "DELETE_USER",
                                    "Developer menghapus akun pengguna: " + user.getNama() + " (" + user.getEmail() + ")",
                                    "user", user.getUid());
                            Toast.makeText(AdminMainActivity.this, "Akun pengguna berhasil dihapus", Toast.LENGTH_SHORT).show();
                            loadDataMasterContent();
                            loadAllStats();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(AdminMainActivity.this, "Gagal menghapus: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    // =========================================================================
    // MODERASI LAPORAN
    // =========================================================================
    @Override
    public void onResolve(KostReport report, int position) {
        AppDialogHelper.showConfirm(this,
                "Tindaklanjuti Laporan",
                "Tandai laporan untuk \"" + report.getNamaKost() + "\" telah diselesaikan?",
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
                            loadReportsData();
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
                "Tolak Laporan",
                "Tolak laporan dari " + report.getNamaPelapor() + " karena dianggap tidak valid?",
                "Tolak Laporan",
                () -> {
                    reportRepository.updateReportStatus(report.getIdReport(), ReportStatus.DITOLAK, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            activityLogRepository.logActivity(sessionManager.getUserId(), "DISMISS_REPORT",
                                    "Admin menolak laporan #" + report.getIdReport(),
                                    "report", report.getIdReport());
                            Toast.makeText(AdminMainActivity.this, "Laporan ditolak", Toast.LENGTH_SHORT).show();
                            loadAllStats();
                            loadReportsData();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(AdminMainActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    // =========================================================================
    // MANAJEMEN MITRA PEMILIK KOST
    // =========================================================================
    @Override
    public void onEditOwner(User user, int position) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_owner, null);
        TextInputEditText etName = dialogView.findViewById(R.id.et_edit_owner_name);
        TextInputEditText etPhone = dialogView.findViewById(R.id.et_edit_owner_phone);
        etName.setText(user.getNama());
        etPhone.setText(user.getNoHp());

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Data Pemilik")
                .setView(dialogView)
                .setPositiveButton("Simpan", (d, w) -> {
                    String newName = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String newPhone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    userRepository.updateProfile(user.getUid(), newName, newPhone, user.getBio(), user.getAvatarUrl(), new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            user.setNama(newName);
                            user.setNoHp(newPhone);
                            ownerAdapter.notifyItemChanged(position);
                            Toast.makeText(AdminMainActivity.this, "Data pemilik berhasil diperbarui", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(AdminMainActivity.this, "Gagal update: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    @Override
    public void onDeleteOwner(User user, int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Hapus Akun Pemilik")
                .setIcon(R.drawable.ic_delete)
                .setMessage("Apakah Anda yakin ingin menghapus akun pemilik \"" + user.getNama() + "\" (" + user.getEmail() + ")? Akun dan data akses pemilik akan dihapus dari sistem.")
                .setPositiveButton("Hapus", (d, w) -> {
                    userRepository.deleteUser(user.getUid(), new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            ownerAdapter.removeItem(position);
                            tvDataCount.setText(ownerAdapter.getItemCount() + " pemilik");
                            Toast.makeText(AdminMainActivity.this, "Akun pemilik berhasil dihapus", Toast.LENGTH_SHORT).show();
                            loadAllStats();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(AdminMainActivity.this, "Gagal menghapus: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showChangePasswordDialog() {
        Intent intent = new Intent(this, com.carikostkita.ui.profile.ChangePasswordActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void performLogout() {
        AppDialogHelper.showLogoutDialog(this, () -> {
            sessionManager.logout();
            Toast.makeText(this, "Berhasil keluar dari akun", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
