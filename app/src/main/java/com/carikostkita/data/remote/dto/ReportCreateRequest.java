package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Request body for POST /api/v1/reports. */
public class ReportCreateRequest {
    @SerializedName("kost_id") public long kostId;
    @SerializedName("reason") public String reason;
    @SerializedName("description") public String description;

    public ReportCreateRequest(long kostId, String reason, String description) {
        this.kostId = kostId;
        this.reason = reason;
        this.description = description;
    }
}
