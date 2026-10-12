package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.AvatarUploadResponse;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/** Avatar upload; object path and owner UUID are assigned by FastAPI. */
public interface FastApiAvatarService {
    @POST("api/v1/me/avatar")
    @Headers("Content-Type: image/jpeg")
    Call<AvatarUploadResponse> uploadMyAvatar(
            @Header("Authorization") String bearerToken,
            @Body RequestBody jpeg
    );
}
