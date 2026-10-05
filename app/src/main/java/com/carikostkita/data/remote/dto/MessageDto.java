package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** DTO untuk tabel messages di Supabase */
public class MessageDto {

    @SerializedName("id")
    public String id;

    @SerializedName("chat_id")
    public String chatId;

    @SerializedName("sender_id")
    public String senderId;

    @SerializedName("message")
    public String message;

    @SerializedName("is_read")
    public Boolean isRead;

    @SerializedName("created_at")
    public String createdAt;

    public MessageDto() {}

    public MessageDto(String chatId, String senderId, String message) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.message = message;
        this.isRead = false;
    }
}
