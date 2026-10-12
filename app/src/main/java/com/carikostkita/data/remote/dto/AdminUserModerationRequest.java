package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Exactly one supported staff moderation change; identity is supplied by the path/token. */
public class AdminUserModerationRequest {
    @SerializedName("role") public final String role;
    @SerializedName("is_active") public final Boolean isActive;
    @SerializedName("reason") public final String reason;

    public AdminUserModerationRequest(String role, Boolean isActive, String reason) {
        this.role = role;
        this.isActive = isActive;
        this.reason = reason;
    }
}
