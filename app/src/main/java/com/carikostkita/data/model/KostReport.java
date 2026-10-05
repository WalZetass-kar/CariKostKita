package com.carikostkita.data.model;

import java.io.Serializable;

public class KostReport implements Serializable {
    private String id;
    private String kostId;
    private String reporterId;
    private String ownerId;
    private String kategoriLaporan;
    private String deskripsi;
    private ReportStatus status;
    private String tindakanAdmin;
    private String createdAt;
    private String updatedAt;

    // Display fields
    private String namaKost;
    private String namaReporter;
    private String namaPemilik;

    public KostReport() {
        this.status = ReportStatus.BARU;
    }

    public KostReport(String kostId, String reporterId, String ownerId, String kategoriLaporan, String deskripsi) {
        this.kostId = kostId;
        this.reporterId = reporterId;
        this.ownerId = ownerId;
        this.kategoriLaporan = kategoriLaporan;
        this.deskripsi = deskripsi;
        this.status = ReportStatus.BARU;
    }

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdReport() {
        return getId();
    }

    public void setIdReport(String id) {
        setId(id);
    }

    public String getKostId() {
        return kostId != null ? kostId : "";
    }

    public void setKostId(String kostId) {
        this.kostId = kostId;
    }

    public String getIdKost() {
        return getKostId();
    }

    public void setIdKost(String kostId) {
        setKostId(kostId);
    }

    public String getReporterId() {
        return reporterId != null ? reporterId : "";
    }

    public void setReporterId(String reporterId) {
        this.reporterId = reporterId;
    }

    public String getIdReporter() {
        return getReporterId();
    }

    public void setIdReporter(String reporterId) {
        setReporterId(reporterId);
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

    public String getKategoriLaporan() {
        return kategoriLaporan != null ? kategoriLaporan : "Laporan Umum";
    }

    public void setKategoriLaporan(String kategoriLaporan) {
        this.kategoriLaporan = kategoriLaporan;
    }

    public String getDeskripsi() {
        return deskripsi != null ? deskripsi : "";
    }

    public void setDeskripsi(String deskripsi) {
        this.deskripsi = deskripsi;
    }

    public ReportStatus getStatus() {
        return status != null ? status : ReportStatus.BARU;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public String getTindakanAdmin() {
        return tindakanAdmin != null ? tindakanAdmin : "";
    }

    public void setTindakanAdmin(String tindakanAdmin) {
        this.tindakanAdmin = tindakanAdmin;
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

    public String getNamaKost() {
        return namaKost != null ? namaKost : "Kost";
    }

    public void setNamaKost(String namaKost) {
        this.namaKost = namaKost;
    }

    public String getNamaReporter() {
        return namaReporter != null ? namaReporter : "Pengguna";
    }

    public void setNamaReporter(String namaReporter) {
        this.namaReporter = namaReporter;
    }

    public String getNamaPemilik() {
        return namaPemilik != null ? namaPemilik : "Pemilik";
    }

    public void setNamaPemilik(String namaPemilik) {
        this.namaPemilik = namaPemilik;
    }

    public String getAlasan() {
        return getKategoriLaporan();
    }

    public String getNamaPelapor() {
        return getNamaReporter();
    }
}
