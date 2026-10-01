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
    private String sortBy; // "TERBARU", "TERMURAH", "TERMAHAL"

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

    public void reset() {
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
