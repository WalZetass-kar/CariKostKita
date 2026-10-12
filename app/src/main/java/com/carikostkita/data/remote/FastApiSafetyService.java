package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.UserReportCreateRequest;
import com.carikostkita.data.remote.dto.UserReportResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** User safety operations authenticated by the Supabase access token. */
public interface FastApiSafetyService {
    @POST("api/v1/me/user-reports/{chatId}")
    Call<UserReportResponse> reportUser(
            @Header("Authorization") String bearerToken,
            @Path("chatId") long chatId,
            @Body UserReportCreateRequest body
    );
}
