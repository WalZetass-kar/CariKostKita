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

    private KostRepository kostRepository;
    private ReportRepository reportRepository;
    private SessionManager sessionManager;
    private Kost currentKost;
    private int idKost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_kost);

        kostRepository = new KostRepository(this);
        reportRepository = new ReportRepository(this);
        sessionManager = new SessionManager(this);

        idKost = getIntent().getIntExtra("kost_id", -1);
        if (idKost == -1) {
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
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(DetailKostActivity.this, "Silakan login terlebih dahulu untuk menyimpan favorit", Toast.LENGTH_SHORT).show();
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

                kostRepository.toggleFavorite(sessionManager.getUserId(), currentKost.getIdKost(), new DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean isFavorite) {
                        currentKost.setFavorite(isFavorite);
                        updateFavoriteIcon(isFavorite);
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
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(DetailKostActivity.this, "Silakan login terlebih dahulu untuk menghubungi pemilik melalui chat", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(DetailKostActivity.this, ChatRoomActivity.class);
            intent.putExtra("kost_id", currentKost.getIdKost());
            intent.putExtra("id_pemilik", currentKost.getIdPemilik());
            intent.putExtra("nama_kost", currentKost.getNamaKost());
            intent.putExtra("foto_kost", currentKost.getFotoUtama());
            intent.putExtra("harga_kost", currentKost.getHarga());
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

    private void loadKostDetail() {
        int userId = sessionManager.getUserId();
        kostRepository.getKostDetail(idKost, userId, new DataCallback<Kost>() {
            @Override
            public void onSuccess(Kost kost) {
                currentKost = kost;
                renderKostData(kost);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(DetailKostActivity.this, message, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void renderKostData(Kost kost) {
        tvName.setText(kost.getNamaKost());
        tvPrice.setText(kost.getFormattedHarga());
        tvAddress.setText(kost.getAlamat() + "\nKel. " + kost.getKelurahan() + ", Kec. " + kost.getKecamatan() + ", Pekanbaru");

        if (kost.getPatokan() != null && !kost.getPatokan().trim().isEmpty()) {
            layoutPatokan.setVisibility(View.VISIBLE);
            tvPatokan.setText("Patokan: " + kost.getPatokan().trim());
        } else {
            layoutPatokan.setVisibility(View.GONE);
        }

        double lat = kost.getLatitude() != 0 ? kost.getLatitude() : 0.463280;
        double lng = kost.getLongitude() != 0 ? kost.getLongitude() : 101.450123;
        tvCoordinates.setText(String.format(Locale.US, "Koordinat: %.6f, %.6f", lat, lng));

        tvDesc.setText(kost.getDeskripsi() != null && !kost.getDeskripsi().isEmpty() ? kost.getDeskripsi() : "Tidak ada deskripsi tambahan.");

        // Room specs
        String ukuran = kost.getUkuranKamar() != null && !kost.getUkuranKamar().isEmpty() ? kost.getUkuranKamar() : "3x4 m";
        tvDetailUkuranKamar.setText(ukuran);
        tvDetailKamarTersedia.setText(kost.getKamarTersedia() + " Kamar");
        int totalKamar = kost.getTotalKamar() > 0 ? kost.getTotalKamar() : 10;
        int kamarTerisi = Math.max(0, totalKamar - kost.getKamarTersedia());
        tvDetailKamarTerisi.setText(kamarTerisi + " / " + totalKamar + " Kamar");

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
        if (kost.getStatus() == StatusKost.TERSEDIA && kost.getKamarTersedia() > 0) {
            tvStatus.setText("Tersedia");
            tvStatus.setBackgroundResource(R.drawable.bg_badge_tersedia);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_tersedia));
        } else {
            tvStatus.setText("Penuh");
            tvStatus.setBackgroundResource(R.drawable.bg_badge_penuh);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_penuh));
        }

        updateFavoriteIcon(kost.isFavorite());

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
    }

    private void updateFavoriteIcon(boolean isFavorite) {
        if (btnFavorite != null) {
            btnFavorite.setImageResource(isFavorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        }
    }

    private void showReportKostDialog() {
        if (currentKost == null) return;
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Silakan login terlebih dahulu untuk melaporkan properti", Toast.LENGTH_SHORT).show();
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
                            currentKost.getIdKost(),
                            sessionManager.getUserId(),
                            currentKost.getIdPemilik(),
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
}
