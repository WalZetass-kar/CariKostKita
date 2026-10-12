package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Identity and display name are derived by FastAPI, never accepted from the app. */
public class ReviewSubmitRequest {
    @SerializedName("rating") public int rating;
    @SerializedName("komentar") public String komentar;

    public ReviewSubmitRequest(int rating, String komentar) {
        this.rating = rating;
        this.komentar = komentar;
    }
}
