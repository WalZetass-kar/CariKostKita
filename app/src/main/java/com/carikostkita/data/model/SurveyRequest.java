package com.carikostkita.data.model;

/** Permintaan survei lokasi dari pencari ke pemilik. */
public class SurveyRequest {
    public static final String MENUNGGU = "MENUNGGU";
    public static final String DIKONFIRMASI = "DIKONFIRMASI";
    public static final String DITOLAK = "DITOLAK";
    public static final String DIBATALKAN = "DIBATALKAN";
    public static final String SELESAI = "SELESAI";

    public String id;
    public String kostId;
    public String namaKost;
    public String pencariId;
    public String namaPencari;
    public String ownerId;
    public String jadwal;
    public String catatan;
    public String status;
    public String alasan;
    public String createdAt;

    public String getStatusLabel() {
        if (DIKONFIRMASI.equals(status)) return "Dikonfirmasi";
        if (DITOLAK.equals(status)) return "Ditolak";
        if (DIBATALKAN.equals(status)) return "Dibatalkan";
        if (SELESAI.equals(status)) return "Selesai";
        return "Menunggu konfirmasi";
    }

    public boolean isActive() {
        return MENUNGGU.equals(status) || DIKONFIRMASI.equals(status);
    }
}
