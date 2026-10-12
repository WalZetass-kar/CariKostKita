package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Minimal survey representation returned by FastAPI. */
public class SurveyDto {
    @SerializedName("id") public String id;
    @SerializedName("kost_id") public String kostId;
    @SerializedName("pencari_id") public String pencariId;
    @SerializedName("owner_id") public String ownerId;
    @SerializedName("jadwal") public String jadwal;
    @SerializedName("catatan") public String catatan;
    @SerializedName("status") public String status;
    @SerializedName("alasan") public String alasan;
    @SerializedName("created_at") public String createdAt;
    @SerializedName("nama_kost") public String namaKost;
    @SerializedName("viewer_role") public String viewerRole;
}
