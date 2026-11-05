package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.AdListResponse;
import retrofit2.Call;
import retrofit2.http.GET;

public interface FastApiSettingsService {
    @GET("api/v1/public/settings/ads")
    Call<AdListResponse> getActiveAds();
}
