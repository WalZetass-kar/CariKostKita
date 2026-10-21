package com.carikostkita.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.carikostkita.data.model.ChatConversation;
import com.carikostkita.data.model.ChatMessage;
import com.carikostkita.data.remote.FastApiAuthService;
import com.carikostkita.data.remote.FastApiChatService;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.dto.AuthResponse;
import com.carikostkita.data.remote.dto.ChatDto;
import com.carikostkita.data.remote.dto.ChatListResponse;
import com.carikostkita.data.remote.dto.ChatResponse;
import com.carikostkita.data.remote.dto.CreateChatRequest;
import com.carikostkita.data.remote.dto.MessageDto;
import com.carikostkita.data.remote.dto.MessageListResponse;
import com.carikostkita.data.remote.dto.MessageResponse;
import com.carikostkita.data.remote.dto.RefreshRequest;
import com.carikostkita.data.remote.dto.SendMessageRequest;
import com.carikostkita.util.ErrorMessages;
import com.carikostkita.util.SessionManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.ResponseBody;
import retrofit2.Response;

/** Chat data access through FastAPI; no client-supplied participant ID is trusted. */
public class ChatRepository {
    private static final String TAG = "ChatRepository";
    private static final int PAGE_SIZE = 100;

    private final FastApiChatService chatService;
    private final FastApiAuthService authService;
    private final SessionManager sessionManager;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public ChatRepository(Context context) {
        SupabaseClient client = SupabaseClient.getInstance();
        this.chatService = client.createChatService();
        this.authService = client.createAuthService();
        this.sessionManager = new SessionManager(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    private String getBearerToken() throws IOException {
        String token = sessionManager.getAccessToken();
        if (token == null || token.isEmpty()) throw new IOException(ErrorMessages.SESSION_EXPIRED);
        if (sessionManager.isTokenExpired()) {
            String refreshToken = sessionManager.getRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) throw new IOException(ErrorMessages.SESSION_EXPIRED);
            try {
                Response<AuthResponse> refreshed = authService.refreshToken(new RefreshRequest(refreshToken)).execute();
                if (!refreshed.isSuccessful() || refreshed.body() == null
                        || refreshed.body().accessToken == null || refreshed.body().accessToken.isEmpty()) {
                    throw new IOException(ErrorMessages.SESSION_EXPIRED);
                }
                AuthResponse auth = refreshed.body();
                sessionManager.saveTokens(auth.accessToken, auth.refreshToken, auth.expiresIn);
                token = auth.accessToken;
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(ErrorMessages.SESSION_EXPIRED, e);
            }
        }
        return "Bearer " + token;
    }

    public void getOrCreateConversation(String kostId, String seekerId, String ownerId,
                                        DataCallback<ChatConversation> callback) {
        getOrCreateConversation(kostId, seekerId, ownerId, true, callback);
    }

    public void getOrCreateConversation(String kostId, String seekerId, String ownerId,
                                        boolean createIfMissing, DataCallback<ChatConversation> callback) {
        executor.execute(() -> {
            try {
                String bearer = getBearerToken();
                if (kostId == null || !kostId.matches("[0-9]+")) {
                    postError(callback, "Data kost tidak valid.");
                    return;
                }
                ChatDto existing = findConversation(bearer, kostId);
                if (existing != null) {
                    ChatConversation conversation = mapDtoToConversation(existing);
                    mainHandler.post(() -> callback.onSuccess(conversation));
                    return;
                }
                if (!createIfMissing) {
                    mainHandler.post(() -> callback.onSuccess(null));
                    return;
                }
                Response<ChatResponse> response = chatService
                        .createChat(bearer, new CreateChatRequest(kostId)).execute();
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    ChatConversation conversation = mapDtoToConversation(response.body().data);
                    mainHandler.post(() -> callback.onSuccess(conversation));
                } else {
                    postError(callback, ErrorMessages.fromResponse("createChat", response));
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to open conversation", e);
                postError(callback, ErrorMessages.fromException("createChat", e));
            }
        });
    }

    /** Legacy overloads intentionally ignore client-supplied user IDs. */
    public void getOrCreateConversation(int legacyKostId, int legacySeekerId, int legacyOwnerId,
                                        DataCallback<ChatConversation> callback) {
        getOrCreateConversation(String.valueOf(legacyKostId), sessionManager.getUserUid(),
                String.valueOf(legacyOwnerId), callback);
    }

    private ChatDto findConversation(String bearer, String kostId) throws IOException {
        String currentUserId = sessionManager.getUserUid();
        for (int page = 1; page <= 20; page++) {
            Response<ChatListResponse> response = chatService.listChats(bearer, page, PAGE_SIZE, kostId).execute();
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Unable to load conversations (HTTP " + response.code() + ").");
            }
            ChatListResponse body = response.body();
            if (body.data != null) {
                for (ChatDto chat : body.data) {
                    if (kostId.equals(chat.kostId) && currentUserId != null
                            && currentUserId.equals(chat.pencariId)) return chat;
                }
            }
            if (body.pagination == null || !body.pagination.has_more) break;
        }
        return null;
    }

