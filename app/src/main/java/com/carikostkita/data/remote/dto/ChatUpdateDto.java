package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** DTO untuk update chat (last_message, unread count) */
public class ChatUpdateDto {

    @SerializedName("last_message")
    public String lastMessage;

    @SerializedName("last_message_at")
    public String lastMessageAt;

    @SerializedName("unread_pencari")
    public Integer unreadPencari;

    @SerializedName("unread_owner")
    public Integer unreadOwner;
}
