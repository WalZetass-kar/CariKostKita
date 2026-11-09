package com.carikostkita.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.carikostkita.R;
import com.carikostkita.data.model.Advertisement;
import com.carikostkita.data.remote.FastApiAdminAdsService;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.ui.adapter.AdminAdsAdapter;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.SessionManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminAdsActivity extends AppCompatActivity implements AdminAdsAdapter.OnAdActionListener {

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvAds;
    private ProgressBar pbLoading;
    private View layoutEmpty;
    private ExtendedFloatingActionButton fabAddAd;

    private AdminAdsAdapter adapter;
    private FastApiAdminAdsService adsService;
    private SessionManager sessionManager;
    private final List<Advertisement> adList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_ads);

        sessionManager = new SessionManager(this);
        adsService = SupabaseClient.getInstance().createAdminAdsService();

        initViews();
        loadAds();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_admin_ads);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        swipeRefresh = findViewById(R.id.swipe_refresh_ads);
        rvAds = findViewById(R.id.rv_admin_ads);
        pbLoading = findViewById(R.id.pb_ads_loading);
        layoutEmpty = findViewById(R.id.layout_ads_empty);
        fabAddAd = findViewById(R.id.fab_add_ad);

        adapter = new AdminAdsAdapter(this);
        rvAds.setLayoutManager(new LinearLayoutManager(this));
        rvAds.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadAds);
        fabAddAd.setOnClickListener(v -> showAddAdDialog());
    }

    private void loadAds() {
        pbLoading.setVisibility(View.VISIBLE);
        String token = "Bearer " + sessionManager.getAccessToken();

        adsService.listAds(token, 1, 50).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    adList.clear();
                    JsonArray arr = response.body().getAsJsonArray("data");
                    if (arr != null) {
                        for (JsonElement el : arr) {
                            if (el.isJsonObject()) {
                                JsonObject obj = el.getAsJsonObject();
                                Advertisement ad = new Advertisement();
                                ad.setId(obj.has("id") && !obj.get("id").isJsonNull() ? obj.get("id").getAsLong() : 0);
                                ad.setTitle(obj.has("title") && !obj.get("title").isJsonNull() ? obj.get("title").getAsString() : "Iklan");
                                ad.setImageUrl(obj.has("image_url") && !obj.get("image_url").isJsonNull() ? obj.get("image_url").getAsString() : "");
                                ad.setTargetUrl(obj.has("target_url") && !obj.get("target_url").isJsonNull() ? obj.get("target_url").getAsString() : "");
                                ad.setActive(obj.has("is_active") && !obj.get("is_active").isJsonNull() && obj.get("is_active").getAsBoolean());
                                ad.setImpressions(obj.has("impressions") && !obj.get("impressions").isJsonNull() ? obj.get("impressions").getAsInt() : 0);
                                ad.setClicks(obj.has("clicks") && !obj.get("clicks").isJsonNull() ? obj.get("clicks").getAsInt() : 0);
                                adList.add(ad);
                            }
                        }
                    }

                    if (adList.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvAds.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvAds.setVisibility(View.VISIBLE);
                        adapter.submitList(adList);
                    }
                } else {
                    Toast.makeText(AdminAdsActivity.this, "Gagal memuat iklan", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(AdminAdsActivity.this, "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddAdDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_ad, null);
        EditText etTitle = dialogView.findViewById(R.id.et_ad_add_title);
        EditText etImage = dialogView.findViewById(R.id.et_ad_add_image);
        EditText etTarget = dialogView.findViewById(R.id.et_ad_add_target);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Tambah Iklan / Banner Baru")
                .setView(dialogView)
                .setPositiveButton("Simpan", (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    String img = etImage.getText().toString().trim();
                    String target = etTarget.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(this, "Judul iklan wajib diisi.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createAdRequest(title, img, target);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void createAdRequest(String title, String imageUrl, String targetUrl) {
        pbLoading.setVisibility(View.VISIBLE);
        JsonObject body = new JsonObject();
        body.addProperty("title", title);
        body.addProperty("image_url", imageUrl);
        body.addProperty("target_url", targetUrl);
        body.addProperty("is_active", true);

        String token = "Bearer " + sessionManager.getAccessToken();
        adsService.createAd(token, body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(AdminAdsActivity.this, "Iklan berhasil ditambahkan", Toast.LENGTH_SHORT).show();
                    loadAds();
                } else {
                    Toast.makeText(AdminAdsActivity.this, "Gagal menambahkan iklan (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(AdminAdsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onToggleActive(Advertisement ad, boolean isActive, int position) {
        pbLoading.setVisibility(View.VISIBLE);
        JsonObject body = new JsonObject();
        body.addProperty("is_active", isActive);

        String token = "Bearer " + sessionManager.getAccessToken();
        adsService.updateAd(token, ad.getId(), body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    ad.setActive(isActive);
                    Toast.makeText(AdminAdsActivity.this, "Status iklan diperbarui", Toast.LENGTH_SHORT).show();
                } else {
                    loadAds();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                loadAds();
            }
        });
    }

    @Override
    public void onDeleteAd(Advertisement ad, int position) {
        AppDialogHelper.showDanger(this,
                "Hapus Iklan",
                "Hapus banner \"" + ad.getTitle() + "\" secara permanen?",
                "Hapus",
                () -> deleteAdRequest(ad));
    }

    private void deleteAdRequest(Advertisement ad) {
        pbLoading.setVisibility(View.VISIBLE);
        String token = "Bearer " + sessionManager.getAccessToken();
        adsService.deleteAd(token, ad.getId()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(AdminAdsActivity.this, "Iklan berhasil dihapus", Toast.LENGTH_SHORT).show();
                    loadAds();
                } else {
                    Toast.makeText(AdminAdsActivity.this, "Gagal menghapus iklan", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(AdminAdsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
