package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.ReviewListResponse;
import com.carikostkita.data.remote.dto.ReviewResponse;
import com.carikostkita.data.remote.dto.ReviewSubmitRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.PUT;
import retrofit2.http.Query;

/** Public review reads and authenticated survey-eligible upserts. */
public interface FastApiReviewService {
    @GET("api/v1/reviews/{kostId}")
    Call<ReviewListResponse> list(
            @Header("Authorization") String bearerToken,
            @Path("kostId") long kostId,
            @Query("page") int page,
            @Query("page_size") int pageSize
    );

    @PUT("api/v1/reviews/{kostId}")
    Call<ReviewResponse> submit(
            @Header("Authorization") String bearerToken,
            @Path("kostId") long kostId,
            @Body ReviewSubmitRequest body
    );
}
