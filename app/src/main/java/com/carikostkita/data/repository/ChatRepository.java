package com.carikostkita.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.carikostkita.data.model.ChatConversation;
import com.carikostkita.data.model.ChatMessage;
import com.carikostkita.data.remote.SupabaseAuthService;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.SupabaseDbService;
import com.carikostkita.data.remote.dto.AuthResponse;
import com.carikostkita.data.remote.dto.ChatDto;
import com.carikostkita.data.remote.dto.ChatUpdateDto;
import com.carikostkita.data.remote.dto.KostDto;
import com.carikostkita.data.remote.dto.MessageDto;
import com.carikostkita.data.remote.dto.ReadUpdateDto;
import com.carikostkita.data.remote.dto.RefreshRequest;
import com.carikostkita.data.remote.dto.UserDto;
import android.text.TextUtils;
import com.carikostkita.util.SessionManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Response;

public class ChatRepository {
    private static final String TAG = "ChatRepository";

    private final SupabaseDbService dbService;
    private final SupabaseAuthService authService;
    private final SessionManager sessionManager;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public ChatRepository(Context context) {
        this.dbService = SupabaseClient.getInstance().createService(SupabaseDbService.class);
        this.authService = SupabaseClient.getInstance().createService(SupabaseAuthService.class);
        this.sessionManager = new SessionManager(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    private void ensureValidAuthToken() {
        String currentToken = sessionManager.getAccessToken();
        if (currentToken != null && !currentToken.isEmpty()) {
            SupabaseClient.getInstance().setAccessToken(currentToken);
        }
        if (sessionManager.isTokenExpired()) {
            String refreshToken = sessionManager.getRefreshToken();
            if (refreshToken != null && !refreshToken.isEmpty()) {
                try {
                    Response<AuthResponse> refreshRes = authService.refreshToken(new RefreshRequest(refreshToken)).execute();
                    if (refreshRes.isSuccessful() && refreshRes.body() != null) {
                        AuthResponse authBody = refreshRes.body();
                        sessionManager.saveTokens(authBody.accessToken, authBody.refreshToken, authBody.expiresIn);
                        SupabaseClient.getInstance().setAccessToken(authBody.accessToken);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    public void getOrCreateConversation(String idKost, String idPencari, String idPemilik, DataCallback<ChatConversation> callback) {
        executor.execute(() -> {
            try {
                ensureValidAuthToken();

                if (idPencari == null || idPencari.trim().isEmpty()) {
                    postError(callback, "Sesi akun Anda tidak valid. Silakan login kembali.");
                    return;
                }

                if (idKost == null || idKost.trim().isEmpty()) {
                    postError(callback, "Data kost tidak valid.");
                    return;
                }

                // Jika owner id kosong di intent, ambil dari tabel kosts
                String resolvedOwnerId = idPemilik;
                String namaKost = "Informasi Kost";
                String thumb = "";
                try {
                    Response<List<KostDto>> kostRes = dbService.getKostById("eq." + idKost, "owner_id,nama_kost,thumbnail_url").execute();
                    if (kostRes.isSuccessful() && kostRes.body() != null && !kostRes.body().isEmpty()) {
                        KostDto kd = kostRes.body().get(0);
                        if (resolvedOwnerId == null || resolvedOwnerId.trim().isEmpty()) {
                            resolvedOwnerId = kd.ownerId;
                        }
                        if (kd.namaKost != null && !kd.namaKost.isEmpty()) namaKost = kd.namaKost;
                        if (kd.thumbnailUrl != null) thumb = kd.thumbnailUrl;
                    }
                } catch (Exception ignored) {}

                if (resolvedOwnerId == null || resolvedOwnerId.trim().isEmpty()) {
                    postError(callback, "Pemilik kost ini belum terdaftar di sistem.");
                    return;
                }

                // Pencegahan Self-Chat: Pencari tidak boleh chat dengan kost miliknya sendiri
                if (idPencari.equals(resolvedOwnerId)) {
                    postError(callback, "Anda tidak dapat memulai percakapan chat dengan properti kost milik Anda sendiri.");
                    return;
                }

                // Cek apakah percakapan antara pencari dan pemilik untuk kost ini sudah ada
                Map<String, String> filters = new HashMap<>();
                filters.put("kost_id", "eq." + idKost);
                filters.put("pencari_id", "eq." + idPencari);
                filters.put("owner_id", "eq." + resolvedOwnerId);

                Response<List<ChatDto>> res = dbService.getChats(filters, "*", null).execute();
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(mapDtoToConversation(res.body().get(0))));
                    return;
                }

                // Ambil nama pencari & nama pemilik
                String namaPencari = "Pencari Kost";
                String namaPemilik = "Pemilik Kost";
                try {
                    Response<List<UserDto>> uRes = dbService.getUserById("in.(" + idPencari + "," + resolvedOwnerId + ")", "id,nama").execute();
                    if (uRes.isSuccessful() && uRes.body() != null) {
                        for (UserDto u : uRes.body()) {
                            if (idPencari.equals(u.id) && u.nama != null) namaPencari = u.nama;
                            if (resolvedOwnerId.equals(u.id) && u.nama != null) namaPemilik = u.nama;
                        }
                    }
                } catch (Exception ignored) {}

                ChatDto newChat = new ChatDto();
                newChat.kostId = idKost;
                newChat.pencariId = idPencari;
                newChat.ownerId = resolvedOwnerId;
                newChat.namaKost = namaKost;
                newChat.thumbnailUrl = thumb;
                newChat.lastMessage = "Percakapan dimulai";

                Response<List<ChatDto>> createRes = dbService.insertChat(newChat).execute();
                if (createRes.isSuccessful() && createRes.body() != null && !createRes.body().isEmpty()) {
                    ChatConversation conv = mapDtoToConversation(createRes.body().get(0));
                    conv.setNamaPencari(namaPencari);
                    conv.setNamaPemilik(namaPemilik);
                    mainHandler.post(() -> callback.onSuccess(conv));
                } else {
                    String errStr = createRes.errorBody() != null ? createRes.errorBody().string() : "";
                    Log.e(TAG, "Gagal membuat chat: code=" + createRes.code() + ", error=" + errStr);

                    // Penanganan kondisi duplikat atau race condition
                    if (errStr.contains("duplicate") || errStr.contains("unique") || errStr.contains("23505")) {
                        Response<List<ChatDto>> retryRes = dbService.getChats(filters, "*", null).execute();
                        if (retryRes.isSuccessful() && retryRes.body() != null && !retryRes.body().isEmpty()) {
                            mainHandler.post(() -> callback.onSuccess(mapDtoToConversation(retryRes.body().get(0))));
                            return;
                        }
                    }

                    String userMsg = "Gagal membuat percakapan chat di server";
                    if (errStr.contains("42501")) {
                        userMsg = "Izin database ditolak (42501): Kebijakan RLS membatasi pembuatan chat. Jalankan script SQL perbaikan di Supabase Dashboard.";
                    } else if (createRes.code() == 401 || createRes.code() == 403) {
                        userMsg = "Sesi akun Anda telah berakhir. Silakan login kembali.";
                    }
                    postError(callback, userMsg);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception membuat chat: " + e.getMessage(), e);
                postError(callback, "Gagal membuat percakapan: " + e.getMessage());
            }
        });
    }

    public void getOrCreateConversation(int legacyKostId, int legacyPencariId, int legacyPemilikId, DataCallback<ChatConversation> callback) {
        getOrCreateConversation(String.valueOf(legacyKostId), sessionManager.getUserUid(), String.valueOf(legacyPemilikId), callback);
    }

    public void getConversationById(String idConversation, DataCallback<ChatConversation> callback) {
        executor.execute(() -> {
            try {
                Map<String, String> filters = new HashMap<>();
                filters.put("id", "eq." + idConversation);
                Response<List<ChatDto>> res = dbService.getChats(filters, "*", null).execute();
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(mapDtoToConversation(res.body().get(0))));
                } else {
                    postError(callback, "Percakapan tidak ditemukan");
                }
            } catch (Exception e) {
                postError(callback, "Error memuat percakapan: " + e.getMessage());
            }
        });
    }

    public void getConversationById(int legacyId, DataCallback<ChatConversation> callback) {
        getConversationById(String.valueOf(legacyId), callback);
    }

    public void getConversationsForUser(String idUser, DataCallback<List<ChatConversation>> callback) {
        executor.execute(() -> {
            try {
                if (idUser == null || idUser.isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                    return;
                }

                ensureValidAuthToken();

                Map<String, String> filters = new HashMap<>();
                filters.put("or", "(pencari_id.eq." + idUser + ",owner_id.eq." + idUser + ")");

                Response<List<ChatDto>> res = dbService.getChats(filters, "*", "last_message_at.desc.nullslast,created_at.desc").execute();
                if (res.isSuccessful() && res.body() != null) {
                    List<ChatDto> chatDtos = res.body();
                    Set<String> userIds = new HashSet<>();
                    for (ChatDto d : chatDtos) {
                        if (d.pencariId != null && !d.pencariId.isEmpty()) userIds.add(d.pencariId);
                        if (d.ownerId != null && !d.ownerId.isEmpty()) userIds.add(d.ownerId);
                    }

                    Map<String, UserDto> userMap = new HashMap<>();
                    if (!userIds.isEmpty()) {
                        try {
                            String inQuery = "in.(" + TextUtils.join(",", userIds) + ")";
                            Response<List<UserDto>> uRes = dbService.getUserById(inQuery, "id,nama,avatar_url,role").execute();
                            if (uRes.isSuccessful() && uRes.body() != null) {
                                for (UserDto u : uRes.body()) {
                                    if (u.id != null) userMap.put(u.id, u);
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    List<ChatConversation> list = new ArrayList<>();
                    for (ChatDto d : chatDtos) {
                        ChatConversation conv = mapDtoToConversation(d);

                        boolean isCurrentUserSeeker = idUser.equals(d.pencariId);
                        String counterpartId = isCurrentUserSeeker ? d.ownerId : d.pencariId;
                        int unread = isCurrentUserSeeker ? (d.unreadPencari != null ? d.unreadPencari : 0)
                                : (d.unreadOwner != null ? d.unreadOwner : 0);
                        conv.setUnreadCount(unread);

                        UserDto counterpartUser = userMap.get(counterpartId);
                        if (counterpartUser != null) {
                            if (isCurrentUserSeeker) {
                                if (counterpartUser.nama != null && !counterpartUser.nama.isEmpty()) {
                                    conv.setNamaPemilik(counterpartUser.nama);
                                }
                            } else {
                                if (counterpartUser.nama != null && !counterpartUser.nama.isEmpty()) {
                                    conv.setNamaPencari(counterpartUser.nama);
                                }
                            }
                            conv.setAvatarLawan(counterpartUser.avatarUrl);
                            conv.setRoleLawan(counterpartUser.role);
                        }
                        list.add(conv);
                    }
                    mainHandler.post(() -> callback.onSuccess(list));
                } else {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                }
            } catch (Exception e) {
                postError(callback, "Gagal memuat percakapan: " + e.getMessage());
            }
        });
    }

    public void markConversationRead(String idConversation, String currentUserId) {
        executor.execute(() -> {
            try {
                ensureValidAuthToken();
                dbService.markMessagesRead("eq." + idConversation, "neq." + currentUserId, new ReadUpdateDto()).execute();

                Response<List<ChatDto>> cRes = dbService.getChats(Collections.singletonMap("id", "eq." + idConversation), "pencari_id,owner_id", null).execute();
                if (cRes.isSuccessful() && cRes.body() != null && !cRes.body().isEmpty()) {
                    ChatDto convChat = cRes.body().get(0);
                    ChatUpdateDto updateDto = new ChatUpdateDto();
                    if (currentUserId.equals(convChat.pencariId)) {
                        updateDto.unreadPencari = 0;
                    } else if (currentUserId.equals(convChat.ownerId)) {
                        updateDto.unreadOwner = 0;
                    }
                    dbService.updateChat("eq." + idConversation, updateDto).execute();
                }
            } catch (Exception ignored) {}
        });
    }

    public void getConversationsForUser(int legacyId, DataCallback<List<ChatConversation>> callback) {
        getConversationsForUser(sessionManager.getUserUid(), callback);
    }

    public void getMessages(String idConversation, String currentUserId, DataCallback<List<ChatMessage>> callback) {
        executor.execute(() -> {
            try {
                ensureValidAuthToken();

                // Tandai pesan sudah dibaca
                try {
                    dbService.markMessagesRead("eq." + idConversation, "neq." + currentUserId, new ReadUpdateDto()).execute();
                } catch (Exception ignored) {}

                Response<List<MessageDto>> res = dbService.getMessages("eq." + idConversation, "*", "created_at.asc").execute();
                if (res.isSuccessful() && res.body() != null) {
                    List<ChatMessage> list = new ArrayList<>();
                    for (MessageDto m : res.body()) {
                        ChatMessage item = new ChatMessage(
                                m.id,
                                m.chatId,
                                m.senderId,
                                m.message,
                                m.createdAt != null ? m.createdAt : "",
                                m.isRead != null && m.isRead
                        );
                        list.add(item);
                    }
                    mainHandler.post(() -> callback.onSuccess(list));
                } else {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                }
            } catch (Exception e) {
                postError(callback, "Gagal memuat pesan: " + e.getMessage());
            }
        });
    }

    public void getMessages(int legacyIdConversation, int legacyUserId, DataCallback<List<ChatMessage>> callback) {
        getMessages(String.valueOf(legacyIdConversation), sessionManager.getUserUid(), callback);
    }

    public void sendMessage(String idConversation, String idSender, String message, DataCallback<ChatMessage> callback) {
        executor.execute(() -> {
            try {
                ensureValidAuthToken();

                MessageDto dto = new MessageDto(idConversation, idSender, message);
                Response<List<MessageDto>> res = dbService.insertMessage(dto).execute();
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    MessageDto created = res.body().get(0);
                    ChatMessage msg = new ChatMessage(
                            created.id,
                            created.chatId,
                            created.senderId,
                            created.message,
                            created.createdAt,
                            false
                    );

                    // Update last message & unread count pada percakapan
                    try {
                        Response<List<ChatDto>> cRes = dbService.getChats(Collections.singletonMap("id", "eq." + idConversation), "pencari_id,owner_id,unread_pencari,unread_owner", null).execute();
                        ChatUpdateDto updateDto = new ChatUpdateDto();
                        updateDto.lastMessage = message;
                        updateDto.lastMessageAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(new Date());

                        if (cRes.isSuccessful() && cRes.body() != null && !cRes.body().isEmpty()) {
                            ChatDto currentChat = cRes.body().get(0);
                            if (idSender.equals(currentChat.pencariId)) {
                                int currentUnread = currentChat.unreadOwner != null ? currentChat.unreadOwner : 0;
                                updateDto.unreadOwner = currentUnread + 1;
                            } else if (idSender.equals(currentChat.ownerId)) {
                                int currentUnread = currentChat.unreadPencari != null ? currentChat.unreadPencari : 0;
                                updateDto.unreadPencari = currentUnread + 1;
                            }
                        }
                        dbService.updateChat("eq." + idConversation, updateDto).execute();
                    } catch (Exception ignored) {}

                    mainHandler.post(() -> callback.onSuccess(msg));
                } else {
                    String err = res.errorBody() != null ? res.errorBody().string() : "";
                    Log.e(TAG, "Gagal mengirim pesan: code=" + res.code() + ", error=" + err);
                    String userMsg = "Gagal mengirim pesan ke server";
                    if (err.contains("42501")) {
                        userMsg = "Izin database ditolak (42501): RLS membatasi pengiriman pesan.";
                    }
                    postError(callback, userMsg);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception sendMessage: " + e.getMessage(), e);
                postError(callback, "Kesalahan pengiriman pesan: " + e.getMessage());
            }
        });
    }

    public void sendMessage(int legacyIdConv, int legacyIdSender, String message, DataCallback<ChatMessage> callback) {
        sendMessage(String.valueOf(legacyIdConv), sessionManager.getUserUid(), message, callback);
    }

    private ChatConversation mapDtoToConversation(ChatDto d) {
        ChatConversation c = new ChatConversation();
        c.setId(d.id);
        c.setKostId(d.kostId);
        c.setPencariId(d.pencariId);
        c.setOwnerId(d.ownerId);
        c.setNamaKost(d.namaKost);
        c.setThumbnailKost(d.thumbnailUrl);
        c.setLastMessage(d.lastMessage);
        c.setLastMessageTime(d.lastMessageAt != null ? d.lastMessageAt : d.createdAt);
        c.setUnreadCount(d.unreadPencari != null ? d.unreadPencari : 0);
        return c;
    }

    private <T> void postError(DataCallback<T> callback, String msg) {
        mainHandler.post(() -> callback.onError(msg));
    }
}
