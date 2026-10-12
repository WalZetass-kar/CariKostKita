package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Exact count of actionable reports visible to the authenticated staff account through RLS. */
public class PendingReportCountResponse {
    @SerializedName("pending_count")
    public Integer pendingCount;
}
