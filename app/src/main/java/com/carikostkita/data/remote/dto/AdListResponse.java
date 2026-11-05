package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AdListResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    public List<AdDto> data;
}
