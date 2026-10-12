package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Safe admin projection returned by GET /api/v1/admin/reports. */
public class AdminReportDto {
    @SerializedName("id") public String id;
    @SerializedName("kost_id") public String kostId;
    @SerializedName("reporter_id") public String reporterId;
    @SerializedName("reason") public String reason;
    @SerializedName("description") public String description;
    @SerializedName("status") public String status;
    @SerializedName("created_at") public String createdAt;
    @SerializedName("updated_at") public String updatedAt;
    @SerializedName("kost_name") public String kostName;
    @SerializedName("reporter_name") public String reporterName;
    @SerializedName("admin_action") public String adminAction;
}
