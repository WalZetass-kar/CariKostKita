package com.carikostkita.data.model;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Kost implements Serializable {
    private String id;
    private String ownerId;
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
    private String patokan;
    private String createdAt;
    private String updatedAt;

    // Lokasi Lengkap & Kamar Details
    private String provinsi;
    private String kota;
    private String kecamatan;
    private String kelurahan;
    private String ukuranKamar;
    private int totalKamar;
    private int kamarTersedia;
    private int idWilayah = 1;

    public int getIdWilayah() {
        return idWilayah;
    }

    public void setIdWilayah(int idWilayah) {
        this.idWilayah = idWilayah;
    }

    // Media & Fasilitas
    private String thumbnailUrl;
    private List<String> imageUrls;
    private List<String> fasilitas;
    private List<Fasilitas> listFasilitas;
    private List<FotoKost> listFoto;
    private boolean isFavorite;

    public Kost() {
        this.tipeKost = TipeKost.CAMPUR;
        this.status = StatusKost.TERSEDIA;
        this.verificationStatus = KostVerificationStatus.APPROVED;
        this.catatanRevisi = "";
        this.locationVerification = "VALID";
        this.patokan = "";
        this.provinsi = "Riau";
        this.kota = "Pekanbaru";
        this.ukuranKamar = "3x4 m";
        this.totalKamar = 10;
        this.kamarTersedia = 3;
        this.imageUrls = new ArrayList<>();
        this.fasilitas = new ArrayList<>();
        this.listFasilitas = new ArrayList<>();
        this.listFoto = new ArrayList<>();
    }

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdKost() {
        return getId();
    }

    public void setIdKost(String id) {
        setId(id);
    }

    public String getOwnerId() {
        return ownerId != null ? ownerId : "";
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getIdPemilik() {
        return getOwnerId();
    }

    public void setIdPemilik(String ownerId) {
        setOwnerId(ownerId);
    }

    public String getNamaKost() {
        return namaKost != null ? namaKost : "";
    }

    public void setNamaKost(String namaKost) {
        this.namaKost = namaKost;
    }

    public String getAlamat() {
        return alamat != null ? alamat : "";
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
        return tipeKost != null ? tipeKost : TipeKost.CAMPUR;
    }

    public void setTipeKost(TipeKost tipeKost) {
        this.tipeKost = tipeKost;
    }

    public String getDeskripsi() {
        return deskripsi != null ? deskripsi : "";
    }

    public void setDeskripsi(String deskripsi) {
        this.deskripsi = deskripsi;
    }

    public String getNoWhatsapp() {
        return noWhatsapp != null ? noWhatsapp : "";
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
        return status != null ? status : StatusKost.TERSEDIA;
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

    public String getPatokan() {
        return patokan != null ? patokan : "";
    }

    public void setPatokan(String patokan) {
        this.patokan = patokan;
    }

    public String getCreatedAt() {
        return createdAt != null ? createdAt : "";
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt != null ? updatedAt : "";
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
        return kelurahan != null ? kelurahan : "";
    }

    public void setKelurahan(String kelurahan) {
        this.kelurahan = kelurahan;
    }

    public String getKecamatan() {
        return kecamatan != null ? kecamatan : "";
    }

    public void setKecamatan(String kecamatan) {
        this.kecamatan = kecamatan;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl != null ? thumbnailUrl : "";
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getThumbnailPath() {
        return getThumbnailUrl();
    }

    public void setThumbnailPath(String thumbnailPath) {
        setThumbnailUrl(thumbnailPath);
    }

    public String getFotoUtama() {
        return getThumbnailUrl();
    }

    public void setFotoUtama(String fotoUtama) {
        setThumbnailUrl(fotoUtama);
    }

    public List<String> getImageUrls() {
        return imageUrls != null ? imageUrls : new ArrayList<>();
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public List<String> getFasilitas() {
        return fasilitas != null ? fasilitas : new ArrayList<>();
    }

    public List<String> getFasilitasNames() {
        return getFasilitas();
    }

    public void setFasilitas(List<String> fasilitas) {
        this.fasilitas = fasilitas;
    }

    public List<Fasilitas> getListFasilitas() {
        if (listFasilitas == null || listFasilitas.isEmpty()) {
            listFasilitas = new ArrayList<>();
            if (fasilitas != null) {
                int idCounter = 1;
                for (String fName : fasilitas) {
                    listFasilitas.add(new Fasilitas(idCounter++, fName));
                }
            }
        }
        return listFasilitas;
    }

    public void setListFasilitas(List<Fasilitas> listFasilitas) {
        this.listFasilitas = listFasilitas;
        if (listFasilitas != null) {
            this.fasilitas = new ArrayList<>();
            for (Fasilitas f : listFasilitas) {
                this.fasilitas.add(f.getNamaFasilitas());
            }
        }
    }

    public List<FotoKost> getListFoto() {
        if (listFoto == null || listFoto.isEmpty()) {
            listFoto = new ArrayList<>();
            if (imageUrls != null && !imageUrls.isEmpty()) {
                int idCounter = 1;
                for (String url : imageUrls) {
                    listFoto.add(new FotoKost(idCounter++, 0, "foto", url, idCounter == 2));
                }
            } else if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                listFoto.add(new FotoKost(1, 0, "cover", thumbnailUrl, true));
            }
        }
        return listFoto;
    }

    public void setListFoto(List<FotoKost> listFoto) {
        this.listFoto = listFoto;
        if (listFoto != null) {
            this.imageUrls = new ArrayList<>();
            for (FotoKost f : listFoto) {
                this.imageUrls.add(f.getPathFile());
                if (f.isThumbnail()) {
                    this.thumbnailUrl = f.getPathFile();
                }
            }
            if ((this.thumbnailUrl == null || this.thumbnailUrl.isEmpty()) && !imageUrls.isEmpty()) {
                this.thumbnailUrl = imageUrls.get(0);
            }
        }
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
        return formatRupiah.format(this.harga) + " /bulan";
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
