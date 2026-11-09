package com.carikostkita.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.carikostkita.R;
import com.carikostkita.data.model.MaintenanceTicket;
import com.carikostkita.data.model.Tenant;
import com.carikostkita.data.remote.FastApiOwnerKmsService;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.ui.adapter.OwnerTenantAdapter;
import com.carikostkita.ui.adapter.OwnerTicketAdapter;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.SessionManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OwnerTenantActivity extends AppCompatActivity
        implements OwnerTenantAdapter.OnTenantActionListener, OwnerTicketAdapter.OnTicketActionListener {

    private long kostId = -1;
    private String kostName = "";

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private LinearLayout containerTenants;
    private LinearLayout containerTickets;
    private SwipeRefreshLayout swipeTenants;
    private SwipeRefreshLayout swipeTickets;
    private RecyclerView rvTenants;
    private RecyclerView rvTickets;
    private ProgressBar pbLoading;
    private View layoutEmpty;
    private ImageView ivEmpty;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;
    private ExtendedFloatingActionButton fabAction;

    private OwnerTenantAdapter tenantAdapter;
    private OwnerTicketAdapter ticketAdapter;
    private FastApiOwnerKmsService kmsService;
    private SessionManager sessionManager;

    private final List<Tenant> tenantList = new ArrayList<>();
    private final List<MaintenanceTicket> ticketList = new ArrayList<>();
    private int currentTab = 0; // 0: Tenants, 1: Tickets

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_tenant);

        kostId = getIntent().getLongExtra("EXTRA_KOST_ID", -1);
        kostName = getIntent().getStringExtra("EXTRA_KOST_NAME");
        if (kostName == null || kostName.isEmpty()) kostName = "Kost Kita";

        sessionManager = new SessionManager(this);
        kmsService = SupabaseClient.getInstance().createOwnerKmsService();

        initViews();
        loadTenants();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_kms);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tabLayout = findViewById(R.id.tab_kms);
        containerTenants = findViewById(R.id.container_kms_tenants);
        containerTickets = findViewById(R.id.container_kms_tickets);
        swipeTenants = findViewById(R.id.swipe_refresh_tenants);
        swipeTickets = findViewById(R.id.swipe_refresh_tickets);
        rvTenants = findViewById(R.id.rv_kms_tenants);
        rvTickets = findViewById(R.id.rv_kms_tickets);
        pbLoading = findViewById(R.id.pb_kms_loading);
        layoutEmpty = findViewById(R.id.layout_kms_empty);
        ivEmpty = findViewById(R.id.iv_kms_empty);
        tvEmptyTitle = findViewById(R.id.tv_kms_empty_title);
        tvEmptyDesc = findViewById(R.id.tv_kms_empty_desc);
        fabAction = findViewById(R.id.fab_kms_action);

        tenantAdapter = new OwnerTenantAdapter(this);
        tenantAdapter.setKostName(kostName);
        rvTenants.setLayoutManager(new LinearLayoutManager(this));
        rvTenants.setAdapter(tenantAdapter);

        ticketAdapter = new OwnerTicketAdapter(this);
        rvTickets.setLayoutManager(new LinearLayoutManager(this));
        rvTickets.setAdapter(ticketAdapter);

        swipeTenants.setOnRefreshListener(this::loadTenants);
        swipeTickets.setOnRefreshListener(this::loadTickets);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                if (currentTab == 0) {
                    containerTenants.setVisibility(View.VISIBLE);
                    containerTickets.setVisibility(View.GONE);
                    fabAction.setText("Tambah Penghuni");
                    loadTenants();
                } else {
                    containerTenants.setVisibility(View.GONE);
                    containerTickets.setVisibility(View.VISIBLE);
                    fabAction.setText("Catat Komplain");
                    loadTickets();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        fabAction.setOnClickListener(v -> {
            if (currentTab == 0) {
                showAddTenantDialog();
            } else {
                showAddTicketDialog();
            }
        });
    }

    private void loadTenants() {
        pbLoading.setVisibility(View.VISIBLE);
        String token = "Bearer " + sessionManager.getAccessToken();
        Long filterKostId = kostId > 0 ? kostId : null;

        kmsService.listTenants(token, filterKostId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbLoading.setVisibility(View.GONE);
                swipeTenants.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    tenantList.clear();
                    JsonArray arr = response.body().getAsJsonArray("data");
                    if (arr != null) {
                        for (JsonElement el : arr) {
                            if (el.isJsonObject()) {
                                JsonObject obj = el.getAsJsonObject();
                                Tenant t = new Tenant();
                                t.setId(obj.has("id") && !obj.get("id").isJsonNull() ? obj.get("id").getAsLong() : 0);
                                t.setKostId(obj.has("kost_id") && !obj.get("kost_id").isJsonNull() ? obj.get("kost_id").getAsLong() : 0);
                                if (obj.has("room_id") && !obj.get("room_id").isJsonNull()) {
                                    t.setRoomId(obj.get("room_id").getAsLong());
                                }
                                t.setName(obj.has("name") && !obj.get("name").isJsonNull() ? obj.get("name").getAsString() : "Penghuni");
                                t.setPhone(obj.has("phone") && !obj.get("phone").isJsonNull() ? obj.get("phone").getAsString() : "");
                                t.setDueDate(obj.has("due_date") && !obj.get("due_date").isJsonNull() ? obj.get("due_date").getAsInt() : 1);
                                t.setRentPrice(obj.has("rent_price") && !obj.get("rent_price").isJsonNull() ? obj.get("rent_price").getAsDouble() : 0);
                                t.setPaymentStatus(obj.has("payment_status") && !obj.get("payment_status").isJsonNull() ? obj.get("payment_status").getAsString() : "BELUM_BAYAR");
                                tenantList.add(t);
                            }
                        }
                    }
                    if (tenantList.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        tvEmptyTitle.setText("Belum Ada Penghuni");
                        tvEmptyDesc.setText("Catat penghuni kost agar mudah mengirim tagihan bulanan via WhatsApp.");
                        rvTenants.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvTenants.setVisibility(View.VISIBLE);
                        tenantAdapter.submitList(tenantList);
                    }
                } else {
                    Toast.makeText(OwnerTenantActivity.this, "Gagal memuat data penghuni", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                swipeTenants.setRefreshing(false);
                Toast.makeText(OwnerTenantActivity.this, "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTickets() {
        pbLoading.setVisibility(View.VISIBLE);
        String token = "Bearer " + sessionManager.getAccessToken();
        Long filterKostId = kostId > 0 ? kostId : null;

        kmsService.listTickets(token, filterKostId, null).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbLoading.setVisibility(View.GONE);
                swipeTickets.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    ticketList.clear();
                    JsonArray arr = response.body().getAsJsonArray("data");
                    if (arr != null) {
                        for (JsonElement el : arr) {
                            if (el.isJsonObject()) {
                                JsonObject obj = el.getAsJsonObject();
                                MaintenanceTicket mt = new MaintenanceTicket();
                                mt.setId(obj.has("id") && !obj.get("id").isJsonNull() ? obj.get("id").getAsLong() : 0);
                                mt.setKostId(obj.has("kost_id") && !obj.get("kost_id").isJsonNull() ? obj.get("kost_id").getAsLong() : 0);
                                mt.setTenantName(obj.has("tenant_name") && !obj.get("tenant_name").isJsonNull() ? obj.get("tenant_name").getAsString() : "Penghuni");
                                mt.setCategory(obj.has("category") && !obj.get("category").isJsonNull() ? obj.get("category").getAsString() : "Fasilitas");
                                mt.setTitle(obj.has("title") && !obj.get("title").isJsonNull() ? obj.get("title").getAsString() : "Komplain");
                                mt.setDescription(obj.has("description") && !obj.get("description").isJsonNull() ? obj.get("description").getAsString() : "");
                                mt.setStatus(obj.has("status") && !obj.get("status").isJsonNull() ? obj.get("status").getAsString() : "BARU");
                                mt.setCreatedAt(obj.has("created_at") && !obj.get("created_at").isJsonNull() ? obj.get("created_at").getAsString() : "");
                                ticketList.add(mt);
                            }
                        }
                    }
                    if (ticketList.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        tvEmptyTitle.setText("Tidak Ada Komplain");
                        tvEmptyDesc.setText("Semua fasilitas kost berfungsi baik dan tidak ada komplain aktif.");
                        rvTickets.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvTickets.setVisibility(View.VISIBLE);
                        ticketAdapter.submitList(ticketList);
                    }
                } else {
                    Toast.makeText(OwnerTenantActivity.this, "Gagal memuat komplain", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                swipeTickets.setRefreshing(false);
                Toast.makeText(OwnerTenantActivity.this, "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddTenantDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_tenant, null);
        EditText etName = dialogView.findViewById(R.id.et_tenant_add_name);
        EditText etPhone = dialogView.findViewById(R.id.et_tenant_add_phone);
        EditText etDueDate = dialogView.findViewById(R.id.et_tenant_add_due_date);
        EditText etPrice = dialogView.findViewById(R.id.et_tenant_add_price);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Tambah Penghuni Baru")
                .setView(dialogView)
                .setPositiveButton("Simpan", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String dueStr = etDueDate.getText().toString().trim();
                    String priceStr = etPrice.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Nama penghuni wajib diisi.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int due = 1;
                    try {
                        if (!dueStr.isEmpty()) due = Integer.parseInt(dueStr);
                    } catch (Exception ignored) {}

                    double price = 0;
                    try {
                        if (!priceStr.isEmpty()) price = Double.parseDouble(priceStr);
                    } catch (Exception ignored) {}

                    createTenantRequest(name, phone, due, price);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void createTenantRequest(String name, String phone, int dueDate, double price) {
        if (kostId <= 0) {
            Toast.makeText(this, "Pilih properti kost terlebih dahulu untuk mencatat penghuni.", Toast.LENGTH_SHORT).show();
            return;
        }
        pbLoading.setVisibility(View.VISIBLE);
        JsonObject body = new JsonObject();
        body.addProperty("kost_id", kostId);
        body.addProperty("name", name);
        body.addProperty("phone", phone);
        body.addProperty("due_date", dueDate);
        body.addProperty("rent_price", price);
        body.addProperty("payment_status", "BELUM_BAYAR");

        String token = "Bearer " + sessionManager.getAccessToken();
        kmsService.createTenant(token, body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(OwnerTenantActivity.this, "Penghuni berhasil ditambahkan", Toast.LENGTH_SHORT).show();
                    loadTenants();
                } else {
                    Toast.makeText(OwnerTenantActivity.this, "Gagal menambahkan (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(OwnerTenantActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddTicketDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_ticket, null);
        EditText etTenant = dialogView.findViewById(R.id.et_ticket_add_tenant);
        EditText etCat = dialogView.findViewById(R.id.et_ticket_add_category);
        EditText etTitle = dialogView.findViewById(R.id.et_ticket_add_title);
        EditText etDesc = dialogView.findViewById(R.id.et_ticket_add_desc);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Catat Komplain Fasilitas")
                .setView(dialogView)
                .setPositiveButton("Kirim", (d, w) -> {
                    String tenant = etTenant.getText().toString().trim();
                    String cat = etCat.getText().toString().trim();
                    String title = etTitle.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(this, "Judul komplain wajib diisi.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (tenant.isEmpty()) tenant = "Penghuni";
                    if (cat.isEmpty()) cat = "FASILITAS";

                    createTicketRequest(tenant, cat, title, desc);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void createTicketRequest(String tenant, String cat, String title, String desc) {
        if (kostId <= 0) {
            Toast.makeText(this, "Pilih properti kost terlebih dahulu untuk mencatat komplain.", Toast.LENGTH_SHORT).show();
            return;
        }
        pbLoading.setVisibility(View.VISIBLE);
        JsonObject body = new JsonObject();
        body.addProperty("kost_id", kostId);
        body.addProperty("tenant_name", tenant);
        body.addProperty("category", cat);
        body.addProperty("title", title);
        body.addProperty("description", desc);

        String token = "Bearer " + sessionManager.getAccessToken();
        kmsService.createTicket(token, body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(OwnerTenantActivity.this, "Komplain berhasil dicatat", Toast.LENGTH_SHORT).show();
                    loadTickets();
                } else {
                    Toast.makeText(OwnerTenantActivity.this, "Gagal mencatat (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(OwnerTenantActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onToggleStatus(Tenant tenant, int position) {
        String[] statuses = {"LUNAS", "BELUM_BAYAR", "JATUH_TEMPO"};
        int selected = 0;
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equalsIgnoreCase(tenant.getPaymentStatus())) {
                selected = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Status Pembayaran: " + tenant.getName())
                .setSingleChoiceItems(statuses, selected, (dialog, which) -> {
                    dialog.dismiss();
                    String newStatus = statuses[which];
                    updateTenantStatusRequest(tenant, newStatus);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void updateTenantStatusRequest(Tenant tenant, String newStatus) {
        pbLoading.setVisibility(View.VISIBLE);
        JsonObject body = new JsonObject();
        body.addProperty("payment_status", newStatus);

        String token = "Bearer " + sessionManager.getAccessToken();
        kmsService.updateTenant(token, tenant.getId(), body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(OwnerTenantActivity.this, "Status diubah ke " + newStatus, Toast.LENGTH_SHORT).show();
                    loadTenants();
                } else {
                    Toast.makeText(OwnerTenantActivity.this, "Gagal mengubah status (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(OwnerTenantActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDeleteTenant(Tenant tenant, int position) {
        AppDialogHelper.showDanger(this,
                "Hapus Penghuni",
                "Hapus data " + tenant.getName() + " dari daftar penghuni aktif?",
                "Hapus",
                () -> deleteTenantRequest(tenant));
    }

    private void deleteTenantRequest(Tenant tenant) {
        pbLoading.setVisibility(View.VISIBLE);
        String token = "Bearer " + sessionManager.getAccessToken();
        kmsService.deleteTenant(token, tenant.getId()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(OwnerTenantActivity.this, "Data penghuni dihapus", Toast.LENGTH_SHORT).show();
                    loadTenants();
                } else {
                    Toast.makeText(OwnerTenantActivity.this, "Gagal menghapus (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(OwnerTenantActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResolveTicket(MaintenanceTicket ticket, int position) {
        String nextStatus = ticket.isBaru() ? "PROSES" : "SELESAI";
        pbLoading.setVisibility(View.VISIBLE);

        JsonObject body = new JsonObject();
        body.addProperty("status", nextStatus);

        String token = "Bearer " + sessionManager.getAccessToken();
        kmsService.updateTicket(token, ticket.getId(), body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(OwnerTenantActivity.this, "Tiket ditandai " + nextStatus, Toast.LENGTH_SHORT).show();
                    loadTickets();
                } else {
                    Toast.makeText(OwnerTenantActivity.this, "Gagal memperbarui tiket", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(OwnerTenantActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
