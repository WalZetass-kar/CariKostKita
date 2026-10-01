package com.carikostkita.data.model;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Kost implements Serializable {
    private int idKost;
    private int idWilayah;
    private String namaKost;
    private String alamat;
    private double harga;
    private TipeKost tipeKost;
    private String deskripsi;
    private String noWhatsapp;
    private double latitude;
    private double longitude;
    private StatusKost status;
    private KostVerificationStatus verificationStatus;
    private String catatanRevisi;
    private String locationVerification;
    private int idPemilik;
    private String patokan;
    private String createdAt;
    private String updatedAt;

    // Lokasi Lengkap & Kamar Details
    private String provinsi;
    private String kota;
    private String ukuranKamar;
    private int totalKamar;
    private int kamarTersedia;

    // Relasi & Tampilan
    private String kelurahan;
    private String kecamatan;
    private String thumbnailPath;
    private List<Fasilitas> listFasilitas;
    private List<FotoKost> listFoto;
    private boolean isFavorite;

    public Kost() {
        this.tipeKost = TipeKost.CAMPUR;
        this.status = StatusKost.TERSEDIA;
        this.verificationStatus = KostVerificationStatus.APPROVED;
        this.catatanRevisi = "";
        this.locationVerification = "VALID";
        this.idPemilik = 2; // Default to seeded owner
        this.patokan = "";
        this.provinsi = "Riau";
        this.kota = "Pekanbaru";
        this.ukuranKamar = "3x4 m";
        this.totalKamar = 10;
        this.kamarTersedia = 3;
        this.listFasilitas = new ArrayList<>();
        this.listFoto = new ArrayList<>();
    }

    public int getIdKost() {
        return idKost;
    }

    public void setIdKost(int idKost) {
        this.idKost = idKost;
    }

    public int getIdWilayah() {
        return idWilayah;
    }

    public void setIdWilayah(int idWilayah) {
        this.idWilayah = idWilayah;
    }

    public String getNamaKost() {
        return namaKost;
    }

    public void setNamaKost(String namaKost) {
        this.namaKost = namaKost;
    }

    public String getAlamat() {
        return alamat;
    }

    public void setAlamat(String alamat) {
        this.alamat = alamat;
    }

    public double getHarga() {
        return harga;
    }

    public void setHarga(double harga) {
        this.harga = harga;
    }

    public TipeKost getTipeKost() {
        return tipeKost;
    }

    public void setTipeKost(TipeKost tipeKost) {
        this.tipeKost = tipeKost;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    public void setDeskripsi(String deskripsi) {
        this.deskripsi = deskripsi;
    }

    public String getNoWhatsapp() {
        return noWhatsapp;
    }

    public void setNoWhatsapp(String noWhatsapp) {
        this.noWhatsapp = noWhatsapp;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public StatusKost getStatus() {
        return status;
    }

    public void setStatus(StatusKost status) {
        this.status = status;
    }

    public KostVerificationStatus getVerificationStatus() {
        return verificationStatus != null ? verificationStatus : KostVerificationStatus.PENDING;
    }

    public void setVerificationStatus(KostVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getCatatanRevisi() {
        return catatanRevisi != null ? catatanRevisi : "";
    }

    public void setCatatanRevisi(String catatanRevisi) {
        this.catatanRevisi = catatanRevisi;
    }

    public String getLocationVerification() {
        return locationVerification != null ? locationVerification : "VALID";
    }

    public void setLocationVerification(String locationVerification) {
        this.locationVerification = locationVerification;
    }

    public int getIdPemilik() {
        return idPemilik > 0 ? idPemilik : 2;
    }

    public void setIdPemilik(int idPemilik) {
        this.idPemilik = idPemilik;
    }

    public String getPatokan() {
        return patokan != null ? patokan : "";
    }

    public void setPatokan(String patokan) {
        this.patokan = patokan;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getProvinsi() {
        return (provinsi != null && !provinsi.isEmpty()) ? provinsi : "Riau";
    }

    public void setProvinsi(String provinsi) {
        this.provinsi = provinsi;
    }

    public String getKota() {
        return (kota != null && !kota.isEmpty()) ? kota : "Pekanbaru";
    }

    public void setKota(String kota) {
        this.kota = kota;
    }

    public String getUkuranKamar() {
        return (ukuranKamar != null && !ukuranKamar.isEmpty()) ? ukuranKamar : "3x4 m";
    }

    public void setUkuranKamar(String ukuranKamar) {
        this.ukuranKamar = ukuranKamar;
    }

    public int getTotalKamar() {
        return Math.max(1, totalKamar);
    }

    public void setTotalKamar(int totalKamar) {
        this.totalKamar = totalKamar;
    }

    public int getKamarTersedia() {
        return Math.max(0, kamarTersedia);
    }

    public void setKamarTersedia(int kamarTersedia) {
        this.kamarTersedia = kamarTersedia;
    }

    public int getKamarTerisi() {
        return Math.max(0, getTotalKamar() - getKamarTersedia());
    }

    public String getKelurahan() {
        return kelurahan;
    }

    public void setKelurahan(String kelurahan) {
        this.kelurahan = kelurahan;
    }

    public String getKecamatan() {
        return kecamatan;
    }

    public void setKecamatan(String kecamatan) {
        this.kecamatan = kecamatan;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getFotoUtama() {
        return getThumbnailPath();
    }

    public void setFotoUtama(String fotoUtama) {
        setThumbnailPath(fotoUtama);
    }

    public List<Fasilitas> getListFasilitas() {
        return listFasilitas != null ? listFasilitas : new ArrayList<>();
    }

    public void setListFasilitas(List<Fasilitas> listFasilitas) {
        this.listFasilitas = listFasilitas;
    }

    public List<FotoKost> getListFoto() {
        return listFoto != null ? listFoto : new ArrayList<>();
    }

    public void setListFoto(List<FotoKost> listFoto) {
        this.listFoto = listFoto;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public String getFormattedHarga() {
        Locale localeID = new Locale("in", "ID");
        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(localeID);
        formatRupiah.setMaximumFractionDigits(0);
        return formatRupiah.format(this.harga) + " / bln";
    }

    public String getFullLocation() {
        StringBuilder sb = new StringBuilder();
        if (kelurahan != null && !kelurahan.isEmpty()) {
            sb.append(kelurahan);
        }
        if (kecamatan != null && !kecamatan.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(kecamatan);
        }
        if (kota != null && !kota.isEmpty() && !sb.toString().contains(kota)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(kota);
        }
        if (provinsi != null && !provinsi.isEmpty() && !sb.toString().contains(provinsi)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(provinsi);
        }
        if (sb.length() == 0 && alamat != null) {
            return alamat;
        }
        return sb.toString();
    }
}
