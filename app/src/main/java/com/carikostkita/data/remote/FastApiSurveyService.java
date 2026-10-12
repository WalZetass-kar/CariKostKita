package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.SurveyCreateRequest;
import com.carikostkita.data.remote.dto.SurveyEligibilityResponse;
import com.carikostkita.data.remote.dto.SurveyListResponse;
import com.carikostkita.data.remote.dto.SurveyResponse;
import com.carikostkita.data.remote.dto.SurveyStatusUpdateRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Participant-scoped survey requests via FastAPI. */
public interface FastApiSurveyService {
    @GET("api/v1/surveys")
    Call<SurveyListResponse> list(
            @Header("Authorization") String bearerToken,
            @Query("scope") String scope,
            @Query("kost_id") Long kostId,
            @Query("page") int page,
            @Query("page_size") int pageSize
    );

    @POST("api/v1/surveys")
    Call<SurveyResponse> create(
            @Header("Authorization") String bearerToken,
            @Body SurveyCreateRequest body
    );

    @GET("api/v1/surveys/eligibility/{kostId}")
    Call<SurveyEligibilityResponse> reviewEligibility(
            @Header("Authorization") String bearerToken,
            @Path("kostId") long kostId
    );

    @PATCH("api/v1/surveys/{surveyId}")
    Call<SurveyResponse> updateStatus(
            @Header("Authorization") String bearerToken,
            @Path("surveyId") long surveyId,
            @Body SurveyStatusUpdateRequest body
    );
}
