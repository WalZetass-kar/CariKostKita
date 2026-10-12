package com.carikostkita.data.remote;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

/** Authenticated, participant-scoped unread message summary. */
public interface FastApiUnreadMessagesService {
    @GET("api/v1/me/unread-messages")
    Call<JsonObject> readMyUnreadMessages(@Header("Authorization") String bearerToken);
}
