package com.carikostkita.data.remote;

import com.google.gson.JsonObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** Minimal analytics ingestion; FastAPI derives user_id from the bearer token. */
public interface FastApiAnalyticsService {
    @POST("api/v1/analytics/events")
    Call<ResponseBody> recordEvent(
            @Header("Authorization") String bearerToken,
            @Body JsonObject event
    );

    @GET("api/v1/admin/analytics/crashes")
    Call<JsonObject> recentCrashCount(
            @Header("Authorization") String bearerToken,
            @Query("days") int days
    );

    @GET("api/v1/admin/analytics/funnel")
    Call<JsonObject> adminFunnel(
            @Header("Authorization") String bearerToken,
            @Query("days") int days
    );
}
