package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Response wrapper for GET /api/v1/kost/{kost_id}. */
public class KostDetailResponse {
    @SerializedName("data")
    public KostDto data;
}
