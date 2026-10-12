package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.OwnerVerificationRequest;
import com.carikostkita.data.remote.dto.OwnerVerificationResponse;
import com.carikostkita.data.remote.dto.OwnerVerificationQueueResponse;
import com.carikostkita.data.remote.dto.OwnerVerificationReviewRequest;
import com.carikostkita.data.remote.dto.OwnerVerificationReviewResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Self-service verification submission; identity and status are server-derived. */
public interface FastApiOwnerVerificationService {
    @GET("api/v1/admin/owner-verifications")
    Call<OwnerVerificationQueueResponse> listAdminQueue(
            @Header("Authorization") String bearerToken,
            @Query("status") String status,
            @Query("page") int page,
            @Query("page_size") int pageSize
    );

    @PATCH("api/v1/admin/owner-verifications/{applicationId}")
    Call<OwnerVerificationReviewResponse> review(
            @Header("Authorization") String bearerToken,
            @Path("applicationId") String applicationId,
            @Body OwnerVerificationReviewRequest request
    );

    @POST("api/v1/me/owner-verification")
    Call<OwnerVerificationResponse> submit(
            @Header("Authorization") String bearerToken,
            @Body OwnerVerificationRequest request
    );
}
