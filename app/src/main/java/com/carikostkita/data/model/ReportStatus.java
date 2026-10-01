package com.carikostkita.data.model;

public enum ReportStatus {
    BARU,
    DITINJAU,
    SELESAI,
    DITOLAK;

    public static ReportStatus fromString(String statusStr) {
        if (statusStr == null) return BARU;
        try {
            return ReportStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return BARU;
        }
    }

    public String getDisplayName() {
        switch (this) {
            case DITINJAU:
                return "Sedang Ditinjau";
            case SELESAI:
                return "Selesai / Ditindak";
            case DITOLAK:
                return "Ditolak";
            case BARU:
            default:
                return "Laporan Baru";
        }
    }
}
