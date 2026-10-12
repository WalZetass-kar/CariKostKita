package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Minimal safe user-directory projection returned by FastAPI for staff. */
public class AdminUserDto {
    @SerializedName("id") public String id;
    @SerializedName("email") public String email;
    @SerializedName("name") public String name;
    @SerializedName("role") public String role;
    @SerializedName("no_hp") public String noHp;
    @SerializedName("avatar_url") public String avatarUrl;
    @SerializedName("bio") public String bio;
    @SerializedName("is_active") public Boolean isActive;
    @SerializedName("verification_status") public String verificationStatus;
    @SerializedName("catatan_revisi") public String catatanRevisi;
    @SerializedName("created_at") public String createdAt;
}
