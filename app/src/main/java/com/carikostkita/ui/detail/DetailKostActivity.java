package com.carikostkita.ui.detail;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.viewpager2.widget.ViewPager2;
import com.carikostkita.R;
import com.carikostkita.data.model.Fasilitas;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.KostReport;
import com.carikostkita.data.model.StatusKost;
import com.carikostkita.data.model.TipeKost;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.data.repository.ReportRepository;
import com.carikostkita.ui.adapter.FotoSliderAdapter;
import com.carikostkita.ui.main.chat.ChatRoomActivity;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.IntentHelper;
import com.carikostkita.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Locale;

public class DetailKostActivity extends AppCompatActivity {

    private ViewPager2 vpPhotos;
    private TextView tvPhotoIndicator;
    private TextView tvTipe;
    private TextView tvStatus;
    private TextView tvName;
    private TextView tvPrice;
    private TextView tvAddress;
    private LinearLayout layoutPatokan;
    private TextView tvPatokan;
    private TextView tvCoordinates;
    private Button btnDetailOpenMap;
    private TextView tvDesc;
    private TextView tvDetailUkuranKamar;
    private TextView tvDetailKamarTersedia;
    private TextView tvDetailKamarTerisi;
    private ViewGroup layoutFacilities;
    private ImageButton btnFavorite;
    private Button btnActionChat;
    private Button btnActionWhatsApp;
    private MaterialButton btnDetailReport;
    private View layoutSkeleton;
    private View scrollContent;

