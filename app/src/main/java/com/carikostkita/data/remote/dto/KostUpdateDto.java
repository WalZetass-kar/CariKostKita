package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** DTO untuk update kost di Supabase */
public class KostUpdateDto {

    @SerializedName(value = "name", alternate = {"nama_kost", "nama"})
    public String namaKost;

    @SerializedName(value = "address", alternate = {"alamat"})
    public String alamat;

    @SerializedName("patokan")
    public String patokan;

    @SerializedName(value = "price_min", alternate = {"harga", "price", "harga_min"})
    public Double harga;

    @SerializedName(value = "price_max", alternate = {"harga_max"})
    public Double priceMax;

    @SerializedName(value = "gender_type", alternate = {"tipe_kost", "tipe"})
    public String tipeKost;

    @SerializedName(value = "description", alternate = {"deskripsi"})
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

    @SerializedName(value = "province", alternate = {"provinsi"})
    public String provinsi;

    @SerializedName(value = "city", alternate = {"kota"})
    public String kota;

    @SerializedName(value = "district", alternate = {"kecamatan"})
    public String kecamatan;

    @SerializedName(value = "village", alternate = {"kelurahan"})
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

    @SerializedName(value = "facilities", alternate = {"fasilitas"})
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
