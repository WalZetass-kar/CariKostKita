package com.carikostkita.ui.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.viewpager2.widget.ViewPager2;
import com.carikostkita.R;
import com.carikostkita.data.model.Fasilitas;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.StatusKost;
import com.carikostkita.data.model.TipeKost;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.ui.adapter.FotoSliderAdapter;
import com.carikostkita.util.IntentHelper;
import com.carikostkita.util.SessionManager;

public class DetailKostActivity extends AppCompatActivity {

    private ViewPager2 vpPhotos;
    private TextView tvPhotoIndicator;
    private TextView tvTipe;
    private TextView tvStatus;
    private TextView tvName;
    private TextView tvPrice;
    private TextView tvAddress;
    private TextView tvDesc;
    private TextView tvDetailUkuranKamar;
    private TextView tvDetailKamarTersedia;
    private TextView tvDetailKamarTerisi;
    private ViewGroup layoutFacilities;
    private ImageButton btnFavorite;
    private Button btnActionMaps;
    private Button btnActionWhatsApp;

    private KostRepository kostRepository;
    private SessionManager sessionManager;
    private Kost currentKost;
    private int idKost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_kost);

        kostRepository = new KostRepository(this);
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
        tvDesc = findViewById(R.id.tv_detail_desc);
        layoutFacilities = findViewById(R.id.layout_detail_facilities);
        btnActionMaps = findViewById(R.id.btn_action_maps);
        btnActionWhatsApp = findViewById(R.id.btn_action_whatsapp);

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

        btnActionMaps.setOnClickListener(v -> {
            if (currentKost != null) {
                IntentHelper.openGoogleMaps(
                        DetailKostActivity.this,
                        currentKost.getLatitude(),
                        currentKost.getLongitude(),
                        currentKost.getNamaKost()
                );
            }
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
            // Hide "1/1 Foto" badge if only 1 photo exists
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
}
