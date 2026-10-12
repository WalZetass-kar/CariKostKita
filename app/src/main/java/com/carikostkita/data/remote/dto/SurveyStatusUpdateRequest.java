package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class SurveyStatusUpdateRequest {
    @SerializedName("status") public String status;
    @SerializedName("alasan") public String alasan;

    public SurveyStatusUpdateRequest(String status, String alasan) {
        this.status = status;
        this.alasan = alasan;
    }
}
