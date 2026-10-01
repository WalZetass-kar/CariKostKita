package com.carikostkita.data.model;

public enum StatusKost {
    TERSEDIA("Tersedia"),
    PENUH("Penuh"),
    TIDAK_AKTIF("Tidak Aktif");

    private final String displayName;

    StatusKost(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static StatusKost fromString(String statusStr) {
        if (statusStr == null) return TERSEDIA;
        try {
            return StatusKost.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TERSEDIA;
        }
    }
}
