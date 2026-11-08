package com.carikostkita.data.remote;

import com.google.gson.JsonObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Endpoint FastAPI untuk Bulk Import Kost oleh Developer / Super Admin.
 */
public interface FastApiAdminBulkImportService {

    @GET("api/v1/admin/kosts/import/template")
    Call<ResponseBody> downloadTemplate(@Header("Authorization") String bearerToken);

    @POST("api/v1/admin/kosts/import/preview")
    Call<JsonObject> previewBulkImport(
            @Header("Authorization") String bearerToken,
            @Body JsonObject body
    );

    @POST("api/v1/admin/kosts/import/commit")
    Call<JsonObject> commitBulkImport(
            @Header("Authorization") String bearerToken,
            @Body JsonObject body
    );
}
