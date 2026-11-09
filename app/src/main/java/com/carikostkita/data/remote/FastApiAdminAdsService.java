package com.carikostkita.data.remote;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FastApiAdminAdsService {
    @GET("api/v1/admin/ads")
    Call<JsonObject> listAds(
            @Header("Authorization") String bearerToken,
            @Query("page") int page,
            @Query("page_size") int pageSize
    );

    @POST("api/v1/admin/ads")
    Call<JsonObject> createAd(
            @Header("Authorization") String bearerToken,
            @Body JsonObject body
    );

    @PATCH("api/v1/admin/ads/{adId}")
    Call<JsonObject> updateAd(
            @Header("Authorization") String bearerToken,
            @Path("adId") long adId,
            @Body JsonObject body
    );

    @DELETE("api/v1/admin/ads/{adId}")
    Call<JsonObject> deleteAd(
            @Header("Authorization") String bearerToken,
            @Path("adId") long adId
    );
}
