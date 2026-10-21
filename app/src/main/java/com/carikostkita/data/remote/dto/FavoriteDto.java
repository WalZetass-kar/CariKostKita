package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** DTO untuk tabel favorites di Supabase */
public class FavoriteDto {

    @SerializedName("id")
    public String id;

    @SerializedName("user_id")
    public String userId;

    @SerializedName("kost_id")
    public String kostId;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("kost")
    public KostDto kost;

    @SerializedName(value = "nama_kost", alternate = {"name", "nama"})
    public String namaKost;

    @SerializedName(value = "alamat", alternate = {"address"})
    public String alamat;

    @SerializedName(value = "harga", alternate = {"price_min", "price", "harga_min"})
    public Double harga;

    @SerializedName("thumbnail_url")
    public String thumbnailUrl;

    @SerializedName(value = "tipe_kost", alternate = {"gender_type", "tipe"})
    public String tipeKost;

    @SerializedName("kamar_tersedia")
    public Integer kamarTersedia;

    @SerializedName("status")
    public String status;

    @SerializedName("verification_status")
    public String verificationStatus;

    public FavoriteDto() {}

    public FavoriteDto(String userId, String kostId) {
        this.userId = userId;
        this.kostId = kostId;
    }
}
