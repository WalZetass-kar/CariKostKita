package com.carikostkita.ui.admin;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
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
import com.carikostkita.data.model.Room;
import com.carikostkita.data.remote.FastApiOwnerRoomService;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.ui.adapter.OwnerRoomAdapter;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.SessionManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OwnerRoomActivity extends AppCompatActivity implements OwnerRoomAdapter.OnRoomActionListener {

    private long kostId = -1;
    private String kostName = "";

    private Toolbar toolbar;
    private TextView tvKostName;
    private TextView tvSummary;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvRooms;
    private ProgressBar pbLoading;
    private View layoutEmpty;
    private ExtendedFloatingActionButton fabAddRoom;

    private OwnerRoomAdapter adapter;
    private FastApiOwnerRoomService roomService;
    private SessionManager sessionManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Room> currentRooms = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_room);

        kostId = getIntent().getLongExtra("EXTRA_KOST_ID", -1);
        kostName = getIntent().getStringExtra("EXTRA_KOST_NAME");
        if (kostName == null || kostName.isEmpty()) kostName = "Kost #" + kostId;

        sessionManager = new SessionManager(this);
        roomService = SupabaseClient.getInstance().createOwnerRoomService();

        initViews();
        loadRooms();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_room);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvKostName = findViewById(R.id.tv_room_kost_name);
        tvSummary = findViewById(R.id.tv_room_summary);
        swipeRefresh = findViewById(R.id.swipe_refresh_room);
        rvRooms = findViewById(R.id.rv_owner_rooms);
        pbLoading = findViewById(R.id.pb_room_loading);
        layoutEmpty = findViewById(R.id.layout_room_empty);
        fabAddRoom = findViewById(R.id.fab_add_room);

        tvKostName.setText(kostName);

        adapter = new OwnerRoomAdapter(this);
        rvRooms.setLayoutManager(new LinearLayoutManager(this));
        rvRooms.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadRooms);
        fabAddRoom.setOnClickListener(v -> showAddRoomDialog());
    }

    private void loadRooms() {
        if (kostId <= 0) {
            Toast.makeText(this, "ID kost tidak valid.", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        String token = "Bearer " + sessionManager.getAccessToken();

        roomService.listRooms(token, kostId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    currentRooms.clear();
                    JsonArray arr = response.body().getAsJsonArray("data");
                    if (arr != null) {
                        for (JsonElement el : arr) {
                            if (el.isJsonObject()) {
                                JsonObject obj = el.getAsJsonObject();
                                Room r = new Room();
                                r.setId(obj.has("id") && !obj.get("id").isJsonNull() ? obj.get("id").getAsLong() : 0);
                                r.setKostId(kostId);
                                r.setName(obj.has("name") && !obj.get("name").isJsonNull() ? obj.get("name").getAsString() : "Kamar");
                                r.setRoomType(obj.has("room_type") && !obj.get("room_type").isJsonNull() ? obj.get("room_type").getAsString() : "Standar");
                                r.setPrice(obj.has("price") && !obj.get("price").isJsonNull() ? obj.get("price").getAsDouble() : 0);
                                r.setStatus(obj.has("status") && !obj.get("status").isJsonNull() ? obj.get("status").getAsString() : "TERSEDIA");
                                currentRooms.add(r);
                            }
                        }
                    }
                    updateUI();
                } else {
                    Toast.makeText(OwnerRoomActivity.this, "Gagal memuat kamar (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(OwnerRoomActivity.this, "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        int total = currentRooms.size();
        int tersedia = 0;
        for (Room r : currentRooms) {
            if (r.isTersedia()) tersedia++;
        }
        tvSummary.setText("Total: " + total + " kamar | Tersedia: " + tersedia);

        if (total == 0) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvRooms.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvRooms.setVisibility(View.VISIBLE);
            adapter.submitList(currentRooms);
        }
    }

    private void showAddRoomDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_room, null);
        EditText etName = dialogView.findViewById(R.id.et_dialog_room_name);
        EditText etType = dialogView.findViewById(R.id.et_dialog_room_type);
        EditText etPrice = dialogView.findViewById(R.id.et_dialog_room_price);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Tambah Unit Kamar")
                .setView(dialogView)
                .setPositiveButton("Simpan", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String type = etType.getText().toString().trim();
                    String priceStr = etPrice.getText().toString().trim();
                    double price = 0;
                    try {
                        if (!priceStr.isEmpty()) price = Double.parseDouble(priceStr);
                    } catch (Exception ignored) {}

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Nama nomor kamar wajib diisi.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (type.isEmpty()) type = "Standar";

                    createRoomRequest(name, type, price);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void createRoomRequest(String name, String type, double price) {
        if (kostId <= 0) {
            Toast.makeText(this, "ID kost tidak valid. Silakan pilih kost terlebih dahulu.", Toast.LENGTH_SHORT).show();
            return;
        }
        pbLoading.setVisibility(View.VISIBLE);
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("room_type", type);
        body.addProperty("price", price);
        body.addProperty("status", "TERSEDIA");

        String token = "Bearer " + sessionManager.getAccessToken();
        roomService.createRoom(token, kostId, body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(OwnerRoomActivity.this, "Kamar berhasil ditambahkan", Toast.LENGTH_SHORT).show();
                    loadRooms();
                } else {
                    Toast.makeText(OwnerRoomActivity.this, "Gagal membuat kamar (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(OwnerRoomActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStatusChangeClick(Room room, int position) {
        String[] statuses = {"TERSEDIA", "TERISI", "BOOKING", "MAINTENANCE"};
        int selected = 0;
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equalsIgnoreCase(room.getStatus())) {
                selected = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Ubah Status: " + room.getName())
                .setSingleChoiceItems(statuses, selected, (dialog, which) -> {
                    dialog.dismiss();
                    String newStatus = statuses[which];
                    updateRoomStatusRequest(room, newStatus);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void updateRoomStatusRequest(Room room, String newStatus) {
        pbLoading.setVisibility(View.VISIBLE);
        JsonObject body = new JsonObject();
        body.addProperty("status", newStatus);

        String token = "Bearer " + sessionManager.getAccessToken();
        roomService.updateRoom(token, kostId, room.getId(), body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(OwnerRoomActivity.this, "Status diubah ke " + newStatus, Toast.LENGTH_SHORT).show();
                    loadRooms();
                } else {
                    Toast.makeText(OwnerRoomActivity.this, "Gagal mengubah status (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(OwnerRoomActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDeleteClick(Room room, int position) {
        AppDialogHelper.showDanger(this,
                "Hapus Kamar",
                "Hapus " + room.getName() + " secara permanen dari daftar kamar kost ini?",
                "Hapus Kamar",
                () -> deleteRoomRequest(room));
    }

    private void deleteRoomRequest(Room room) {
        pbLoading.setVisibility(View.VISIBLE);
        String token = "Bearer " + sessionManager.getAccessToken();
        roomService.deleteRoom(token, kostId, room.getId()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(OwnerRoomActivity.this, "Kamar dihapus", Toast.LENGTH_SHORT).show();
                    loadRooms();
                } else {
                    Toast.makeText(OwnerRoomActivity.this, "Gagal menghapus (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(OwnerRoomActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
