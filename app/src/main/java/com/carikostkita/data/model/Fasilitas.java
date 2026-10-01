package com.carikostkita.data.model;

import java.io.Serializable;

public class Fasilitas implements Serializable {
    private int idFasilitas;
    private String namaFasilitas;
    private boolean isSelected;

    public Fasilitas() {
    }

    public Fasilitas(int idFasilitas, String namaFasilitas) {
        this.idFasilitas = idFasilitas;
        this.namaFasilitas = namaFasilitas;
    }

    public int getIdFasilitas() {
        return idFasilitas;
    }

    public void setIdFasilitas(int idFasilitas) {
        this.idFasilitas = idFasilitas;
    }

    public String getNamaFasilitas() {
        return namaFasilitas;
    }

    public void setNamaFasilitas(String namaFasilitas) {
        this.namaFasilitas = namaFasilitas;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Fasilitas fasilitas = (Fasilitas) o;
        return idFasilitas == fasilitas.idFasilitas;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idFasilitas);
    }
}
