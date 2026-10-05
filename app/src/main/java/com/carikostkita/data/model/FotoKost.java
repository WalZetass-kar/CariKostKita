package com.carikostkita.data.model;

import java.io.Serializable;

public class FotoKost implements Serializable {
    private int idFoto;
    private String idKostStr;
    private String namaFile;
    private String pathFile;
    private boolean isThumbnail;

    public FotoKost() {
    }

    public FotoKost(int idFoto, int idKost, String namaFile, String pathFile, boolean isThumbnail) {
        this.idFoto = idFoto;
        this.idKostStr = String.valueOf(idKost);
        this.namaFile = namaFile;
        this.pathFile = pathFile;
        this.isThumbnail = isThumbnail;
    }

    public FotoKost(int idFoto, String idKost, String namaFile, String pathFile, boolean isThumbnail) {
        this.idFoto = idFoto;
        this.idKostStr = idKost;
        this.namaFile = namaFile;
        this.pathFile = pathFile;
        this.isThumbnail = isThumbnail;
    }

    public int getIdFoto() {
        return idFoto;
    }

    public void setIdFoto(int idFoto) {
        this.idFoto = idFoto;
    }

    public int getIdKost() {
        try {
            return Integer.parseInt(idKostStr);
        } catch (Exception e) {
            return idKostStr != null ? Math.abs(idKostStr.hashCode()) : 0;
        }
    }

    public String getIdKostString() {
        return idKostStr;
    }

    public void setIdKost(int idKost) {
        this.idKostStr = String.valueOf(idKost);
    }

    public void setIdKost(String idKost) {
        this.idKostStr = idKost;
    }

    public String getNamaFile() {
        return namaFile;
    }

    public void setNamaFile(String namaFile) {
        this.namaFile = namaFile;
    }

    public String getPathFile() {
        return pathFile;
    }

    public void setPathFile(String pathFile) {
        this.pathFile = pathFile;
    }

    public boolean isThumbnail() {
        return isThumbnail;
    }

    public void setThumbnail(boolean thumbnail) {
        isThumbnail = thumbnail;
    }
}
