package com.carikostkita.ui.main;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.carikostkita.R;
import com.carikostkita.data.model.AppNotification;
import com.carikostkita.data.remote.FastApiNotificationService;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.ui.adapter.NotificationAdapter;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.SessionManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {

    private Toolbar toolbar;
    private TextView tvUnreadBadge;
    private TextView btnMarkAllRead;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvNotifications;
    private ProgressBar pbLoading;
    private View layoutEmpty;

    private NotificationAdapter adapter;
    private FastApiNotificationService notifService;
    private SessionManager sessionManager;
    private final List<AppNotification> notifList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        sessionManager = new SessionManager(this);
        notifService = SupabaseClient.getInstance().createNotificationService();

        initViews();
        loadNotifications();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_notifications);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvUnreadBadge = findViewById(R.id.tv_notif_unread_badge);
        btnMarkAllRead = findViewById(R.id.btn_notif_mark_all_read);
        swipeRefresh = findViewById(R.id.swipe_refresh_notif);
        rvNotifications = findViewById(R.id.rv_notifications);
        pbLoading = findViewById(R.id.pb_notif_loading);
        layoutEmpty = findViewById(R.id.layout_notif_empty);

        adapter = new NotificationAdapter(this);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadNotifications);

        btnMarkAllRead.setOnClickListener(v -> markAllRead());
    }

    private void loadNotifications() {
        pbLoading.setVisibility(View.VISIBLE);
        String token = "Bearer " + sessionManager.getAccessToken();

        notifService.listNotifications(token, 1, 50).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    notifList.clear();
                    JsonArray arr = response.body().getAsJsonArray("data");
                    int unreadCount = 0;
                    if (arr != null) {
                        for (JsonElement el : arr) {
                            if (el.isJsonObject()) {
                                JsonObject obj = el.getAsJsonObject();
                                AppNotification n = new AppNotification();
                                n.setId(obj.has("id") && !obj.get("id").isJsonNull() ? obj.get("id").getAsLong() : 0);
                                n.setUserId(obj.has("user_id") && !obj.get("user_id").isJsonNull() ? obj.get("user_id").getAsLong() : 0);
                                n.setTitle(obj.has("title") && !obj.get("title").isJsonNull() ? obj.get("title").getAsString() : "Notifikasi");
                                n.setMessage(obj.has("message") && !obj.get("message").isJsonNull() ? obj.get("message").getAsString() : "");
                                n.setType(obj.has("type") && !obj.get("type").isJsonNull() ? obj.get("type").getAsString() : "INFO");
                                boolean isRead = obj.has("is_read") && !obj.get("is_read").isJsonNull() && obj.get("is_read").getAsBoolean();
                                n.setRead(isRead);
                                if (!isRead) unreadCount++;
                                n.setCreatedAt(obj.has("created_at") && !obj.get("created_at").isJsonNull() ? obj.get("created_at").getAsString() : "");
                                notifList.add(n);
                            }
                        }
                    }

                    if (unreadCount > 0) {
                        tvUnreadBadge.setText(unreadCount + " notifikasi belum dibaca");
                    } else {
                        tvUnreadBadge.setText("Semua notifikasi sudah dibaca");
                    }

                    if (notifList.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvNotifications.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvNotifications.setVisibility(View.VISIBLE);
                        adapter.submitList(notifList);
                    }
                } else {
                    Toast.makeText(NotificationActivity.this, "Gagal memuat notifikasi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(NotificationActivity.this, "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAllRead() {
        String token = "Bearer " + sessionManager.getAccessToken();
        notifService.markAllAsRead(token).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(NotificationActivity.this, "Semua notifikasi ditandai dibaca", Toast.LENGTH_SHORT).show();
                    loadNotifications();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }

    @Override
    public void onNotificationClick(AppNotification notification, int position) {
        if (!notification.isRead()) {
            notification.setRead(true);
            adapter.notifyItemChanged(position);
            String token = "Bearer " + sessionManager.getAccessToken();
            notifService.markAsRead(token, notification.getId()).enqueue(new Callback<JsonObject>() {
                @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}
                @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
            });
        }
        AppDialogHelper.showInfo(this, notification.getTitle(), notification.getMessage());
    }
}
