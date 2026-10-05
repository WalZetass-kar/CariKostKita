package com.carikostkita.ui.admin;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.Fasilitas;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.ui.adapter.FasilitasAdapter;
import com.carikostkita.util.SessionManager;
import java.util.ArrayList;
import java.util.List;

public class AdminFasilitasActivity extends AppCompatActivity {

    private EditText etNamaFasilitas;
    private RecyclerView rvFasilitas;
    private FasilitasAdapter adapter;
    private KostRepository kostRepository;
    private final List<Fasilitas> currentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn() || !sessionManager.isDeveloper()) {
            Toast.makeText(this, "Akses ditolak: Hanya Developer / Super Admin yang dapat mengelola fasilitas", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_admin_fasilitas);

        kostRepository = new KostRepository(this);

        etNamaFasilitas = findViewById(R.id.et_fasilitas_nama);
        rvFasilitas = findViewById(R.id.rv_admin_fasilitas);
        Button btnTambah = findViewById(R.id.btn_fasilitas_tambah);
        ImageButton btnBack = findViewById(R.id.btn_fasilitas_back);

        btnBack.setOnClickListener(v -> finish());

        adapter = new FasilitasAdapter();
        rvFasilitas.setLayoutManager(new LinearLayoutManager(this));
        rvFasilitas.setAdapter(adapter);

        btnTambah.setOnClickListener(v -> handleTambahFasilitas());

        loadFasilitas();
    }

    private void loadFasilitas() {
        kostRepository.getAllFasilitas(new DataCallback<List<Fasilitas>>() {
            @Override
            public void onSuccess(List<Fasilitas> data) {
                currentList.clear();
                currentList.addAll(data);
                adapter.submitList(new ArrayList<>(currentList));
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AdminFasilitasActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleTambahFasilitas() {
        String nama = etNamaFasilitas.getText().toString().trim();
        if (nama.isEmpty()) {
            etNamaFasilitas.setError("Nama fasilitas wajib diisi");
            return;
        }

        int newId = currentList.size() + 1;
        currentList.add(new Fasilitas(newId, nama));
        adapter.submitList(new ArrayList<>(currentList));
        etNamaFasilitas.setText("");
        Toast.makeText(this, "Fasilitas berhasil ditambahkan", Toast.LENGTH_SHORT).show();
    }
}
