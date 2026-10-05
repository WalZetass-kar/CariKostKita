package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Request body untuk refresh JWT token */
public class RefreshRequest {

    @SerializedName("refresh_token")
    public String refreshToken;

    public RefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
