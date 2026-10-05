package com.carikostkita.data.remote;

import android.os.Handler;
import android.os.Looper;
import com.carikostkita.BuildConfig;
import com.carikostkita.data.remote.dto.MessageDto;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * WebSocket client untuk Supabase Realtime menggunakan Phoenix Protocol.
 * Digunakan untuk menerima pesan chat realtime pada tabel `messages` dan `chats`.
 */
public class SupabaseRealtimeClient {

    public interface MessageListener {
        void onMessageReceived(MessageDto message);
        default void onMessageUpdated(MessageDto message) {}
    }

    public interface ChatUpdateListener {
        void onChatUpdated();
    }

    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;
    private WebSocket webSocket;
    private ScheduledExecutorService heartbeatExecutor;
    private final AtomicInteger refCounter = new AtomicInteger(1);
    private MessageListener listener;
    private ChatUpdateListener chatUpdateListener;
    private String currentChatId;
    private boolean isConnected = false;

    public SupabaseRealtimeClient() {
        this.client = SupabaseClient.getInstance().getOkHttpClient();
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    public void setChatUpdateListener(ChatUpdateListener listener) {
        this.chatUpdateListener = listener;
    }

    public synchronized void connect(String chatId) {
        this.currentChatId = chatId;
        disconnect();

        String url = BuildConfig.SUPABASE_URL
                .replace("https://", "wss://")
                .replace("http://", "ws://");
        String wsUrl = url + "/realtime/v1/websocket?apikey=" + BuildConfig.SUPABASE_ANON_KEY + "&vsn=1.0.0";

        Request request = new Request.Builder()
                .url(wsUrl)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                isConnected = true;
                joinChannel(ws);
                startHeartbeat();
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                handleIncomingMessage(text);
            }

            @Override
            public void onClosing(WebSocket ws, int code, String reason) {
                isConnected = false;
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                isConnected = false;
                stopHeartbeat();
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                isConnected = false;
                stopHeartbeat();
            }
        });
    }

    private void joinChannel(WebSocket ws) {
        String ref = String.valueOf(refCounter.getAndIncrement());
        JsonObject joinMsg = new JsonObject();
        joinMsg.addProperty("topic", "realtime:public:messages");
        joinMsg.addProperty("event", "phx_join");

        JsonObject payload = new JsonObject();
        JsonObject config = new JsonObject();

        JsonArray changes = new JsonArray();

        JsonObject changeMsg = new JsonObject();
        changeMsg.addProperty("event", "*");
        changeMsg.addProperty("schema", "public");
        changeMsg.addProperty("table", "messages");
        changes.add(changeMsg);

        JsonObject changeChat = new JsonObject();
        changeChat.addProperty("event", "*");
        changeChat.addProperty("schema", "public");
        changeChat.addProperty("table", "chats");
        changes.add(changeChat);

        config.add("postgres_changes", changes);
        payload.add("config", config);
        joinMsg.add("payload", payload);

        joinMsg.addProperty("ref", ref);
        joinMsg.addProperty("join_ref", ref);

        ws.send(gson.toJson(joinMsg));
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (webSocket != null && isConnected) {
                String ref = String.valueOf(refCounter.getAndIncrement());
                JsonObject hb = new JsonObject();
                hb.addProperty("topic", "phoenix");
                hb.addProperty("event", "heartbeat");
                hb.add("payload", new JsonObject());
                hb.addProperty("ref", ref);
                webSocket.send(gson.toJson(hb));
            }
        }, 25, 25, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
            heartbeatExecutor.shutdown();
            heartbeatExecutor = null;
        }
    }

    private void handleIncomingMessage(String text) {
        try {
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            if (root.has("event") && "postgres_changes".equals(root.get("event").getAsString())) {
                if (root.has("payload")) {
                    JsonObject payload = root.getAsJsonObject("payload");
                    if (payload.has("data")) {
                        JsonObject data = payload.getAsJsonObject("data");
                        String table = data.has("table") ? data.get("table").getAsString() : "";
                        String type = data.has("type") ? data.get("type").getAsString() : "";
                        if (data.has("record") && data.get("record").isJsonObject()) {
                            JsonObject record = data.getAsJsonObject("record");
                            if ("messages".equals(table)) {
                                MessageDto msg = gson.fromJson(record, MessageDto.class);
                                if (msg != null && (currentChatId == null || currentChatId.equals(msg.chatId))) {
                                    mainHandler.post(() -> {
                                        if (listener != null) {
                                            if ("UPDATE".equalsIgnoreCase(type)) {
                                                listener.onMessageUpdated(msg);
                                            } else {
                                                listener.onMessageReceived(msg);
                                            }
                                        }
                                    });
                                }
                            } else if ("chats".equals(table)) {
                                mainHandler.post(() -> {
                                    if (chatUpdateListener != null) {
                                        chatUpdateListener.onChatUpdated();
                                    }
                                });
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Ignore parse errors from heartbeat or system replies
        }
    }

    public synchronized void disconnect() {
        stopHeartbeat();
        if (webSocket != null) {
            webSocket.close(1000, "Client disconnect");
            webSocket = null;
        }
        isConnected = false;
    }
}
