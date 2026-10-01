package com.carikostkita.data.model;

import java.io.Serializable;

public class Wilayah implements Serializable {
    private int idWilayah;
    private String kecamatan;
    private String kelurahan;
    private String kota;

    public Wilayah() {
        this.kota = "Pekanbaru";
    }

    public Wilayah(int idWilayah, String kecamatan, String kelurahan, String kota) {
        this.idWilayah = idWilayah;
        this.kecamatan = kecamatan;
        this.kelurahan = kelurahan;
        this.kota = kota != null ? kota : "Pekanbaru";
    }

    public int getIdWilayah() {
        return idWilayah;
    }

    public void setIdWilayah(int idWilayah) {
        this.idWilayah = idWilayah;
    }

    public String getKecamatan() {
        return kecamatan;
    }

    public void setKecamatan(String kecamatan) {
        this.kecamatan = kecamatan;
    }

    public String getKelurahan() {
        return kelurahan;
    }

    public void setKelurahan(String kelurahan) {
        this.kelurahan = kelurahan;
    }

    public String getKota() {
        return kota;
    }

    public void setKota(String kota) {
        this.kota = kota;
    }

    public String getNamaLengkap() {
        return kelurahan + ", " + kecamatan + ", " + kota;
    }

    @Override
    public String toString() {
        return kelurahan + " (" + kecamatan + ")";
    }
}
