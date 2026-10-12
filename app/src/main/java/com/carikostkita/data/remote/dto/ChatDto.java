package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** DTO untuk tabel chats di Supabase */
public class ChatDto {

    @SerializedName("id")
    public String id;

    @SerializedName("kost_id")
    public String kostId;

    @SerializedName("user_id")
    public String pencariId;

    @SerializedName("owner_id")
    public String ownerId;

    @SerializedName("nama_kost")
    public String namaKost;

    @SerializedName("thumbnail_url")
    public String thumbnailUrl;

    @SerializedName("pencari_name")
    public String pencariName;

    @SerializedName("owner_name")
    public String ownerName;

    @SerializedName("pencari_avatar_url")
    public String pencariAvatarUrl;

    @SerializedName("owner_avatar_url")
    public String ownerAvatarUrl;

    @SerializedName("owner_role")
    public String ownerRole;

    @SerializedName("last_message")
    public String lastMessage;

    @SerializedName("last_message_at")
    public String lastMessageAt;

    @SerializedName("unread_pencari")
    public Integer unreadPencari;

    @SerializedName("unread_owner")
    public Integer unreadOwner;

    @SerializedName("created_at")
    public String createdAt;
}
