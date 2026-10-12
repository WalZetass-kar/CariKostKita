package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Review decision only; application owner/role are never supplied by the client. */
public class OwnerVerificationReviewRequest {
    @SerializedName("status") public final String status;
    @SerializedName("review_note") public final String reviewNote;

    public OwnerVerificationReviewRequest(String status, String reviewNote) {
        this.status = status;
        this.reviewNote = reviewNote;
    }
}
