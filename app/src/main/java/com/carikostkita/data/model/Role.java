package com.carikostkita.data.model;

public enum Role {
    USER,
    PEMILIK_KOST,
    /** Super Admin (developer). */
    ADMIN,
    /** Staf moderasi: memverifikasi & menangguhkan, tidak bisa menghapus akun atau mengangkat staf. */
    MODERATOR;

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
            case "moderator":
                return MODERATOR;
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
            case MODERATOR:
                return "moderator";
            case USER:
            default:
                return "user";
        }
    }

    public boolean isPemilikKost() {
        return this == PEMILIK_KOST;
    }

    /** Staf (Super Admin atau Moderator): akses dashboard moderasi. */
    public boolean isDeveloper() {
        return this == ADMIN || this == MODERATOR;
    }

    public boolean isAdmin() {
        return isDeveloper();
    }

    public boolean isSuperAdmin() {
        return this == ADMIN;
    }

    public String getDisplayName() {
        switch (this) {
            case PEMILIK_KOST:
                return "Pemilik Kost";
            case ADMIN:
                return "Super Admin";
            case MODERATOR:
                return "Moderator";
            case USER:
            default:
                return "Pencari Kost";
        }
    }
}
