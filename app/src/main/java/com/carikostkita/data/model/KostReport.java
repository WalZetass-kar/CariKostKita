package com.carikostkita.data.model;

import java.io.Serializable;

public class KostReport implements Serializable {
    private int idReport;
    private int idKost;
    private int idReporter;
    private int idPemilik;
    private String kategoriLaporan;
    private String deskripsi;
    private ReportStatus status;
    private String tindakanAdmin;
    private String createdAt;
    private String updatedAt;

    // Display fields populated from JOINs
    private String namaKost;
    private String namaReporter;
    private String namaPemilik;

    public KostReport() {
        this.status = ReportStatus.BARU;
    }

    public KostReport(int idKost, int idReporter, int idPemilik, String kategoriLaporan, String deskripsi) {
        this.idKost = idKost;
        this.idReporter = idReporter;
        this.idPemilik = idPemilik;
        this.kategoriLaporan = kategoriLaporan;
        this.deskripsi = deskripsi;
        this.status = ReportStatus.BARU;
    }

    public int getIdReport() {
        return idReport;
    }

    public void setIdReport(int idReport) {
        this.idReport = idReport;
    }

    public int getIdKost() {
        return idKost;
    }

    public void setIdKost(int idKost) {
        this.idKost = idKost;
    }

    public int getIdReporter() {
        return idReporter;
    }

    public void setIdReporter(int idReporter) {
        this.idReporter = idReporter;
    }

    public int getIdPemilik() {
        return idPemilik;
    }

    public void setIdPemilik(int idPemilik) {
        this.idPemilik = idPemilik;
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
        return namaKost != null ? namaKost : "Kost #" + idKost;
    }

    public void setNamaKost(String namaKost) {
        this.namaKost = namaKost;
    }

    public String getNamaReporter() {
        return namaReporter != null ? namaReporter : "Pengguna #" + idReporter;
    }

    public void setNamaReporter(String namaReporter) {
        this.namaReporter = namaReporter;
    }

    public String getNamaPemilik() {
        return namaPemilik != null ? namaPemilik : "Pemilik #" + idPemilik;
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
