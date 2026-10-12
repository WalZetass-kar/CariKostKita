package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Request for POST /api/v1/me/user-reports/{chat_id}; reporter ID is omitted. */
public class UserReportCreateRequest {
    @SerializedName("reported_user_id") public long reportedUserId;
    @SerializedName("reason") public String reason;
    @SerializedName("details") public String details;

    public UserReportCreateRequest(long reportedUserId, String reason, String details) {
        this.reportedUserId = reportedUserId;
        this.reason = reason;
        this.details = details;
    }
}