    private KostRepository kostRepository;
    private ReportRepository reportRepository;
    private SessionManager sessionManager;
    private Kost currentKost;
    private org.osmdroid.views.MapView miniMap;
    private String idKost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_kost);

        kostRepository = new KostRepository(this);
        reportRepository = new ReportRepository(this);
        sessionManager = new SessionManager(this);

        String extraStr = getIntent().getStringExtra("kost_id");
        if (extraStr != null && !extraStr.isEmpty()) {
            idKost = extraStr;
        } else {
            int legacy = getIntent().getIntExtra("kost_id", -1);
            if (legacy != -1) idKost = String.valueOf(legacy);
        }

        if (idKost == null || idKost.isEmpty() || "-1".equals(idKost)) {
            Toast.makeText(this, "Data kost tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadKostDetail();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_detail_back);
        ImageButton btnShare = findViewById(R.id.btn_detail_share);
        btnFavorite = findViewById(R.id.btn_detail_favorite);

        vpPhotos = findViewById(R.id.vp_detail_photos);
        tvPhotoIndicator = findViewById(R.id.tv_photo_indicator);
        tvTipe = findViewById(R.id.tv_detail_tipe);
        tvStatus = findViewById(R.id.tv_detail_status);
        tvName = findViewById(R.id.tv_detail_name);
        tvPrice = findViewById(R.id.tv_detail_price);
        tvDetailUkuranKamar = findViewById(R.id.tv_detail_ukuran_kamar);
        tvDetailKamarTersedia = findViewById(R.id.tv_detail_kamar_tersedia);
        tvDetailKamarTerisi = findViewById(R.id.tv_detail_kamar_terisi);
        tvAddress = findViewById(R.id.tv_detail_address);
        layoutPatokan = findViewById(R.id.layout_detail_patokan);
        tvPatokan = findViewById(R.id.tv_detail_patokan);
        tvCoordinates = findViewById(R.id.tv_detail_coordinates);
        btnDetailOpenMap = findViewById(R.id.btn_detail_open_map);
        tvDesc = findViewById(R.id.tv_detail_desc);
        layoutFacilities = findViewById(R.id.layout_detail_facilities);
        btnActionChat = findViewById(R.id.btn_action_chat);
        btnActionWhatsApp = findViewById(R.id.btn_action_whatsapp);
        btnDetailReport = findViewById(R.id.btn_detail_report);
        layoutSkeleton = findViewById(R.id.skeleton_detail_kost);
        scrollContent = findViewById(R.id.scroll_detail_content);

        if (btnDetailReport != null) {
            btnDetailReport.setOnClickListener(v -> showReportKostDialog());
        }

        btnBack.setOnClickListener(v -> finish());

        btnShare.setOnClickListener(v -> {
            if (currentKost != null) {
                IntentHelper.shareKost(DetailKostActivity.this, currentKost);
            }
        });

        btnFavorite.setOnClickListener(v -> {
            if (!com.carikostkita.util.AuthPrompt.require(DetailKostActivity.this,
                    "Masuk untuk menyimpan kost favorit dan membukanya lagi kapan saja.")) {
                return;
            }
            if (currentKost != null) {
                // Heart bounce micro-animation: 0.8 -> 1.15 -> 1.0 (200ms)
                btnFavorite.setScaleX(0.8f);
                btnFavorite.setScaleY(0.8f);
                btnFavorite.animate()
                        .scaleX(1.15f)
                        .scaleY(1.15f)
                        .setDuration(100)
                        .setInterpolator(new FastOutSlowInInterpolator())
                        .withEndAction(() -> btnFavorite.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .setInterpolator(new FastOutSlowInInterpolator())
                                .start())
                        .start();

                kostRepository.toggleFavorite(sessionManager.getUserUid(), currentKost.getId(), new DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean isFavorite) {
                        currentKost.setFavorite(isFavorite);
                        updateFavoriteIcon(isFavorite);
                        if (isFavorite) new com.carikostkita.data.repository.AnalyticsRepository(DetailKostActivity.this)
                                .log(com.carikostkita.data.repository.AnalyticsRepository.FAVORITE_ADD, currentKost.getId());
                        String msg = isFavorite ? "Ditambahkan ke Favorit" : "Dihapus dari Favorit";
                        Toast.makeText(DetailKostActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(DetailKostActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnDetailOpenMap.setOnClickListener(v -> {
            if (currentKost != null) {
                IntentHelper.openGoogleMaps(
                        DetailKostActivity.this,
                        currentKost.getLatitude(),
                        currentKost.getLongitude(),
                        currentKost.getNamaKost()
                );
            }
        });

        btnActionChat.setOnClickListener(v -> {
            if (currentKost == null) return;
            if (!com.carikostkita.util.AuthPrompt.require(DetailKostActivity.this,
                    "Masuk untuk bertanya langsung ke pemilik kost lewat chat.")) {
                return;
            }
            Intent intent = new Intent(DetailKostActivity.this, ChatRoomActivity.class);
            intent.putExtra("kost_id", currentKost.getId());
            intent.putExtra("id_pemilik", currentKost.getOwnerId());
            intent.putExtra("nama_kost", currentKost.getNamaKost());
            intent.putExtra("foto_kost", currentKost.getFotoUtama());
            intent.putExtra("harga_kost", currentKost.getHarga());
            String lokasi = currentKost.getKecamatan() != null && !currentKost.getKecamatan().isEmpty()
                    ? (currentKost.getKecamatan() + ", " + currentKost.getKota())
                    : currentKost.getKota();
            intent.putExtra("lokasi_kost", lokasi);
            intent.putExtra("status_kost", currentKost.getStatus() != null ? currentKost.getStatus().name() : "TERSEDIA");
            startActivity(intent);
        });

        btnActionWhatsApp.setOnClickListener(v -> {
            if (currentKost != null) {
                String template = "Halo pemilik " + currentKost.getNamaKost() +
                        ", saya melihat informasi kost Anda di aplikasi CariKostKita. Apakah kamar masih tersedia?";
                IntentHelper.openWhatsApp(
                        DetailKostActivity.this,
                        currentKost.getNoWhatsapp(),
                        template
                );
            }
        });
    }

    private void showLoadError(String message) {
        View errorLayout = findViewById(R.id.layout_detail_error);
        View bottomBar = findViewById(R.id.bottom_action_bar);
        if (scrollContent != null) scrollContent.setVisibility(View.GONE);
        if (bottomBar != null) bottomBar.setVisibility(View.GONE);
        if (errorLayout == null) return;
        errorLayout.setVisibility(View.VISIBLE);
        boolean offline = com.carikostkita.util.ErrorMessages.isOffline(message);
        TextView title = findViewById(R.id.tv_detail_error_title);
        TextView desc = findViewById(R.id.tv_detail_error_desc);
        android.widget.ImageView icon = findViewById(R.id.iv_detail_error_icon);
        if (title != null) title.setText(offline ? "Kamu Sedang Offline" : "Kost Tidak Dapat Dibuka");
        if (desc != null) desc.setText(message);
        if (icon != null) icon.setImageResource(offline ? R.drawable.ic_error_circle : R.drawable.ic_warning);
        View retry = findViewById(R.id.btn_detail_retry);
        if (retry != null) {
            retry.setOnClickListener(v -> {
                errorLayout.setVisibility(View.GONE);
                if (bottomBar != null) bottomBar.setVisibility(View.VISIBLE);
                loadKostDetail();
            });
        }
    }

    private void loadKostDetail() {
        long startTime = com.carikostkita.util.SkeletonHelper.markStart();
        if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.VISIBLE);
        if (scrollContent != null) scrollContent.setVisibility(View.GONE);

        String userId = sessionManager.getUserUid();
        kostRepository.getKostDetail(idKost, userId, new DataCallback<Kost>() {
            @Override
            public void onSuccess(Kost kost) {
                com.carikostkita.util.SkeletonHelper.complete(startTime, () -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.GONE);
                    if (scrollContent != null) scrollContent.setVisibility(View.VISIBLE);
                    currentKost = kost;
                    renderKostData(kost);
                });
            }

            @Override
            public void onError(String message) {
                com.carikostkita.util.SkeletonHelper.complete(startTime, () -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.GONE);
                    showLoadError(message);
                });
            }
        });
    }

    private void renderKostData(Kost kost) {
        tvName.setText(kost.getNamaKost());
        tvPrice.setText(kost.getFormattedHarga());
        String address = kost.getFullAddress();
        tvAddress.setText(address.isEmpty() ? "Alamat belum diisi pemilik" : address);

        // Ringkasan area + jarak dari lokasi yang dipilih pengguna
        TextView tvAreaSummary = findViewById(R.id.tv_detail_area_summary);
        if (tvAreaSummary != null) {
            String area = kost.getFullLocation();
            double myLat = sessionManager.getUserSelectedLat();
            double myLng = sessionManager.getUserSelectedLng();
            if (myLat != 0 && myLng != 0 && kost.hasCoordinates()) {
                area = com.carikostkita.util.GeoUtil.formatDistance(com.carikostkita.util.GeoUtil.distanceKm(
                        myLat, myLng, kost.getLatitude(), kost.getLongitude())) + " dari lokasimu • " + area;
            }
            tvAreaSummary.setText(area);
            tvAreaSummary.setVisibility(area == null || area.isEmpty() ? View.GONE : View.VISIBLE);
        }

        if (kost.getPatokan() != null && !kost.getPatokan().trim().isEmpty()) {
            layoutPatokan.setVisibility(View.VISIBLE);
            tvPatokan.setText("Patokan: " + kost.getPatokan().trim());
        } else {
            layoutPatokan.setVisibility(View.GONE);
        }

        setupMiniMap(kost);

        tvDesc.setText(kost.getDeskripsi() != null && !kost.getDeskripsi().isEmpty() ? kost.getDeskripsi() : "Tidak ada deskripsi tambahan.");

        // Room specs
        // Tampilkan hanya data yang diisi pemilik, tanpa angka karangan
        tvDetailUkuranKamar.setText(kost.getUkuranKamar() != null ? kost.getUkuranKamar() : "Belum diisi");
        tvDetailKamarTersedia.setText(kost.hasRoomInfo() ? kost.getKamarTersedia() + " Kamar" : "Tanya pemilik");
        if (kost.hasRoomInfo() && kost.getTotalKamar() > 0) {
            tvDetailKamarTerisi.setText(kost.getKamarTerisi() + " / " + kost.getTotalKamar() + " Kamar");
        } else {
            tvDetailKamarTerisi.setText("–");
        }

        // Tipe Kost Badge
        if (kost.getTipeKost() == TipeKost.PUTRI) {
            tvTipe.setText("PUTRI");
            tvTipe.setBackgroundResource(R.drawable.bg_badge_putri);
            tvTipe.setTextColor(ContextCompat.getColor(this, R.color.badge_putri));
        } else if (kost.getTipeKost() == TipeKost.PUTRA) {
            tvTipe.setText("PUTRA");
            tvTipe.setBackgroundResource(R.drawable.bg_badge_putra);
            tvTipe.setTextColor(ContextCompat.getColor(this, R.color.badge_putra));
        } else {
            tvTipe.setText("CAMPUR");
            tvTipe.setBackgroundResource(R.drawable.bg_badge_campur);
            tvTipe.setTextColor(ContextCompat.getColor(this, R.color.badge_campur));
        }

        // Status Badge
        if (kost.isAvailable()) {
            tvStatus.setText("Tersedia");
            tvStatus.setBackgroundResource(R.drawable.bg_badge_tersedia);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_tersedia));
        } else {
            tvStatus.setText(kost.getStatus() == StatusKost.TIDAK_AKTIF ? "Nonaktif" : "Penuh");
            tvStatus.setBackgroundResource(R.drawable.bg_badge_penuh);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_penuh));
        }

        updateFavoriteIcon(kost.isFavorite());

        boolean hasWhatsapp = kost.getNoWhatsapp() != null && !kost.getNoWhatsapp().trim().isEmpty();
        if (btnActionWhatsApp != null) btnActionWhatsApp.setVisibility(hasWhatsapp ? View.VISIBLE : View.GONE);
        loadOwnerCard(kost.getOwnerId());
        renderBiayaAturan(kost);
        renderRating(kost);
        setupSurvey(kost);
        loadReviews(kost);
        new com.carikostkita.data.repository.AnalyticsRepository(this).log(
                com.carikostkita.data.repository.AnalyticsRepository.VIEW_DETAIL, kost.getId());

        // Setup Photo Slider
        FotoSliderAdapter sliderAdapter = new FotoSliderAdapter();
        vpPhotos.setAdapter(sliderAdapter);
        sliderAdapter.submitList(kost.getListFoto());

        int totalPhotos = (kost.getListFoto() != null) ? kost.getListFoto().size() : 0;
        if (totalPhotos <= 1) {
            tvPhotoIndicator.setVisibility(View.GONE);
        } else {
            tvPhotoIndicator.setVisibility(View.VISIBLE);
            tvPhotoIndicator.setText("1/" + totalPhotos + " Foto");
            vpPhotos.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    tvPhotoIndicator.setText((position + 1) + "/" + totalPhotos + " Foto");
                }
            });
        }

        // Setup Facilities List
        layoutFacilities.removeAllViews();
        if (kost.getListFasilitas() != null && !kost.getListFasilitas().isEmpty()) {
            LayoutInflater inflater = LayoutInflater.from(this);
            for (Fasilitas f : kost.getListFasilitas()) {
                View chip = inflater.inflate(R.layout.item_fasilitas_chip, layoutFacilities, false);
                TextView tv = chip.findViewById(R.id.tv_fasilitas_name);
                tv.setText(f.getNamaFasilitas());
                layoutFacilities.addView(chip);
            }
        } else {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Tidak ada rincian fasilitas tercatat.");
            tvEmpty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            layoutFacilities.addView(tvEmpty);
        }

        // Role-Based Contextual Adaptations:
        if (sessionManager.isDeveloper()) {
            // Mode Moderasi Developer: sembunyikan aksi pencari
            if (btnFavorite != null) btnFavorite.setVisibility(View.GONE);
            if (btnActionChat != null) btnActionChat.setVisibility(View.GONE);
            if (btnDetailReport != null) btnDetailReport.setVisibility(View.GONE);
        } else if (sessionManager.isPemilikKost() && (kost.getOwnerId() != null && kost.getOwnerId().equals(sessionManager.getUserUid()))) {
            // Mode Pratinjau Pemilik: pemilik meninjau properti kost miliknya sendiri
            if (btnFavorite != null) btnFavorite.setVisibility(View.GONE);
            if (btnActionChat != null) btnActionChat.setVisibility(View.GONE);
            if (btnDetailReport != null) btnDetailReport.setVisibility(View.GONE);
        }
    }

    private void updateFavoriteIcon(boolean isFavorite) {
        if (btnFavorite != null) {
            btnFavorite.setImageResource(isFavorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        }
    }

    private void showReportKostDialog() {
        if (currentKost == null) return;
        if (!com.carikostkita.util.AuthPrompt.require(this, "Masuk untuk melaporkan data kost yang tidak sesuai.")) {
            return;
        }

        String[] categories = new String[]{
                "Alamat tidak sesuai",
                "Foto tidak sesuai",
                "Harga tidak sesuai",
                "Kost penuh / tidak tersedia",
                "Informasi fasilitas salah",
                "Masalah dengan pemilik",
                "Lainnya"
        };
        final int[] selectedCategoryIndex = {0};

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 20, 48, 16);

        TextView tvCategoryPrompt = new TextView(this);
        tvCategoryPrompt.setText("Pilih Alasan Pelaporan:");
        tvCategoryPrompt.setTextSize(13);
        tvCategoryPrompt.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tvCategoryPrompt.setTypeface(null, android.graphics.Typeface.BOLD);
        container.addView(tvCategoryPrompt);

        android.widget.Spinner spCategory = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> spinnerAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, categories);
        spCategory.setAdapter(spinnerAdapter);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        spLp.topMargin = 12;
        spCategory.setLayoutParams(spLp);
        container.addView(spCategory);

        TextInputLayout tilDeskripsi = new TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        tilDeskripsi.setHint("Jelaskan masalah secara detail (opsional)");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 20;
        tilDeskripsi.setLayoutParams(lp);

        TextInputEditText etDeskripsi = new TextInputEditText(this);
        etDeskripsi.setLines(3);
        tilDeskripsi.addView(etDeskripsi);
        container.addView(tilDeskripsi);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Laporkan Kost Bermasalah")
                .setView(container)
                .setPositiveButton("Kirim Laporan", (dialog, which) -> {
                    String selectedCategory = categories[spCategory.getSelectedItemPosition()];
                    String deskripsi = etDeskripsi.getText() != null ? etDeskripsi.getText().toString().trim() : "";

                    KostReport report = new KostReport(
                            currentKost.getId(),
                            sessionManager.getUserUid(),
                            currentKost.getOwnerId(),
                            selectedCategory,
                            deskripsi.isEmpty() ? "Laporan data tidak sesuai kategori " + selectedCategory : deskripsi
                    );

                    reportRepository.submitReport(report, sessionManager.getUserName(), new DataCallback<Long>() {
                        @Override
                        public void onSuccess(Long reportId) {
                            AppDialogHelper.showSuccessDialog(DetailKostActivity.this,
                                    "Laporan Terkirim",
                                    "Terima kasih atas laporan Anda. Tim Developer/Admin CariKostKita akan segera memverifikasi dan menindaklanjuti informasi properti ini.");
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(DetailKostActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    /** Peta mini non-interaktif; ketuk untuk membuka rute di aplikasi peta. */
    private void setupMiniMap(Kost kost) {
        View frame = findViewById(R.id.frame_detail_mini_map);
        if (!kost.hasCoordinates()) {
            if (frame != null) frame.setVisibility(View.GONE);
            tvCoordinates.setText("Titik peta belum diatur pemilik. Gunakan alamat di atas atau tanyakan lewat chat.");
            btnDetailOpenMap.setVisibility(View.GONE);
            return;
        }
        tvCoordinates.setText("Ketuk peta untuk melihat rute");
        btnDetailOpenMap.setVisibility(View.VISIBLE);
        if (frame == null) return;
        frame.setVisibility(View.VISIBLE);
        miniMap = findViewById(R.id.map_detail_mini);
        if (miniMap == null) return;
        miniMap.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        miniMap.setMultiTouchControls(false);
        miniMap.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);
        org.osmdroid.util.GeoPoint point = new org.osmdroid.util.GeoPoint(kost.getLatitude(), kost.getLongitude());
        miniMap.getController().setZoom(16.0);
        miniMap.getController().setCenter(point);
        miniMap.getOverlays().clear();
        org.osmdroid.views.overlay.Marker marker = new org.osmdroid.views.overlay.Marker(miniMap);
        marker.setPosition(point);
        marker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_map_pin_primary));
        marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
        marker.setInfoWindow(null);
        miniMap.getOverlays().add(marker);
        View touch = findViewById(R.id.view_detail_map_touch);
        if (touch != null) touch.setOnClickListener(v -> btnDetailOpenMap.performClick());
    }

    /** Kartu pemilik: nama, foto, status terverifikasi, dan lama bergabung. */
    private void loadOwnerCard(String ownerId) {
        View card = findViewById(R.id.card_detail_owner);
        if (card == null || ownerId == null || ownerId.isEmpty()) return;
        new com.carikostkita.data.repository.UserRepository(this).getUserById(ownerId,
                new DataCallback<com.carikostkita.data.model.User>() {
                    @Override
                    public void onSuccess(com.carikostkita.data.model.User owner) {
                        if (isFinishing() || isDestroyed()) return;
                        card.setVisibility(View.VISIBLE);
                        TextView name = findViewById(R.id.tv_detail_owner_name);
                        TextView badge = findViewById(R.id.tv_detail_owner_badge);
                        TextView since = findViewById(R.id.tv_detail_owner_since);
                        android.widget.ImageView avatar = findViewById(R.id.iv_detail_owner_avatar);
                        if (name != null) name.setText(owner.getNama() != null ? owner.getNama() : "Pemilik Kost");
                        if (avatar != null) com.carikostkita.util.UserAvatarHelper.loadAvatar(avatar, owner.getAvatarUrl());
                        if (badge != null) {
                            if (owner.getRole() == com.carikostkita.data.model.Role.PEMILIK_KOST) {
                                badge.setText("Pemilik terverifikasi");
                            } else if (owner.getRole() != null && owner.getRole().isDeveloper()) {
                                badge.setText("Dikelola tim CariKostKita");
                            } else {
                                badge.setVisibility(View.GONE);
                            }
                        }
                        String joined = formatJoined(owner.getCreatedAt());
                        if (since != null && joined != null) {
                            since.setText("Bergabung sejak " + joined);
                            since.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        card.setVisibility(View.GONE);
                    }
                });
    }

    private static String formatJoined(String isoDate) {
        if (isoDate == null || isoDate.length() < 7) return null;
        try {
            java.util.Date d = new java.text.SimpleDateFormat("yyyy-MM", Locale.US).parse(isoDate.substring(0, 7));
            return d == null ? null : new java.text.SimpleDateFormat("MMMM yyyy", new Locale("in", "ID")).format(d);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (miniMap != null) miniMap.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (miniMap != null) miniMap.onPause();
    }

    // ===== Biaya, rating, survei, ulasan =====

    private void renderBiayaAturan(Kost kost) {
        View section = findViewById(R.id.layout_detail_biaya);
        TextView tv = findViewById(R.id.tv_detail_biaya);
        com.google.android.material.chip.ChipGroup cg = findViewById(R.id.cg_detail_aturan);
        if (section == null || tv == null || cg == null) return;
        StringBuilder sb = new StringBuilder();
        if (kost.getDeposit() != null) sb.append("Deposit: ").append(com.carikostkita.util.FormatUtil.formatRupiah(kost.getDeposit())).append("\n");
        if (kost.getMinimalSewaBulan() != null) sb.append("Minimal sewa: ").append(kost.getMinimalSewaBulan()).append(" bulan\n");
        if (kost.getBiayaTambahan() != null) sb.append("Biaya lain: ").append(kost.getBiayaTambahan());
        String text = sb.toString().trim();
        tv.setText(text);
        tv.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
        cg.removeAllViews();
        for (String rule : kost.getAturan()) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(rule);
            chip.setClickable(false);
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_soft)));
            chip.setTextColor(ContextCompat.getColor(this, R.color.primary));
            cg.addView(chip);
        }
        section.setVisibility(text.isEmpty() && kost.getAturan().isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void renderRating(Kost kost) {
        TextView tv = findViewById(R.id.tv_detail_rating);
        if (tv == null) return;
        String label = kost.getRatingLabel();
        tv.setVisibility(label == null ? View.GONE : View.VISIBLE);
        if (label != null) tv.setText(label + " ulasan");
    }

    /** Kartu survei: status pengajuan saya, atau tombol untuk mengajukan jadwal baru. */
    private void setupSurvey(Kost kost) {
        View card = findViewById(R.id.card_detail_survey);
        com.google.android.material.button.MaterialButton btn = findViewById(R.id.btn_detail_survey);
        TextView title = findViewById(R.id.tv_detail_survey_title);
        TextView sub = findViewById(R.id.tv_detail_survey_sub);
        if (card == null || btn == null) return;
        boolean ownListing = kost.getOwnerId() != null && kost.getOwnerId().equals(sessionManager.getUserUid());
        if (sessionManager.isDeveloper() || ownListing || kost.getVerificationStatus() != com.carikostkita.data.model.KostVerificationStatus.APPROVED) {
            card.setVisibility(View.GONE);
            btn.setVisibility(View.GONE);
            return;
        }
        btn.setOnClickListener(v -> {
            if (!com.carikostkita.util.AuthPrompt.require(this, "Masuk untuk mengajukan jadwal survei ke pemilik.")) return;
            pickSurveySchedule(kost);
        });
        if (!sessionManager.isLoggedIn()) return;
        new com.carikostkita.data.repository.SurveyRepository(this).findMineForKost(kost.getId(),
                new DataCallback<com.carikostkita.data.model.SurveyRequest>() {
                    @Override
                    public void onSuccess(com.carikostkita.data.model.SurveyRequest s) {
                        if (s == null || isFinishing()) return;
                        if (s.isActive()) {
                            if (title != null) title.setText("Survei " + s.getStatusLabel().toLowerCase());
                            if (sub != null) sub.setText(formatJadwal(s.jadwal));
                            btn.setText("Lihat jadwal survei saya");
                            btn.setOnClickListener(v -> startActivity(new android.content.Intent(DetailKostActivity.this,
                                    com.carikostkita.ui.survey.SurveyListActivity.class)));
                        } else if (com.carikostkita.data.model.SurveyRequest.SELESAI.equals(s.status)) {
                            if (title != null) title.setText("Kamu sudah survei kost ini");
                            if (sub != null) sub.setText("Bagikan ulasan untuk membantu pencari lain.");
                        }
                    }

                    @Override
                    public void onError(String message) {}
                });
    }

    private void pickSurveySchedule(Kost kost) {
        long tomorrow = System.currentTimeMillis() + 86400000L;
        com.google.android.material.datepicker.CalendarConstraints constraints = new com.google.android.material.datepicker.CalendarConstraints.Builder()
                .setValidator(com.google.android.material.datepicker.DateValidatorPointForward.from(tomorrow - 86400000L))
                .build();
        com.google.android.material.datepicker.MaterialDatePicker<Long> datePicker =
                com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Pilih tanggal survei")
                        .setSelection(tomorrow)
                        .setCalendarConstraints(constraints)
                        .build();
        datePicker.addOnPositiveButtonClickListener(dateUtc -> {
            com.google.android.material.timepicker.MaterialTimePicker timePicker = new com.google.android.material.timepicker.MaterialTimePicker.Builder()
                    .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
                    .setHour(10).setMinute(0)
                    .setTitleText("Jam survei")
                    .build();
            timePicker.addOnPositiveButtonClickListener(v -> {
                java.util.Calendar utc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
                utc.setTimeInMillis(dateUtc);
                java.util.Calendar local = java.util.Calendar.getInstance();
                local.set(utc.get(java.util.Calendar.YEAR), utc.get(java.util.Calendar.MONTH), utc.get(java.util.Calendar.DAY_OF_MONTH),
                        timePicker.getHour(), timePicker.getMinute(), 0);
                if (local.getTimeInMillis() <= System.currentTimeMillis()) {
                    Toast.makeText(this, "Pilih waktu yang belum lewat", Toast.LENGTH_SHORT).show();
                    return;
                }
                String iso = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(local.getTime());
                AppDialogHelper.showInput(this, "Catatan untuk pemilik",
                        "Opsional. Jadwal: " + formatJadwal(iso), "Contoh: saya datang berdua", "", "Ajukan Survei",
                        note -> new com.carikostkita.data.repository.SurveyRepository(this).request(kost.getId(), kost.getOwnerId(), iso, note,
                                new DataCallback<com.carikostkita.data.model.SurveyRequest>() {
                                    @Override
                                    public void onSuccess(com.carikostkita.data.model.SurveyRequest r) {
                                        AppDialogHelper.showSuccessDialog(DetailKostActivity.this, "Survei Diajukan",
                                                "Pemilik akan mengonfirmasi jadwalmu. Kamu akan mendapat notifikasi saat statusnya berubah.");
                                        setupSurvey(kost);
                                    }

                                    @Override
                                    public void onError(String message) {
                                        AppDialogHelper.showError(DetailKostActivity.this, "Gagal Mengajukan Survei", message);
                                    }
                                }));
            });
            timePicker.show(getSupportFragmentManager(), "survey_time");
        });
        datePicker.show(getSupportFragmentManager(), "survey_date");
    }

    static String formatJadwal(String iso) {
        if (iso == null || iso.length() < 16) return "";
        try {
            java.util.Date d = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(iso.length() > 25 ? iso.substring(0, 19) + "Z" : iso);
            if (d == null) return "";
            return new java.text.SimpleDateFormat("EEEE, d MMM yyyy • HH:mm", new Locale("in", "ID")).format(d);
        } catch (Exception e) {
            return iso.substring(0, 16).replace('T', ' ');
        }
    }

    private void loadReviews(Kost kost) {
        android.widget.LinearLayout container = findViewById(R.id.layout_detail_reviews);
        TextView btnWrite = findViewById(R.id.btn_detail_write_review);
        TextView title = findViewById(R.id.tv_detail_reviews_title);
        if (container == null) return;
        com.carikostkita.data.repository.ReviewRepository repo = new com.carikostkita.data.repository.ReviewRepository(this);
        repo.list(kost.getId(), new DataCallback<java.util.List<com.carikostkita.data.model.Review>>() {
            @Override
            public void onSuccess(java.util.List<com.carikostkita.data.model.Review> list) {
                if (isFinishing()) return;
                container.removeAllViews();
                if (title != null && !list.isEmpty()) title.setText("Ulasan Penghuni (" + list.size() + ")");
                if (list.isEmpty()) {
                    TextView empty = new TextView(DetailKostActivity.this);
                    empty.setText("Belum ada ulasan. Ulasan hanya bisa ditulis oleh pencari yang sudah survei, jadi lebih bisa dipercaya.");
                    empty.setTextColor(ContextCompat.getColor(DetailKostActivity.this, R.color.text_secondary));
                    empty.setTextSize(13);
                    container.addView(empty);
                }
                for (int i = 0; i < Math.min(5, list.size()); i++) {
                    com.carikostkita.data.model.Review r = list.get(i);
                    View row = getLayoutInflater().inflate(R.layout.item_review, container, false);
                    ((TextView) row.findViewById(R.id.tv_review_name)).setText(r.namaUser != null ? r.namaUser : "Penghuni");
                    StringBuilder stars = new StringBuilder();
                    for (int k = 0; k < 5; k++) stars.append(k < r.rating ? "★" : "☆");
                    ((TextView) row.findViewById(R.id.tv_review_stars)).setText(stars.toString());
                    TextView body = row.findViewById(R.id.tv_review_body);
                    body.setText(r.komentar != null ? r.komentar : "");
                    body.setVisibility(r.komentar == null || r.komentar.isEmpty() ? View.GONE : View.VISIBLE);
                    container.addView(row);
                }
            }

            @Override
            public void onError(String message) {}
        });
        if (btnWrite == null || !sessionManager.isLoggedIn()) return;
        repo.canReview(kost.getId(), new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean can) {
                btnWrite.setVisibility(Boolean.TRUE.equals(can) ? View.VISIBLE : View.GONE);
                btnWrite.setOnClickListener(v -> showReviewDialog(kost));
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void showReviewDialog(Kost kost) {
        View view = getLayoutInflater().inflate(R.layout.dialog_write_review, null);
        android.widget.RatingBar bar = view.findViewById(R.id.rb_review);
        com.google.android.material.textfield.TextInputEditText et = view.findViewById(R.id.et_review);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Ulasan untuk " + kost.getNamaKost())
                .setView(view)
                .setNegativeButton("Batal", null)
                .setPositiveButton("Kirim", (d, w) -> {
                    int rating = Math.round(bar.getRating());
                    if (rating < 1) {
                        Toast.makeText(this, "Pilih jumlah bintang", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String text = et.getText() != null ? et.getText().toString().trim() : "";
                    new com.carikostkita.data.repository.ReviewRepository(this).submit(kost.getId(), rating, text, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean ok) {
                            Toast.makeText(DetailKostActivity.this, "Terima kasih atas ulasanmu", Toast.LENGTH_SHORT).show();
                            loadReviews(kost);
                        }

                        @Override
                        public void onError(String message) {
                            AppDialogHelper.showError(DetailKostActivity.this, "Ulasan Gagal Dikirim", message);
                        }
                    });
                })
                .show();
    }
}
