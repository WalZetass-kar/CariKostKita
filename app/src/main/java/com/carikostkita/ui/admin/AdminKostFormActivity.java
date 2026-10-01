package com.carikostkita.ui.admin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.Fasilitas;
import com.carikostkita.data.model.FotoKost;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.StatusKost;
import com.carikostkita.data.model.TipeKost;
import com.carikostkita.data.model.Wilayah;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.ui.adapter.FormFotoAdapter;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.SessionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminKostFormActivity extends AppCompatActivity {

    private TextView tvTitle;
    private EditText etNama;
    private RadioGroup rgTipe;
    private RadioButton rbPutra, rbPutri, rbCampur;
    private EditText etHarga;
    private EditText etProvinsi, etKota;
    private Spinner spWilayah;
    private EditText etAlamat, etPatokan;

    private RecyclerView rvFormFotos;
    private Button btnAddFoto;
    private TextView tvFotoCounter;
    private TextView tvFotoEmpty;
    private FormFotoAdapter formFotoAdapter;

    private EditText etUkuranKamar, etTotalKamar, etKamarTersedia;
    private TextView tvKamarTerisiInfo;
    private EditText etWhatsapp;
    private EditText etLat, etLng;
    private Button btnCurrentLocation, btnTestMaps;
    private TextView tvLocationPreviewDesc;

    private EditText etDeskripsi;
    private RadioGroup rgStatus;
    private RadioButton rbTersedia, rbPenuh, rbTidakAktif;
    private LinearLayout layoutFasilitasChecks;
    private Button btnSave;

    private com.google.android.material.card.MaterialCardView cardRevisiAlert;
    private TextView tvRevisiNote;
    private SessionManager sessionManager;
    private com.carikostkita.data.repository.ActivityLogRepository activityLogRepository;

    private KostRepository kostRepository;
    private List<Wilayah> wilayahList = new ArrayList<>();
    private List<Fasilitas> masterFasilitas = new ArrayList<>();
    private List<CheckBox> fasilitasCheckBoxes = new ArrayList<>();

    private int editKostId = -1;
    private Kost existingKost;

    private final ActivityResultLauncher<String> pickMultipleImagesLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    for (Uri uri : uris) {
                        if (formFotoAdapter.getItemCount() >= 6) {
                            Toast.makeText(this, "Maksimal 6 foto kost", Toast.LENGTH_SHORT).show();
                            break;
                        }
                        FotoKost foto = new FotoKost(0, editKostId != -1 ? editKostId : 0,
                                "foto_" + System.currentTimeMillis() + ".jpg", uri.toString(), false);
                        formFotoAdapter.addFoto(foto);
                    }
                    updateFotoCounter();
                }
            });

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fine = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                Boolean coarse = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                if ((fine != null && fine) || (coarse != null && coarse)) {
                    fetchCurrentLocation();
                } else {
                    AppDialogHelper.showInfoDialog(this, "Izin Lokasi Diperlukan",
                            "Izinkan akses lokasi agar aplikasi dapat mengisi koordinat GPS lokasi kost secara akurat.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_kost_form);

        sessionManager = new SessionManager(this);
        kostRepository = new KostRepository(this);
        activityLogRepository = new com.carikostkita.data.repository.ActivityLogRepository(this);
        editKostId = getIntent().getIntExtra("kost_id", -1);

        initViews();
        setupPhotoGallery();
        setupLocationControls();
        loadWilayahAndFasilitas();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_form_title);
        etNama = findViewById(R.id.et_form_nama);
        rgTipe = findViewById(R.id.rg_form_tipe);
        rbPutra = findViewById(R.id.rb_form_putra);
        rbPutri = findViewById(R.id.rb_form_putri);
        rbCampur = findViewById(R.id.rb_form_campur);
        etHarga = findViewById(R.id.et_form_harga);
        etProvinsi = findViewById(R.id.et_form_provinsi);
        etKota = findViewById(R.id.et_form_kota);
        spWilayah = findViewById(R.id.sp_form_wilayah);
        etAlamat = findViewById(R.id.et_form_alamat);
        etPatokan = findViewById(R.id.et_form_patokan);

        rvFormFotos = findViewById(R.id.rv_form_fotos);
        btnAddFoto = findViewById(R.id.btn_form_add_foto);
        tvFotoCounter = findViewById(R.id.tv_form_foto_count);
        tvFotoEmpty = findViewById(R.id.tv_form_foto_empty);

        etUkuranKamar = findViewById(R.id.et_form_ukuran_kamar);
        etTotalKamar = findViewById(R.id.et_form_total_kamar);
        etKamarTersedia = findViewById(R.id.et_form_kamar_tersedia);
        tvKamarTerisiInfo = findViewById(R.id.tv_form_kamar_terisi_info);
        etWhatsapp = findViewById(R.id.et_form_whatsapp);
        etLat = findViewById(R.id.et_form_lat);
        etLng = findViewById(R.id.et_form_lng);
        btnCurrentLocation = findViewById(R.id.btn_form_current_location);
        btnTestMaps = findViewById(R.id.btn_form_test_maps);
        tvLocationPreviewDesc = findViewById(R.id.tv_form_coord_preview);

        etDeskripsi = findViewById(R.id.et_form_deskripsi);
        rgStatus = findViewById(R.id.rg_form_status);
        rbTersedia = findViewById(R.id.rb_status_tersedia);
        rbPenuh = findViewById(R.id.rb_status_penuh);
        rbTidakAktif = findViewById(R.id.rb_status_tidak_aktif);
        layoutFasilitasChecks = findViewById(R.id.layout_form_fasilitas_checks);
        btnSave = findViewById(R.id.btn_form_save);

        cardRevisiAlert = findViewById(R.id.card_form_revisi_alert);
        tvRevisiNote = findViewById(R.id.tv_form_revisi_note);

        ImageButton btnBack = findViewById(R.id.btn_form_back);
        btnBack.setOnClickListener(v -> checkDiscardAndExit());

        TextWatcher roomWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateRoomVacancyCalculation();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        etTotalKamar.addTextChangedListener(roomWatcher);
        etKamarTersedia.addTextChangedListener(roomWatcher);

        if (editKostId != -1) {
            tvTitle.setText("Edit Properti Kost");
            btnSave.setText("Simpan Perubahan");
        } else {
            tvTitle.setText("Tambah Kost Baru");
            btnSave.setText("Simpan Properti Kost");
        }

        btnSave.setOnClickListener(v -> handleSave());
    }

    private void setupPhotoGallery() {
        formFotoAdapter = new FormFotoAdapter(this, new FormFotoAdapter.OnFotoActionListener() {
            @Override
            public void onDeleteFoto(int position) {
                AppDialogHelper.showConfirmationDialog(AdminKostFormActivity.this,
                        "Hapus Foto",
                        "Apakah Anda yakin ingin menghapus foto ini dari daftar?",
                        () -> {
                            formFotoAdapter.removeFoto(position);
                            updateFotoCounter();
                        });
            }

            @Override
            public void onSetAsCover(int position) {
                formFotoAdapter.setCover(position);
                Toast.makeText(AdminKostFormActivity.this, "Foto utama diperbarui", Toast.LENGTH_SHORT).show();
            }
        });

        rvFormFotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvFormFotos.setAdapter(formFotoAdapter);

        btnAddFoto.setOnClickListener(v -> {
            if (formFotoAdapter.getItemCount() >= 6) {
                Toast.makeText(this, "Maksimal 6 foto telah tercapai", Toast.LENGTH_SHORT).show();
                return;
            }
            pickMultipleImagesLauncher.launch("image/*");
        });

        updateFotoCounter();
    }

    private void updateFotoCounter() {
        if (tvFotoCounter != null && formFotoAdapter != null) {
            tvFotoCounter.setText(formFotoAdapter.getItemCount() + " / 6 foto");
            if (tvFotoEmpty != null) {
                tvFotoEmpty.setVisibility(formFotoAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
            }
        }
    }

    private void setupLocationControls() {
        btnCurrentLocation.setOnClickListener(v -> requestLocation());

        btnTestMaps.setOnClickListener(v -> {
            String latStr = etLat.getText().toString().trim();
            String lngStr = etLng.getText().toString().trim();
            double lat = 0.463280;
            double lng = 101.450123;
            try {
                if (!latStr.isEmpty()) lat = Double.parseDouble(latStr);
                if (!lngStr.isEmpty()) lng = Double.parseDouble(lngStr);
            } catch (Exception ignored) {}

            String label = etNama.getText().toString().trim();
            if (label.isEmpty()) label = "Lokasi Kost";

            Uri gmmIntentUri = Uri.parse("geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(" + Uri.encode(label) + ")");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Intent webIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng));
                startActivity(webIntent);
            }
        });

        TextWatcher locWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLocationPreview();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        etLat.addTextChangedListener(locWatcher);
        etLng.addTextChangedListener(locWatcher);
        etAlamat.addTextChangedListener(locWatcher);
        etPatokan.addTextChangedListener(locWatcher);
    }

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void fetchCurrentLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                Location loc = null;
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (loc == null) {
                        loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

                if (loc != null) {
                    etLat.setText(String.format(Locale.US, "%.6f", loc.getLatitude()));
                    etLng.setText(String.format(Locale.US, "%.6f", loc.getLongitude()));
                    updateLocationPreview();
                    Toast.makeText(this, "Koordinat GPS berhasil disinkronkan", Toast.LENGTH_SHORT).show();
                } else {
                    etLat.setText("0.463280");
                    etLng.setText("101.450123");
                    updateLocationPreview();
                    Toast.makeText(this, "Lokasi GPS belum tersedia, diset ke Bukit Raya default", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Gagal mengambil lokasi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLocationPreview() {
        String latStr = etLat.getText().toString().trim();
        String lngStr = etLng.getText().toString().trim();
        String patokan = etPatokan.getText().toString().trim();
        StringBuilder sb = new StringBuilder();
        if (!latStr.isEmpty() && !lngStr.isEmpty()) {
            sb.append("Koordinat: ").append(latStr).append(", ").append(lngStr);
        } else {
            sb.append("Koordinat belum diisi");
        }
        if (!patokan.isEmpty()) {
            sb.append("\nPatokan: ").append(patokan);
        }
        tvLocationPreviewDesc.setText(sb.toString());
    }

    private void updateRoomVacancyCalculation() {
        try {
            int total = Integer.parseInt(etTotalKamar.getText().toString().trim());
            int tersedia = Integer.parseInt(etKamarTersedia.getText().toString().trim());
            int terisi = Math.max(0, total - tersedia);
            tvKamarTerisiInfo.setText("Kamar Terisi: " + terisi + " kamar (Total: " + total + ", Kosong: " + tersedia + ")");
        } catch (Exception e) {
            tvKamarTerisiInfo.setText("Kamar Terisi: dihitung otomatis dari (Total - Kosong)");
        }
    }

    @Override
    public void onBackPressed() {
        checkDiscardAndExit();
    }

    private void checkDiscardAndExit() {
        AppDialogHelper.showConfirmationDialog(this,
                "Batalkan Pengisian?",
                "Perubahan atau data properti kost yang sudah dimasukkan belum disimpan. Anda yakin ingin keluar?",
                this::finish);
    }

    private void loadWilayahAndFasilitas() {
        kostRepository.getAllWilayah(new DataCallback<List<Wilayah>>() {
            @Override
            public void onSuccess(List<Wilayah> data) {
                wilayahList = data;
                List<String> labels = new ArrayList<>();
                for (Wilayah w : data) {
                    labels.add(w.getKelurahan() + " (" + w.getKecamatan() + ")");
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AdminKostFormActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, labels);
                spWilayah.setAdapter(adapter);

                if (editKostId != -1) {
                    checkAndPopulateEdit();
                }
            }

            @Override
            public void onError(String message) {}
        });

        kostRepository.getAllFasilitas(new DataCallback<List<Fasilitas>>() {
            @Override
            public void onSuccess(List<Fasilitas> data) {
                masterFasilitas = data;
                layoutFasilitasChecks.removeAllViews();
                fasilitasCheckBoxes.clear();

                for (Fasilitas f : data) {
                    CheckBox cb = new CheckBox(AdminKostFormActivity.this);
                    cb.setText(f.getNamaFasilitas());
                    cb.setTag(f.getIdFasilitas());
                    layoutFasilitasChecks.addView(cb);
                    fasilitasCheckBoxes.add(cb);
                }

                if (editKostId != -1) {
                    checkAndPopulateEdit();
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void checkAndPopulateEdit() {
        if (existingKost != null) return;

        kostRepository.getKostDetail(editKostId, 0, new DataCallback<Kost>() {
            @Override
            public void onSuccess(Kost kost) {
                // Security RBAC: Pemilik can only edit their own property
                if (sessionManager.isPemilikKost() && !sessionManager.isDeveloper()) {
                    if (kost.getIdPemilik() > 0 && kost.getIdPemilik() != sessionManager.getUserId()) {
                        Toast.makeText(AdminKostFormActivity.this, "Akses ditolak: Anda hanya dapat mengelola kost milik Anda sendiri", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }

                existingKost = kost;

                // Show revision note banner if revision was requested
                if (kost.getVerificationStatus() == com.carikostkita.data.model.KostVerificationStatus.REVISION_REQUIRED) {
                    if (cardRevisiAlert != null) cardRevisiAlert.setVisibility(View.VISIBLE);
                    if (tvRevisiNote != null) {
                        String note = kost.getCatatanRevisi();
                        tvRevisiNote.setText("Catatan Reviewer: " + (note != null && !note.isEmpty() ? note : "Mohon perbaiki data/foto kost sesuai catatan admin."));
                    }
                } else {
                    if (cardRevisiAlert != null) cardRevisiAlert.setVisibility(View.GONE);
                }

                etNama.setText(kost.getNamaKost());
                etHarga.setText(String.valueOf((long) kost.getHarga()));
                if (kost.getProvinsi() != null && !kost.getProvinsi().isEmpty()) {
                    etProvinsi.setText(kost.getProvinsi());
                }
                if (kost.getKota() != null && !kost.getKota().isEmpty()) {
                    etKota.setText(kost.getKota());
                }
                etAlamat.setText(kost.getAlamat());
                if (kost.getPatokan() != null) {
                    etPatokan.setText(kost.getPatokan());
                }
                if (kost.getUkuranKamar() != null && !kost.getUkuranKamar().isEmpty()) {
                    etUkuranKamar.setText(kost.getUkuranKamar());
                }
                etTotalKamar.setText(String.valueOf(kost.getTotalKamar() > 0 ? kost.getTotalKamar() : 10));
                etKamarTersedia.setText(String.valueOf(kost.getKamarTersedia()));
                updateRoomVacancyCalculation();

                etWhatsapp.setText(kost.getNoWhatsapp());
                etLat.setText(String.valueOf(kost.getLatitude()));
                etLng.setText(String.valueOf(kost.getLongitude()));
                updateLocationPreview();
                etDeskripsi.setText(kost.getDeskripsi());

                // Tipe
                if (kost.getTipeKost() == TipeKost.PUTRI) rbPutri.setChecked(true);
                else if (kost.getTipeKost() == TipeKost.PUTRA) rbPutra.setChecked(true);
                else rbCampur.setChecked(true);

                // Status
                if (kost.getStatus() == StatusKost.TERSEDIA) rbTersedia.setChecked(true);
                else if (kost.getStatus() == StatusKost.PENUH) rbPenuh.setChecked(true);
                else rbTidakAktif.setChecked(true);

                // Select Wilayah in Spinner
                for (int i = 0; i < wilayahList.size(); i++) {
                    if (wilayahList.get(i).getIdWilayah() == kost.getIdWilayah()) {
                        spWilayah.setSelection(i);
                        break;
                    }
                }

                // Check existing facilities
                if (kost.getListFasilitas() != null) {
                    List<Integer> existingIds = new ArrayList<>();
                    for (Fasilitas f : kost.getListFasilitas()) {
                        existingIds.add(f.getIdFasilitas());
                    }
                    for (CheckBox cb : fasilitasCheckBoxes) {
                        int fId = (int) cb.getTag();
                        if (existingIds.contains(fId)) {
                            cb.setChecked(true);
                        }
                    }
                }

                // Load photos
                if (kost.getListFoto() != null && !kost.getListFoto().isEmpty()) {
                    formFotoAdapter.setFotos(kost.getListFoto());
                } else if (kost.getFotoUtama() != null && !kost.getFotoUtama().isEmpty()) {
                    List<FotoKost> fl = new ArrayList<>();
                    fl.add(new FotoKost(0, kost.getIdKost(), "cover.jpg", kost.getFotoUtama(), true));
                    formFotoAdapter.setFotos(fl);
                }
                updateFotoCounter();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AdminKostFormActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSave() {
        String nama = etNama.getText().toString().trim();
        String hargaStr = etHarga.getText().toString().trim();
        String provinsi = etProvinsi.getText().toString().trim();
        String kota = etKota.getText().toString().trim();
        String alamat = etAlamat.getText().toString().trim();
        String patokan = etPatokan.getText().toString().trim();
        String ukuranKamar = etUkuranKamar.getText().toString().trim();
        String totalKamarStr = etTotalKamar.getText().toString().trim();
        String kamarTersediaStr = etKamarTersedia.getText().toString().trim();
        String wa = etWhatsapp.getText().toString().trim();
        String latStr = etLat.getText().toString().trim();
        String lngStr = etLng.getText().toString().trim();
        String deskripsi = etDeskripsi.getText().toString().trim();

        if (nama.isEmpty()) {
            etNama.setError("Nama kost wajib diisi");
            etNama.requestFocus();
            return;
        }

        if (hargaStr.isEmpty()) {
            etHarga.setError("Harga wajib diisi");
            etHarga.requestFocus();
            return;
        }

        double harga;
        try {
            harga = Double.parseDouble(hargaStr);
            if (harga <= 0) {
                etHarga.setError("Harga harus lebih dari 0");
                return;
            }
        } catch (NumberFormatException e) {
            etHarga.setError("Format harga tidak valid");
            return;
        }

        if (provinsi.isEmpty()) {
            provinsi = "Riau";
        }
        if (kota.isEmpty()) {
            kota = "Pekanbaru";
        }

        if (alamat.isEmpty()) {
            etAlamat.setError("Alamat lengkap wajib diisi");
            etAlamat.requestFocus();
            return;
        }

        int totalKamar = 10;
        int kamarTersedia = 3;
        try {
            if (!totalKamarStr.isEmpty()) totalKamar = Integer.parseInt(totalKamarStr);
            if (!kamarTersediaStr.isEmpty()) kamarTersedia = Integer.parseInt(kamarTersediaStr);
        } catch (Exception ignored) {}

        if (kamarTersedia > totalKamar) {
            etKamarTersedia.setError("Kamar tersedia tidak boleh melebihi total kamar");
            etKamarTersedia.requestFocus();
            return;
        }

        if (spWilayah.getSelectedItemPosition() < 0 || spWilayah.getSelectedItemPosition() >= wilayahList.size()) {
            Toast.makeText(this, "Silakan pilih kelurahan", Toast.LENGTH_SHORT).show();
            return;
        }

        int idWilayah = wilayahList.get(spWilayah.getSelectedItemPosition()).getIdWilayah();

        // Tipe Kost
        TipeKost tipe = TipeKost.CAMPUR;
        if (rbPutri.isChecked()) tipe = TipeKost.PUTRI;
        else if (rbPutra.isChecked()) tipe = TipeKost.PUTRA;

        // Status Kost
        StatusKost status = StatusKost.TERSEDIA;
        if (kamarTersedia == 0 || rbPenuh.isChecked()) {
            status = StatusKost.PENUH;
        } else if (rbTidakAktif.isChecked()) {
            status = StatusKost.TIDAK_AKTIF;
        }

        double latitude = 0.463280;
        double longitude = 101.450123;
        try {
            if (!latStr.isEmpty()) latitude = Double.parseDouble(latStr);
            if (!lngStr.isEmpty()) longitude = Double.parseDouble(lngStr);
        } catch (Exception ignored) {}

        // Selected Fasilitas
        List<Integer> selectedFasilitas = new ArrayList<>();
        for (CheckBox cb : fasilitasCheckBoxes) {
            if (cb.isChecked()) {
                selectedFasilitas.add((int) cb.getTag());
            }
        }

        Kost kost = (editKostId != -1 && existingKost != null) ? existingKost : new Kost();
        kost.setNamaKost(nama);
        kost.setIdWilayah(idWilayah);
        kost.setProvinsi(provinsi);
        kost.setKota(kota);
        kost.setAlamat(alamat);
        kost.setPatokan(patokan);
        kost.setUkuranKamar(ukuranKamar.isEmpty() ? "3x4 m" : ukuranKamar);
        kost.setTotalKamar(totalKamar);
        kost.setKamarTersedia(kamarTersedia);
        kost.setHarga(harga);
        kost.setTipeKost(tipe);
        kost.setNoWhatsapp(wa);
        kost.setLatitude(latitude);
        kost.setLongitude(longitude);
        kost.setDeskripsi(deskripsi);
        kost.setStatus(status);

        // Associate owner ID
        int currentUserId = sessionManager.getUserId();
        if (kost.getIdPemilik() <= 0 && currentUserId > 0) {
            kost.setIdPemilik(currentUserId);
        }

        boolean isOwner = sessionManager.isPemilikKost() && !sessionManager.isDeveloper();
        if (isOwner) {
            kost.setVerificationStatus(com.carikostkita.data.model.KostVerificationStatus.PENDING);
            kost.setCatatanRevisi(null);
        } else {
            if (editKostId == -1) {
                kost.setVerificationStatus(com.carikostkita.data.model.KostVerificationStatus.APPROVED);
            }
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

        btnSave.setEnabled(false);
        if (editKostId == -1) {
            kostRepository.saveKostWithFotos(kost, selectedFasilitas, fotosToSave, new DataCallback<Long>() {
                @Override
                public void onSuccess(Long id) {
                    if (isOwner) {
                        activityLogRepository.logActivity(currentUserId, "SUBMIT_KOST",
                                "Pemilik mengajukan kost baru: " + kost.getNamaKost(), "kost", id.intValue());
                        AppDialogHelper.showSuccessDialog(AdminKostFormActivity.this, "Pengajuan Berhasil",
                                "Properti kost Anda berhasil dikirim dan masuk ke antrean verifikasi Developer / Admin sebelum dipublikasikan.",
                                AdminKostFormActivity.this::finish);
                    } else {
                        activityLogRepository.logActivity(currentUserId, "CREATE_KOST",
                                "Admin menambahkan kost baru: " + kost.getNamaKost(), "kost", id.intValue());
                        AppDialogHelper.showSuccessDialog(AdminKostFormActivity.this, "Berhasil Disimpan",
                                "Data properti kost dan foto berhasil disimpan ke sistem.",
                                AdminKostFormActivity.this::finish);
                    }
                }

                @Override
                public void onError(String message) {
                    btnSave.setEnabled(true);
                    AppDialogHelper.showErrorDialog(AdminKostFormActivity.this, "Gagal Menyimpan", message);
                }
            });
        } else {
            kost.setIdKost(editKostId);
            kostRepository.updateKostWithFotos(kost, selectedFasilitas, fotosToSave, new DataCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean ok) {
                    if (isOwner) {
                        activityLogRepository.logActivity(currentUserId, "RESUBMIT_KOST",
                                "Pemilik memperbarui & mengajukan ulang kost: " + kost.getNamaKost(), "kost", editKostId);
                        AppDialogHelper.showSuccessDialog(AdminKostFormActivity.this, "Perubahan Terkirim",
                                "Perbaikan data kost berhasil disimpan dan diajukan kembali ke antrean verifikasi Admin.",
                                AdminKostFormActivity.this::finish);
                    } else {
                        activityLogRepository.logActivity(currentUserId, "UPDATE_KOST",
                                "Admin memperbarui kost: " + kost.getNamaKost(), "kost", editKostId);
                        AppDialogHelper.showSuccessDialog(AdminKostFormActivity.this, "Perubahan Disimpan",
                                "Data properti kost dan galeri foto berhasil diperbarui.",
                                AdminKostFormActivity.this::finish);
                    }
                }

                @Override
                public void onError(String message) {
                    btnSave.setEnabled(true);
                    AppDialogHelper.showErrorDialog(AdminKostFormActivity.this, "Gagal Memperbarui", message);
                }
            });
        }
    }
}
