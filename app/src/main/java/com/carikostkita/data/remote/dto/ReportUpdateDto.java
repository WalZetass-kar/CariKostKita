package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** DTO untuk update laporan di Supabase */
public class ReportUpdateDto {

    @SerializedName("status")
    public String status;

    @SerializedName("tindakan_admin")
    public String tindakanAdmin;
}
