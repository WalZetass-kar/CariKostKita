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
import com.carikostkita.data.repository.ChatRepository;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.ui.adapter.ChatBubbleAdapter;
import com.carikostkita.util.FormatUtil;
import com.carikostkita.util.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class ChatRoomActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageView ivAvatar;
    private TextView tvCounterpartName;
    private TextView tvKostSubtitle;

    private MaterialCardView cardKostContext;
    private ImageView ivKostThumb;
    private TextView tvContextKostName;
    private TextView tvContextKostPrice;

    private RecyclerView rvMessages;
    private ProgressBar pbLoading;
    private LinearLayout layoutEmpty;

    private TextView chipQuick1, chipQuick2, chipQuick3, chipQuick4;
    private EditText etInput;
    private FloatingActionButton btnSend;

    private ChatRepository chatRepository;
    private SessionManager sessionManager;
    private ChatBubbleAdapter bubbleAdapter;

    private int conversationId = -1;
    private int idKost = -1;
    private int idPemilik = -1;
    private String namaKost = "";
    private String namaCounterpart = "";
    private String fotoKost = "";
    private double hargaKost = 0;
    private int currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        sessionManager = new SessionManager(this);
        chatRepository = new ChatRepository(this);
        currentUserId = sessionManager.getUserId();

        if (currentUserId <= 0) {
            Toast.makeText(this, "Silakan login terlebih dahulu untuk menggunakan fitur chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        readIntentData();
        initViews();
        setupRecyclerView();
        setupQuickReplies();
        loadOrInitializeChat();
    }

    private void readIntentData() {
        conversationId = getIntent().getIntExtra("conversation_id", -1);
        idKost = getIntent().getIntExtra("kost_id", -1);
        idPemilik = getIntent().getIntExtra("id_pemilik", -1);
        namaKost = getIntent().getStringExtra("nama_kost");
        namaCounterpart = getIntent().getStringExtra("nama_counterpart");
        fotoKost = getIntent().getStringExtra("foto_kost");
        hargaKost = getIntent().getDoubleExtra("harga_kost", 0);

        if (namaKost == null) namaKost = "Informasi Kost";
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
        if (hargaKost > 0) {
            tvContextKostPrice.setText(FormatUtil.formatRupiah(hargaKost) + " / bulan");
        } else {
            tvContextKostPrice.setText("CariKostKita Bukit Raya");
        }

        if (fotoKost != null && !fotoKost.isEmpty()) {
            Glide.with(this).load(fotoKost).placeholder(R.drawable.ic_bed).into(ivKostThumb);
        }

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> handleSendMessage());
    }

    private void setupRecyclerView() {
        bubbleAdapter = new ChatBubbleAdapter(currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(bubbleAdapter);
    }

    private void setupQuickReplies() {
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

    private void loadOrInitializeChat() {
        pbLoading.setVisibility(View.VISIBLE);
        if (conversationId != -1) {
            loadMessages();
        } else if (idKost != -1) {
            chatRepository.getOrCreateConversation(idKost, currentUserId, idPemilik, new DataCallback<ChatConversation>() {
                @Override
                public void onSuccess(ChatConversation data) {
                    conversationId = data.getIdConversation();
                    loadMessages();
                }

                @Override
                public void onError(String message) {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(ChatRoomActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            pbLoading.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        }
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

        if (conversationId == -1) {
            Toast.makeText(this, "Menghubungkan percakapan...", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSend.setEnabled(false);
        chatRepository.sendMessage(conversationId, currentUserId, text, new DataCallback<ChatMessage>() {
            @Override
            public void onSuccess(ChatMessage msg) {
                btnSend.setEnabled(true);
                etInput.setText("");
                layoutEmpty.setVisibility(View.GONE);
                bubbleAdapter.addMessage(msg);
                rvMessages.scrollToPosition(bubbleAdapter.getItemCount() - 1);
            }

            @Override
            public void onError(String message) {
                btnSend.setEnabled(true);
                Toast.makeText(ChatRoomActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
