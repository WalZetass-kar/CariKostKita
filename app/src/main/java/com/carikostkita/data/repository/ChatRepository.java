package com.carikostkita.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.carikostkita.data.local.DatabaseHelper;
import com.carikostkita.data.local.dao.ChatDAO;
import com.carikostkita.data.model.ChatConversation;
import com.carikostkita.data.model.ChatMessage;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatRepository {
    private final ChatDAO chatDAO;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public ChatRepository(Context context) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        this.chatDAO = new ChatDAO(dbHelper);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void getOrCreateConversation(int idKost, int idPencari, int idPemilik, DataCallback<ChatConversation> callback) {
        executor.execute(() -> {
            try {
                ChatConversation conv = chatDAO.getOrCreateConversation(idKost, idPencari, idPemilik);
                mainHandler.post(() -> callback.onSuccess(conv));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Gagal membuat percakapan: " + e.getMessage()));
            }
        });
    }

    public void getConversationById(int idConversation, DataCallback<ChatConversation> callback) {
        executor.execute(() -> {
            try {
                ChatConversation conv = chatDAO.findConversationById(idConversation);
                if (conv != null) {
                    mainHandler.post(() -> callback.onSuccess(conv));
                } else {
                    mainHandler.post(() -> callback.onError("Percakapan tidak ditemukan"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    public void getConversationsForUser(int idUser, DataCallback<List<ChatConversation>> callback) {
        executor.execute(() -> {
            try {
                List<ChatConversation> list = chatDAO.getConversationsForUser(idUser);
                mainHandler.post(() -> callback.onSuccess(list));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Gagal memuat percakapan: " + e.getMessage()));
            }
        });
    }

    public void getMessages(int idConversation, int currentUserId, DataCallback<List<ChatMessage>> callback) {
        executor.execute(() -> {
            try {
                chatDAO.markMessagesAsRead(idConversation, currentUserId);
                List<ChatMessage> list = chatDAO.getMessages(idConversation);
                mainHandler.post(() -> callback.onSuccess(list));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Gagal memuat pesan: " + e.getMessage()));
            }
        });
    }

    public void sendMessage(int idConversation, int idSender, String message, DataCallback<ChatMessage> callback) {
        executor.execute(() -> {
            try {
                ChatMessage msg = chatDAO.sendMessage(idConversation, idSender, message);
                mainHandler.post(() -> callback.onSuccess(msg));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Gagal mengirim pesan: " + e.getMessage()));
            }
        });
    }
}
