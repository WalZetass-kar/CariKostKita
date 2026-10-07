package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** DTO untuk tabel users di Supabase PostgreSQL */
public class UserDto {

    @SerializedName("id")
    public String id;

    @SerializedName("nama")
    public String nama;

    @SerializedName("email")
    public String email;

    @SerializedName("role")
    public String role;

    @SerializedName("no_hp")
    public String noHp;

    @SerializedName("avatar_url")
    public String avatarUrl;

    @SerializedName("bio")
    public String bio;

    @SerializedName("verification_status")
    public String verificationStatus;

    @SerializedName("pengajuan_at")
    public String pengajuanAt;

    @SerializedName("pengajuan_catatan")
    public String pengajuanCatatan;

    @SerializedName("catatan_revisi")
    public String catatanRevisi;

    @SerializedName("is_active")
    public Boolean isActive;

    @SerializedName("auth_provider")
    public String authProvider;

    @SerializedName("created_at")
    public String createdAt;
}
