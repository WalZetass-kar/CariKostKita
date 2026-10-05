package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;

/** Request body untuk registrasi akun baru ke Supabase Auth */
public class SignUpRequest {

    @SerializedName("email")
    public String email;

    @SerializedName("password")
    public String password;

    @SerializedName("data")
    public Map<String, String> data;

    public SignUpRequest(String email, String password, String nama) {
        this.email = email;
        this.password = password;
        this.data = new HashMap<>();
        this.data.put("nama", nama);
    }
}
