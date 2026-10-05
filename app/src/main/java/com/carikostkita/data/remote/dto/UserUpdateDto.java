package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** DTO untuk update kolom user di Supabase */
public class UserUpdateDto {

    @SerializedName("nama")
    public String nama;

    @SerializedName("no_hp")
    public String noHp;

    @SerializedName("bio")
    public String bio;

    @SerializedName("avatar_url")
    public String avatarUrl;

    @SerializedName("verification_status")
    public String verificationStatus;

    @SerializedName("pengajuan_catatan")
    public String pengajuanCatatan;

    @SerializedName("catatan_revisi")
    public String catatanRevisi;

    @SerializedName("role")
    public String role;

    @SerializedName("is_active")
    public Boolean isActive;
}
