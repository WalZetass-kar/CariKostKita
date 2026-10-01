package com.carikostkita.data.model;

public enum TipeKost {
    PUTRA("Putra"),
    PUTRI("Putri"),
    CAMPUR("Campur");

    private final String displayName;

    TipeKost(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TipeKost fromString(String tipeStr) {
        if (tipeStr == null) return CAMPUR;
        try {
            return TipeKost.valueOf(tipeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CAMPUR;
        }
    }
}
