package com.carikostkita.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class KostFilterCriteria implements Serializable {
    private String keyword;
    private Double maxHarga;
    private Double minHarga;
    private TipeKost tipeKost;
    private StatusKost status;
    private Integer idWilayah;
    private List<Integer> fasilitasIds;
    private String sortBy; // "TERBARU", "TERMURAH", "TERMAHAL", "TERDEKAT"
    private String kecamatan;
    private Double maxDistanceKm;
    /** Titik acuan jarak selain lokasi pengguna, mis. kampus atau kantor. */
    private Double originLat;
    private Double originLng;
    private String originLabel;

    public Double getOriginLat() { return originLat; }
    public Double getOriginLng() { return originLng; }
    public String getOriginLabel() { return originLabel; }

    public void setOrigin(Double lat, Double lng, String label) {
        this.originLat = lat;
        this.originLng = lng;
        this.originLabel = label;
    }

    public KostFilterCriteria() {
        this.fasilitasIds = new ArrayList<>();
        this.sortBy = "TERBARU";
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Double getMaxHarga() {
        return maxHarga;
    }

    public void setMaxHarga(Double maxHarga) {
        this.maxHarga = maxHarga;
    }

    public Double getMinHarga() {
        return minHarga;
    }

    public void setMinHarga(Double minHarga) {
        this.minHarga = minHarga;
    }

    public TipeKost getTipeKost() {
        return tipeKost;
    }

    public void setTipeKost(TipeKost tipeKost) {
        this.tipeKost = tipeKost;
    }

    public StatusKost getStatus() {
        return status;
    }

    public void setStatus(StatusKost status) {
        this.status = status;
    }

    public Integer getIdWilayah() {
        return idWilayah;
    }

    public void setIdWilayah(Integer idWilayah) {
        this.idWilayah = idWilayah;
    }

    public List<Integer> getFasilitasIds() {
        return fasilitasIds != null ? fasilitasIds : new ArrayList<>();
    }

    public void setFasilitasIds(List<Integer> fasilitasIds) {
        this.fasilitasIds = (fasilitasIds != null) ? new ArrayList<>(fasilitasIds) : new ArrayList<>();
    }

    public String getSortBy() {
        return sortBy != null ? sortBy : "TERBARU";
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getKecamatan() {
        return kecamatan;
    }

    public void setKecamatan(String kecamatan) {
        this.kecamatan = kecamatan;
    }

    public Double getMaxDistanceKm() {
        return maxDistanceKm;
    }

    public void setMaxDistanceKm(Double maxDistanceKm) {
        this.maxDistanceKm = maxDistanceKm;
    }

    /** Salinan dangkal agar sheet filter bisa diedit tanpa mengubah kriteria aktif. */
    public KostFilterCriteria copy() {
        KostFilterCriteria c = new KostFilterCriteria();
        c.keyword = keyword;
        c.maxHarga = maxHarga;
        c.minHarga = minHarga;
        c.tipeKost = tipeKost;
        c.status = status;
        c.idWilayah = idWilayah;
        c.fasilitasIds = fasilitasIds != null ? new ArrayList<>(fasilitasIds) : new ArrayList<>();
        c.sortBy = sortBy;
        c.kecamatan = kecamatan;
        c.maxDistanceKm = maxDistanceKm;
        c.originLat = originLat;
        c.originLng = originLng;
        c.originLabel = originLabel;
        return c;
    }

    public void reset() {
        this.originLat = null;
        this.originLng = null;
        this.originLabel = null;
        this.kecamatan = null;
        this.maxDistanceKm = null;
        this.keyword = null;
        this.maxHarga = null;
        this.minHarga = null;
        this.tipeKost = null;
        this.status = null;
        this.idWilayah = null;
        this.fasilitasIds = new ArrayList<>();
        this.sortBy = "TERBARU";
    }
}
