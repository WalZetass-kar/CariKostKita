package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Response wrapper for GET /api/v1/kost. */
public class KostListResponse {
    @SerializedName("data")
    public List<KostDto> data;
}
