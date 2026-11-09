package com.carikostkita.data.remote;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FastApiNotificationService {
    @POST("api/v1/notifications/fcm-token")
    Call<JsonObject> registerFcmToken(
            @Header("Authorization") String bearerToken,
            @Body JsonObject body
    );

    @GET("api/v1/notifications")
    Call<JsonObject> listNotifications(
            @Header("Authorization") String bearerToken,
            @Query("page") int page,
            @Query("page_size") int pageSize
    );

    @GET("api/v1/notifications/unread-count")
    Call<JsonObject> getUnreadCount(
            @Header("Authorization") String bearerToken
    );

    @PATCH("api/v1/notifications/{notificationId}/read")
    Call<JsonObject> markAsRead(
            @Header("Authorization") String bearerToken,
            @Path("notificationId") long notificationId
    );

    @PATCH("api/v1/notifications/read-all")
    Call<JsonObject> markAllAsRead(
            @Header("Authorization") String bearerToken
    );
}
