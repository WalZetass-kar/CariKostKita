package com.carikostkita.data.model;

public enum Role {
    USER,
    PEMILIK_KOST,
    ADMIN;

    public static Role fromString(String roleStr) {
        if (roleStr == null) return USER;
        String normalized = roleStr.trim().toLowerCase();
        switch (normalized) {
            case "owner":
            case "pemilik_kost":
                return PEMILIK_KOST;
            case "developer":
            case "admin":
                return ADMIN;
            case "user":
            default:
                try {
                    return Role.valueOf(roleStr.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    return USER;
                }
        }
    }

    public String toSupabaseRole() {
        switch (this) {
            case PEMILIK_KOST:
                return "owner";
            case ADMIN:
                return "developer";
            case USER:
            default:
                return "user";
        }
    }

    public boolean isPemilikKost() {
        return this == PEMILIK_KOST;
    }

    public boolean isDeveloper() {
        return this == ADMIN;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public String getDisplayName() {
        switch (this) {
            case PEMILIK_KOST:
                return "Pemilik Kost";
            case ADMIN:
                return "Developer";
            case USER:
            default:
                return "Pencari Kost";
        }
    }
}
