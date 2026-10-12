package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Strict FastAPI report decision payload; it contains no actor/user ID. */
public class ReportModerationRequest {
    @SerializedName("status")
    public final String status;

    @SerializedName("admin_action")
    public final String adminAction;

    public ReportModerationRequest(String status, String adminAction) {
        this.status = status;
        this.adminAction = adminAction;
    }
}
