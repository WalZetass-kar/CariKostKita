package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Response dari Supabase Auth (login / register / refresh token) */
public class AuthResponse {

    @SerializedName("access_token")
    public String accessToken;

    @SerializedName("refresh_token")
    public String refreshToken;

    @SerializedName("expires_in")
    public long expiresIn;

    @SerializedName("token_type")
    public String tokenType;

    @SerializedName("user")
    public AuthUser user;

    public static class AuthUser {
        @SerializedName("id")
        public String id;

        @SerializedName("email")
        public String email;

        @SerializedName("app_metadata")
        public AppMetadata appMetadata;

        @SerializedName("user_metadata")
        public java.util.Map<String, Object> userMetadata;

        public static class AppMetadata {
            @SerializedName("provider")
            public String provider;
        }
    }
}
