package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Result for a successfully submitted user report. */
public class UserReportResponse {
    @SerializedName("submitted") public boolean submitted;
}
