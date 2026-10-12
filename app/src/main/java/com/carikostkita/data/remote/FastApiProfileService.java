package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.ProfileUpdateRequest;
import com.carikostkita.data.remote.dto.UserProfileResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.PUT;

/** User-scoped profile API; the server derives the profile ID from the verified JWT. */
public interface FastApiProfileService {
    @GET("api/v1/me/profile")
    Call<UserProfileResponse> readMyProfile(@Header("Authorization") String bearerToken);

    @PATCH("api/v1/me/profile")
    Call<UserProfileResponse> updateMyProfile(
            @Header("Authorization") String bearerToken,
            @Body ProfileUpdateRequest body
    );

    @PUT("api/v1/me/profile")
    Call<UserProfileResponse> provisionMyProfile(
            @Header("Authorization") String bearerToken,
            @Body ProfileUpdateRequest body
    );
}
