package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CreateChatRequest {
    @SerializedName("kost_id")
    public final String kostId;

    public CreateChatRequest(String kostId) {
        this.kostId = kostId;
    }
}
