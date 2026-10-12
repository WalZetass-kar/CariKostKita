package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Safe public review fields; internal user ID is intentionally omitted. */
public class ReviewDto {
    @SerializedName("id") public String id;
    @SerializedName("kost_id") public String kostId;
    @SerializedName("nama_user") public String namaUser;
    @SerializedName("rating") public int rating;
    @SerializedName("komentar") public String komentar;
    @SerializedName("created_at") public String createdAt;
}
