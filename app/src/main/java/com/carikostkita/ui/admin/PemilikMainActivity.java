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
import com.google.android.material.switchmaterial.SwitchMaterial;
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

    // Header Views
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
    private android.widget.CompoundButton switchNotification;
    private boolean notificationsEnabled = true;
    private int kostTanpaDataKamar = 0;

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
        com.carikostkita.notifications.AppNotifications.onAppOpened(this);
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
        populateProfileData();
        loadDashboardStats();
        if (currentTab == TabPemilik.KOST_SAYA) {
            loadOwnerKosts();
        } else if (currentTab == TabPemilik.PESAN) {
            loadOwnerChats();
        }
    }

    private void initViews() {
        View headerBanner = findViewById(R.id.header_pemilik_banner);
        if (headerBanner != null) headerBanner.setClipToOutline(true);
        tvHeaderSub = findViewById(R.id.tv_pemilik_header_sub);
        tvHeaderTitle = findViewById(R.id.tv_pemilik_header_title);
        pbLoading = findViewById(R.id.pb_pemilik_loading);

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
        notificationsEnabled = sessionManager.isChatNotificationEnabled();
        switchNotification = com.carikostkita.util.SettingRowBinder.bindToggle(
                findViewById(R.id.item_pemilik_notification_settings), R.drawable.ic_bell,
                "Notifikasi Pesan", "Kabari saya saat ada calon penyewa bertanya",
                notificationsEnabled, (btn, isChecked) -> {
                    notificationsEnabled = isChecked;
                    sessionManager.setChatNotificationEnabled(isChecked);
                });
        com.carikostkita.util.SettingRowBinder.bind(itemChangePassword, R.drawable.ic_lock,
                "Ganti Kata Sandi", "Jaga akun pemilik tetap aman", null);
        com.carikostkita.util.SettingRowBinder.bind(findViewById(R.id.item_pemilik_surveys), R.drawable.ic_clock,
                "Jadwal Survei", "Konfirmasi kunjungan calon penyewa",
                v -> startActivity(new Intent(this, com.carikostkita.ui.survey.SurveyListActivity.class)));
        com.carikostkita.util.SettingRowBinder.bind(findViewById(R.id.item_pemilik_delete_account), R.drawable.ic_delete,
                "Hapus Akun", "Akun dan semua kost milikmu dihapus permanen",
                v -> com.carikostkita.util.AccountDeletion.confirm(this));
        View seeAllChats = findViewById(R.id.btn_pemilik_see_all_chats);
        if (seeAllChats != null) seeAllChats.setOnClickListener(v -> switchTab(TabPemilik.PESAN));

        // Chip Filters
        chipAll.setOnClickListener(v -> applyKostFilter("ALL"));
        chipApproved.setOnClickListener(v -> applyKostFilter("APPROVED"));
        chipPending.setOnClickListener(v -> applyKostFilter("PENDING"));
        chipRevision.setOnClickListener(v -> applyKostFilter("REVISION"));

        // FAB Tambah Kost
        fabAddKost.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminKostFormActivity.class);
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

        setupProfilMenuItems();
    }

    private void setupProfilMenuItems() {
        View itemTips = findViewById(R.id.item_pemilik_tips);
        View itemBantuan = findViewById(R.id.item_pemilik_bantuan);
        View itemTentang = findViewById(R.id.item_pemilik_tentang);
        View btnLogoutProfile = findViewById(R.id.btn_pemilik_logout_profile);

        com.carikostkita.util.SettingRowBinder.bind(itemTips, R.drawable.ic_sparkle,
                "Tips & Panduan", "Cara membuat listing yang cepat laku", null);
        com.carikostkita.util.SettingRowBinder.bind(itemBantuan, R.drawable.ic_info,
                "Pusat Bantuan", "Pertanyaan umum & kontak tim", null);
        com.carikostkita.util.SettingRowBinder.bind(itemTentang, R.drawable.ic_verified,
                "Tentang CariKostKita", "Versi " + com.carikostkita.BuildConfig.VERSION_NAME + ", syarat & privasi", null);
        if (itemTips != null) itemTips.setOnClickListener(v -> showTipsPanduanDialog());
        if (itemBantuan != null) itemBantuan.setOnClickListener(v -> showBantuanDialog());
        if (itemTentang != null) itemTentang.setOnClickListener(v -> showAboutDialog());
        if (btnLogoutProfile != null) btnLogoutProfile.setOnClickListener(v -> performLogout());
    }

    private static class TipSlideItem {
        final String title;
        final String desc;
        final int iconRes;
        final int iconColor;
        final int bgRes;

        TipSlideItem(String title, String desc, int iconRes, int iconColor, int bgRes) {
            this.title = title;
            this.desc = desc;
            this.iconRes = iconRes;
            this.iconColor = iconColor;
            this.bgRes = bgRes;
        }
    }

    private void showTipsPanduanDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_tips_panduan, null);
        dialog.setContentView(view);

        final TipSlideItem[] tips = new TipSlideItem[]{
                new TipSlideItem("1. Cara Menambahkan Kost",
                        "Buka menu 'Kost Saya', lalu tekan tombol '+ Tambah Kost'. Ikuti formulir multi-langkah mulai dari info umum, lokasi peta, hingga foto kost.",
                        R.drawable.ic_add, ContextCompat.getColor(this, R.color.primary), R.drawable.bg_circle_icon_blue),
                new TipSlideItem("2. Cara Mengubah Data Kost",
                        "Pilih kost di tab 'Kost Saya' dan tekan tombol 'Edit'. Anda dapat memperbarui foto, fasilitas terbaru, atau harga sewa bulanan kapan saja.",
                        R.drawable.ic_edit, 0xFF8B5CF6, R.drawable.bg_circle_icon_purple),
                new TipSlideItem("3. Cara Memperbarui Kamar Kosong",
                        "Selalu perbarui jumlah ketersediaan kamar kosong di kartu properti Anda. Status kamar yang akurat menarik minat pencari kost lebih cepat.",
                        R.drawable.ic_bed, 0xFF10B981, R.drawable.bg_circle_icon_green),
                new TipSlideItem("4. Menangani Pesan Calon Penyewa",
                        "Cek tab 'Pesan' secara berkala. Berikan respons cepat dan sopan terhadap pertanyaan calon penyewa mengenai peraturan, fasilitas, atau jadwal survei.",
                        R.drawable.ic_nav_chat, ContextCompat.getColor(this, R.color.primary), R.drawable.bg_circle_icon_blue),
                new TipSlideItem("5. Tingkatkan Peluang Ditemukan",
                        "Tentukan titik peta secara presisi, gunakan foto kamar yang terang, dan tandai semua fasilitas yang Anda sediakan (WiFi, AC, Parkir, Kamar Mandi).",
                        R.drawable.ic_sparkle, 0xFFF59E0B, R.drawable.bg_circle_icon_amber),
                new TipSlideItem("6. Pastikan Informasi Selalu Akurat",
                        "Hindari perbedaan harga atau fasilitas antara aplikasi dan kondisi lapangan untuk membangun reputasi kost yang terpercaya dan terverifikasi.",
                        R.drawable.ic_check, 0xFFEF4444, R.drawable.bg_circle_icon_peach)
        };

        LinearLayout dotsContainer = view.findViewById(R.id.dots_tips_container);
        View btnPrev = view.findViewById(R.id.btn_tips_prev);
        View btnNext = view.findViewById(R.id.btn_tips_next);
        View btnDismiss = view.findViewById(R.id.btn_dismiss_tips);
        View btnClose = view.findViewById(R.id.btn_close_tips);

        final int[] currentStep = {0};

        if (dotsContainer != null) {
            dotsContainer.removeAllViews();
            int dotSize = (int) (8 * getResources().getDisplayMetrics().density);
            int dotMargin = (int) (4 * getResources().getDisplayMetrics().density);
            for (int i = 0; i < tips.length; i++) {
                View dot = new View(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSize, dotSize);
                params.setMargins(dotMargin, 0, dotMargin, 0);
                dot.setLayoutParams(params);
                dot.setBackgroundResource(i == 0 ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
                final int targetIndex = i;
                dot.setOnClickListener(v -> {
                    currentStep[0] = targetIndex;
                    renderTipSlide(view, tips, currentStep[0]);
                });
                dotsContainer.addView(dot);
            }
        }

        renderTipSlide(view, tips, 0);

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                if (currentStep[0] < tips.length - 1) {
                    currentStep[0]++;
                    renderTipSlide(view, tips, currentStep[0]);
                }
            });
        }

        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                if (currentStep[0] > 0) {
                    currentStep[0]--;
                    renderTipSlide(view, tips, currentStep[0]);
                }
            });
        }

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnDismiss != null) btnDismiss.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void renderTipSlide(View view, TipSlideItem[] tips, int index) {
        if (index < 0 || index >= tips.length) return;
        TipSlideItem item = tips[index];

        TextView tvProgress = view.findViewById(R.id.tv_tips_progress);
        View flIconBg = view.findViewById(R.id.fl_tip_icon_bg);
        ImageView ivIcon = view.findViewById(R.id.iv_tip_icon);
        TextView tvTitle = view.findViewById(R.id.tv_tip_title);
        TextView tvDesc = view.findViewById(R.id.tv_tip_desc);
        LinearLayout dotsContainer = view.findViewById(R.id.dots_tips_container);
        View btnPrev = view.findViewById(R.id.btn_tips_prev);
        View btnNext = view.findViewById(R.id.btn_tips_next);
        View btnDismiss = view.findViewById(R.id.btn_dismiss_tips);

        if (tvProgress != null) tvProgress.setText("Langkah " + (index + 1) + " dari " + tips.length);
        if (flIconBg != null) flIconBg.setBackgroundResource(item.bgRes);
        if (ivIcon != null) {
            ivIcon.setImageResource(item.iconRes);
            ivIcon.setColorFilter(item.iconColor);
        }
        if (tvTitle != null) tvTitle.setText(item.title);
        if (tvDesc != null) tvDesc.setText(item.desc);

        if (dotsContainer != null) {
            for (int i = 0; i < dotsContainer.getChildCount(); i++) {
                View dot = dotsContainer.getChildAt(i);
                dot.setBackgroundResource(i == index ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
            }
        }

        if (btnPrev != null) btnPrev.setVisibility(index > 0 ? View.VISIBLE : View.GONE);
        if (index == tips.length - 1) {
            if (btnNext != null) btnNext.setVisibility(View.GONE);
            if (btnDismiss != null) btnDismiss.setVisibility(View.VISIBLE);
        } else {
            if (btnNext != null) btnNext.setVisibility(View.VISIBLE);
            if (btnDismiss != null) btnDismiss.setVisibility(View.GONE);
        }
    }

    private void showBantuanDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Pusat Bantuan Pemilik")
                .setMessage("Pertanyaan yang sering muncul:\n\n"
                        + "• Kenapa kost saya belum tampil?\nKost baru tampil setelah disetujui tim. Cek status di tab Kost Saya.\n\n"
                        + "• Perlu review ulang setiap edit?\nHanya perubahan nama, alamat, lokasi, foto, deskripsi, atau tipe. Harga dan jumlah kamar kosong langsung tampil.\n\n"
                        + "• Ada kendala lain?\nKirim email ke " + getString(R.string.support_email) + " beserta nama kost kamu.")
                .setPositiveButton("Tutup", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Kirim Email", (dialog, which) -> {
                    Intent email = new Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:" + getString(R.string.support_email)));
                    email.putExtra(Intent.EXTRA_SUBJECT, "Bantuan Pemilik CariKostKita");
                    try {
                        startActivity(email);
                    } catch (Exception e) {
                        Toast.makeText(this, "Tidak ada aplikasi email di perangkat ini", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showPengaturanDialog() {
        boolean[] checked = {sessionManager.isChatNotificationEnabled()};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Pengaturan")
                .setMultiChoiceItems(new String[]{"Tampilkan pemberitahuan pesan baru"}, checked,
                        (dialog, which, isChecked) -> checked[0] = isChecked)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    notificationsEnabled = checked[0];
                    sessionManager.setChatNotificationEnabled(checked[0]);
                    if (switchNotification != null) switchNotification.setChecked(checked[0]);
                    Toast.makeText(this, "Pengaturan disimpan", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showAboutDialog() {
        com.carikostkita.util.AppInfoSheets.showAbout(this);
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

    private boolean dashboardLoadedOnce = false;
    private long dashboardSkeletonStart = 0;

    /** Skeleton konten dashboard hanya pada pemuatan pertama. */
    private void setDashboardSkeleton(boolean show) {
        View skeleton = findViewById(R.id.skeleton_pemilik_dashboard);
        if (skeleton == null) return;
        if (show) {
            dashboardSkeletonStart = com.carikostkita.util.SkeletonHelper.markStart();
            skeleton.setVisibility(View.VISIBLE);
            containerDashboard.setVisibility(View.GONE);
        } else {
            com.carikostkita.util.SkeletonHelper.complete(dashboardSkeletonStart, () -> {
                if (isFinishing() || isDestroyed()) return;
                skeleton.setVisibility(View.GONE);
                if (currentTab == TabPemilik.DASHBOARD) containerDashboard.setVisibility(View.VISIBLE);
            });
        }
    }

    private void loadDashboardStats() {
        String ownerId = sessionManager.getUserUid();
        tvWelcomeName.setText(sessionManager.getUserName());
        if (!dashboardLoadedOnce) setDashboardSkeleton(true);
        loadRecentChats();
        new com.carikostkita.data.repository.SurveyRepository(this).list(false, new DataCallback<List<com.carikostkita.data.model.SurveyRequest>>() {
            @Override
            public void onSuccess(List<com.carikostkita.data.model.SurveyRequest> data) {
                pendingSurveys = 0;
                for (com.carikostkita.data.model.SurveyRequest r : data) {
                    if (com.carikostkita.data.model.SurveyRequest.MENUNGGU.equals(r.status)) pendingSurveys++;
                }
                if (lastOwnerKosts != null) renderActionNeeded(lastOwnerKosts);
            }

            @Override
            public void onError(String message) {}
        });
        loadOwnerPerformance();

        kostRepository.getPemilikStats(ownerId, new DataCallback<KostRepository.PemilikStats>() {
            @Override
            public void onSuccess(KostRepository.PemilikStats stats) {
                if (!dashboardLoadedOnce) {
                    dashboardLoadedOnce = true;
                    setDashboardSkeleton(false);
                }
                TextView tvSummary = findViewById(R.id.tv_pemilik_dashboard_summary);
                if (tvSummary != null) {
                    tvSummary.setText(stats.totalKost == 0
                            ? "Belum ada kost. Pasang kost pertamamu, sekitar 5 menit."
                            : stats.totalAktif + " kost tampil • " + stats.totalTersedia + " kamar kosong");
                }
                TextView pk = findViewById(R.id.tv_pemilik_profile_kost_count);
                TextView pf = findViewById(R.id.tv_pemilik_profile_fav_count);
                if (pk != null) pk.setText(String.valueOf(stats.totalKost));
                if (pf != null) pf.setText(String.valueOf(stats.totalFavorit));
                tvStatTotal.setText(String.valueOf(stats.totalKost));
                tvStatAktif.setText(String.valueOf(stats.totalAktif));
                tvStatPending.setText(String.valueOf(stats.totalPending));
                tvStatRevisi.setText(String.valueOf(stats.totalRevisi));

                int totalKamar = stats.totalTersedia + stats.totalTerisi;
                int percent = totalKamar > 0 ? (stats.totalTerisi * 100) / totalKamar : 0;
                if (tvOccupancyPercent != null) tvOccupancyPercent.setText(percent + "%");
                if (pbOccupancy != null) pbOccupancy.setProgress(percent);
                if (tvOccupancySub != null) {
                    String sub = totalKamar > 0
                            ? stats.totalTerisi + " dari " + totalKamar + " kamar terisi (" + stats.totalTersedia + " kamar kosong)"
                            : "Isi jumlah kamar di Edit Kost untuk melihat tingkat hunian";
                    if (stats.kostTanpaDataKamar > 0 && totalKamar > 0) {
                        sub += ". " + stats.kostTanpaDataKamar + " kost belum mengisi data kamar.";
                    }
                    tvOccupancySub.setText(sub);
                }
                if (tvKamarTerisi != null) tvKamarTerisi.setText(String.valueOf(stats.totalTerisi));
                if (tvKamarTersedia != null) tvKamarTersedia.setText(String.valueOf(stats.totalTersedia));
                if (tvKamarTotal != null) tvKamarTotal.setText(String.valueOf(totalKamar));
            }

            @Override
            public void onError(String message) {
                if (!dashboardLoadedOnce) {
                    dashboardLoadedOnce = true;
                    setDashboardSkeleton(false);
                }
                Toast.makeText(PemilikMainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });

        // Load primary property highlight for dashboard
        kostRepository.getKostByPemilik(ownerId, new DataCallback<List<Kost>>() {
            @Override
            public void onSuccess(List<Kost> list) {
                lastOwnerKosts = list;
                renderActionNeeded(list);
                renderQuickRooms(list);
                if (list != null && !list.isEmpty()) {
                    Kost primary = list.get(0);
                    if (cardHighlight != null) cardHighlight.setVisibility(View.VISIBLE);
                    if (tvHighlightTitle != null) tvHighlightTitle.setText(primary.getNamaKost());
                    if (tvHighlightLocation != null) tvHighlightLocation.setText(primary.getFullLocation());
                    if (tvHighlightPrice != null) tvHighlightPrice.setText(primary.getFormattedHarga());
                    if (tvHighlightRooms != null) {
                        tvHighlightRooms.setText(primary.hasRoomInfo() ? primary.getKamarTersedia() + " Kamar Kosong" : primary.getAvailabilityLabel());
                    }
                } else {
                    if (cardHighlight != null) cardHighlight.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    /** Kartu "Perlu tindakan": revisi dari tim, kost tanpa data kamar, dan pesan belum dibaca. */
    private void renderActionNeeded(List<Kost> list) {
        View card = findViewById(R.id.card_pemilik_action_needed);
        android.widget.LinearLayout container = findViewById(R.id.layout_pemilik_action_items);
        if (card == null || container == null) return;
        container.removeAllViews();
        if (list != null) {
            for (Kost k : list) {
                if (k.getVerificationStatus() == com.carikostkita.data.model.KostVerificationStatus.REVISION_REQUIRED
                        || k.getVerificationStatus() == com.carikostkita.data.model.KostVerificationStatus.REJECTED) {
                    String note = k.getCatatanRevisi() != null && !k.getCatatanRevisi().isEmpty() ? k.getCatatanRevisi() : "Cek catatan dari tim";
                    addActionRow(container, R.drawable.ic_edit, "Perbaiki \"" + k.getNamaKost() + "\"", note, v -> onEditClick(k));
                } else if (!k.hasRoomInfo()) {
                    addActionRow(container, R.drawable.ic_bed, "Isi jumlah kamar \"" + k.getNamaKost() + "\"",
                            "Pencari lebih percaya bila jumlah kamar kosong jelas", v -> onEditClick(k));
                }
            }
        }
        if (pendingSurveys > 0) {
            addActionRow(container, R.drawable.ic_clock, pendingSurveys + " permintaan survei menunggu",
                    "Konfirmasi atau tawarkan waktu lain", v -> startActivity(new Intent(this, com.carikostkita.ui.survey.SurveyListActivity.class)));
        }
        if (pendingUnreadChats > 0) {
            addActionRow(container, R.drawable.ic_nav_chat, pendingUnreadChats + " pesan belum dibalas",
                    "Balas cepat menaikkan peluang kamar terisi", v -> switchTab(TabPemilik.PESAN));
        }
        card.setVisibility(container.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private int pendingUnreadChats = 0;
    private int pendingSurveys = 0;
    private List<Kost> lastOwnerKosts;

    private void addActionRow(android.widget.LinearLayout container, int icon, String title, String sub, View.OnClickListener onClick) {
        View row = getLayoutInflater().inflate(R.layout.item_setting_row, container, false);
        com.carikostkita.util.SettingRowBinder.bind(row, icon, title, sub, onClick);
        if (container.getChildCount() > 0) {
            View divider = new View(this);
            divider.setBackgroundColor(ContextCompat.getColor(this, R.color.border_subtle));
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
            lp.setMarginStart((int) (72 * getResources().getDisplayMetrics().density));
            container.addView(divider, lp);
        }
        container.addView(row);
    }

    /** Stepper −/+ kamar kosong per kost yang sudah tampil; tersimpan langsung tanpa review ulang. */
    private void renderQuickRooms(List<Kost> list) {
        android.widget.LinearLayout container = findViewById(R.id.layout_pemilik_quick_rooms);
        if (container == null) return;
        container.removeAllViews();
        int shown = 0;
        if (list != null) {
            for (Kost k : list) {
                if (shown >= 5) break;
                if (k.getVerificationStatus() != com.carikostkita.data.model.KostVerificationStatus.APPROVED) continue;
                View row = getLayoutInflater().inflate(R.layout.item_quick_room, container, false);
                bindQuickRoom(row, k);
                if (shown > 0) {
                    View divider = new View(this);
                    divider.setBackgroundColor(ContextCompat.getColor(this, R.color.border_subtle));
                    container.addView(divider, new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
                }
                container.addView(row);
                shown++;
            }
        }
        if (shown == 0) {
            TextView empty = new TextView(this);
            empty.setText("Kost yang sudah disetujui akan muncul di sini supaya kamar kosong bisa diperbarui dengan sekali ketuk.");
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            empty.setTextSize(13);
            int pad = (int) (16 * getResources().getDisplayMetrics().density);
            empty.setPadding(pad, pad, pad, pad);
            container.addView(empty);
        }
    }

    private void bindQuickRoom(View row, Kost kost) {
        TextView name = row.findViewById(R.id.tv_quick_room_name);
        TextView sub = row.findViewById(R.id.tv_quick_room_sub);
        TextView value = row.findViewById(R.id.tv_quick_room_value);
        View minus = row.findViewById(R.id.btn_quick_room_minus);
        View plus = row.findViewById(R.id.btn_quick_room_plus);
        name.setText(kost.getNamaKost());
        final int total = kost.getTotalKamar();
        final int[] current = {kost.hasRoomInfo() ? kost.getKamarTersedia() : 0};
        Runnable render = () -> {
            value.setText(String.valueOf(current[0]));
            sub.setText(current[0] == 0 ? "Penuh" : (total > 0 ? "dari " + total + " kamar" : "kamar kosong"));
            minus.setEnabled(current[0] > 0);
            minus.setAlpha(current[0] > 0 ? 1f : 0.4f);
            boolean canAdd = total <= 0 || current[0] < total;
            plus.setEnabled(canAdd);
            plus.setAlpha(canAdd ? 1f : 0.4f);
        };
        render.run();
        View.OnClickListener change = v -> {
            int next = current[0] + (v == plus ? 1 : -1);
            if (next < 0 || (total > 0 && next > total)) return;
            int previous = current[0];
            current[0] = next;
            render.run();
            kostRepository.updateRoomAvailability(kost.getIdKost(), next, new DataCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean ok) {
                    kost.setKamarTersedia(next);
                    kost.setStatus(next > 0 ? StatusKost.TERSEDIA : StatusKost.PENUH);
                }

                @Override
                public void onError(String message) {
                    current[0] = previous;
                    render.run();
                    Toast.makeText(PemilikMainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        };
        minus.setOnClickListener(change);
        plus.setOnClickListener(change);
    }

    /** Tiga percakapan terbaru di dashboard + jumlah pesan belum dibaca untuk "Perlu tindakan". */
    private void loadRecentChats() {
        chatRepository.getConversationsForUser(sessionManager.getUserUid(), new DataCallback<List<ChatConversation>>() {
            @Override
            public void onSuccess(List<ChatConversation> list) {
                android.widget.LinearLayout container = findViewById(R.id.layout_pemilik_recent_chats);
                TextView chatCount = findViewById(R.id.tv_pemilik_profile_chat_count);
                if (chatCount != null) chatCount.setText(String.valueOf(list != null ? list.size() : 0));
                pendingUnreadChats = 0;
                if (list != null) for (ChatConversation c : list) pendingUnreadChats += c.getUnreadCount();
                if (lastOwnerKosts != null) renderActionNeeded(lastOwnerKosts);
                if (container == null) return;
                container.removeAllViews();
                if (list == null || list.isEmpty()) {
                    View row = getLayoutInflater().inflate(R.layout.item_setting_row, container, false);
                    com.carikostkita.util.SettingRowBinder.bind(row, R.drawable.ic_nav_chat, "Belum ada pesan",
                            "Pertanyaan calon penyewa akan muncul di sini", null);
                    row.findViewById(R.id.iv_setting_chevron).setVisibility(View.GONE);
                    container.addView(row);
                    return;
                }
                for (int i = 0; i < Math.min(3, list.size()); i++) {
                    ChatConversation c = list.get(i);
                    String who = c.getNamaPencari() != null ? c.getNamaPencari() : "Calon penyewa";
                    String msg = c.getLastMessage() != null ? c.getLastMessage() : "Belum ada pesan";
                    String title = c.getUnreadCount() > 0 ? who + " • " + c.getUnreadCount() + " baru" : who;
                    View row = getLayoutInflater().inflate(R.layout.item_setting_row, container, false);
                    com.carikostkita.util.SettingRowBinder.bind(row, R.drawable.ic_nav_chat, title,
                            (c.getNamaKost() != null ? c.getNamaKost() + ": " : "") + msg, v -> onConversationClick(c));
                    if (i > 0) {
                        View divider = new View(PemilikMainActivity.this);
                        divider.setBackgroundColor(ContextCompat.getColor(PemilikMainActivity.this, R.color.border_subtle));
                        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
                        lp.setMarginStart((int) (72 * getResources().getDisplayMetrics().density));
                        container.addView(divider, lp);
                    }
                    container.addView(row);
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    /** Performa 30 hari per kost dari fungsi SQL owner_kost_stats. */
    private void loadOwnerPerformance() {
        new com.carikostkita.data.repository.AnalyticsRepository(this).ownerKostStats(new DataCallback<List<com.carikostkita.data.model.OwnerKostStat>>() {
            @Override
            public void onSuccess(List<com.carikostkita.data.model.OwnerKostStat> list) {
                android.widget.LinearLayout container = findViewById(R.id.layout_pemilik_performance);
                if (container == null) return;
                container.removeAllViews();
                if (list.isEmpty()) {
                    View row = getLayoutInflater().inflate(R.layout.item_setting_row, container, false);
                    com.carikostkita.util.SettingRowBinder.bind(row, R.drawable.ic_activity, "Belum ada data",
                            "Data muncul setelah kost disetujui dan dilihat pencari", null);
                    row.findViewById(R.id.iv_setting_chevron).setVisibility(View.GONE);
                    container.addView(row);
                    return;
                }
                for (int i = 0; i < Math.min(5, list.size()); i++) {
                    com.carikostkita.data.model.OwnerKostStat st = list.get(i);
                    View row = getLayoutInflater().inflate(R.layout.item_setting_row, container, false);
                    com.carikostkita.util.SettingRowBinder.bind(row, R.drawable.ic_activity, st.namaKost,
                            st.dilihat + " dilihat • " + st.disimpan + " disimpan • " + st.chat + " chat • " + st.survei + " survei", null);
                    row.findViewById(R.id.iv_setting_chevron).setVisibility(View.GONE);
                    if (i > 0) {
                        View divider = new View(PemilikMainActivity.this);
                        divider.setBackgroundColor(ContextCompat.getColor(PemilikMainActivity.this, R.color.border_subtle));
                        container.addView(divider, new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
                    }
                    container.addView(row);
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void loadOwnerKosts() {
        String ownerId = sessionManager.getUserUid();
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
                    tvPortfolioFavoritCount.setText(stats.totalFavorit + " kali disimpan");
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
            tvPortfolioChartTitle.setText("Jumlah Kamar Terdaftar (" + periodTitle + ")");
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

            // Data historis yang benar-benar ada: kamar dari kost yang sudah terdaftar pada tanggal itu
            int kamarAtPoint = 0;
            for (KostDto k : cachedOwnerKosts) {
                long t = parseIsoDate(k.createdAt);
                if (t > 0 && t <= bucketTime && k.totalKamar != null) {
                    kamarAtPoint += Math.max(0, k.totalKamar);
                }
            }
            points.add(new ModernLineChartView.DataPoint(label, kamarAtPoint));
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
        intent.putExtra("avatar_counterpart", conversation.getAvatarLawan());
        startActivity(intent);
    }

    @Override
    public void onEditClick(Kost kost) {
        Intent intent = new Intent(this, AdminKostFormActivity.class);
        intent.putExtra("kost_id", kost.getIdKost());
                startActivity(intent);
    }

    @Override
    public void onResubmitClick(Kost kost, int position) {
        AppDialogHelper.showConfirm(this, "Ajukan ulang kost?",
                "Pastikan catatan dari tim sudah diperbaiki lewat Edit. \"" + kost.getNamaKost() + "\" akan masuk antrean verifikasi lagi.",
                "Ajukan Ulang", () -> kostRepository.resubmit(kost.getIdKost(), new DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean ok) {
                        Toast.makeText(PemilikMainActivity.this, "Kost diajukan ulang ke tim verifikasi", Toast.LENGTH_SHORT).show();
                        loadOwnerKosts();
                        loadDashboardStats();
                    }

                    @Override
                    public void onError(String message) {
                        AppDialogHelper.showError(PemilikMainActivity.this, "Gagal Mengajukan Ulang", message);
                    }
                }));
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
