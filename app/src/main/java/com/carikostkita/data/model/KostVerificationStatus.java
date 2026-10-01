package com.carikostkita.data.model;

public enum KostVerificationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    REVISION_REQUIRED;

    public static KostVerificationStatus fromString(String statusStr) {
        if (statusStr == null) return PENDING;
        try {
            return KostVerificationStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }

    public String getDisplayName() {
        switch (this) {
            case APPROVED:
                return "Disetujui";
            case REVISION_REQUIRED:
                return "Perlu Revisi";
            case REJECTED:
                return "Ditolak";
            case PENDING:
            default:
                return "Menunggu Verifikasi";
        }
    }

    public boolean isApproved() {
        return this == APPROVED;
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isRevisionRequired() {
        return this == REVISION_REQUIRED;
    }

    public boolean isRejected() {
        return this == REJECTED;
    }
}
