package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.UserProfileResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

/** Minimal public owner cards and chat-participant profile lookups. */
public interface FastApiUserLookupService {
    @GET("api/v1/me/contacts/{userId}")
    Call<UserProfileResponse> readChatContact(
            @Header("Authorization") String bearerToken,
            @Path("userId") long userId
    );

    @GET("api/v1/public/owners/{ownerId}")
    Call<UserProfileResponse> readPublicOwner(@Path("ownerId") long ownerId);
}
