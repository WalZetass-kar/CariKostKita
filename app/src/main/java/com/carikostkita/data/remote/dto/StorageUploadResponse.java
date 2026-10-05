package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Response dari Supabase Storage setelah upload file */
public class StorageUploadResponse {

    @SerializedName("Key")
    public String key;

    @SerializedName("Id")
    public String id;
}
