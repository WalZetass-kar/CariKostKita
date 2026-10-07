package com.carikostkita.ui.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.carikostkita.R;
import java.util.Set;
import java.util.HashSet;
import com.carikostkita.data.location.IndonesiaLocationData;
import com.carikostkita.data.location.WilayahIndonesiaService;
import com.carikostkita.data.model.Fasilitas;
import com.carikostkita.data.model.FotoKost;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.KostVerificationStatus;
import com.carikostkita.data.model.StatusKost;
import com.carikostkita.data.model.TipeKost;
import com.carikostkita.data.model.Wilayah;
import com.carikostkita.data.repository.ActivityLogRepository;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.ui.adapter.FormFotoAdapter;
import com.carikostkita.ui.map.MapPinPickerActivity;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.FormatUtil;
import com.carikostkita.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminKostFormActivity extends AppCompatActivity {

    private int currentStep = 1;

    // Wizard Headers
    private TextView tvTitle;
    private TextView tvStepTitle;
    private TextView stepIndicator1, stepIndicator2, stepIndicator3, stepIndicator4, stepIndicator5;
    private View stepLine1, stepLine2, stepLine3, stepLine4;

    // Step Containers
    private LinearLayout stepContainer1, stepContainer2, stepContainer3, stepContainer4, stepContainer5;

    // Step 1: Info Views
    private TextInputEditText etNama;
    private RadioGroup rgTipe;
    private RadioButton rbPutra, rbPutri, rbCampur;
    private TextInputEditText etDeskripsi;
    private TextInputEditText etTotalKamar, etKamarTersedia;
    private TextInputEditText etWhatsapp;
    private TextInputEditText etUkuranKamar;

    // Step 2: Harga & Lokasi Views
    private TextInputEditText etHarga;
    private AutoCompleteTextView actProvinsi, actKota, actKecamatan, actKelurahan, actJalan;
    private TextInputEditText etAlamat;
    private MaterialButton btnOpenMap;
    private MaterialCardView cardLocationSummary;
    private TextView tvCoords, tvMapAddress;

    // Step 3: Fasilitas CheckBoxes
    private CheckBox cbKamarMandi, cbAc, cbWifi, cbLemari, cbKasur, cbMeja;
    private CheckBox cbDapur, cbParkirMotor, cbParkirMobil, cbAkses24, cbListrik, cbAir, cbCctv;
    private final List<CheckBox> allCheckBoxes = new ArrayList<>();

    // Step 4: Foto Views
    private MaterialButton btnAddFoto;
    private TextView tvFotoCounter;
    private RecyclerView rvFotos;
    private FormFotoAdapter formFotoAdapter;

    // Step 5: Preview Views
    private ImageView ivPreviewCover;
    private TextView tvPreviewTipeBadge, tvPreviewKamarBadge, tvPreviewNama, tvPreviewAlamat, tvPreviewHarga, tvPreviewFasilitas;

    // Bottom Navigation
    private MaterialButton btnPrev, btnNext;
    private ProgressBar pbSaving;

    // Repositories & State
    private KostRepository kostRepository;
    private ActivityLogRepository activityLogRepository;
    private SessionManager sessionManager;
    private WilayahIndonesiaService wilayahService;
    private String editKostId = null;
    private Kost existingKost;
    private List<Wilayah> wilayahList = new ArrayList<>();

    private double selectedLat = 0.5071;
    private double selectedLng = 101.4478;
    /** Lokasi kost harus dipilih sendiri di peta; GPS pemilik belum tentu berada di kost. */
    private boolean pinConfirmed = false;

    // Map Pin Launcher
    private final ActivityResultLauncher<Intent> mapPinLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    selectedLat = data.getDoubleExtra(MapPinPickerActivity.EXTRA_LAT, selectedLat);
                    selectedLng = data.getDoubleExtra(MapPinPickerActivity.EXTRA_LNG, selectedLng);
                    pinConfirmed = true;
                    if (cardLocationSummary != null) cardLocationSummary.setVisibility(View.VISIBLE);
                    if (btnOpenMap != null) btnOpenMap.setText("Ubah Titik di Peta");
                    String alamat = data.getStringExtra(MapPinPickerActivity.EXTRA_ALAMAT);
                    String kota = data.getStringExtra(MapPinPickerActivity.EXTRA_KOTA);
                    String kec = data.getStringExtra(MapPinPickerActivity.EXTRA_KECAMATAN);
                    String kel = data.getStringExtra(MapPinPickerActivity.EXTRA_KELURAHAN);
                    String prov = data.getStringExtra(MapPinPickerActivity.EXTRA_PROVINSI);

                    if (alamat != null && !alamat.isEmpty()) {
                        actJalan.setText(alamat, false);
                    }
                    if (prov != null && !prov.isEmpty()) {
                        actProvinsi.setText(prov, false);
                        setupKotaDropdown(prov);
                    }
                    if (kota != null && !kota.isEmpty()) {
                        actKota.setText(kota, false);
                        setupKecamatanDropdown(kota);
                    }
                    if (kec != null && !kec.isEmpty()) {
                        actKecamatan.setText(kec, false);
                        setupKelurahanDropdown(kec);
                    }
                    if (kel != null && !kel.isEmpty()) {
                        actKelurahan.setText(kel, false);
                    }

                    updateLocationSummaryUI(alamat, kota, prov);
                }
            });

    // Image Picker Launcher
    private final ActivityResultLauncher<String> pickMultipleImagesLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    for (Uri uri : uris) {
                        if (formFotoAdapter.getItemCount() >= 6) {
                            Toast.makeText(this, "Maksimal 6 foto kost", Toast.LENGTH_SHORT).show();
                            break;
                        }
                        FotoKost foto = new FotoKost(0, 0,
                                "foto_" + System.currentTimeMillis() + ".jpg", uri.toString(), false);
                        formFotoAdapter.addFoto(foto);
                    }
                    updateFotoCounter();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_kost_form);

        kostRepository = new KostRepository(this);
        activityLogRepository = new ActivityLogRepository(this);
        sessionManager = new SessionManager(this);
        wilayahService = WilayahIndonesiaService.getInstance();

        editKostId = getIntent().getStringExtra("kost_id");

        if (!sessionManager.isLoggedIn()
                || (editKostId == null && !sessionManager.isPemilikKost())
                || (editKostId != null && !sessionManager.isPemilikKost() && !sessionManager.isDeveloper())) {
            Toast.makeText(this, "Hanya pemilik kost terverifikasi yang dapat menambahkan kost.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();
        initCoordinatesFromGps();
        setupStep1();
        setupStep2();
        setupStep3();
        setupStep4();
        setupBottomActions();
        loadWilayahData();

        updateStepUI();
    }

    private void initCoordinatesFromGps() {
        if (editKostId != null) return;

        double savedLat = sessionManager.getUserSelectedLat();
        double savedLng = sessionManager.getUserSelectedLng();
        if (savedLat != 0.0 && savedLng != 0.0) {
            selectedLat = savedLat;
            selectedLng = savedLng;
        }

        com.carikostkita.data.location.UserLocationManager locManager =
                com.carikostkita.data.location.UserLocationManager.getInstance(this);
        if (locManager.hasLocationPermission()) {
            android.location.LocationManager lm = (android.location.LocationManager) getSystemService(android.content.Context.LOCATION_SERVICE);
            if (lm != null) {
                try {
                    android.location.Location loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
                    if (loc == null) loc = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
                    if (loc != null) {
                        selectedLat = loc.getLatitude();
                        selectedLng = loc.getLongitude();
                    }
                } catch (SecurityException ignored) {}
            }
        }
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_form_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackNavigation());
        }

        tvTitle = findViewById(R.id.tv_form_title);
        tvStepTitle = findViewById(R.id.tv_form_step_title);

        stepIndicator1 = findViewById(R.id.step_indicator_1);
        stepIndicator2 = findViewById(R.id.step_indicator_2);
        stepIndicator3 = findViewById(R.id.step_indicator_3);
        stepIndicator4 = findViewById(R.id.step_indicator_4);
        stepIndicator5 = findViewById(R.id.step_indicator_5);

        stepLine1 = findViewById(R.id.step_line_1);
        stepLine2 = findViewById(R.id.step_line_2);
        stepLine3 = findViewById(R.id.step_line_3);
        stepLine4 = findViewById(R.id.step_line_4);

        stepContainer1 = findViewById(R.id.step_container_1);
        stepContainer2 = findViewById(R.id.step_container_2);
        stepContainer3 = findViewById(R.id.step_container_3);
        stepContainer4 = findViewById(R.id.step_container_4);
        stepContainer5 = findViewById(R.id.step_container_5);

        btnPrev = findViewById(R.id.btn_form_prev);
        btnNext = findViewById(R.id.btn_form_next);
        pbSaving = findViewById(R.id.pb_form_saving);

        if (editKostId != null) {
            tvTitle.setText("Edit Properti Kost");
        } else {
            tvTitle.setText("Tambah Kost Baru");
        }
    }

    private void setupStep1() {
        etNama = findViewById(R.id.et_form_nama);
        rgTipe = findViewById(R.id.rg_form_tipe);
        rbPutra = findViewById(R.id.rb_form_putra);
        rbPutri = findViewById(R.id.rb_form_putri);
        rbCampur = findViewById(R.id.rb_form_campur);
        etDeskripsi = findViewById(R.id.et_form_deskripsi);
        etTotalKamar = findViewById(R.id.et_form_total_kamar);
        etKamarTersedia = findViewById(R.id.et_form_kamar_tersedia);
        etWhatsapp = findViewById(R.id.et_form_whatsapp);
        etUkuranKamar = findViewById(R.id.et_form_ukuran_kamar);
    }

    private void setupStep2() {
        etHarga = findViewById(R.id.et_form_harga);
        actProvinsi = findViewById(R.id.act_form_provinsi);
        actKota = findViewById(R.id.act_form_kota);
        actKecamatan = findViewById(R.id.act_form_kecamatan);
        actKelurahan = findViewById(R.id.act_form_kelurahan);
        actJalan = findViewById(R.id.act_form_jalan);
        etAlamat = findViewById(R.id.et_form_alamat);

        btnOpenMap = findViewById(R.id.btn_form_open_map);
        cardLocationSummary = findViewById(R.id.card_form_location_summary);
        tvCoords = findViewById(R.id.tv_form_coords);
        tvMapAddress = findViewById(R.id.tv_form_map_address);

        // 1. Populate Provinsi
        List<String> provList = IndonesiaLocationData.getProvinsiList();
        ArrayAdapter<String> provAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, provList);
        actProvinsi.setAdapter(provAdapter);

        // Default: Riau
        actProvinsi.setText("Riau", false);
        setupKotaDropdown("Riau");

        actProvinsi.setOnItemClickListener((parent, view, position, id) -> {
            String selectedProv = (String) parent.getItemAtPosition(position);
            actKota.setText("", false);
            actKecamatan.setText("", false);
            actKelurahan.setText("", false);
            actKecamatan.setEnabled(false);
            actKelurahan.setEnabled(false);
            setupKotaDropdown(selectedProv);
        });

        // Autocomplete Streets
        ArrayAdapter<String> streetAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, IndonesiaLocationData.getCommonStreets());
        actJalan.setAdapter(streetAdapter);

        btnOpenMap.setOnClickListener(v -> {
            Intent mapIntent = new Intent(this, MapPinPickerActivity.class);
            mapIntent.putExtra(MapPinPickerActivity.EXTRA_LAT, selectedLat);
            mapIntent.putExtra(MapPinPickerActivity.EXTRA_LNG, selectedLng);
            mapPinLauncher.launch(mapIntent);
        });

        // Ringkasan baru tampil setelah pemilik memilih titik di peta
        if (cardLocationSummary != null) cardLocationSummary.setVisibility(View.GONE);
        attachRupiahFormatter(etHarga);
    }

    /** Format "1500000" menjadi "1.500.000" saat diketik agar nominal mudah dibaca. */
    private void attachRupiahFormatter(TextInputEditText field) {
        field.addTextChangedListener(new android.text.TextWatcher() {
            private boolean editing = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable e) {
                if (editing) return;
                String digits = e.toString().replaceAll("[^0-9]", "");
                if (digits.length() > 12) digits = digits.substring(0, 12);
                String formatted = digits.isEmpty() ? "" : String.format(new Locale("in", "ID"), "%,d", Long.parseLong(digits));
                if (!formatted.equals(e.toString())) {
                    editing = true;
                    field.setText(formatted);
                    field.setSelection(formatted.length());
                    editing = false;
                }
            }
        });
    }

    private static long parseRupiah(String text) {
        String digits = text == null ? "" : text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String digitsOnly(String text) {
        return text == null ? "" : text.replaceAll("[^0-9]", "");
    }

    private void setupKotaDropdown(String provinsi) {
        List<String> kotaList = IndonesiaLocationData.getKotaList(provinsi);
        ArrayAdapter<String> kotaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, kotaList);
        actKota.setAdapter(kotaAdapter);
        actKota.setEnabled(true);

        if ("Riau".equalsIgnoreCase(provinsi) && actKota.getText().toString().isEmpty()) {
            actKota.setText("Kota Pekanbaru", false);
            setupKecamatanDropdown("Kota Pekanbaru");
        }

        actKota.setOnItemClickListener((parent, view, position, id) -> {
            String selectedKota = (String) parent.getItemAtPosition(position);
            actKecamatan.setText("", false);
            actKelurahan.setText("", false);
            actKelurahan.setEnabled(false);
            setupKecamatanDropdown(selectedKota);
        });
    }

    private void setupKecamatanDropdown(String kota) {
        List<String> initialList = IndonesiaLocationData.getKecamatanList(kota);
        ArrayAdapter<String> initialAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>(initialList));
        actKecamatan.setAdapter(initialAdapter);
        actKecamatan.setEnabled(true);

        String prov = actProvinsi != null ? actProvinsi.getText().toString().trim() : "Riau";
        wilayahService.getKecamatanList(kota, prov, new WilayahIndonesiaService.WilayahCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> data) {
                if (isFinishing() || isDestroyed()) return;
                if (data != null && !data.isEmpty()) {
                    ArrayAdapter<String> liveAdapter = new ArrayAdapter<>(AdminKostFormActivity.this,
                            android.R.layout.simple_dropdown_item_1line, data);
                    actKecamatan.setAdapter(liveAdapter);
                }
            }

            @Override
            public void onError(String message, List<String> fallbackData) {
                if (isFinishing() || isDestroyed()) return;
                if (fallbackData != null && !fallbackData.isEmpty()) {
                    ArrayAdapter<String> fallbackAdapter = new ArrayAdapter<>(AdminKostFormActivity.this,
                            android.R.layout.simple_dropdown_item_1line, fallbackData);
                    actKecamatan.setAdapter(fallbackAdapter);
                }
            }
        });

        actKecamatan.setOnItemClickListener((parent, view, position, id) -> {
            String selectedKec = (String) parent.getItemAtPosition(position);
            actKelurahan.setText("", false);
            setupKelurahanDropdown(selectedKec);
        });

        actKelurahan.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                String currentKec = actKecamatan.getText().toString().trim();
                if (!currentKec.isEmpty()) {
                    setupKelurahanDropdown(currentKec);
                }
            }
        });
    }

    private void setupKelurahanDropdown(String kecamatan) {
        List<String> initialList = IndonesiaLocationData.getKelurahanList(kecamatan);
        ArrayAdapter<String> initialAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>(initialList));
        actKelurahan.setAdapter(initialAdapter);
        actKelurahan.setEnabled(true);

        String prov = actProvinsi != null ? actProvinsi.getText().toString().trim() : "Riau";
        String kota = actKota != null ? actKota.getText().toString().trim() : "Kota Pekanbaru";

        wilayahService.getKelurahanList(kecamatan, kota, prov, new WilayahIndonesiaService.WilayahCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> data) {
                if (isFinishing() || isDestroyed()) return;
                if (data != null && !data.isEmpty()) {
                    ArrayAdapter<String> liveAdapter = new ArrayAdapter<>(AdminKostFormActivity.this,
                            android.R.layout.simple_dropdown_item_1line, data);
                    actKelurahan.setAdapter(liveAdapter);
                }
            }

            @Override
            public void onError(String message, List<String> fallbackData) {
                if (isFinishing() || isDestroyed()) return;
                if (fallbackData != null && !fallbackData.isEmpty()) {
                    ArrayAdapter<String> fallbackAdapter = new ArrayAdapter<>(AdminKostFormActivity.this,
                            android.R.layout.simple_dropdown_item_1line, fallbackData);
                    actKelurahan.setAdapter(fallbackAdapter);
                }
            }
        });
    }

    private void updateLocationSummaryUI(String street, String kota, String prov) {
        if (tvCoords != null) {
            tvCoords.setText(String.format(Locale.getDefault(), "%.6f, %.6f", selectedLat, selectedLng));
        }
        if (tvMapAddress != null) {
            StringBuilder sb = new StringBuilder();
            if (street != null && !street.isEmpty()) sb.append(street).append(", ");
            if (kota != null && !kota.isEmpty()) sb.append(kota).append(", ");
            sb.append(prov != null ? prov : "Indonesia");
            tvMapAddress.setText(sb.toString());
        }
    }

    private void setupStep3() {
        cbKamarMandi = findViewById(R.id.cb_fas_kamar_mandi);
        cbAc = findViewById(R.id.cb_fas_ac);
        cbWifi = findViewById(R.id.cb_fas_wifi);
        cbLemari = findViewById(R.id.cb_fas_lemari);
        cbKasur = findViewById(R.id.cb_fas_kasur);
        cbMeja = findViewById(R.id.cb_fas_meja);
        cbDapur = findViewById(R.id.cb_fas_dapur);
        cbParkirMotor = findViewById(R.id.cb_fas_parkir_motor);
        cbParkirMobil = findViewById(R.id.cb_fas_parkir_mobil);
        cbAkses24 = findViewById(R.id.cb_fas_akses_24);
        cbListrik = findViewById(R.id.cb_fas_listrik);
        cbAir = findViewById(R.id.cb_fas_air);
        cbCctv = findViewById(R.id.cb_fas_cctv);

        allCheckBoxes.add(cbKamarMandi);
        allCheckBoxes.add(cbAc);
        allCheckBoxes.add(cbWifi);
        allCheckBoxes.add(cbLemari);
        allCheckBoxes.add(cbKasur);
        allCheckBoxes.add(cbMeja);
        allCheckBoxes.add(cbDapur);
        allCheckBoxes.add(cbParkirMotor);
        allCheckBoxes.add(cbParkirMobil);
        allCheckBoxes.add(cbAkses24);
        allCheckBoxes.add(cbListrik);
        allCheckBoxes.add(cbAir);
        allCheckBoxes.add(cbCctv);

        // Tags corresponding to getMasterFasilitas
        cbWifi.setTag(1);
        cbParkirMotor.setTag(2);
        cbParkirMobil.setTag(3);
        cbAc.setTag(4);
        cbKamarMandi.setTag(5);
        cbKasur.setTag(6);
        cbLemari.setTag(7);
        cbMeja.setTag(8);
        cbDapur.setTag(9);
        cbListrik.setTag(10);
        cbAkses24.setTag(11);
        cbCctv.setTag(12);
        cbAir.setTag(13);

        com.google.android.material.chip.ChipGroup cgAturan = findViewById(R.id.cg_form_aturan);
        if (cgAturan != null) {
            for (String rule : Kost.ATURAN_MASTER) {
                com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
                chip.setText(rule);
                chip.setCheckable(true);
                chip.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.chip_background_selector));
                chip.setChipStrokeColor(ContextCompat.getColorStateList(this, R.color.chip_stroke_selector));
                chip.setChipStrokeWidth(getResources().getDisplayMetrics().density);
                chip.setTextColor(ContextCompat.getColorStateList(this, R.color.chip_text_selector));
                cgAturan.addView(chip);
            }
        }
        TextInputEditText etDeposit = findViewById(R.id.et_form_deposit);
        if (etDeposit != null) attachRupiahFormatter(etDeposit);

        // Label checkbox = nama yang disimpan & dicari, supaya tidak ada salah tafsir data
        for (CheckBox cb : allCheckBoxes) {
            String name = Fasilitas.nameForId((Integer) cb.getTag());
            if (name != null) cb.setText(name);
        }
    }

    private void setupStep4() {
        btnAddFoto = findViewById(R.id.btn_form_add_foto);
        tvFotoCounter = findViewById(R.id.tv_form_foto_counter);
        rvFotos = findViewById(R.id.rv_form_fotos);

        formFotoAdapter = new FormFotoAdapter(this, new FormFotoAdapter.OnFotoActionListener() {
            @Override
            public void onDeleteFoto(int position) {
                formFotoAdapter.removeFoto(position);
                updateFotoCounter();
            }

            @Override
            public void onSetAsCover(int position) {
                formFotoAdapter.setCover(position);
            }
        });

        rvFotos.setLayoutManager(new LinearLayoutManager(this));
        rvFotos.setAdapter(formFotoAdapter);

        btnAddFoto.setOnClickListener(v -> {
            if (formFotoAdapter.getItemCount() >= 6) {
                Toast.makeText(this, "Maksimal 6 foto kost", Toast.LENGTH_SHORT).show();
                return;
            }
            pickMultipleImagesLauncher.launch("image/*");
        });

        updateFotoCounter();
    }

    private void updateFotoCounter() {
        int count = formFotoAdapter != null ? formFotoAdapter.getItemCount() : 0;
        if (tvFotoCounter != null) {
            tvFotoCounter.setText(count + " / 6 Foto");
        }
    }

    private void setupStep5Preview() {
        ivPreviewCover = findViewById(R.id.iv_preview_cover);
        tvPreviewTipeBadge = findViewById(R.id.tv_preview_tipe_badge);
        tvPreviewKamarBadge = findViewById(R.id.tv_preview_kamar_badge);
        tvPreviewNama = findViewById(R.id.tv_preview_nama);
        tvPreviewAlamat = findViewById(R.id.tv_preview_alamat);
        tvPreviewHarga = findViewById(R.id.tv_preview_harga);
        tvPreviewFasilitas = findViewById(R.id.tv_preview_fasilitas);

        String nama = etNama.getText().toString().trim();
        tvPreviewNama.setText(!nama.isEmpty() ? nama : "Nama Kost");

        tvPreviewHarga.setText(FormatUtil.formatRupiah(parseRupiah(etHarga.getText().toString())) + " / bulan");

        String tipe = "PUTRI";
        if (rbPutra.isChecked()) {
            tipe = "PUTRA";
            tvPreviewTipeBadge.setBackgroundResource(R.drawable.bg_badge_putra);
            tvPreviewTipeBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_putra));
        } else if (rbCampur.isChecked()) {
            tipe = "CAMPUR";
            tvPreviewTipeBadge.setBackgroundResource(R.drawable.bg_badge_campur);
            tvPreviewTipeBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_campur));
        } else {
            tvPreviewTipeBadge.setBackgroundResource(R.drawable.bg_badge_putri);
            tvPreviewTipeBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_putri));
        }
        tvPreviewTipeBadge.setText(tipe);

        String kosong = etKamarTersedia.getText().toString().trim();
        tvPreviewKamarBadge.setText(!kosong.isEmpty() && !"0".equals(kosong) ? kosong + " kamar kosong" : "Penuh");

        StringBuilder sbAlamat = new StringBuilder();
        String jalan = actJalan.getText().toString().trim();
        String detailAlamat = etAlamat.getText().toString().trim();
        String kec = actKecamatan.getText().toString().trim();
        String kota = actKota.getText().toString().trim();

        if (!jalan.isEmpty()) sbAlamat.append(jalan).append(", ");
        if (!detailAlamat.isEmpty()) sbAlamat.append(detailAlamat).append(", ");
        if (!kec.isEmpty()) sbAlamat.append(kec).append(", ");
        if (!kota.isEmpty()) sbAlamat.append(kota);
        tvPreviewAlamat.setText(sbAlamat.toString());

        // Selected Facilities List
        StringBuilder sbFas = new StringBuilder();
        for (CheckBox cb : allCheckBoxes) {
            if (cb.isChecked()) {
                if (sbFas.length() > 0) sbFas.append(" • ");
                sbFas.append(cb.getText().toString().replaceAll("[^a-zA-Z0-9 ()&/]", "").trim());
            }
        }
        if (sbFas.length() == 0) {
            sbFas.append("Belum ada fasilitas dipilih");
        }
        tvPreviewFasilitas.setText(sbFas.toString());

        // Cover Photo
        List<FotoKost> fotos = formFotoAdapter.getFotoList();
        if (!fotos.isEmpty()) {
            String coverPath = null;
            for (FotoKost f : fotos) {
                if (f.isThumbnail()) {
                    coverPath = f.getPathFile();
                    break;
                }
            }
            if (coverPath == null) coverPath = fotos.get(0).getPathFile();
            Glide.with(this).load(coverPath).placeholder(R.drawable.bg_thumb_placeholder).into(ivPreviewCover);
        }
    }

    private void setupBottomActions() {
        btnPrev.setOnClickListener(v -> handleBackNavigation());
        btnNext.setOnClickListener(v -> handleNextNavigation());
    }

    private void updateStepUI() {
        // Toggle containers
        stepContainer1.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);
        stepContainer2.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);
        stepContainer3.setVisibility(currentStep == 3 ? View.VISIBLE : View.GONE);
        stepContainer4.setVisibility(currentStep == 4 ? View.VISIBLE : View.GONE);
        stepContainer5.setVisibility(currentStep == 5 ? View.VISIBLE : View.GONE);

        // Update Dots & Lines
        updateStepIndicator(stepIndicator1, currentStep >= 1);
        updateStepIndicator(stepIndicator2, currentStep >= 2);
        updateStepIndicator(stepIndicator3, currentStep >= 3);
        updateStepIndicator(stepIndicator4, currentStep >= 4);
        updateStepIndicator(stepIndicator5, currentStep >= 5);

        if (stepLine1 != null) stepLine1.setBackgroundColor(ContextCompat.getColor(this, currentStep >= 2 ? R.color.primary : R.color.border));
        if (stepLine2 != null) stepLine2.setBackgroundColor(ContextCompat.getColor(this, currentStep >= 3 ? R.color.primary : R.color.border));
        if (stepLine3 != null) stepLine3.setBackgroundColor(ContextCompat.getColor(this, currentStep >= 4 ? R.color.primary : R.color.border));
        if (stepLine4 != null) stepLine4.setBackgroundColor(ContextCompat.getColor(this, currentStep >= 5 ? R.color.primary : R.color.border));

        // Subtitle
        switch (currentStep) {
            case 1:
                tvStepTitle.setText("Langkah 1 dari 5 — Informasi Umum");
                btnPrev.setVisibility(View.GONE);
                btnNext.setText("Lanjut ke Lokasi ›");
                break;
            case 2:
                tvStepTitle.setText("Langkah 2 dari 5 — Harga & Lokasi");
                btnPrev.setVisibility(View.VISIBLE);
                btnNext.setText("Lanjut ke Fasilitas ›");
                break;
            case 3:
                tvStepTitle.setText("Langkah 3 dari 5 — Fasilitas");
                btnPrev.setVisibility(View.VISIBLE);
                btnNext.setText("Lanjut ke Foto ›");
                break;
            case 4:
                tvStepTitle.setText("Langkah 4 dari 5 — Foto Kost");
                btnPrev.setVisibility(View.VISIBLE);
                btnNext.setText("Lanjut ke Preview ›");
                break;
            case 5:
                tvStepTitle.setText("Langkah 5 dari 5 — Pratinjau & Publikasi");
                btnPrev.setVisibility(View.VISIBLE);
                btnNext.setText(editKostId != null ? "Simpan Perubahan" : "Publikasikan Kost");
                setupStep5Preview();
                break;
        }
    }

    private void updateStepIndicator(TextView tv, boolean active) {
        if (tv == null) return;
        if (active) {
            tv.setBackgroundResource(R.drawable.bg_nav_pill_active);
            tv.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            tv.setBackgroundResource(R.drawable.bg_circle_icon_blue);
            tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void handleNextNavigation() {
        if (currentStep == 1) {
            if (!validateStep1()) return;
            currentStep = 2;
            updateStepUI();
        } else if (currentStep == 2) {
            if (!validateStep2()) return;
            currentStep = 3;
            updateStepUI();
        } else if (currentStep == 3) {
            currentStep = 4;
            updateStepUI();
        } else if (currentStep == 4) {
            if (!validateStep4()) return;
            currentStep = 5;
            updateStepUI();
        } else if (currentStep == 5) {
            submitKost();
        }
    }

    private void handleBackNavigation() {
        if (currentStep > 1) {
            currentStep--;
            updateStepUI();
        } else {
            AppDialogHelper.showConfirmationDialog(this,
                    "Batalkan Pengisian?",
                    "Data kost yang belum disimpan akan hilang jika Anda keluar sekarang. Lanjutkan?",
                    this::finish);
        }
    }

    @Override
    public void onBackPressed() {
        handleBackNavigation();
    }

    private boolean validateStep1() {
        String nama = etNama.getText().toString().trim();
        String deskripsi = etDeskripsi.getText().toString().trim();
        String totalKamarStr = etTotalKamar.getText().toString().trim();
        String kamarTersediaStr = etKamarTersedia.getText().toString().trim();
        String wa = etWhatsapp.getText().toString().trim();

        if (nama.isEmpty()) {
            etNama.setError("Nama kost wajib diisi");
            etNama.requestFocus();
            return false;
        }

        if (deskripsi.isEmpty()) {
            etDeskripsi.setError("Deskripsi kost wajib diisi");
            etDeskripsi.requestFocus();
            return false;
        }

        if (totalKamarStr.isEmpty()) {
            etTotalKamar.setError("Total kamar wajib diisi");
            etTotalKamar.requestFocus();
            return false;
        }

        int total = Integer.parseInt(totalKamarStr);
        if (total <= 0) {
            etTotalKamar.setError("Total kamar harus lebih dari 0");
            etTotalKamar.requestFocus();
            return false;
        }

        int kosong = 0;
        if (!kamarTersediaStr.isEmpty()) {
            kosong = Integer.parseInt(kamarTersediaStr);
        }
        if (kosong > total) {
            etKamarTersedia.setError("Kamar kosong tidak boleh melebihi total kamar");
            etKamarTersedia.requestFocus();
            return false;
        }

        String waDigits = digitsOnly(wa);
        if (waDigits.startsWith("62")) waDigits = "0" + waDigits.substring(2);
        if (waDigits.length() < 9 || waDigits.length() > 13 || !waDigits.startsWith("08")) {
            etWhatsapp.setError("Masukkan nomor WhatsApp aktif, contoh 081234567890");
            etWhatsapp.requestFocus();
            return false;
        }

        return true;
    }

    private boolean validateStep2() {
        String hargaStr = etHarga.getText().toString().trim();
        String prov = actProvinsi.getText().toString().trim();
        String kota = actKota.getText().toString().trim();
        String kec = actKecamatan.getText().toString().trim();
        String jalan = actJalan.getText().toString().trim();
        String alamat = etAlamat.getText().toString().trim();

        if (hargaStr.isEmpty()) {
            etHarga.setError("Harga bulanan wajib diisi");
            etHarga.requestFocus();
            return false;
        }

        long h = parseRupiah(hargaStr);
        if (h < 50000) {
            etHarga.setError("Harga sewa per bulan minimal Rp 50.000");
            etHarga.requestFocus();
            return false;
        }

        if (prov.isEmpty()) {
            Toast.makeText(this, "Silakan pilih Provinsi", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (kota.isEmpty()) {
            Toast.makeText(this, "Silakan pilih Kota / Kabupaten", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (kec.isEmpty()) {
            Toast.makeText(this, "Silakan pilih Kecamatan", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (jalan.isEmpty()) {
            actJalan.setError("Nama jalan wajib diisi");
            actJalan.requestFocus();
            return false;
        }
        if (alamat.isEmpty()) {
            etAlamat.setError("Nomor bangunan / alamat lengkap wajib diisi");
            etAlamat.requestFocus();
            return false;
        }
        if (!pinConfirmed) {
            AppDialogHelper.showInfo(this, "Tentukan Titik di Peta",
                    "Geser peta lalu letakkan pin tepat di lokasi kost. Titik ini dipakai pencari untuk melihat jarak dan rute.");
            return false;
        }

        return true;
    }

    private boolean validateStep4() {
        if (formFotoAdapter.getItemCount() == 0) {
            Toast.makeText(this, "Harap pilih minimal 1 foto utama kost", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void loadWilayahData() {
        kostRepository.getAllWilayah(new DataCallback<List<Wilayah>>() {
            @Override
            public void onSuccess(List<Wilayah> data) {
                wilayahList = data;
                if (editKostId != null) {
                    checkAndPopulateEdit();
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void checkAndPopulateEdit() {
        if (existingKost != null || editKostId == null) return;

        kostRepository.getKostDetail(editKostId, sessionManager.getUserUid(), new DataCallback<Kost>() {
            @Override
            public void onSuccess(Kost kost) {
                existingKost = kost;
                etNama.setText(kost.getNamaKost());
                if (kost.getTipeKost() == TipeKost.PUTRA) rbPutra.setChecked(true);
                else if (kost.getTipeKost() == TipeKost.CAMPUR) rbCampur.setChecked(true);
                else rbPutri.setChecked(true);

                etDeskripsi.setText(kost.getDeskripsi());
                etTotalKamar.setText(kost.getTotalKamar() > 0 ? String.valueOf(kost.getTotalKamar()) : "");
                etKamarTersedia.setText(kost.hasRoomInfo() ? String.valueOf(kost.getKamarTersedia()) : "");
                etWhatsapp.setText(kost.getNoWhatsapp());
                if (etUkuranKamar != null && kost.getUkuranKamar() != null) etUkuranKamar.setText(kost.getUkuranKamar());

                etHarga.setText(String.valueOf((long) kost.getHarga()));
                if (kost.getProvinsi() != null && !kost.getProvinsi().isEmpty()) {
                    actProvinsi.setText(kost.getProvinsi(), false);
                    setupKotaDropdown(kost.getProvinsi());
                }
                if (kost.getKota() != null && !kost.getKota().isEmpty()) {
                    actKota.setText(kost.getKota(), false);
                    setupKecamatanDropdown(kost.getKota());
                }
                if (!kost.getKecamatan().isEmpty()) {
                    actKecamatan.setText(kost.getKecamatan(), false);
                    setupKelurahanDropdown(kost.getKecamatan());
                }
                if (!kost.getKelurahan().isEmpty()) actKelurahan.setText(kost.getKelurahan(), false);

                // Alamat disimpan sebagai "Nama Jalan, Nomor/detail"
                String alamat = kost.getAlamat() != null ? kost.getAlamat() : "";
                int sep = alamat.indexOf(", ");
                if (sep > 0) {
                    actJalan.setText(alamat.substring(0, sep), false);
                    etAlamat.setText(alamat.substring(sep + 2));
                } else {
                    actJalan.setText(alamat, false);
                    etAlamat.setText("");
                }

                selectedLat = kost.getLatitude();
                selectedLng = kost.getLongitude();
                pinConfirmed = kost.hasCoordinates();
                if (pinConfirmed) {
                    if (cardLocationSummary != null) cardLocationSummary.setVisibility(View.VISIBLE);
                    if (btnOpenMap != null) btnOpenMap.setText("Ubah Titik di Peta");
                    updateLocationSummaryUI(alamat, kost.getKota(), kost.getProvinsi());
                }

                TextInputEditText etDep = findViewById(R.id.et_form_deposit);
                TextInputEditText etMin = findViewById(R.id.et_form_min_sewa);
                TextInputEditText etBiaya = findViewById(R.id.et_form_biaya_tambahan);
                if (etDep != null && kost.getDeposit() != null) etDep.setText(String.valueOf(kost.getDeposit()));
                if (etMin != null && kost.getMinimalSewaBulan() != null) etMin.setText(String.valueOf(kost.getMinimalSewaBulan()));
                if (etBiaya != null && kost.getBiayaTambahan() != null) etBiaya.setText(kost.getBiayaTambahan());
                com.google.android.material.chip.ChipGroup cgAt = findViewById(R.id.cg_form_aturan);
                if (cgAt != null) for (int i = 0; i < cgAt.getChildCount(); i++) {
                    com.google.android.material.chip.Chip c = (com.google.android.material.chip.Chip) cgAt.getChildAt(i);
                    c.setChecked(kost.getAturan().contains(c.getText().toString()));
                }

                Set<String> saved = new HashSet<>();
                for (String fName : kost.getFasilitas()) {
                    if (fName != null) saved.add(fName.trim().toLowerCase());
                }
                for (CheckBox cb : allCheckBoxes) {
                    String name = Fasilitas.nameForId((Integer) cb.getTag());
                    cb.setChecked(name != null && saved.contains(name.toLowerCase()));
                }

                // Load photos
                List<FotoKost> fotos = kost.getListFoto();
                if (fotos != null && !fotos.isEmpty()) {
                    formFotoAdapter.setFotos(fotos);
                    updateFotoCounter();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AdminKostFormActivity.this, "Gagal memuat detail kost: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitKost() {
        String nama = etNama.getText().toString().trim();
        double harga = parseRupiah(etHarga.getText().toString());
        String prov = actProvinsi.getText().toString().trim();
        String kota = actKota.getText().toString().trim();
        String kec = actKecamatan.getText().toString().trim();
        String kel = actKelurahan.getText().toString().trim();
        String jalan = actJalan.getText().toString().trim();
        String alamatLengkap = etAlamat.getText().toString().trim();
        String deskripsi = etDeskripsi.getText().toString().trim();
        int totalKamar = Integer.parseInt(etTotalKamar.getText().toString().trim());
        String kosongStr = etKamarTersedia.getText().toString().trim();
        int kamarTersedia = kosongStr.isEmpty() ? 0 : Integer.parseInt(kosongStr);
        String wa = digitsOnly(etWhatsapp.getText().toString());
        if (wa.startsWith("62")) wa = "0" + wa.substring(2);
        String ukuran = etUkuranKamar != null && etUkuranKamar.getText() != null ? etUkuranKamar.getText().toString().trim() : "";

        TipeKost tipe = TipeKost.PUTRI;
        if (rbPutra.isChecked()) tipe = TipeKost.PUTRA;
        else if (rbCampur.isChecked()) tipe = TipeKost.CAMPUR;

        StatusKost status = kamarTersedia > 0 ? StatusKost.TERSEDIA : StatusKost.PENUH;

        // Collect Facility IDs
        List<Integer> selectedFasilitas = new ArrayList<>();
        for (CheckBox cb : allCheckBoxes) {
            if (cb.isChecked() && cb.getTag() != null) {
                selectedFasilitas.add((Integer) cb.getTag());
            }
        }

        Kost kost = (editKostId != null && existingKost != null) ? existingKost : new Kost();
        kost.setNamaKost(nama);
        kost.setIdWilayah(wilayahList.isEmpty() ? 1 : wilayahList.get(0).getIdWilayah());
        kost.setProvinsi(prov);
        kost.setKota(kota);
        kost.setKecamatan(kec);
        kost.setKelurahan(kel);
        kost.setAlamat(jalan + ", " + alamatLengkap);
        kost.setUkuranKamar(ukuran.isEmpty() ? null : ukuran);
        kost.setTotalKamar(totalKamar);
        kost.setKamarTersedia(kamarTersedia);
        kost.setHarga(harga);
        kost.setTipeKost(tipe);
        kost.setNoWhatsapp(wa);
        kost.setLatitude(selectedLat);
        kost.setLongitude(selectedLng);
        kost.setDeskripsi(deskripsi);
        kost.setStatus(status);

        TextInputEditText etDep = findViewById(R.id.et_form_deposit);
        TextInputEditText etMin = findViewById(R.id.et_form_min_sewa);
        TextInputEditText etBiaya = findViewById(R.id.et_form_biaya_tambahan);
        long deposit = etDep != null ? parseRupiah(etDep.getText() != null ? etDep.getText().toString() : "") : 0;
        kost.setDeposit(deposit > 0 ? (int) Math.min(deposit, Integer.MAX_VALUE) : null);
        String minStr = etMin != null && etMin.getText() != null ? digitsOnly(etMin.getText().toString()) : "";
        kost.setMinimalSewaBulan(minStr.isEmpty() ? null : Math.max(1, Math.min(24, Integer.parseInt(minStr))));
        String biaya = etBiaya != null && etBiaya.getText() != null ? etBiaya.getText().toString().trim() : "";
        kost.setBiayaTambahan(biaya.isEmpty() ? null : biaya);
        java.util.List<String> rules = new ArrayList<>();
        com.google.android.material.chip.ChipGroup cgAt = findViewById(R.id.cg_form_aturan);
        if (cgAt != null) for (int i = 0; i < cgAt.getChildCount(); i++) {
            com.google.android.material.chip.Chip c = (com.google.android.material.chip.Chip) cgAt.getChildAt(i);
            if (c.isChecked()) rules.add(c.getText().toString());
        }
        kost.setAturan(rules);

        String currentUserId = sessionManager.getUserUid();
        if ((kost.getOwnerId() == null || kost.getOwnerId().isEmpty()) && currentUserId != null) {
            kost.setOwnerId(currentUserId);
        }

        // Status verifikasi final ditentukan trigger server (perubahan isi listing -> review ulang)
        boolean isOwner = sessionManager.isPemilikKost() && !sessionManager.isDeveloper();
        if (editKostId == null) {
            kost.setVerificationStatus(KostVerificationStatus.PENDING);
            kost.setCatatanRevisi(null);
        }

        List<FotoKost> fotosToSave = formFotoAdapter.getFotoList();
        if (!fotosToSave.isEmpty()) {
            boolean hasCover = false;
            for (FotoKost f : fotosToSave) {
                if (f.isThumbnail()) {
                    hasCover = true;
                    break;
                }
            }
            if (!hasCover) {
                fotosToSave.get(0).setThumbnail(true);
            }
        }

        pbSaving.setVisibility(View.VISIBLE);
        btnNext.setEnabled(false);
        btnPrev.setEnabled(false);

        DataCallback<KostRepository.SaveResult> saveCallback = new DataCallback<KostRepository.SaveResult>() {
            @Override
            public void onSuccess(KostRepository.SaveResult result) {
                pbSaving.setVisibility(View.GONE);
                if (isOwner) {
                    activityLogRepository.logActivity(currentUserId,
                            editKostId == null ? "SUBMIT_KOST" : "RESUBMIT_KOST",
                            (editKostId == null ? "Pemilik mengajukan kost baru: " : "Pemilik memperbarui kost: ") + kost.getNamaKost(),
                            "KOST", result.kostId);
                }
                showSuccessPublishDialog(result);
            }

            @Override
            public void onError(String message) {
                pbSaving.setVisibility(View.GONE);
                btnNext.setEnabled(true);
                btnPrev.setEnabled(true);
                AppDialogHelper.showErrorDialog(AdminKostFormActivity.this,
                        editKostId == null ? "Gagal Menyimpan Kost" : "Gagal Menyimpan Perubahan", message);
            }
        };

        if (editKostId == null) {
            kostRepository.saveKostWithFotos(kost, selectedFasilitas, fotosToSave, saveCallback);
        } else {
            kost.setId(editKostId);
            kostRepository.updateKostWithFotos(kost, selectedFasilitas, fotosToSave, saveCallback);
        }
    }

    private void showSuccessPublishDialog(KostRepository.SaveResult result) {
        String title = editKostId != null ? "Perubahan Tersimpan" : "Kost Berhasil Diajukan";
        StringBuilder message = new StringBuilder();
        if (result.sentToReview) {
            message.append(editKostId != null
                    ? "Perubahan isi listing akan ditinjau tim CariKostKita. Selama ditinjau, kost tidak tampil di pencarian."
                    : "Kost kamu masuk antrean verifikasi. Setelah disetujui, kost langsung tampil di pencarian.");
        } else {
            message.append("Data terbaru sudah tampil untuk pencari kost.");
        }
        if (result.failedUploads > 0) {
            message.append("\n\n").append(result.failedUploads)
                    .append(" foto gagal diunggah. Buka Edit Kost untuk mencoba mengunggahnya lagi.");
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message.toString())
                .setCancelable(false)
                .setPositiveButton("Selesai", (dialog, which) -> {
                    dialog.dismiss();
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }
}
