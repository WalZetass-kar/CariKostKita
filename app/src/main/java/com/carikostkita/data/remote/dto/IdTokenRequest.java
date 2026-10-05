package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Request body untuk tukar Google ID token dengan Supabase session */
public class IdTokenRequest {

    @SerializedName("provider")
    public String provider;

    @SerializedName("id_token")
    public String idToken;

    public IdTokenRequest(String idToken) {
        this.provider = "google";
        this.idToken = idToken;
    }
}
