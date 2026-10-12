package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Strict self-profile fields accepted by FastAPI (never includes role/status). */
public class ProfileUpdateRequest {
    @SerializedName("nama")
    public String nama;

    @SerializedName("no_hp")
    public String noHp;

    @SerializedName("bio")
    public String bio;

    @SerializedName("avatar_url")
    public String avatarUrl;

    public ProfileUpdateRequest(String nama, String noHp, String bio, String avatarUrl) {
        this.nama = nama;
        this.noHp = noHp;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
    }
}
