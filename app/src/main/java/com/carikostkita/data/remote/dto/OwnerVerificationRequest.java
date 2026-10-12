package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Application details only; the client cannot choose user ID, role, or status. */
public class OwnerVerificationRequest {
    @SerializedName("submission_note")
    public final String submissionNote;

    public OwnerVerificationRequest(String submissionNote) {
        this.submissionNote = submissionNote;
    }
}
