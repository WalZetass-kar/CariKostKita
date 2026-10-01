package com.carikostkita.ui.admin;

import android.os.Bundle;
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
import androidx.appcompat.app.AppCompatActivity;
import com.carikostkita.R;
import com.carikostkita.data.model.Fasilitas;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.StatusKost;
import android.text.Editable;
import android.text.TextWatcher;
import com.carikostkita.data.model.TipeKost;
import com.carikostkita.data.model.Wilayah;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;

public class AdminKostFormActivity extends AppCompatActivity {

    private TextView tvTitle;
    private EditText etNama;
    private RadioGroup rgTipe;
    private RadioButton rbPutra, rbPutri, rbCampur;
    private EditText etHarga;
    private EditText etProvinsi, etKota;
    private Spinner spWilayah;
    private EditText etAlamat;
    private EditText etUkuranKamar, etTotalKamar, etKamarTersedia;
    private TextView tvKamarTerisiInfo;
    private EditText etWhatsapp;
    private EditText etLat;
    private EditText etLng;
    private EditText etDeskripsi;
    private RadioGroup rgStatus;
    private RadioButton rbTersedia, rbPenuh, rbTidakAktif;
    private LinearLayout layoutFasilitasChecks;
    private Button btnSave;

    private KostRepository kostRepository;
    private List<Wilayah> wilayahList = new ArrayList<>();
    private List<Fasilitas> masterFasilitas = new ArrayList<>();
    private List<CheckBox> fasilitasCheckBoxes = new ArrayList<>();

    private int editKostId = -1;
    private Kost existingKost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_kost_form);

        kostRepository = new KostRepository(this);
        editKostId = getIntent().getIntExtra("kost_id", -1);

        initViews();
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
        etUkuranKamar = findViewById(R.id.et_form_ukuran_kamar);
        etTotalKamar = findViewById(R.id.et_form_total_kamar);
        etKamarTersedia = findViewById(R.id.et_form_kamar_tersedia);
        tvKamarTerisiInfo = findViewById(R.id.tv_form_kamar_terisi_info);
        etWhatsapp = findViewById(R.id.et_form_whatsapp);
        etLat = findViewById(R.id.et_form_lat);
        etLng = findViewById(R.id.et_form_lng);
        etDeskripsi = findViewById(R.id.et_form_deskripsi);
        rgStatus = findViewById(R.id.rg_form_status);
        rbTersedia = findViewById(R.id.rb_status_tersedia);
        rbPenuh = findViewById(R.id.rb_status_penuh);
        rbTidakAktif = findViewById(R.id.rb_status_tidak_aktif);
        layoutFasilitasChecks = findViewById(R.id.layout_form_fasilitas_checks);
        btnSave = findViewById(R.id.btn_form_save);

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
        new MaterialAlertDialogBuilder(this)
                .setTitle("Batalkan Pengisian?")
                .setMessage("Perubahan atau data yang sudah Anda masukkan belum disimpan. Anda yakin ingin keluar?")
                .setPositiveButton("Ya, Keluar", (dialog, which) -> finish())
                .setNegativeButton("Lanjutkan Mengisi", null)
                .show();
    }

    private void loadWilayahAndFasilitas() {
        // Load Wilayah
        kostRepository.getAllWilayah(new DataCallback<List<Wilayah>>() {
            @Override
            public void onSuccess(List<Wilayah> data) {
                wilayahList = data;
                List<String> labels = new ArrayList<>();
                for (Wilayah w : data) {
                    labels.add(w.getKelurahan() + " (" + w.getKecamatan() + ")");
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AdminKostFormActivity.this, android.R.layout.simple_spinner_dropdown_item, labels);
                spWilayah.setAdapter(adapter);

                if (editKostId != -1) {
                    checkAndPopulateEdit();
                }
            }

            @Override
            public void onError(String message) {}
        });

        // Load Fasilitas
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
                existingKost = kost;
                etNama.setText(kost.getNamaKost());
                etHarga.setText(String.valueOf((long) kost.getHarga()));
                if (kost.getProvinsi() != null && !kost.getProvinsi().isEmpty()) {
                    etProvinsi.setText(kost.getProvinsi());
                }
                if (kost.getKota() != null && !kost.getKota().isEmpty()) {
                    etKota.setText(kost.getKota());
                }
                etAlamat.setText(kost.getAlamat());
                if (kost.getUkuranKamar() != null && !kost.getUkuranKamar().isEmpty()) {
                    etUkuranKamar.setText(kost.getUkuranKamar());
                }
                etTotalKamar.setText(String.valueOf(kost.getTotalKamar() > 0 ? kost.getTotalKamar() : 10));
                etKamarTersedia.setText(String.valueOf(kost.getKamarTersedia()));
                updateRoomVacancyCalculation();

                etWhatsapp.setText(kost.getNoWhatsapp());
                etLat.setText(String.valueOf(kost.getLatitude()));
                etLng.setText(String.valueOf(kost.getLongitude()));
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

        double latitude = 0.4632801;
        double longitude = 101.4501234;
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

        btnSave.setEnabled(false);
        if (editKostId == -1) {
            kostRepository.saveKost(kost, selectedFasilitas, new DataCallback<Long>() {
                @Override
                public void onSuccess(Long id) {
                    Toast.makeText(AdminKostFormActivity.this, "Kost berhasil disimpan!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String message) {
                    btnSave.setEnabled(true);
                    Toast.makeText(AdminKostFormActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            kost.setIdKost(editKostId);
            kostRepository.updateKost(kost, selectedFasilitas, new DataCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean ok) {
                    Toast.makeText(AdminKostFormActivity.this, "Perubahan kost berhasil disimpan!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String message) {
                    btnSave.setEnabled(true);
                    Toast.makeText(AdminKostFormActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
