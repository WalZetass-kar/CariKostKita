package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Response wrapper for POST /api/v1/favorites/{kost_id}. */
public class FavoriteResponse {
    @SerializedName("data")
    public FavoriteDto data;
}
