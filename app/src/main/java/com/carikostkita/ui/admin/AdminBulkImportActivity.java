package com.carikostkita.ui.admin;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.carikostkita.BuildConfig;
import com.carikostkita.R;
import com.carikostkita.data.remote.FastApiAdminBulkImportService;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fitur Bulk Import Data Kost (Excel / CSV) khusus Super Admin / Developer.
 * Menghubungkan aplikasi mobile langsung ke backend FastAPI bulk import pipeline.
 */
public class AdminBulkImportActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private FastApiAdminBulkImportService bulkImportService;

    private ProgressBar pbHeader;
    private TextView tvSelectedFilename;
    private TextView tvSelectedFilesize;
    private MaterialButton btnPickFile;
    private MaterialButton btnDownloadTemplate;
    private MaterialButton btnPreviewImport;
    private RadioGroup rgDuplicateAction;

    private View layoutPreviewContainer;
    private TextView tvBadgeTotal;
    private TextView tvBadgeValid;
    private TextView tvBadgeErrors;
    private TextView tvPreviewDetail;
    private MaterialButton btnCommitImport;

    private Uri selectedFileUri = null;
    private String selectedFileName = null;
    private String selectedFileBase64 = null;
    private JsonArray cachedBatchRows = null;

    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    processSelectedFile(uri);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn() || !sessionManager.isDeveloper()) {
            Toast.makeText(this, "Akses ditolak: Khusus Developer / Super Admin", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_admin_bulk_import);
        bulkImportService = SupabaseClient.getInstance().createAdminBulkImportService();

        initViews();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_bulk_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        pbHeader = findViewById(R.id.pb_bulk_header);
        tvSelectedFilename = findViewById(R.id.tv_selected_filename);
        tvSelectedFilesize = findViewById(R.id.tv_selected_filesize);
        btnPickFile = findViewById(R.id.btn_pick_file);
        btnDownloadTemplate = findViewById(R.id.btn_download_template);
        btnPreviewImport = findViewById(R.id.btn_preview_import);
        rgDuplicateAction = findViewById(R.id.rg_duplicate_action);

        layoutPreviewContainer = findViewById(R.id.layout_preview_container);
        tvBadgeTotal = findViewById(R.id.tv_badge_total);
        tvBadgeValid = findViewById(R.id.tv_badge_valid);
        tvBadgeErrors = findViewById(R.id.tv_badge_errors);
        tvPreviewDetail = findViewById(R.id.tv_preview_detail);
        btnCommitImport = findViewById(R.id.btn_commit_import);

        btnPickFile.setOnClickListener(v -> {
            String[] mimeTypes = new String[]{
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel",
                    "text/csv",
                    "text/comma-separated-values",
                    "application/csv",
                    "*/*"
            };
            filePickerLauncher.launch(mimeTypes);
        });

        btnDownloadTemplate.setOnClickListener(v -> downloadTemplate());
        btnPreviewImport.setOnClickListener(v -> executePreview());
        btnCommitImport.setOnClickListener(v -> executeCommit());
    }

    private void downloadTemplate() {
        try {
            String url = BuildConfig.API_BASE_URL.replaceAll("/+$", "") + "/api/v1/admin/kosts/import/template";
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle("Template Import Kost");
            request.setDescription("Mengunduh template spreadsheet resmi CariKostKita...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Template_Import_Kost.xlsx");

            String token = sessionManager.getAccessToken();
            if (token != null && !token.isEmpty()) {
                request.addRequestHeader("Authorization", "Bearer " + token);
            }

            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(this, "Unduhan dimulai. Cek panel notifikasi.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            // Fallback buka via browser
            String url = BuildConfig.API_BASE_URL.replaceAll("/+$", "") + "/api/v1/admin/kosts/import/template";
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }

    private void processSelectedFile(Uri uri) {
        selectedFileUri = uri;
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            long fileSize = 0;
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIndex != -1) selectedFileName = cursor.getString(nameIndex);
                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex);
                cursor.close();
            }
            if (selectedFileName == null) selectedFileName = "data_kost.xlsx";

            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) {
                Toast.makeText(this, "Gagal membuka berkas terpilih", Toast.LENGTH_SHORT).show();
                return;
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] bytes = buffer.toByteArray();
            is.close();

            if (bytes.length > 10 * 1024 * 1024) {
                Toast.makeText(this, "Ukuran berkas terlalu besar (Maksimal 10MB)", Toast.LENGTH_LONG).show();
                return;
            }

            selectedFileBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP);

            tvSelectedFilename.setText(selectedFileName);
            String sizeStr = (bytes.length / 1024) + " KB";
            tvSelectedFilesize.setText("Ukuran: " + sizeStr + " • Siap dipratinjau");
            btnPreviewImport.setEnabled(true);
            layoutPreviewContainer.setVisibility(View.GONE);
            cachedBatchRows = null;

        } catch (Exception e) {
            Toast.makeText(this, "Gagal membaca berkas: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void executePreview() {
        if (selectedFileBase64 == null || selectedFileName == null) {
            Toast.makeText(this, "Silakan pilih berkas spreadsheet terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = sessionManager.getAccessToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Sesi login tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }

        pbHeader.setVisibility(View.VISIBLE);
        btnPreviewImport.setEnabled(false);

        JsonObject body = new JsonObject();
        body.addProperty("file_name", selectedFileName);
        body.addProperty("file_base64", selectedFileBase64);

        bulkImportService.previewBulkImport("Bearer " + token, body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbHeader.setVisibility(View.GONE);
                btnPreviewImport.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    displayPreviewResult(response.body());
                } else {
                    String errorMsg = "Validasi gagal: Kode HTTP " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    AppDialogHelper.showError(AdminBulkImportActivity.this, "Gagal Validasi Berkas", errorMsg);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbHeader.setVisibility(View.GONE);
                btnPreviewImport.setEnabled(true);
                AppDialogHelper.showError(AdminBulkImportActivity.this, "Koneksi Gagal", t.getMessage());
            }
        });
    }

    private void displayPreviewResult(JsonObject body) {
        layoutPreviewContainer.setVisibility(View.VISIBLE);

        JsonObject summary = body.has("summary") && body.get("summary").isJsonObject()
                ? body.getAsJsonObject("summary") : new JsonObject();

        int total = summary.has("total_rows") ? summary.get("total_rows").getAsInt() : 0;
        int valid = summary.has("valid_rows") ? summary.get("valid_rows").getAsInt() : 0;
        int errors = summary.has("error_rows") ? summary.get("error_rows").getAsInt() : 0;
        int warnings = summary.has("warning_rows") ? summary.get("warning_rows").getAsInt() : 0;

        tvBadgeTotal.setText("Total: " + total);
        tvBadgeValid.setText("Valid: " + valid);
        tvBadgeErrors.setText("Error: " + errors);

        if (body.has("batch_rows") && body.get("batch_rows").isJsonArray()) {
            cachedBatchRows = body.getAsJsonArray("batch_rows");
        }

        StringBuilder previewText = new StringBuilder();
        previewText.append("Status Validasi:\n");
        previewText.append("• Baris Valid: ").append(valid).append(" data siap diimpor\n");
        previewText.append("• Baris Error: ").append(errors).append(" baris tidak valid\n");
        if (warnings > 0) {
            previewText.append("• Peringatan: ").append(warnings).append(" data perlu perhatian\n");
        }

        if (body.has("preview_rows") && body.get("preview_rows").isJsonArray()) {
            JsonArray rows = body.getAsJsonArray("preview_rows");
            previewText.append("\nSampel Data Pertama:\n");
            int maxShow = Math.min(3, rows.size());
            for (int i = 0; i < maxShow; i++) {
                JsonElement el = rows.get(i);
                if (el.isJsonObject()) {
                    JsonObject r = el.getAsJsonObject();
                    String name = r.has("nama_kost") ? r.get("nama_kost").getAsString() : "-";
                    String tipe = r.has("tipe_kost") ? r.get("tipe_kost").getAsString() : "-";
                    String kota = r.has("kota") ? r.get("kota").getAsString() : "-";
                    previewText.append(i + 1).append(". ").append(name)
                            .append(" (").append(tipe).append(", ").append(kota).append(")\n");
                }
            }
        }

        tvPreviewDetail.setText(previewText.toString());

        btnCommitImport.setEnabled(valid > 0 && cachedBatchRows != null && cachedBatchRows.size() > 0);
        if (valid == 0) {
            btnCommitImport.setText("Tidak Ada Data Valid untuk Diimpor");
        } else {
            btnCommitImport.setText("Eksekusi Impor " + valid + " Data Kost");
        }
    }

    private void executeCommit() {
        if (cachedBatchRows == null || cachedBatchRows.size() == 0) {
            Toast.makeText(this, "Tidak ada data baris valid untuk diimpor", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = sessionManager.getAccessToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Sesi login tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isUpdate = rgDuplicateAction.getCheckedRadioButtonId() == R.id.rb_action_update;
        String actionStr = isUpdate ? "update" : "skip";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Konfirmasi Impor Kost")
                .setMessage("Apakah Anda yakin ingin mengeksekusi impor " + cachedBatchRows.size()
                        + " data kost ke dalam database?\n\nTindakan Duplikasi: " + (isUpdate ? "Perbarui Data" : "Lewati Duplikat"))
                .setNegativeButton("Batal", null)
                .setPositiveButton("Eksekusi Sekarang", (d, w) -> {
                    pbHeader.setVisibility(View.VISIBLE);
                    btnCommitImport.setEnabled(false);

                    JsonObject body = new JsonObject();
                    body.addProperty("file_name", selectedFileName != null ? selectedFileName : "import_kost.xlsx");
                    body.addProperty("duplicate_action", actionStr);
                    body.add("batch_rows", cachedBatchRows);

                    bulkImportService.commitBulkImport("Bearer " + token, body).enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            pbHeader.setVisibility(View.GONE);
                            btnCommitImport.setEnabled(true);

                            if (response.isSuccessful() && response.body() != null) {
                                JsonObject res = response.body();
                                JsonObject summary = res.has("summary") && res.get("summary").isJsonObject()
                                        ? res.getAsJsonObject("summary") : new JsonObject();

                                int inserted = summary.has("inserted") ? summary.get("inserted").getAsInt() : 0;
                                int updated = summary.has("updated") ? summary.get("updated").getAsInt() : 0;
                                int skipped = summary.has("skipped") ? summary.get("skipped").getAsInt() : 0;
                                int failed = summary.has("failed") ? summary.get("failed").getAsInt() : 0;

                                String msg = "Impor Berhasil Diselesaikan!\n\n"
                                        + "• Baru Ditambahkan: " + inserted + " properti\n"
                                        + "• Diperbarui: " + updated + " properti\n"
                                        + "• Dilewati: " + skipped + " properti\n"
                                        + (failed > 0 ? ("• Gagal: " + failed + " properti\n") : "");

                                new MaterialAlertDialogBuilder(AdminBulkImportActivity.this)
                                        .setTitle("Hasil Impor Data Kost")
                                        .setMessage(msg)
                                        .setPositiveButton("Selesai", (dialog, which) -> finish())
                                        .setCancelable(false)
                                        .show();

                            } else {
                                String errorMsg = "Gagal commit impor: Kode HTTP " + response.code();
                                try {
                                    if (response.errorBody() != null) {
                                        errorMsg = response.errorBody().string();
                                    }
                                } catch (Exception ignored) {}
                                AppDialogHelper.showError(AdminBulkImportActivity.this, "Gagal Eksekusi Impor", errorMsg);
                            }
                        }

                        @Override
                        public void onFailure(Call<JsonObject> call, Throwable t) {
                            pbHeader.setVisibility(View.GONE);
                            btnCommitImport.setEnabled(true);
                            AppDialogHelper.showError(AdminBulkImportActivity.this, "Koneksi Gagal", t.getMessage());
                        }
                    });
                })
                .show();
    }
}
