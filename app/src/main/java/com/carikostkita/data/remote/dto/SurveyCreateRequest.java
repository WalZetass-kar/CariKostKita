package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Caller supplies only the listing and desired time; participant IDs are server-derived. */
public class SurveyCreateRequest {
    @SerializedName("kost_id") public long kostId;
    @SerializedName("jadwal") public String jadwal;
    @SerializedName("catatan") public String catatan;

    public SurveyCreateRequest(long kostId, String jadwal, String catatan) {
        this.kostId = kostId;
        this.jadwal = jadwal;
        this.catatan = catatan;
    }
}
