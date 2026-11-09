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

public interface FastApiOwnerRoomService {
    @GET("api/v1/owner/kosts/{kostId}/rooms")
    Call<JsonObject> listRooms(
            @Header("Authorization") String bearerToken,
            @Path("kostId") long kostId
    );

    @POST("api/v1/owner/kosts/{kostId}/rooms")
    Call<JsonObject> createRoom(
            @Header("Authorization") String bearerToken,
            @Path("kostId") long kostId,
            @Body JsonObject body
    );

    @PATCH("api/v1/owner/kosts/{kostId}/rooms/{roomId}")
    Call<JsonObject> updateRoom(
            @Header("Authorization") String bearerToken,
            @Path("kostId") long kostId,
            @Path("roomId") long roomId,
            @Body JsonObject body
    );

    @DELETE("api/v1/owner/kosts/{kostId}/rooms/{roomId}")
    Call<JsonObject> deleteRoom(
            @Header("Authorization") String bearerToken,
            @Path("kostId") long kostId,
            @Path("roomId") long roomId
    );
}
