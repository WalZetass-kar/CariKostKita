package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.ActivityLogCreateRequest;
import com.carikostkita.data.remote.dto.ActivityLogListResponse;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** Activity log adapter; the API derives actor identity from the bearer token. */
public interface FastApiActivityLogService {
    @POST("api/v1/me/activity-logs")
    Call<ResponseBody> createMyLog(
            @Header("Authorization") String bearerToken,
            @Body ActivityLogCreateRequest body
    );

    @GET("api/v1/admin/activity-logs")
    Call<ActivityLogListResponse> listAdminLogs(
            @Header("Authorization") String bearerToken,
            @Query("page") int page,
            @Query("page_size") int pageSize
    );
}
