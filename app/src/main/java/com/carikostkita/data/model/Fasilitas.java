package com.carikostkita.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Fasilitas implements Serializable {

    /**
     * Daftar fasilitas standar. Nama di sini adalah nilai yang disimpan di kolom
     * kosts.fasilitas, jadi form, filter, dan peta harus memakai daftar yang sama.
     */
    private static final List<Fasilitas> MASTER;
    static {
        List<Fasilitas> list = new ArrayList<>();
        list.add(new Fasilitas(1, "WiFi Cepat"));
        list.add(new Fasilitas(2, "Parkir Motor"));
        list.add(new Fasilitas(3, "Parkir Mobil"));
        list.add(new Fasilitas(4, "AC Dingin"));
        list.add(new Fasilitas(5, "Kamar Mandi Dalam"));
        list.add(new Fasilitas(6, "Kasur Springbed"));
        list.add(new Fasilitas(7, "Lemari Pakaian"));
        list.add(new Fasilitas(8, "Meja & Kursi Belajar"));
        list.add(new Fasilitas(9, "Dapur Bersama"));
        list.add(new Fasilitas(10, "Listrik Termasuk"));
        list.add(new Fasilitas(11, "Akses 24 Jam"));
        list.add(new Fasilitas(12, "CCTV & Keamanan"));
        list.add(new Fasilitas(13, "Air Bersih 24 Jam"));
        MASTER = Collections.unmodifiableList(list);
    }

    public static List<Fasilitas> getMaster() {
        List<Fasilitas> copy = new ArrayList<>();
        for (Fasilitas f : MASTER) copy.add(new Fasilitas(f.idFasilitas, f.namaFasilitas));
        return copy;
    }

    /** ID master untuk nama fasilitas, atau 0 bila bukan fasilitas standar. */
    public static int idForName(String name) {
        if (name == null) return 0;
        for (Fasilitas f : MASTER) {
            if (f.namaFasilitas.equalsIgnoreCase(name.trim())) return f.idFasilitas;
        }
        return 0;
    }

    public static String nameForId(int id) {
        for (Fasilitas f : MASTER) {
            if (f.idFasilitas == id) return f.namaFasilitas;
        }
        return null;
    }
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
