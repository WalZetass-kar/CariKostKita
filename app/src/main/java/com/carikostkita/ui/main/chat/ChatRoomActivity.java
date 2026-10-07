package com.carikostkita.ui.main.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.carikostkita.R;
import com.carikostkita.data.model.ChatConversation;
import com.carikostkita.data.model.ChatMessage;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.User;
import com.carikostkita.data.remote.SupabaseRealtimeClient;
import com.carikostkita.data.repository.ChatRepository;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.data.repository.UserRepository;
import com.carikostkita.ui.adapter.ChatBubbleAdapter;
import com.carikostkita.util.FormatUtil;
import com.carikostkita.util.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class ChatRoomActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private com.google.android.material.imageview.ShapeableImageView ivAvatar;
    private TextView tvCounterpartName;
    private TextView tvKostSubtitle;

    private MaterialCardView cardKostContext;
    private ImageView ivKostThumb;
    private TextView tvContextKostName;
    private TextView tvContextKostPrice;
    private TextView tvContextKostStatus;
    private TextView tvContextKostLocation;

    private RecyclerView rvMessages;
    private ProgressBar pbLoading;
    private LinearLayout layoutEmpty;

    private TextView chipQuick1, chipQuick2, chipQuick3, chipQuick4;
    private EditText etInput;
    private FloatingActionButton btnSend;

    private ChatRepository chatRepository;
    private KostRepository kostRepository;
    private UserRepository userRepository;
    private SessionManager sessionManager;
    private ChatBubbleAdapter bubbleAdapter;
    private SupabaseRealtimeClient realtimeClient;

    private String conversationId = null;
    private String idKost = null;
    private String idPencari = null;
    private String idPemilik = null;
    private String namaKost = "";
    private String namaCounterpart = "";
    private String avatarCounterpart = "";
    private String fotoKost = "";
    private String lokasiKost = "";
    private String statusKost = "";
    private double hargaKost = 0;
    private String currentUserId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        sessionManager = new SessionManager(this);
        chatRepository = new ChatRepository(this);
        kostRepository = new KostRepository(this);
        userRepository = new UserRepository(this);
        currentUserId = sessionManager.getUserUid();

        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Silakan login terlebih dahulu untuk menggunakan fitur chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        readIntentData();
        initViews();
        setupRecyclerView();
        setupQuickReplies();
        setupRealtime();
        loadOrInitializeChat();
        fetchKostContextDetails();
        refreshBlockedState();
        new com.carikostkita.data.repository.AnalyticsRepository(this).log(
                com.carikostkita.data.repository.AnalyticsRepository.CHAT_START, idKost);
    }

    private void readIntentData() {
        String convExtra = getIntent().getStringExtra("conversation_id");
        if (convExtra != null && !convExtra.isEmpty()) {
            conversationId = convExtra;
        } else {
            int legacyConv = getIntent().getIntExtra("conversation_id", -1);
            if (legacyConv != -1) conversationId = String.valueOf(legacyConv);
        }

        String kostExtra = getIntent().getStringExtra("kost_id");
        if (kostExtra != null && !kostExtra.isEmpty()) {
            idKost = kostExtra;
        } else {
            int legacyKost = getIntent().getIntExtra("kost_id", -1);
            if (legacyKost != -1) idKost = String.valueOf(legacyKost);
        }

        String pemilikExtra = getIntent().getStringExtra("id_pemilik");
        if (pemilikExtra != null && !pemilikExtra.isEmpty()) {
            idPemilik = pemilikExtra;
        } else {
            int legacyPemilik = getIntent().getIntExtra("id_pemilik", -1);
            if (legacyPemilik != -1) idPemilik = String.valueOf(legacyPemilik);
        }

        String pencariExtra = getIntent().getStringExtra("id_pencari");
        if (pencariExtra != null && !pencariExtra.isEmpty()) {
            idPencari = pencariExtra;
        } else {
            int legacyPencari = getIntent().getIntExtra("id_pencari", -1);
            if (legacyPencari != -1) idPencari = String.valueOf(legacyPencari);
        }

        namaKost = getIntent().getStringExtra("nama_kost");
        namaCounterpart = getIntent().getStringExtra("nama_counterpart");
        avatarCounterpart = getIntent().getStringExtra("avatar_counterpart");
        fotoKost = getIntent().getStringExtra("foto_kost");
        lokasiKost = getIntent().getStringExtra("lokasi_kost");
        statusKost = getIntent().getStringExtra("status_kost");
        hargaKost = getIntent().getDoubleExtra("harga_kost", 0);

        if (namaKost == null) namaKost = "Informasi Kost";
        if (avatarCounterpart == null) avatarCounterpart = "";
        if (namaCounterpart == null || namaCounterpart.isEmpty()) {
            namaCounterpart = sessionManager.isPemilikKost() ? "Pencari Kost" : "Pemilik Kost";
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_chat_back);
        ivAvatar = findViewById(R.id.iv_chat_avatar);
        tvCounterpartName = findViewById(R.id.tv_chat_counterpart_name);
        tvKostSubtitle = findViewById(R.id.tv_chat_kost_subtitle);

        cardKostContext = findViewById(R.id.card_chat_kost_context);
        ivKostThumb = findViewById(R.id.iv_context_kost_thumb);
        tvContextKostName = findViewById(R.id.tv_context_kost_name);
        tvContextKostPrice = findViewById(R.id.tv_context_kost_price);
        tvContextKostStatus = findViewById(R.id.tv_context_kost_status);
        tvContextKostLocation = findViewById(R.id.tv_context_kost_location);

        rvMessages = findViewById(R.id.rv_chat_messages);
        pbLoading = findViewById(R.id.pb_chat_loading);
        layoutEmpty = findViewById(R.id.layout_chat_empty);

        chipQuick1 = findViewById(R.id.chip_quick_1);
        chipQuick2 = findViewById(R.id.chip_quick_2);
        chipQuick3 = findViewById(R.id.chip_quick_3);
        chipQuick4 = findViewById(R.id.chip_quick_4);

        etInput = findViewById(R.id.et_chat_input);
        btnSend = findViewById(R.id.btn_chat_send);

        tvCounterpartName.setText(namaCounterpart);
        tvKostSubtitle.setText(namaKost);
        tvContextKostName.setText(namaKost);

        com.carikostkita.util.UserAvatarHelper.loadAvatar(ivAvatar, avatarCounterpart);
        fetchCounterpartProfileIfNeeded();

        if (lokasiKost != null && !lokasiKost.trim().isEmpty()) {
            tvContextKostLocation.setText(lokasiKost.trim());
        } else {
            tvContextKostLocation.setText("Informasi Lokasi");
        }

        if ("PENUH".equalsIgnoreCase(statusKost)) {
            tvContextKostStatus.setText("Penuh");
            tvContextKostStatus.setBackgroundResource(R.drawable.bg_pill_penuh);
            tvContextKostStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.status_penuh));
        } else {
            tvContextKostStatus.setText("Tersedia");
            tvContextKostStatus.setBackgroundResource(R.drawable.bg_pill_tersedia);
            tvContextKostStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.status_tersedia));
        }

        if (hargaKost > 0) {
            tvContextKostPrice.setText(FormatUtil.formatRupiah(hargaKost) + " / bulan");
        } else {
            tvContextKostPrice.setText("CariKostKita");
        }

        if (fotoKost != null && !fotoKost.isEmpty()) {
            Glide.with(this)
                    .load(fotoKost)
                    .centerCrop()
                    .placeholder(R.drawable.bg_thumb_placeholder)
                    .error(R.drawable.bg_thumb_placeholder)
                    .into(ivKostThumb);
        }

        cardKostContext.setOnClickListener(v -> {
            if (idKost != null && !idKost.isEmpty()) {
                android.content.Intent detailIntent = new android.content.Intent(this, com.carikostkita.ui.detail.DetailKostActivity.class);
                detailIntent.putExtra("kost_id", idKost);
                startActivity(detailIntent);
            }
        });

        btnBack.setOnClickListener(v -> finish());
        com.carikostkita.util.TouchFeedbackUtil.attachPress(btnBack);
        btnSend.setOnClickListener(v -> handleSendMessage());
        View btnMore = findViewById(R.id.btn_chat_more);
        if (btnMore != null) btnMore.setOnClickListener(v -> showSafetyMenu());
    }

    private void fetchKostContextDetails() {
        if (idKost == null || idKost.trim().isEmpty()) return;
        kostRepository.getKostDetail(idKost, currentUserId, new DataCallback<com.carikostkita.data.model.Kost>() {
            @Override
            public void onSuccess(com.carikostkita.data.model.Kost kost) {
                if (kost == null || isFinishing() || isDestroyed()) return;
                tvContextKostName.setText(kost.getNamaKost());
                tvKostSubtitle.setText(kost.getNamaKost());
                if (kost.getHarga() > 0) {
                    tvContextKostPrice.setText(FormatUtil.formatRupiah(kost.getHarga()) + " / bulan");
                }

                String loc = "";
                if (kost.getKecamatan() != null && !kost.getKecamatan().isEmpty()) {
                    loc = kost.getKecamatan() + ", " + kost.getKota();
                } else if (kost.getKota() != null && !kost.getKota().isEmpty()) {
                    loc = kost.getKota();
                } else {
                    loc = "Indonesia";
                }
                tvContextKostLocation.setText(loc);

                if (kost.getStatus() == com.carikostkita.data.model.StatusKost.PENUH || kost.getKamarTersedia() <= 0) {
                    tvContextKostStatus.setText("Penuh");
                    tvContextKostStatus.setBackgroundResource(R.drawable.bg_pill_penuh);
                    tvContextKostStatus.setTextColor(androidx.core.content.ContextCompat.getColor(ChatRoomActivity.this, R.color.status_penuh));
                } else {
                    tvContextKostStatus.setText("Tersedia");
                    tvContextKostStatus.setBackgroundResource(R.drawable.bg_pill_tersedia);
                    tvContextKostStatus.setTextColor(androidx.core.content.ContextCompat.getColor(ChatRoomActivity.this, R.color.status_tersedia));
                }

                String thumbUrl = kost.getFotoUtama();
                if (thumbUrl != null && !thumbUrl.isEmpty()) {
                    Glide.with(ChatRoomActivity.this)
                            .load(thumbUrl)
                            .centerCrop()
                            .placeholder(R.drawable.bg_thumb_placeholder)
                            .error(R.drawable.bg_thumb_placeholder)
                            .into(ivKostThumb);
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void setupRecyclerView() {
        bubbleAdapter = new ChatBubbleAdapter(currentUserId);
        bubbleAdapter.setOnMessageRetryListener(this::retrySendMessage);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(bubbleAdapter);

        rvMessages.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom && bubbleAdapter.getItemCount() > 0) {
                rvMessages.post(() -> rvMessages.scrollToPosition(bubbleAdapter.getItemCount() - 1));
            }
        });
    }

    private String counterpartId() {
        return (currentUserId != null && currentUserId.equals(idPemilik)) ? idPencari : idPemilik;
    }

    private boolean blockedByMe = false;

    /** Laporkan atau blokir lawan bicara (keamanan & syarat Google Play untuk konten pengguna). */
    private void showSafetyMenu() {
        String other = counterpartId();
        if (other == null || other.isEmpty()) return;
        String[] items = {"Laporkan pengguna", blockedByMe ? "Buka blokir" : "Blokir pengguna"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(namaCounterpart != null && !namaCounterpart.isEmpty() ? namaCounterpart : "Opsi percakapan")
                .setItems(items, (d, which) -> {
                    if (which == 0) showReportUserDialog(other);
                    else if (blockedByMe) unblock(other);
                    else confirmBlock(other);
                })
                .show();
    }

    private void showReportUserDialog(String other) {
        String[] reasons = com.carikostkita.data.repository.SafetyRepository.USER_REPORT_REASONS;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Kenapa kamu melaporkan pengguna ini?")
                .setItems(reasons, (d, which) -> com.carikostkita.util.AppDialogHelper.showInput(this,
                        "Detail laporan", "Opsional: ceritakan singkat apa yang terjadi. Tim akan meninjau percakapan ini.",
                        "Contoh: minta transfer DP sebelum survei", "", "Kirim Laporan",
                        detail -> new com.carikostkita.data.repository.SafetyRepository(this).reportUser(other, conversationId,
                                reasons[which], detail, new DataCallback<Boolean>() {
                                    @Override
                                    public void onSuccess(Boolean ok) {
                                        com.carikostkita.util.AppDialogHelper.showSuccessDialog(ChatRoomActivity.this, "Laporan Terkirim",
                                                "Terima kasih. Kamu juga bisa memblokir pengguna ini agar tidak bisa mengirim pesan lagi.");
                                    }

                                    @Override
                                    public void onError(String message) {
                                        Toast.makeText(ChatRoomActivity.this, message, Toast.LENGTH_SHORT).show();
                                    }
                                })))
                .setNegativeButton("Batal", null)
                .show();
    }

    private void confirmBlock(String other) {
        com.carikostkita.util.AppDialogHelper.showDanger(this, "Blokir pengguna?",
                "Kalian tidak akan bisa saling mengirim pesan atau jadwal survei. Kamu bisa membuka blokir kapan saja.",
                "Blokir", () -> new com.carikostkita.data.repository.SafetyRepository(this).block(other, new DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean ok) {
                        blockedByMe = true;
                        applyBlockedState(true, true);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(ChatRoomActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                }));
    }

    private void unblock(String other) {
        new com.carikostkita.data.repository.SafetyRepository(this).unblock(other, new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean ok) {
                blockedByMe = false;
                refreshBlockedState();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ChatRoomActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshBlockedState() {
        String other = counterpartId();
        if (other == null || other.isEmpty()) return;
        com.carikostkita.data.repository.SafetyRepository repo = new com.carikostkita.data.repository.SafetyRepository(this);
        repo.isBlockedByMe(other, new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean mine) {
                blockedByMe = mine;
                repo.isBlockedWith(other, new DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean any) {
                        if (!isFinishing()) applyBlockedState(any, mine);
                    }

                    @Override
                    public void onError(String message) {}
                });
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void applyBlockedState(boolean blocked, boolean byMe) {
        TextView banner = findViewById(R.id.tv_chat_blocked);
        View quick = findViewById(R.id.layout_chat_quick_replies);
        etInput.setVisibility(blocked ? View.GONE : View.VISIBLE);
        btnSend.setVisibility(blocked ? View.GONE : View.VISIBLE);
        if (quick != null && blocked) quick.setVisibility(View.GONE);
        if (banner != null) {
            banner.setVisibility(blocked ? View.VISIBLE : View.GONE);
            banner.setText(byMe ? "Kamu memblokir pengguna ini. Buka blokir lewat menu di kanan atas."
                    : "Percakapan ini tidak bisa dilanjutkan.");
        }
    }

    private void setupQuickReplies() {
        // Pemilik mendapat templat balasan cepat; pencari mendapat pertanyaan umum
        if (sessionManager.isPemilikKost()) {
            chipQuick1.setText("Kamar masih tersedia, silakan survei dulu");
            chipQuick2.setText("Harga sudah termasuk listrik dan air");
            chipQuick3.setText("Jam malam pukul 22.00");
            chipQuick4.setText("Silakan ajukan jadwal survei di aplikasi");
        }
        View.OnClickListener listener = v -> {
            TextView tv = (TextView) v;
            etInput.setText(tv.getText().toString());
            etInput.setSelection(etInput.getText().length());
        };
        chipQuick1.setOnClickListener(listener);
        chipQuick2.setOnClickListener(listener);
        chipQuick3.setOnClickListener(listener);
        chipQuick4.setOnClickListener(listener);
    }

    private void setupRealtime() {
        realtimeClient = new SupabaseRealtimeClient();
        realtimeClient.setListener(new SupabaseRealtimeClient.MessageListener() {
            @Override
            public void onMessageReceived(com.carikostkita.data.remote.dto.MessageDto msg) {
                if (msg == null) return;
                // Jika pesan dari lawan bicara
                if (msg.senderId != null && !msg.senderId.equals(currentUserId)) {
                    ChatMessage chatMsg = new ChatMessage(msg.id, msg.chatId, msg.senderId, msg.message, msg.createdAt, true);
                    bubbleAdapter.addMessage(chatMsg);
                    layoutEmpty.setVisibility(View.GONE);
                    rvMessages.scrollToPosition(bubbleAdapter.getItemCount() - 1);
                    // Tandai langsung sebagai dibaca karena chat room sedang terbuka
                    if (conversationId != null && !conversationId.isEmpty()) {
                        chatRepository.markConversationRead(conversationId, currentUserId);
                    }
                }
            }

            @Override
            public void onMessageUpdated(com.carikostkita.data.remote.dto.MessageDto msg) {
                if (msg == null) return;
                if (msg.isRead != null && msg.isRead) {
                    if (msg.senderId != null && msg.senderId.equals(currentUserId)) {
                        bubbleAdapter.updateMessageRead(msg.id);
                    }
                }
            }
        });
    }

    private void fetchCounterpartProfileIfNeeded() {
        String counterpartId = (currentUserId != null && currentUserId.equals(idPemilik)) ? idPencari : idPemilik;
        if (counterpartId == null || counterpartId.trim().isEmpty()) return;

        if (avatarCounterpart == null || avatarCounterpart.trim().isEmpty() ||
                "Pencari Kost".equalsIgnoreCase(namaCounterpart) || "Pemilik Kost".equalsIgnoreCase(namaCounterpart)) {
            userRepository.getUserById(counterpartId, new DataCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    if (user == null || isFinishing() || isDestroyed()) return;
                    if (user.getNama() != null && !user.getNama().trim().isEmpty()) {
                        namaCounterpart = user.getNama().trim();
                        tvCounterpartName.setText(namaCounterpart);
                    }
                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().trim().isEmpty()) {
                        avatarCounterpart = user.getAvatarUrl().trim();
                        com.carikostkita.util.UserAvatarHelper.loadAvatar(ivAvatar, avatarCounterpart);
                    }
                }

                @Override
                public void onError(String message) {}
            });
        }
    }

    private void loadOrInitializeChat() {
        pbLoading.setVisibility(View.VISIBLE);
        if (conversationId != null && !conversationId.isEmpty()) {
            realtimeClient.connect(conversationId);
            loadMessages();
            if (idPemilik == null || idPemilik.isEmpty() || idPencari == null || idPencari.isEmpty() ||
                    avatarCounterpart == null || avatarCounterpart.isEmpty() || idKost == null || idKost.isEmpty()) {
                chatRepository.getConversationById(conversationId, new DataCallback<ChatConversation>() {
                    @Override
                    public void onSuccess(ChatConversation data) {
                        if (data == null || isFinishing() || isDestroyed()) return;
                        if (idKost == null || idKost.isEmpty()) {
                            idKost = data.getIdKost();
                            fetchKostContextDetails();
                        }
                        if (idPencari == null || idPencari.isEmpty()) {
                            idPencari = data.getIdPencari();
                        }
                        if (idPemilik == null || idPemilik.isEmpty()) {
                            idPemilik = data.getIdPemilik();
                        }
                        if (avatarCounterpart == null || avatarCounterpart.isEmpty()) {
                            if (data.getAvatarLawan() != null && !data.getAvatarLawan().isEmpty()) {
                                avatarCounterpart = data.getAvatarLawan();
                                com.carikostkita.util.UserAvatarHelper.loadAvatar(ivAvatar, avatarCounterpart);
                            }
                        }
                        fetchCounterpartProfileIfNeeded();
                    }

                    @Override
                    public void onError(String message) {}
                });
            }
        } else if (idKost != null && !idKost.isEmpty()) {
            // Hanya cari percakapan yang sudah ada; yang baru dibuat saat pesan pertama dikirim
            chatRepository.getOrCreateConversation(idKost, currentUserId, idPemilik, false, new DataCallback<ChatConversation>() {
                @Override
                public void onSuccess(ChatConversation data) {
                    if (isFinishing() || isDestroyed()) return;
                    if (data == null) {
                        pbLoading.setVisibility(View.GONE);
                        layoutEmpty.setVisibility(View.VISIBLE);
                        fetchCounterpartProfileIfNeeded();
                        return;
                    }
                    onConversationReady(data);
                    loadMessages();
                }

                @Override
                public void onError(String message) {
                    pbLoading.setVisibility(View.GONE);
                    layoutEmpty.setVisibility(View.VISIBLE);
                    Toast.makeText(ChatRoomActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            pbLoading.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void onConversationReady(ChatConversation data) {
        conversationId = data.getId();
        if (idPencari == null || idPencari.isEmpty()) {
            idPencari = data.getIdPencari();
        }
        if (idPemilik == null || idPemilik.isEmpty()) {
            idPemilik = data.getIdPemilik();
        }
        if (avatarCounterpart == null || avatarCounterpart.isEmpty()) {
            if (data.getAvatarLawan() != null && !data.getAvatarLawan().isEmpty()) {
                avatarCounterpart = data.getAvatarLawan();
                com.carikostkita.util.UserAvatarHelper.loadAvatar(ivAvatar, avatarCounterpart);
            }
        }
        fetchCounterpartProfileIfNeeded();
        refreshBlockedState();
        realtimeClient.connect(conversationId);
    }

    private void loadMessages() {
        chatRepository.getMessages(conversationId, currentUserId, new DataCallback<List<ChatMessage>>() {
            @Override
            public void onSuccess(List<ChatMessage> data) {
                pbLoading.setVisibility(View.GONE);
                bubbleAdapter.setMessages(data);
                if (data.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    rvMessages.scrollToPosition(data.size() - 1);
                    // Tandai seluruh pesan lawan sebagai terbaca
                    chatRepository.markConversationRead(conversationId, currentUserId);
                }
            }

            @Override
            public void onError(String message) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(ChatRoomActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSendMessage() {
        String text = etInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        // Optimistic UI update: tampilkan pesan seketika dengan status SENDING
        String tempId = "temp_" + System.currentTimeMillis();
        String nowIso = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).format(new java.util.Date());
        ChatMessage pendingMsg = new ChatMessage(tempId, conversationId, currentUserId, text, nowIso, false);
        pendingMsg.setStatus(ChatMessage.STATUS_SENDING);

        etInput.setText("");
        layoutEmpty.setVisibility(View.GONE);
        bubbleAdapter.addMessage(pendingMsg);
        rvMessages.scrollToPosition(bubbleAdapter.getItemCount() - 1);

        DataCallback<ChatMessage> sendCallback = new DataCallback<ChatMessage>() {
            @Override
            public void onSuccess(ChatMessage msg) {
                msg.setStatus(ChatMessage.STATUS_SENT);
                bubbleAdapter.updateMessage(msg);
            }

            @Override
            public void onError(String message) {
                pendingMsg.setStatus(ChatMessage.STATUS_FAILED);
                bubbleAdapter.updateMessage(pendingMsg);
                Toast.makeText(ChatRoomActivity.this, "Pesan belum terkirim. Ketuk pesan untuk mengirim ulang.", Toast.LENGTH_SHORT).show();
            }
        };

        if (conversationId != null && !conversationId.isEmpty()) {
            chatRepository.sendMessage(conversationId, currentUserId, text, sendCallback);
            return;
        }
        if (idKost == null || idKost.isEmpty()) {
            sendCallback.onError("Data kost tidak ditemukan");
            return;
        }
        // Pesan pertama: buat percakapan lalu kirim
        chatRepository.getOrCreateConversation(idKost, currentUserId, idPemilik, true, new DataCallback<ChatConversation>() {
            @Override
            public void onSuccess(ChatConversation data) {
                if (isFinishing() || isDestroyed()) return;
                onConversationReady(data);
                chatRepository.sendMessage(conversationId, currentUserId, text, sendCallback);
            }

            @Override
            public void onError(String message) {
                pendingMsg.setStatus(ChatMessage.STATUS_FAILED);
                bubbleAdapter.updateMessage(pendingMsg);
                com.carikostkita.util.AppDialogHelper.showError(ChatRoomActivity.this, "Pesan Belum Terkirim", message);
            }
        });
    }

    private void retrySendMessage(ChatMessage msg) {
        if (msg == null) return;
        if (conversationId == null || conversationId.isEmpty()) {
            bubbleAdapter.removeMessage(msg);
            etInput.setText(msg.getMessageText());
            handleSendMessage();
            return;
        }
        msg.setStatus(ChatMessage.STATUS_SENDING);
        bubbleAdapter.updateMessage(msg);

        chatRepository.sendMessage(conversationId, currentUserId, msg.getMessageText(), new DataCallback<ChatMessage>() {
            @Override
            public void onSuccess(ChatMessage res) {
                res.setStatus(ChatMessage.STATUS_SENT);
                bubbleAdapter.updateMessage(res);
            }

            @Override
            public void onError(String error) {
                msg.setStatus(ChatMessage.STATUS_FAILED);
                bubbleAdapter.updateMessage(msg);
                Toast.makeText(ChatRoomActivity.this, "Gagal mengirim ulang: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (conversationId != null && !conversationId.isEmpty()) {
            realtimeClient.connect(conversationId);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        realtimeClient.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realtimeClient.disconnect();
    }
}
