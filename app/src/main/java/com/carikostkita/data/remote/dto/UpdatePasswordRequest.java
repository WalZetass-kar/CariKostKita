package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Request body untuk ganti password via Supabase Auth */
public class UpdatePasswordRequest {

    @SerializedName("password")
    public String password;

    public UpdatePasswordRequest(String newPassword) {
        this.password = newPassword;
    }
}
