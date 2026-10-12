package com.carikostkita.data.remote;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;
import retrofit2.http.POST;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.Body;

/** Owner-only listing status projection; ownership is derived from the bearer token. */
public interface FastApiOwnerKostService {
    @GET("api/v1/me/owner-kosts")
    Call<JsonObject> listMyOwnerKosts(
            @Header("Authorization") String bearerToken,
            @Query("page") int page,
            @Query("page_size") int pageSize
    );

    @POST("api/v1/me/owner-kosts/{kostId}/resubmit")
    Call<JsonObject> resubmitMyKost(
            @Header("Authorization") String bearerToken,
            @Path("kostId") String kostId
    );

    @PATCH("api/v1/me/owner-kosts/{kostId}/status")
    Call<JsonObject> updateMyKostStatus(
            @Header("Authorization") String bearerToken,
            @Path("kostId") String kostId,
            @Body JsonObject body
    );
}
