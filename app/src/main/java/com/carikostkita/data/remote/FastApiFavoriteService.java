package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.FavoriteListResponse;
import com.carikostkita.data.remote.dto.FavoriteResponse;
import com.google.gson.JsonObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** User-scoped favorite operations; identity is derived from the bearer token. */
public interface FastApiFavoriteService {
    @GET("api/v1/favorites")
    Call<FavoriteListResponse> listFavorites(@Header("Authorization") String bearerToken);

    @GET("api/v1/favorites/availability")
    Call<JsonObject> listFavoriteAvailability(@Header("Authorization") String bearerToken);

    @GET("api/v1/favorites/{kostId}")
    Call<FavoriteResponse> readFavorite(
            @Header("Authorization") String bearerToken,
            @Path("kostId") String kostId
    );

    @POST("api/v1/favorites/{kostId}")
    Call<FavoriteResponse> addFavorite(
            @Header("Authorization") String bearerToken,
            @Path("kostId") String kostId
    );

    @DELETE("api/v1/favorites/{kostId}")
    Call<ResponseBody> removeFavorite(
            @Header("Authorization") String bearerToken,
            @Path("kostId") String kostId
    );
}
