package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.BlockStatusResponse;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** User-block operations; blocker identity is never sent by the client. */
public interface FastApiBlockService {
    @GET("api/v1/me/blocks/{otherUserId}")
    Call<BlockStatusResponse> readStatus(
            @Header("Authorization") String bearerToken,
            @Path("otherUserId") long otherUserId
    );

    @POST("api/v1/me/blocks/{otherUserId}")
    Call<BlockStatusResponse> block(
            @Header("Authorization") String bearerToken,
            @Path("otherUserId") long otherUserId
    );

    @DELETE("api/v1/me/blocks/{otherUserId}")
    Call<BlockStatusResponse> unblock(
            @Header("Authorization") String bearerToken,
            @Path("otherUserId") long otherUserId
    );
}
