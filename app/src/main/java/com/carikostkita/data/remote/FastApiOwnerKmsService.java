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
import retrofit2.http.Query;

public interface FastApiOwnerKmsService {
    @GET("api/v1/owner/tenants")
    Call<JsonObject> listTenants(
            @Header("Authorization") String bearerToken,
            @Query("kost_id") Long kostId
    );

    @POST("api/v1/owner/tenants")
    Call<JsonObject> createTenant(
            @Header("Authorization") String bearerToken,
            @Body JsonObject body
    );

    @PATCH("api/v1/owner/tenants/{tenantId}")
    Call<JsonObject> updateTenant(
            @Header("Authorization") String bearerToken,
            @Path("tenantId") long tenantId,
            @Body JsonObject body
    );

    @DELETE("api/v1/owner/tenants/{tenantId}")
    Call<JsonObject> deleteTenant(
            @Header("Authorization") String bearerToken,
            @Path("tenantId") long tenantId
    );

    @GET("api/v1/owner/maintenance-tickets")
    Call<JsonObject> listTickets(
            @Header("Authorization") String bearerToken,
            @Query("kost_id") Long kostId,
            @Query("status") String status
    );

    @POST("api/v1/owner/maintenance-tickets")
    Call<JsonObject> createTicket(
            @Header("Authorization") String bearerToken,
            @Body JsonObject body
    );

    @PATCH("api/v1/owner/maintenance-tickets/{ticketId}")
    Call<JsonObject> updateTicket(
            @Header("Authorization") String bearerToken,
            @Path("ticketId") long ticketId,
            @Body JsonObject body
    );
}
