package com.carikostkita.ui.admin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.carikostkita.BuildConfig;
import com.carikostkita.R;
import com.carikostkita.data.model.Advertisement;
import com.carikostkita.data.remote.FastApiAdminAdsService;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.ui.adapter.AdminAdsAdapter;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.ImageLoaderUtil;
import com.carikostkita.util.SessionManager;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
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
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Dialog views references for image picker callback
    private ImageView currentDialogPreview;
    private View currentDialogPlaceholder;
    private ProgressBar currentDialogPb;
    private EditText currentDialogEtImage;
    private TextView currentDialogTvStatus;

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadAdImageFromUri(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_ads);

        sessionManager = new SessionManager(this);
        adsService = SupabaseClient.getInstance().createAdminAdsService();

        initViews();
        loadAds();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
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
        ImageView ivPreview = dialogView.findViewById(R.id.iv_ad_preview);
        View placeholder = dialogView.findViewById(R.id.layout_preview_placeholder);
        ProgressBar pbBanner = dialogView.findViewById(R.id.pb_upload_banner);
        MaterialButton btnPickImage = dialogView.findViewById(R.id.btn_pick_ad_image);
        TextView tvStatus = dialogView.findViewById(R.id.tv_upload_status);
        AutoCompleteTextView actvPlatform = dialogView.findViewById(R.id.actv_ad_platform);
        AutoCompleteTextView actvPosition = dialogView.findViewById(R.id.actv_ad_position);
        MaterialSwitch switchActive = dialogView.findViewById(R.id.switch_ad_active);

        // Keep references for gallery upload
        currentDialogPreview = ivPreview;
        currentDialogPlaceholder = placeholder;
        currentDialogPb = pbBanner;
        currentDialogEtImage = etImage;
        currentDialogTvStatus = tvStatus;

        // Platform items
        String[] platforms = new String[]{"Semua Platform", "Aplikasi Mobile", "Website"};
        ArrayAdapter<String> platformAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, platforms);
        actvPlatform.setAdapter(platformAdapter);

        // Position items
        String[] positions = new String[]{"Top (Banner Utama)", "Popup (Pop-up Promo)", "Middle (Tengah)"};
        ArrayAdapter<String> positionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, positions);
        actvPosition.setAdapter(positionAdapter);

        btnPickImage.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Tambah Iklan / Banner Baru")
                .setView(dialogView)
                .setPositiveButton("Simpan", (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    String img = etImage.getText().toString().trim();
                    String target = etTarget.getText().toString().trim();
                    String platform = actvPlatform.getText().toString().trim();
                    String position = actvPosition.getText().toString().trim();
                    boolean isActive = switchActive.isChecked();

                    if (title.isEmpty()) {
                        Toast.makeText(this, "Judul iklan wajib diisi.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createAdRequest(title, img, target, platform, position, isActive);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void uploadAdImageFromUri(Uri uri) {
        if (currentDialogPb != null) currentDialogPb.setVisibility(View.VISIBLE);
        if (currentDialogTvStatus != null) currentDialogTvStatus.setText("Mengunggah...");

        executorService.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    mainHandler.post(() -> {
                        if (currentDialogPb != null) currentDialogPb.setVisibility(View.GONE);
                        if (currentDialogTvStatus != null) currentDialogTvStatus.setText("Gagal membuka berkas");
                        Toast.makeText(this, "Gagal membaca berkas gambar", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 1;
                Bitmap bmp = BitmapFactory.decodeStream(inputStream, null, opts);
                inputStream.close();

                if (bmp == null) {
                    mainHandler.post(() -> {
                        if (currentDialogPb != null) currentDialogPb.setVisibility(View.GONE);
                        if (currentDialogTvStatus != null) currentDialogTvStatus.setText("Format tidak didukung");
                        Toast.makeText(this, "Format gambar tidak didukung", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                int maxDim = Math.max(bmp.getWidth(), bmp.getHeight());
                if (maxDim > 1280) {
                    float scale = 1280f / maxDim;
                    Bitmap scaled = Bitmap.createScaledBitmap(bmp, Math.round(bmp.getWidth() * scale), Math.round(bmp.getHeight() * scale), true);
                    if (scaled != bmp) bmp.recycle();
                    bmp = scaled;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                bmp.recycle();
                byte[] bytes = baos.toByteArray();

                String filename = "banner_ad_" + System.currentTimeMillis() + ".jpg";
                RequestBody fileBody = RequestBody.create(bytes, MediaType.parse("image/jpeg"));
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", filename, fileBody)
                        .build();

                String uploadUrl = BuildConfig.API_BASE_URL.replaceAll("/+$", "") + "/api/v1/upload-image";
                Request request = new Request.Builder()
                        .url(uploadUrl)
                        .post(requestBody)
                        .build();

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build();

                try (okhttp3.Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String respStr = response.body().string();
                        JsonObject json = new Gson().fromJson(respStr, JsonObject.class);
                        if (json != null && json.has("url")) {
                            String publicUrl = json.get("url").getAsString();
                            mainHandler.post(() -> {
                                if (currentDialogPb != null) currentDialogPb.setVisibility(View.GONE);
                                if (currentDialogPlaceholder != null) currentDialogPlaceholder.setVisibility(View.GONE);
                                if (currentDialogTvStatus != null) currentDialogTvStatus.setText("Foto siap!");
                                if (currentDialogEtImage != null) currentDialogEtImage.setText(publicUrl);
                                if (currentDialogPreview != null) {
                                    ImageLoaderUtil.loadImage(currentDialogPreview, publicUrl);
                                }
                                Toast.makeText(AdminAdsActivity.this, "Banner berhasil diunggah!", Toast.LENGTH_SHORT).show();
                            });
                            return;
                        }
                    }
                    mainHandler.post(() -> {
                        if (currentDialogPb != null) currentDialogPb.setVisibility(View.GONE);
                        if (currentDialogTvStatus != null) currentDialogTvStatus.setText("Unggah gagal (" + response.code() + ")");
                        Toast.makeText(AdminAdsActivity.this, "Gagal mengunggah banner", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (currentDialogPb != null) currentDialogPb.setVisibility(View.GONE);
                    if (currentDialogTvStatus != null) currentDialogTvStatus.setText("Error unggah");
                    Toast.makeText(AdminAdsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void createAdRequest(String title, String imageUrl, String targetUrl, String platformLabel, String positionLabel, boolean isActive) {
        pbLoading.setVisibility(View.VISIBLE);
        JsonObject body = new JsonObject();
        body.addProperty("title", title);
        body.addProperty("image_url", imageUrl);
        body.addProperty("target_url", targetUrl);
        body.addProperty("is_active", isActive);

        // Normalize platform
        String platform = "Semua";
        if (platformLabel.contains("Mobile")) platform = "Mobile";
        else if (platformLabel.contains("Website")) platform = "Website";
        body.addProperty("platform", platform);

        // Normalize position & ad_type
        String pos = "Top";
        String adType = "Banner";
        if (positionLabel.contains("Popup")) {
            pos = "Popup";
            adType = "Popup";
        } else if (positionLabel.contains("Middle")) {
            pos = "Middle";
        }
        body.addProperty("position", pos);
        body.addProperty("ad_type", adType);

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
