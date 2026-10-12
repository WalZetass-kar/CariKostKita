package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Response wrapper for GET /api/v1/favorites. */
public class FavoriteListResponse {
    @SerializedName("data")
    public List<FavoriteDto> data;
}
