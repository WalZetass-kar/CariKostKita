package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class AvatarUploadResponse {
    @SerializedName("data") public Data data;

    public static class Data {
        @SerializedName("path") public String path;
        @SerializedName("public_url") public String publicUrl;
    }
}
