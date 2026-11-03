package com.carikostkita.ui.roommate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.carikostkita.R;
import com.carikostkita.data.model.RoommatePost;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.RoommateRepository;
import com.carikostkita.ui.adapter.RoommateAdapter;
import com.carikostkita.util.PageHeader;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.util.List;

public class RoommateActivity extends AppCompatActivity {

    private RoommateRepository repository;
    private RoommateAdapter adapter;

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvRoommates;
    private ProgressBar pbLoading;
    private View layoutEmpty;
    private ExtendedFloatingActionButton fabAdd;

    private TextView chipAll, chipUnri, chipUir, chipUin, chipPutra, chipPutri;

    private String selectedCampus = null;
    private String selectedGender = null;

    private final ActivityResultLauncher<Intent> addLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadRoommates(false);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roommate);

        repository = new RoommateRepository(this);

        // Header bind
        PageHeader.bind(findViewById(android.R.id.content), "Cari Teman Sekamar",
                "Hemat biaya kost hingga 50% di Pekanbaru");

        findViewById(R.id.btn_roommate_back).setOnClickListener(v -> finish());

        // Views
        swipeRefresh = findViewById(R.id.swipe_refresh_roommate);
        rvRoommates = findViewById(R.id.rv_roommates);
        pbLoading = findViewById(R.id.pb_roommate_loading);
        layoutEmpty = findViewById(R.id.layout_roommate_empty);
        fabAdd = findViewById(R.id.fab_add_roommate);

        chipAll = findViewById(R.id.chip_roommate_all);
        chipUnri = findViewById(R.id.chip_roommate_unri);
        chipUir = findViewById(R.id.chip_roommate_uir);
        chipUin = findViewById(R.id.chip_roommate_uin);
        chipPutra = findViewById(R.id.chip_roommate_putra);
        chipPutri = findViewById(R.id.chip_roommate_putri);

        // Setup RecyclerView
        rvRoommates.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RoommateAdapter(this);
        rvRoommates.setAdapter(adapter);

        // Setup filter clicks
        setupFilterChips();

        swipeRefresh.setOnRefreshListener(() -> loadRoommates(false));

        View.OnClickListener openPostAction = v -> {
            Intent intent = new Intent(this, AddRoommateActivity.class);
            addLauncher.launch(intent);
        };
        fabAdd.setOnClickListener(openPostAction);
        findViewById(R.id.btn_roommate_empty_post).setOnClickListener(openPostAction);

        loadRoommates(true);
    }

    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> {
            selectedCampus = null;
            selectedGender = null;
            updateChipStyles(chipAll);
            loadRoommates(false);
        });

        chipUnri.setOnClickListener(v -> {
            selectedCampus = "UNRI";
            updateChipStyles(chipUnri);
            loadRoommates(false);
        });

        chipUir.setOnClickListener(v -> {
            selectedCampus = "UIR";
            updateChipStyles(chipUir);
            loadRoommates(false);
        });

        chipUin.setOnClickListener(v -> {
            selectedCampus = "UIN";
            updateChipStyles(chipUin);
            loadRoommates(false);
        });

        chipPutra.setOnClickListener(v -> {
            selectedGender = "PUTRA";
            updateChipStyles(chipPutra);
            loadRoommates(false);
        });

        chipPutri.setOnClickListener(v -> {
            selectedGender = "PUTRI";
            updateChipStyles(chipPutri);
            loadRoommates(false);
        });
    }

    private void updateChipStyles(TextView selectedChip) {
        TextView[] allChips = {chipAll, chipUnri, chipUir, chipUin, chipPutra, chipPutri};
        for (TextView chip : allChips) {
            boolean isSel = (chip == selectedChip);
            chip.setBackgroundResource(isSel ? R.drawable.bg_badge_kategori_selected : R.drawable.bg_badge_kategori_unselected);
            chip.setTextColor(ContextCompat.getColor(this, isSel ? R.color.white : R.color.text_secondary));
        }
    }

    private void loadRoommates(boolean showSpinner) {
        if (showSpinner) pbLoading.setVisibility(View.VISIBLE);

        repository.getRoommates(selectedCampus, selectedGender, null, null, new DataCallback<List<RoommatePost>>() {
            @Override
            public void onSuccess(List<RoommatePost> data) {
                pbLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (data == null || data.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvRoommates.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    rvRoommates.setVisibility(View.VISIBLE);
                    adapter.setData(data);
                }
            }

            @Override
            public void onError(String message) {
                pbLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(RoommateActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
