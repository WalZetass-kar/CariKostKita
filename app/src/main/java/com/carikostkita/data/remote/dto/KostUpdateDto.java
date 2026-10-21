package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** DTO untuk update kost di Supabase */
public class KostUpdateDto {

    @SerializedName(value = "nama_kost", alternate = {"name", "nama"})
    public String namaKost;

    @SerializedName(value = "alamat", alternate = {"address"})
    public String alamat;

    @SerializedName("patokan")
    public String patokan;

    @SerializedName(value = "harga", alternate = {"price_min", "price", "harga_min"})
    public Double harga;

    @SerializedName(value = "price_max", alternate = {"harga_max"})
    public Double priceMax;

    @SerializedName(value = "tipe_kost", alternate = {"gender_type", "tipe"})
    public String tipeKost;

    @SerializedName(value = "deskripsi", alternate = {"description"})
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

    @SerializedName(value = "provinsi", alternate = {"province"})
    public String provinsi;

    @SerializedName(value = "kota", alternate = {"city"})
    public String kota;

    @SerializedName(value = "kecamatan", alternate = {"district"})
    public String kecamatan;

    @SerializedName(value = "kelurahan", alternate = {"village"})
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

    @SerializedName(value = "fasilitas", alternate = {"facilities"})
    public List<String> fasilitas;

    @SerializedName("deposit")
    public Integer deposit;

    @SerializedName("minimal_sewa_bulan")
    public Integer minimalSewaBulan;

    @SerializedName("biaya_tambahan")
    public String biayaTambahan;

    @SerializedName("aturan")
    public List<String> aturan;
}
