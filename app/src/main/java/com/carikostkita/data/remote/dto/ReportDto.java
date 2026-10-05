package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** DTO untuk tabel reports di Supabase */
public class ReportDto {

    @SerializedName("id")
    public String id;

    @SerializedName("kost_id")
    public String kostId;

    @SerializedName("reporter_id")
    public String reporterId;

    @SerializedName("owner_id")
    public String ownerId;

    @SerializedName("kategori_laporan")
    public String kategoriLaporan;

    @SerializedName("deskripsi")
    public String deskripsi;

    @SerializedName("status")
    public String status;

    @SerializedName("tindakan_admin")
    public String tindakanAdmin;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("updated_at")
    public String updatedAt;
}
