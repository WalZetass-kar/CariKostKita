package com.carikostkita.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.KostVerificationStatus;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.ui.adapter.KostAdapter;
import com.carikostkita.ui.detail.DetailKostActivity;
import com.carikostkita.util.SessionManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Halaman pratinjau tunggal bagi Pemilik Kost untuk melihat bagaimana kost
 * miliknya ditampilkan di katalog publik kepada para pencari kost.
 * Akses tunggal dibuka melalui: Profil Pemilik -> Tombol "Lihat Katalog Publik".
 */
public class OwnerKatalogPublikActivity extends AppCompatActivity implements KostAdapter.OnKostClickListener {

    private RecyclerView rvKatalog;
    private ProgressBar pbLoading;
    private LinearLayout layoutEmpty;
    private KostAdapter adapter;
    private KostRepository kostRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_katalog_publik);

        sessionManager = new SessionManager(this);
        kostRepository = new KostRepository(this);

        if (!sessionManager.isLoggedIn() || !sessionManager.isPemilikKost()) {
            Toast.makeText(this, "Akses ditolak: Khusus Pemilik Kost", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageButton btnBack = findViewById(R.id.btn_katalog_back);
        btnBack.setOnClickListener(v -> finish());

        rvKatalog = findViewById(R.id.rv_katalog_publik);
        pbLoading = findViewById(R.id.pb_katalog_loading);
        layoutEmpty = findViewById(R.id.layout_katalog_empty);

        adapter = new KostAdapter(this);
        rvKatalog.setLayoutManager(new LinearLayoutManager(this));
        rvKatalog.setAdapter(adapter);

        loadPublicKostList();
    }

    private void loadPublicKostList() {
        pbLoading.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        rvKatalog.setVisibility(View.GONE);

        int ownerId = sessionManager.getUserId();
        kostRepository.getKostByPemilik(ownerId, new DataCallback<List<Kost>>() {
            @Override
            public void onSuccess(List<Kost> data) {
                pbLoading.setVisibility(View.GONE);
                List<Kost> approvedKosts = new ArrayList<>();
                for (Kost k : data) {
                    if (k.getVerificationStatus() == KostVerificationStatus.APPROVED) {
                        approvedKosts.add(k);
                    }
                }

                if (approvedKosts.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvKatalog.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    rvKatalog.setVisibility(View.VISIBLE);
                    adapter.submitList(approvedKosts);
                }
            }

            @Override
            public void onError(String message) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(OwnerKatalogPublikActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onKostClick(Kost kost) {
        Intent intent = new Intent(this, DetailKostActivity.class);
        intent.putExtra("kost_id", kost.getIdKost());
        startActivity(intent);
    }

    @Override
    public void onFavoriteToggle(Kost kost, int position) {
        // Pemilik tidak perlu memfavoritkan kost sendiri
    }
}
