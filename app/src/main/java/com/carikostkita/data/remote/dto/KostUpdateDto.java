package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** DTO untuk update kost di Supabase */
public class KostUpdateDto {

    @SerializedName("nama_kost")
    public String namaKost;

    @SerializedName("alamat")
    public String alamat;

    @SerializedName("patokan")
    public String patokan;

    @SerializedName("harga")
    public Double harga;

    @SerializedName("tipe_kost")
    public String tipeKost;

    @SerializedName("deskripsi")
    public String deskripsi;

    @SerializedName("no_whatsapp")
    public String noWhatsapp;

    @SerializedName("latitude")
    public Double latitude;

    @SerializedName("longitude")
    public Double longitude;

    @SerializedName("status")
    public String status;

    @SerializedName("verification_status")
    public String verificationStatus;

    @SerializedName("catatan_revisi")
    public String catatanRevisi;

    @SerializedName("provinsi")
    public String provinsi;

    @SerializedName("kota")
    public String kota;

    @SerializedName("kecamatan")
    public String kecamatan;

    @SerializedName("kelurahan")
    public String kelurahan;

    @SerializedName("ukuran_kamar")
    public String ukuranKamar;

    @SerializedName("total_kamar")
    public Integer totalKamar;

    @SerializedName("kamar_tersedia")
    public Integer kamarTersedia;

    @SerializedName("thumbnail_url")
    public String thumbnailUrl;

    @SerializedName("image_urls")
    public List<String> imageUrls;

    @SerializedName("fasilitas")
    public List<String> fasilitas;
}
