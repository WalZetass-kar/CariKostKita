package com.carikostkita.ui.admin;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
import com.carikostkita.data.model.ChatConversation;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.KostVerificationStatus;
import com.carikostkita.data.model.StatusKost;
import com.carikostkita.data.model.User;
import com.carikostkita.data.repository.ChatRepository;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.data.repository.UserRepository;
import com.carikostkita.ui.adapter.AdminKostAdapter;
import com.carikostkita.ui.adapter.ChatConversationAdapter;
import com.carikostkita.ui.auth.LoginActivity;
import com.carikostkita.ui.main.MainActivity;
import com.carikostkita.ui.main.chat.ChatRoomActivity;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.SessionManager;
import com.carikostkita.util.UserAvatarHelper;
import com.carikostkita.ui.view.ModernLineChartView;
import com.carikostkita.data.remote.dto.KostDto;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.List;

public class PemilikMainActivity extends AppCompatActivity implements
        AdminKostAdapter.OnAdminKostClickListener,
        ChatConversationAdapter.OnConversationClickListener {

    private enum TabPemilik {
        DASHBOARD,
        KOST_SAYA,
        PESAN,
        PROFIL
    }

    private TabPemilik currentTab = TabPemilik.DASHBOARD;

    // Drawer & Header Views
    private DrawerLayout drawerLayout;
    private TextView tvHeaderSub;
    private TextView tvHeaderTitle;
    private ProgressBar pbLoading;

    // Tab Containers
    private View containerDashboard;
    private View containerKost;
    private View containerPesan;
    private View containerProfil;
    private FloatingActionButton fabAddKost;

    // Dashboard Tab Views
    private TextView tvWelcomeName;
    private TextView tvStatTotal;
    private TextView tvStatAktif;
    private TextView tvStatPending;
    private TextView tvStatRevisi;
    private TextView tvOccupancyPercent;
    private TextView tvOccupancySub;
    private ProgressBar pbOccupancy;
    private TextView tvKamarTerisi;
    private TextView tvKamarTersedia;
    private TextView tvKamarTotal;
    private View btnQuickAdd;
    private View btnQuickChat;
    private View btnQuickKatalog;
    private View btnQuickKost;
    private View cardHighlight;
    private TextView tvHighlightTitle;
    private TextView tvHighlightLocation;
    private TextView tvHighlightPrice;
    private TextView tvHighlightRooms;
    private View btnGotoKostTab;

    // Kost Saya Tab Views
    private TextView tvKostCount;
    private TextView chipAll, chipApproved, chipPending, chipRevision;
    private RecyclerView rvKost;
    private LinearLayout layoutEmptyKost;
    private AdminKostAdapter kostAdapter;
    private List<Kost> allOwnerKosts = new ArrayList<>();
    private String currentKostFilter = "ALL";

    // Portfolio Statistics Views ("Property Anda")
    private TextView tvPortfolioTotalKost;
    private TextView tvPortfolioKostStatus;
    private TextView tvPortfolioKamarStatus;
    private TextView tvPortfolioOccupancy;
    private TextView tvPortfolioChatCount;
    private TextView tvPortfolioFavoritCount;
    private TextView tvPortfolioChartTitle;
    private ModernLineChartView chartPemilikPortfolio;
    private TextView chipPortfolio7d, chipPortfolio30d, chipPortfolio3m;
    private int currentPortfolioDays = 7;
    private final List<KostDto> cachedOwnerKosts = new ArrayList<>();
    private int cachedOwnerChatCount = 0;
    private int cachedOwnerFavoritCount = 0;

    // Pesan Tab Views
    private RecyclerView rvChats;
    private LinearLayout layoutEmptyChat;
    private ChatConversationAdapter chatAdapter;

    // Profil Tab Views
    private ImageView ivAvatar;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvProfilePhone;
    private MaterialButton btnEditProfile;
    private MaterialButton btnOpenKatalogPublik;
    private View itemChangePassword;
    private View itemNotification;
    private TextView tvNotificationStatus;
    private boolean notificationsEnabled = true;

    // Bottom Navigation Views
    private final View[] navTabs = new View[4];
    private final LinearLayout[] navPills = new LinearLayout[4];
    private final ImageView[] navIcons = new ImageView[4];
    private final TextView[] navLabels = new TextView[4];
    private View badgeUnreadChat;

    // Repositories
    private KostRepository kostRepository;
    private ChatRepository chatRepository;
    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        kostRepository = new KostRepository(this);
        chatRepository = new ChatRepository(this);
        userRepository = new UserRepository(this);

        // Strict Role-Based Access Control: HANYA Pemilik Kost
        if (!sessionManager.isLoggedIn() || !sessionManager.isPemilikKost()) {
            Toast.makeText(this, "Akses ditolak: Halaman ini khusus Pemilik Kost", Toast.LENGTH_SHORT).show();
            if (sessionManager.isDeveloper()) {
                startActivity(new Intent(this, AdminMainActivity.class));
            } else {
                startActivity(new Intent(this, MainActivity.class));
            }
            finish();
            return;
        }

        setContentView(R.layout.activity_pemilik_main);

        initViews();
        setupBottomNav();
        setupAdapters();
        switchTab(TabPemilik.DASHBOARD);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sessionManager.isLoggedIn() || !sessionManager.isPemilikKost()) {
            finish();
            return;
        }
        updateDrawerProfile();
        populateProfileData();
        loadDashboardStats();
        if (currentTab == TabPemilik.KOST_SAYA) {
            loadOwnerKosts();
        } else if (currentTab == TabPemilik.PESAN) {
            loadOwnerChats();
        }
    }

    private void initViews() {
        tvHeaderSub = findViewById(R.id.tv_pemilik_header_sub);
        tvHeaderTitle = findViewById(R.id.tv_pemilik_header_title);
        pbLoading = findViewById(R.id.pb_pemilik_loading);
        
        drawerLayout = findViewById(R.id.drawer_pemilik_layout);
        ImageButton btnMenu = findViewById(R.id.btn_pemilik_menu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
        setupDrawer();

        containerDashboard = findViewById(R.id.container_tab_dashboard);
        containerKost = findViewById(R.id.container_tab_kost);
        containerPesan = findViewById(R.id.container_tab_pesan);
        containerProfil = findViewById(R.id.container_tab_profil);
        fabAddKost = findViewById(R.id.fab_pemilik_add_kost);

        // Dashboard
        tvWelcomeName = findViewById(R.id.tv_pemilik_welcome_name);
        tvStatTotal = findViewById(R.id.tv_pemilik_stat_total);
        tvStatAktif = findViewById(R.id.tv_pemilik_stat_aktif);
        tvStatPending = findViewById(R.id.tv_pemilik_stat_pending);
        tvStatRevisi = findViewById(R.id.tv_pemilik_stat_revisi);
        tvOccupancyPercent = findViewById(R.id.tv_pemilik_occupancy_percent);
        tvOccupancySub = findViewById(R.id.tv_pemilik_occupancy_sub);
        pbOccupancy = findViewById(R.id.pb_pemilik_occupancy);
        tvKamarTerisi = findViewById(R.id.tv_pemilik_kamar_terisi);
        tvKamarTersedia = findViewById(R.id.tv_pemilik_kamar_tersedia);
        tvKamarTotal = findViewById(R.id.tv_pemilik_kamar_total);

        btnQuickAdd = findViewById(R.id.btn_pemilik_quick_add);
        btnQuickChat = findViewById(R.id.btn_pemilik_quick_chat);
        btnQuickKatalog = findViewById(R.id.btn_pemilik_quick_katalog);
        btnQuickKost = findViewById(R.id.btn_pemilik_quick_kost);

        cardHighlight = findViewById(R.id.card_pemilik_highlight);
        tvHighlightTitle = findViewById(R.id.tv_pemilik_highlight_title);
        tvHighlightLocation = findViewById(R.id.tv_pemilik_highlight_location);
        tvHighlightPrice = findViewById(R.id.tv_pemilik_highlight_price);
        tvHighlightRooms = findViewById(R.id.tv_pemilik_highlight_rooms);
        btnGotoKostTab = findViewById(R.id.btn_pemilik_goto_kost_tab);

        if (btnQuickAdd != null) {
            btnQuickAdd.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminKostFormActivity.class);
                intent.putExtra("owner_id", sessionManager.getUserId());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
        if (btnQuickChat != null) {
            btnQuickChat.setOnClickListener(v -> switchTab(TabPemilik.PESAN));
        }
        if (btnQuickKatalog != null) {
            btnQuickKatalog.setOnClickListener(v -> {
                Intent intent = new Intent(this, OwnerKatalogPublikActivity.class);
                intent.putExtra("owner_id", sessionManager.getUserId());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
        if (btnQuickKost != null) {
            btnQuickKost.setOnClickListener(v -> switchTab(TabPemilik.KOST_SAYA));
        }
        if (btnGotoKostTab != null) {
            btnGotoKostTab.setOnClickListener(v -> switchTab(TabPemilik.KOST_SAYA));
        }
        if (cardHighlight != null) {
            cardHighlight.setOnClickListener(v -> switchTab(TabPemilik.KOST_SAYA));
        }

        // Kost Saya
        tvKostCount = findViewById(R.id.tv_pemilik_count);
        rvKost = findViewById(R.id.rv_pemilik_kost);
        layoutEmptyKost = findViewById(R.id.layout_pemilik_empty);
        chipAll = findViewById(R.id.chip_pemilik_filter_all);
        chipApproved = findViewById(R.id.chip_pemilik_filter_approved);
        chipPending = findViewById(R.id.chip_pemilik_filter_pending);
        chipRevision = findViewById(R.id.chip_pemilik_filter_revision);

        // Portfolio Views
        tvPortfolioTotalKost = findViewById(R.id.tv_portfolio_total_kost);
        tvPortfolioKostStatus = findViewById(R.id.tv_portfolio_kost_status);
        tvPortfolioKamarStatus = findViewById(R.id.tv_portfolio_kamar_status);
        tvPortfolioOccupancy = findViewById(R.id.tv_portfolio_occupancy);
        tvPortfolioChatCount = findViewById(R.id.tv_portfolio_chat_count);
        tvPortfolioFavoritCount = findViewById(R.id.tv_portfolio_favorit_count);
        tvPortfolioChartTitle = findViewById(R.id.tv_portfolio_chart_title);
        chartPemilikPortfolio = findViewById(R.id.chart_pemilik_portfolio);
        chipPortfolio7d = findViewById(R.id.chip_portfolio_7d);
        chipPortfolio30d = findViewById(R.id.chip_portfolio_30d);
        chipPortfolio3m = findViewById(R.id.chip_portfolio_3m);

        if (chipPortfolio7d != null) chipPortfolio7d.setOnClickListener(v -> setPortfolioPeriod(7));
        if (chipPortfolio30d != null) chipPortfolio30d.setOnClickListener(v -> setPortfolioPeriod(30));
        if (chipPortfolio3m != null) chipPortfolio3m.setOnClickListener(v -> setPortfolioPeriod(90));
        setPortfolioPeriod(7);

        // Pesan
        rvChats = findViewById(R.id.rv_pemilik_chats);
        layoutEmptyChat = findViewById(R.id.layout_pemilik_chat_empty);

        // Profil
        ivAvatar = findViewById(R.id.iv_pemilik_profile_avatar);
        tvProfileName = findViewById(R.id.tv_pemilik_profile_name);
        tvProfileEmail = findViewById(R.id.tv_pemilik_profile_email);
        tvProfilePhone = findViewById(R.id.tv_pemilik_profile_phone);
        btnEditProfile = findViewById(R.id.btn_pemilik_edit_profile);
        btnOpenKatalogPublik = findViewById(R.id.btn_pemilik_open_katalog_publik);
        itemChangePassword = findViewById(R.id.item_pemilik_change_password);
        itemNotification = findViewById(R.id.item_pemilik_notification_settings);
        tvNotificationStatus = findViewById(R.id.tv_pemilik_notification_status);

        // Chip Filters
        chipAll.setOnClickListener(v -> applyKostFilter("ALL"));
        chipApproved.setOnClickListener(v -> applyKostFilter("APPROVED"));
        chipPending.setOnClickListener(v -> applyKostFilter("PENDING"));
        chipRevision.setOnClickListener(v -> applyKostFilter("REVISION"));

        // FAB Tambah Kost
        fabAddKost.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminKostFormActivity.class);
            intent.putExtra("owner_id", sessionManager.getUserId());
            startActivity(intent);
        });

        // Profil Actions
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        // SATU AKSES TUNGGAL: Katalog Publik
        btnOpenKatalogPublik.setOnClickListener(v -> {
            startActivity(new Intent(this, OwnerKatalogPublikActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        itemChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        itemNotification.setOnClickListener(v -> {
            notificationsEnabled = !notificationsEnabled;
            tvNotificationStatus.setText(notificationsEnabled ? "Aktif" : "Senyap");
            tvNotificationStatus.setTextColor(ContextCompat.getColor(this,
                    notificationsEnabled ? R.color.status_tersedia : R.color.text_muted));
            Toast.makeText(this, notificationsEnabled ? "Notifikasi chat aktif" : "Notifikasi chat disenyapkan", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupDrawer() {
        if (drawerLayout == null) return;
        updateDrawerProfile();

        // 5 Primary Hamburger Menu Items
        View itemTips = findViewById(R.id.item_drawer_pemilik_tips);
        View itemBantuan = findViewById(R.id.item_drawer_pemilik_bantuan);
        View itemPengaturan = findViewById(R.id.item_drawer_pemilik_pengaturan);
        View itemTentang = findViewById(R.id.item_drawer_pemilik_tentang);
        View btnLogoutDrawer = findViewById(R.id.btn_drawer_pemilik_logout);

        if (itemTips != null) {
            itemTips.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                showTipsPanduanDialog();
            });
        }
        if (itemBantuan != null) {
            itemBantuan.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                showBantuanDialog();
            });
        }
        if (itemPengaturan != null) {
            itemPengaturan.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                switchTab(TabPemilik.PROFIL);
            });
        }
        if (itemTentang != null) {
            itemTentang.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                showAboutDialog();
            });
        }
        if (btnLogoutDrawer != null) {
            btnLogoutDrawer.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                performLogout();
            });
        }
    }

    private void showTipsPanduanDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_tips_panduan, null);
        dialog.setContentView(view);

        View btnClose = view.findViewById(R.id.btn_close_tips);
        View btnDismiss = view.findViewById(R.id.btn_dismiss_tips);

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnDismiss != null) btnDismiss.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showBantuanDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Pusat Bantuan Pemilik")
                .setMessage("Butuh bantuan teknis atau informasi mengenai verifikasi akun pemilik kost?\n\n• Email Dukungan: support@carikostkita.com\n• WhatsApp Care: +62 822-8390-1234\n• Jam Operasional: Setiap Hari (08.00 - 21.00 WIB)")
                .setPositiveButton("Tutup", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Tentang CariKostKita")
                .setMessage("CariKostKita Mobile v1.0.0\n\nPlatform pencarian dan pengelolaan kost modern berbasis peta di Pekanbaru.\n\n© 2026 CariKostKita. Hak cipta dilindungi undang-undang.")
                .setPositiveButton("Tutup", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateDrawerProfile() {
        ImageView ivDrawerAvatar = findViewById(R.id.iv_pemilik_drawer_avatar);
        TextView tvDrawerName = findViewById(R.id.tv_pemilik_drawer_name);
        TextView tvDrawerEmail = findViewById(R.id.tv_pemilik_drawer_email);
        TextView tvDrawerRole = findViewById(R.id.tv_pemilik_drawer_role);

        if (tvDrawerName != null) tvDrawerName.setText(sessionManager.getUserName());
        if (tvDrawerEmail != null) tvDrawerEmail.setText(sessionManager.getUserEmail());
        if (ivDrawerAvatar != null) {
            UserAvatarHelper.loadAvatar(ivDrawerAvatar, sessionManager.getUserAvatar());
        }
        if (tvDrawerRole != null) {
            if (sessionManager.isDeveloper()) {
                tvDrawerRole.setText("DEVELOPER / SUPER ADMIN");
                tvDrawerRole.setBackgroundResource(R.drawable.bg_badge_campur);
                tvDrawerRole.setTextColor(ContextCompat.getColor(this, R.color.badge_campur));
            } else {
                tvDrawerRole.setText("PEMILIK KOST TERVERIFIKASI");
                tvDrawerRole.setBackgroundResource(R.drawable.bg_badge_putra);
                tvDrawerRole.setTextColor(ContextCompat.getColor(this, R.color.badge_putra));
            }
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
        View bottomNav = findViewById(R.id.bottom_nav_pemilik);
        if (bottomNav == null) return;

        navTabs[0] = bottomNav.findViewById(R.id.tab_pemilik_nav_dashboard);
        navTabs[1] = bottomNav.findViewById(R.id.tab_pemilik_nav_kost);
        navTabs[2] = bottomNav.findViewById(R.id.tab_pemilik_nav_pesan);
        navTabs[3] = bottomNav.findViewById(R.id.tab_pemilik_nav_profil);

        navPills[0] = bottomNav.findViewById(R.id.pill_pemilik_dashboard);
        navPills[1] = bottomNav.findViewById(R.id.pill_pemilik_kost);
        navPills[2] = bottomNav.findViewById(R.id.pill_pemilik_pesan);
        navPills[3] = bottomNav.findViewById(R.id.pill_pemilik_profil);

        navIcons[0] = bottomNav.findViewById(R.id.iv_pemilik_nav_dashboard);
        navIcons[1] = bottomNav.findViewById(R.id.iv_pemilik_nav_kost);
        navIcons[2] = bottomNav.findViewById(R.id.iv_pemilik_nav_pesan);
        navIcons[3] = bottomNav.findViewById(R.id.iv_pemilik_nav_profil);

        navLabels[0] = bottomNav.findViewById(R.id.tv_pemilik_nav_dashboard);
        navLabels[1] = bottomNav.findViewById(R.id.tv_pemilik_nav_kost);
        navLabels[2] = bottomNav.findViewById(R.id.tv_pemilik_nav_pesan);
        navLabels[3] = bottomNav.findViewById(R.id.tv_pemilik_nav_profil);

        badgeUnreadChat = bottomNav.findViewById(R.id.badge_unread_pemilik_chat);

        navTabs[0].setOnClickListener(v -> switchTab(TabPemilik.DASHBOARD));
        navTabs[1].setOnClickListener(v -> switchTab(TabPemilik.KOST_SAYA));
        navTabs[2].setOnClickListener(v -> switchTab(TabPemilik.PESAN));
        navTabs[3].setOnClickListener(v -> switchTab(TabPemilik.PROFIL));
    }

    private void setupAdapters() {
        kostAdapter = new AdminKostAdapter(this);
        rvKost.setLayoutManager(new LinearLayoutManager(this));
        rvKost.setAdapter(kostAdapter);

        chatAdapter = new ChatConversationAdapter(this, true, this);
        rvChats.setLayoutManager(new LinearLayoutManager(this));
        rvChats.setAdapter(chatAdapter);
    }

    private void switchTab(TabPemilik tab) {
        currentTab = tab;
        int tabIndex = tab.ordinal();

        // Hide all containers
        containerDashboard.setVisibility(View.GONE);
        containerKost.setVisibility(View.GONE);
        containerPesan.setVisibility(View.GONE);
        containerProfil.setVisibility(View.GONE);
        fabAddKost.setVisibility(View.GONE);

        View bottomNav = findViewById(R.id.bottom_nav_pemilik);
        if (bottomNav != null) {
            ViewGroup navContainer = bottomNav.findViewById(R.id.nav_pemilik_container);
            if (navContainer != null) {
                AutoTransition transition = new AutoTransition();
                transition.setDuration(250);
                transition.setInterpolator(new FastOutSlowInInterpolator());
                TransitionManager.beginDelayedTransition(navContainer, transition);
            }
        }

        int colorSecondary = ContextCompat.getColor(this, R.color.text_secondary);

        for (int i = 0; i < 4; i++) {
            if (i == tabIndex) {
                if (navTabs[i] != null) {
                    navTabs[i].setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.6f));
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

        // Switch active tab view and load data
        switch (tab) {
            case DASHBOARD:
                tvHeaderSub.setText("Pemilik Properti Terverifikasi");
                tvHeaderTitle.setText("Dashboard Pemilik");
                animateContainerIn(containerDashboard);
                loadDashboardStats();
                break;

            case KOST_SAYA:
                tvHeaderSub.setText("Manajemen Properti");
                tvHeaderTitle.setText("Kost Saya");
                animateContainerIn(containerKost);
                fabAddKost.setVisibility(View.VISIBLE);
                loadOwnerKosts();
                break;

            case PESAN:
                tvHeaderSub.setText("Komunikasi Calon Penyewa");
                tvHeaderTitle.setText("Pusat Pesan");
                animateContainerIn(containerPesan);
                loadOwnerChats();
                break;

            case PROFIL:
                tvHeaderSub.setText("Kelola Akun");
                tvHeaderTitle.setText("Profil Pemilik");
                animateContainerIn(containerProfil);
                populateProfileData();
                break;
        }
    }

    private void loadDashboardStats() {
        int ownerId = sessionManager.getUserId();
        tvWelcomeName.setText(sessionManager.getUserName());

        kostRepository.getPemilikStats(ownerId, new DataCallback<KostRepository.PemilikStats>() {
            @Override
            public void onSuccess(KostRepository.PemilikStats stats) {
                tvStatTotal.setText(String.valueOf(stats.totalKost));
                tvStatAktif.setText(String.valueOf(stats.totalAktif));
                tvStatPending.setText(String.valueOf(stats.totalPending));
                tvStatRevisi.setText(String.valueOf(stats.totalRevisi));

                int totalKamar = stats.totalTersedia + stats.totalTerisi;
                int percent = totalKamar > 0 ? (stats.totalTerisi * 100) / totalKamar : 0;
                if (tvOccupancyPercent != null) tvOccupancyPercent.setText(percent + "%");
                if (pbOccupancy != null) pbOccupancy.setProgress(percent);
                if (tvOccupancySub != null) {
                    tvOccupancySub.setText(stats.totalTerisi + " dari " + totalKamar + " kamar aktif terisi (" + stats.totalTersedia + " kamar kosong)");
                }
                if (tvKamarTerisi != null) tvKamarTerisi.setText(String.valueOf(stats.totalTerisi));
                if (tvKamarTersedia != null) tvKamarTersedia.setText(String.valueOf(stats.totalTersedia));
                if (tvKamarTotal != null) tvKamarTotal.setText(String.valueOf(totalKamar));
            }

            @Override
            public void onError(String message) {}
        });

        // Load primary property highlight for dashboard
        kostRepository.getKostByPemilik(ownerId, new DataCallback<List<Kost>>() {
            @Override
            public void onSuccess(List<Kost> list) {
                if (list != null && !list.isEmpty()) {
                    Kost primary = list.get(0);
                    if (cardHighlight != null) cardHighlight.setVisibility(View.VISIBLE);
                    if (tvHighlightTitle != null) tvHighlightTitle.setText(primary.getNamaKost());
                    if (tvHighlightLocation != null) tvHighlightLocation.setText(primary.getFullLocation());
                    if (tvHighlightPrice != null) tvHighlightPrice.setText(primary.getFormattedHarga());
                    if (tvHighlightRooms != null) {
                        tvHighlightRooms.setText(primary.getKamarTersedia() + " Kamar Kosong");
                    }
                } else {
                    if (cardHighlight != null) cardHighlight.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void loadOwnerKosts() {
        int ownerId = sessionManager.getUserId();
        pbLoading.setVisibility(View.VISIBLE);

        kostRepository.getKostByPemilik(ownerId, new DataCallback<List<Kost>>() {
            @Override
            public void onSuccess(List<Kost> data) {
                pbLoading.setVisibility(View.GONE);
                allOwnerKosts = data;
                applyKostFilter(currentKostFilter);
            }

            @Override
            public void onError(String message) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(PemilikMainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });

        loadPortfolioStats();
    }

    private void loadPortfolioStats() {
        String ownerUid = sessionManager.getUserUid();

        kostRepository.getPemilikStats(ownerUid, new DataCallback<KostRepository.PemilikStats>() {
            @Override
            public void onSuccess(KostRepository.PemilikStats stats) {
                if (tvPortfolioTotalKost != null) tvPortfolioTotalKost.setText(String.valueOf(stats.totalKost));
                if (tvPortfolioKostStatus != null) {
                    tvPortfolioKostStatus.setText(stats.totalAktif + " aktif • " + stats.totalPending + " pending");
                }
                if (tvPortfolioKamarStatus != null) {
                    tvPortfolioKamarStatus.setText(stats.totalTersedia + " kosong / " + stats.totalTerisi + " terisi");
                }
                int totalKamar = stats.totalTersedia + stats.totalTerisi;
                int percent = totalKamar > 0 ? (stats.totalTerisi * 100) / totalKamar : 0;
                if (tvPortfolioOccupancy != null) {
                    tvPortfolioOccupancy.setText(percent + "% terisi (" + totalKamar + " total)");
                }
                if (tvPortfolioFavoritCount != null) {
                    tvPortfolioFavoritCount.setText(stats.totalFavorit + " Pengguna");
                }

                cachedOwnerKosts.clear();
                if (stats.kostList != null) {
                    cachedOwnerKosts.addAll(stats.kostList);
                }

                updatePortfolioChart();
            }

            @Override
            public void onError(String message) {}
        });

        chatRepository.getConversationsForUser(ownerUid, new DataCallback<List<ChatConversation>>() {
            @Override
            public void onSuccess(List<ChatConversation> list) {
                int chatCount = (list != null) ? list.size() : 0;
                cachedOwnerChatCount = chatCount;
                if (tvPortfolioChatCount != null) {
                    tvPortfolioChatCount.setText(chatCount + " Percakapan");
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void setPortfolioPeriod(int days) {
        currentPortfolioDays = days;
        int activeBg = R.drawable.bg_chip_filter_active;
        int inactiveBg = R.drawable.bg_chip_filter_inactive;
        int activeColor = ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);

        if (chipPortfolio7d != null) {
            chipPortfolio7d.setBackgroundResource(days == 7 ? activeBg : inactiveBg);
            chipPortfolio7d.setTextColor(days == 7 ? activeColor : inactiveColor);
            chipPortfolio7d.setTypeface(null, days == 7 ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (chipPortfolio30d != null) {
            chipPortfolio30d.setBackgroundResource(days == 30 ? activeBg : inactiveBg);
            chipPortfolio30d.setTextColor(days == 30 ? activeColor : inactiveColor);
            chipPortfolio30d.setTypeface(null, days == 30 ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (chipPortfolio3m != null) {
            chipPortfolio3m.setBackgroundResource(days == 90 ? activeBg : inactiveBg);
            chipPortfolio3m.setTextColor(days == 90 ? activeColor : inactiveColor);
            chipPortfolio3m.setTypeface(null, days == 90 ? Typeface.BOLD : Typeface.NORMAL);
        }

        updatePortfolioChart();
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

    private void updatePortfolioChart() {
        if (chartPemilikPortfolio == null) return;

        String periodTitle = (currentPortfolioDays == 7 ? "7 Hari Terakhir" :
                              currentPortfolioDays == 30 ? "30 Hari Terakhir" : "3 Bulan Terakhir");

        if (tvPortfolioChartTitle != null) {
            tvPortfolioChartTitle.setText("Tren Kapasitas Kamar Terisi (" + periodTitle + ")");
        }

        List<ModernLineChartView.DataPoint> points = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        long now = cal.getTimeInMillis();
        long startTime = now - ((long) currentPortfolioDays * 24L * 60L * 60L * 1000L);

        int numBuckets = (currentPortfolioDays == 7 ? 7 : (currentPortfolioDays == 30 ? 7 : 7));
        long intervalStep = (now - startTime) / Math.max(1, numBuckets - 1);

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

        for (int i = 0; i < numBuckets; i++) {
            long bucketTime = (i == numBuckets - 1) ? now : (startTime + (i * intervalStep));
            String label = dayFormat.format(new Date(bucketTime));

            int terisiAtPoint = 0;
            for (KostDto k : cachedOwnerKosts) {
                long t = parseIsoDate(k.createdAt);
                if (t <= bucketTime) {
                    if (k.totalKamar != null && k.kamarTersedia != null) {
                        terisiAtPoint += Math.max(0, k.totalKamar - k.kamarTersedia);
                    }
                }
            }
            points.add(new ModernLineChartView.DataPoint(label, terisiAtPoint));
        }

        chartPemilikPortfolio.setData(points);
    }

    private void applyKostFilter(String filter) {
        currentKostFilter = filter;
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
            layoutEmptyKost.setVisibility(View.VISIBLE);
            rvKost.setVisibility(View.GONE);
        } else {
            layoutEmptyKost.setVisibility(View.GONE);
            rvKost.setVisibility(View.VISIBLE);
            kostAdapter.submitList(filtered);
        }
        tvKostCount.setText(filtered.size() + " properti");
    }

    private void updateChipStyles() {
        int activeBg = R.drawable.bg_chip_filter_active;
        int inactiveBg = R.drawable.bg_chip_filter_inactive;
        int activeColor = ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);

        chipAll.setBackgroundResource("ALL".equals(currentKostFilter) ? activeBg : inactiveBg);
        chipAll.setTextColor("ALL".equals(currentKostFilter) ? activeColor : inactiveColor);
        chipApproved.setBackgroundResource("APPROVED".equals(currentKostFilter) ? activeBg : inactiveBg);
        chipApproved.setTextColor("APPROVED".equals(currentKostFilter) ? activeColor : inactiveColor);
        chipPending.setBackgroundResource("PENDING".equals(currentKostFilter) ? activeBg : inactiveBg);
        chipPending.setTextColor("PENDING".equals(currentKostFilter) ? activeColor : inactiveColor);
        chipRevision.setBackgroundResource("REVISION".equals(currentKostFilter) ? activeBg : inactiveBg);
        chipRevision.setTextColor("REVISION".equals(currentKostFilter) ? activeColor : inactiveColor);
    }

    private void loadOwnerChats() {
        String ownerUid = sessionManager.getUserUid();
        pbLoading.setVisibility(View.VISIBLE);

        chatRepository.getConversationsForUser(ownerUid, new DataCallback<List<ChatConversation>>() {
            @Override
            public void onSuccess(List<ChatConversation> data) {
                pbLoading.setVisibility(View.GONE);
                if (data == null || data.isEmpty()) {
                    layoutEmptyChat.setVisibility(View.VISIBLE);
                    rvChats.setVisibility(View.GONE);
                } else {
                    layoutEmptyChat.setVisibility(View.GONE);
                    rvChats.setVisibility(View.VISIBLE);
                    chatAdapter.setConversations(data);
                }
            }

            @Override
            public void onError(String message) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(PemilikMainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateProfileData() {
        tvProfileName.setText(sessionManager.getUserName());
        tvProfileEmail.setText(sessionManager.getUserEmail());

        String phone = sessionManager.getUserPhone();
        tvProfilePhone.setText((phone != null && !phone.isEmpty()) ? "No. WhatsApp: " + phone : "No. WhatsApp: Belum diatur");
        if (ivAvatar != null) {
            UserAvatarHelper.loadAvatar(ivAvatar, sessionManager.getUserAvatar());
        }
    }

    @Override
    public void onConversationClick(ChatConversation conversation) {
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra("conversation_id", conversation.getIdConversation());
        intent.putExtra("kost_id", conversation.getIdKost());
        intent.putExtra("id_pencari", conversation.getIdPencari());
        intent.putExtra("id_pemilik", conversation.getIdPemilik());
        intent.putExtra("nama_kost", conversation.getNamaKost());
        intent.putExtra("foto_kost", conversation.getFotoKost());
        intent.putExtra("nama_counterpart", conversation.getNamaPencari());
        startActivity(intent);
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
        StatusKost nextStatus = (kost.getStatus() == StatusKost.TERSEDIA) ? StatusKost.PENUH : StatusKost.TERSEDIA;

        AppDialogHelper.showConfirm(this,
                "Ubah Status Ketersediaan",
                "Ubah ketersediaan properti \"" + kost.getNamaKost() + "\" menjadi " + nextStatus.getDisplayName() + "?",
                "Ya, Ubah",
                () -> {
                    kostRepository.updateStatus(kost.getIdKost(), nextStatus, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean success) {
                            kost.setStatus(nextStatus);
                            kostAdapter.notifyItemChanged(position);
                            Toast.makeText(PemilikMainActivity.this, "Status diubah menjadi: " + nextStatus.getDisplayName(), Toast.LENGTH_SHORT).show();
                            loadDashboardStats();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(PemilikMainActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    private void showEditProfileDialog() {
        Intent intent = new Intent(this, com.carikostkita.ui.profile.EditProfileActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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
