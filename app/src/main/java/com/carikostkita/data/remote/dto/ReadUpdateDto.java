package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** DTO untuk menandai pesan sudah dibaca */
public class ReadUpdateDto {

    @SerializedName("is_read")
    public boolean isRead = true;
}
