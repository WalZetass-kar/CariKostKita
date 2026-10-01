package com.carikostkita.data.model;

public enum VerificationStatus {
    NONE,
    PENDING,
    APPROVED,
    REJECTED,
    REVISION_REQUIRED;

    public static VerificationStatus fromString(String statusStr) {
        if (statusStr == null) return NONE;
        try {
            return VerificationStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }

    public String getDisplayName() {
        switch (this) {
            case PENDING:
                return "Menunggu Verifikasi";
            case APPROVED:
                return "Terverifikasi";
            case REVISION_REQUIRED:
                return "Perlu Perbaikan";
            case REJECTED:
                return "Ditolak";
            case NONE:
            default:
                return "Belum Diajukan";
        }
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isApproved() {
        return this == APPROVED;
    }

    public boolean isRejected() {
        return this == REJECTED;
    }

    public boolean isRevisionRequired() {
        return this == REVISION_REQUIRED;
    }
}
