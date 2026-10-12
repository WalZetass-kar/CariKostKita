package com.carikostkita.data.remote;

import com.google.gson.JsonObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/** Authenticated crash ingestion; FastAPI derives the numeric domain actor from the token. */
public interface FastApiCrashReportService {
    @POST("api/v1/me/crash-reports")
    Call<ResponseBody> submit(
            @Header("Authorization") String bearerToken,
            @Body JsonObject report
    );
}