    public void getConversationById(String conversationId, DataCallback<ChatConversation> callback) {
        executor.execute(() -> {
            try {
                Response<ChatResponse> response = chatService
                        .readChat(getBearerToken(), conversationId).execute();
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    ChatConversation conversation = mapDtoToConversation(response.body().data);
                    mainHandler.post(() -> callback.onSuccess(conversation));
                } else {
                    postError(callback, ErrorMessages.fromResponse("readChat", response));
                }
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("readChat", e));
            }
        });
    }

    public void getConversationById(int legacyId, DataCallback<ChatConversation> callback) {
        getConversationById(String.valueOf(legacyId), callback);
    }

    public void getConversationsForUser(String ignoredUserId, DataCallback<List<ChatConversation>> callback) {
        executor.execute(() -> {
            try {
                String bearer = getBearerToken();
                List<ChatConversation> conversations = new ArrayList<>();
                for (int page = 1; page <= 20; page++) {
                    Response<ChatListResponse> response = chatService.listChats(bearer, page, PAGE_SIZE, null).execute();
                    if (!response.isSuccessful() || response.body() == null) {
                        postError(callback, ErrorMessages.fromResponse("getConversations", response));
                        return;
                    }
                    ChatListResponse body = response.body();
                    if (body.data != null) {
                        for (ChatDto dto : body.data) conversations.add(mapDtoToConversation(dto));
                    }
                    if (body.pagination == null || !body.pagination.has_more) break;
                }
                mainHandler.post(() -> callback.onSuccess(conversations));
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("getConversations", e));
            }
        });
    }

    public void getConversationsForUser(int ignoredLegacyId, DataCallback<List<ChatConversation>> callback) {
        getConversationsForUser(sessionManager.getUserUid(), callback);
    }

    public void markConversationRead(String conversationId, String ignoredUserId) {
        executor.execute(() -> {
            try {
                chatService.markRead(getBearerToken(), conversationId).execute();
            } catch (Exception e) {
                Log.w(TAG, "Unable to mark conversation read", e);
            }
        });
    }

    public void getMessages(String conversationId, String ignoredUserId,
                            DataCallback<List<ChatMessage>> callback) {
        executor.execute(() -> {
            try {
                String bearer = getBearerToken();
                Response<ResponseBody> readResponse = chatService.markRead(bearer, conversationId).execute();
                if (!readResponse.isSuccessful()) {
                    postError(callback, ErrorMessages.fromResponse("markChatRead", readResponse));
                    return;
                }
                List<ChatMessage> messages = new ArrayList<>();
                for (int page = 1; page <= 20; page++) {
                    Response<MessageListResponse> response = chatService
                            .listMessages(bearer, conversationId, page, PAGE_SIZE).execute();
                    if (!response.isSuccessful() || response.body() == null) {
                        postError(callback, ErrorMessages.fromResponse("getMessages", response));
                        return;
                    }
                    MessageListResponse body = response.body();
                    if (body.data != null) {
                        for (MessageDto dto : body.data) {
                            messages.add(new ChatMessage(dto.id, dto.chatId, dto.senderId, dto.message,
                                    dto.createdAt != null ? dto.createdAt : "", dto.isRead != null && dto.isRead));
                        }
                    }
                    if (body.pagination == null || !body.pagination.has_more) break;
                }
                mainHandler.post(() -> callback.onSuccess(messages));
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("getMessages", e));
            }
        });
    }

    public void getMessages(int legacyConversationId, int ignoredLegacyUserId,
                            DataCallback<List<ChatMessage>> callback) {
        getMessages(String.valueOf(legacyConversationId), sessionManager.getUserUid(), callback);
    }

    public void sendMessage(String conversationId, String ignoredSenderId, String message,
                            DataCallback<ChatMessage> callback) {
        executor.execute(() -> {
            try {
                Response<MessageResponse> response = chatService
                        .sendMessage(getBearerToken(), conversationId, new SendMessageRequest(message))
                        .execute();
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    MessageDto dto = response.body().data;
                    ChatMessage sent = new ChatMessage(dto.id, dto.chatId, dto.senderId, dto.message,
                            dto.createdAt, dto.isRead != null && dto.isRead);
                    mainHandler.post(() -> callback.onSuccess(sent));
                } else {
                    postError(callback, ErrorMessages.fromResponse("sendMessage", response));
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to send message", e);
                postError(callback, ErrorMessages.fromException("sendMessage", e));
            }
        });
    }

    public void sendMessage(int legacyConversationId, int ignoredLegacySenderId, String message,
                            DataCallback<ChatMessage> callback) {
        sendMessage(String.valueOf(legacyConversationId), sessionManager.getUserUid(), message, callback);
    }

    private ChatConversation mapDtoToConversation(ChatDto dto) {
        ChatConversation conversation = new ChatConversation();
        conversation.setId(dto.id);
        conversation.setKostId(dto.kostId);
        conversation.setPencariId(dto.pencariId);
        conversation.setOwnerId(dto.ownerId);
        conversation.setNamaKost(dto.namaKost);
        conversation.setThumbnailKost(dto.thumbnailUrl);
        conversation.setNamaPencari(dto.pencariName);
        conversation.setNamaPemilik(dto.ownerName);
        conversation.setLastMessage(dto.lastMessage);
        conversation.setLastMessageTime(dto.lastMessageAt != null ? dto.lastMessageAt : dto.createdAt);
        String currentUserId = sessionManager.getUserUid();
        boolean isSeeker = currentUserId != null && currentUserId.equals(dto.pencariId);
        conversation.setAvatarLawan(isSeeker ? dto.ownerAvatarUrl : dto.pencariAvatarUrl);
        conversation.setRoleLawan(isSeeker ? dto.ownerRole : "user");
        // Production schema has no unread counters; do not invent them client-side.
        conversation.setUnreadCount(0);
        return conversation;
    }

    private <T> void postError(DataCallback<T> callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}
