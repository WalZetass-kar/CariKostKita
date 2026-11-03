package com.carikostkita.data.remote.dto;

import com.carikostkita.data.model.RoommatePost;
import com.google.gson.annotations.SerializedName;

public class RoommateResponse {
    @SerializedName("message")
    public String message;

    @SerializedName("data")
    public RoommatePost data;
}
