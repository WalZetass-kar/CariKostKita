package com.carikostkita.data.location;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface NominatimApiService {

    @GET("reverse?format=jsonv2&addressdetails=1")
    Call<NominatimPlace> reverseGeocode(
        @Header("User-Agent") String userAgent,
        @Query("lat") double latitude,
        @Query("lon") double longitude,
        @Query("accept-language") String acceptLanguage
    );

    @GET("search?format=jsonv2&addressdetails=1&countrycodes=id")
    Call<List<NominatimPlace>> searchPlaces(
        @Header("User-Agent") String userAgent,
        @Query("q") String query,
        @Query("limit") int limit,
        @Query("accept-language") String acceptLanguage
    );
}
