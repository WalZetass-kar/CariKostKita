package com.carikostkita.data.remote;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

/** Owner aggregates are resolved from the authenticated identity, never a client owner ID. */
public interface FastApiOwnerStatsService {
    @GET("api/v1/me/owner-stats/favorites")
    Call<JsonObject> getMyFavoriteCount(@Header("Authorization") String bearerToken);
}
