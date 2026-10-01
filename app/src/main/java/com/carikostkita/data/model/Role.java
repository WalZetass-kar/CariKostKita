package com.carikostkita.data.model;

public enum Role {
    USER,
    PEMILIK_KOST,
    ADMIN;

    public static Role fromString(String roleStr) {
        if (roleStr == null) return USER;
        try {
            return Role.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return USER;
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
