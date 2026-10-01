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
import com.carikostkita.data.local.DatabaseHelper;
import com.carikostkita.data.local.dao.FasilitasDAO;
import com.carikostkita.data.model.Fasilitas;
import com.carikostkita.ui.adapter.FasilitasAdapter;
import java.util.List;

public class AdminFasilitasActivity extends AppCompatActivity {

    private EditText etNamaFasilitas;
    private RecyclerView rvFasilitas;
    private FasilitasAdapter adapter;
    private FasilitasDAO fasilitasDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_fasilitas);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        fasilitasDAO = new FasilitasDAO(dbHelper);

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
        List<Fasilitas> list = fasilitasDAO.findAll();
        adapter.submitList(list);
    }

    private void handleTambahFasilitas() {
        String nama = etNamaFasilitas.getText().toString().trim();
        if (nama.isEmpty()) {
            etNamaFasilitas.setError("Nama fasilitas wajib diisi");
            return;
        }

        long id = fasilitasDAO.insert(nama);
        if (id != -1) {
            Toast.makeText(this, "Fasilitas berhasil ditambahkan", Toast.LENGTH_SHORT).show();
            etNamaFasilitas.setText("");
            loadFasilitas();
        } else {
            Toast.makeText(this, "Fasilitas sudah ada atau gagal ditambahkan", Toast.LENGTH_SHORT).show();
        }
    }
}
