package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class AdDto {
    @SerializedName("id")
    public int id;

    @SerializedName("title")
    public String title;

    @SerializedName("description")
    public String description;

    @SerializedName("image_url")
    public String imageUrl;

    @SerializedName("target_url")
    public String targetUrl;

    @SerializedName("ad_type")
    public String adType;

    @SerializedName("position")
    public String position;

    @SerializedName("platform")
    public String platform;

    @SerializedName("start_date")
    public String startDate;

    @SerializedName("end_date")
    public String endDate;
}
