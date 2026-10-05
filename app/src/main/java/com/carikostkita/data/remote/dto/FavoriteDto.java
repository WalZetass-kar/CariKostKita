package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** DTO untuk tabel favorites di Supabase */
public class FavoriteDto {

    @SerializedName("id")
    public String id;

    @SerializedName("user_id")
    public String userId;

    @SerializedName("kost_id")
    public String kostId;

    @SerializedName("created_at")
    public String createdAt;

    public FavoriteDto() {}

    public FavoriteDto(String userId, String kostId) {
        this.userId = userId;
        this.kostId = kostId;
    }
}
