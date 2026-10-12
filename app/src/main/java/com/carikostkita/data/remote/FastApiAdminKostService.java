package com.carikostkita.data.remote;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.Path;

/** Staff-only listing moderation operations. Server verifies the staff role. */
public interface FastApiAdminKostService {
    @PATCH("api/v1/admin/kosts/{kostId}/status")
    Call<JsonObject> updateKostStatus(
            @Header("Authorization") String bearerToken,
            @Path("kostId") String kostId,
            @Body JsonObject body
    );
}
