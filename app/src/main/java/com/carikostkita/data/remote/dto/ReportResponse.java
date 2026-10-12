package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Minimal response wrapper for a successfully created report. */
public class ReportResponse {
    @SerializedName("data") public CreatedReport data;

    public static class CreatedReport {
        @SerializedName("id") public long id;
    }
}
