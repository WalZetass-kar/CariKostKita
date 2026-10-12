package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.AdminUserListResponse;
import com.carikostkita.data.remote.dto.AdminUserModerationRequest;
import com.carikostkita.data.remote.dto.AdminUserModerationResponse;
import retrofit2.http.Body;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Read-only admin user directory; role is validated server-side from the JWT. */
public interface FastApiAdminUserService {
    @GET("api/v1/admin/users")
    Call<AdminUserListResponse> listUsers(
            @Header("Authorization") String bearerToken,
            @Query("page") int page,
            @Query("page_size") int pageSize,
            @Query("role") String role,
            @Query("verification_status") String verificationStatus
    );

    @PATCH("api/v1/admin/users/{userId}")
    Call<AdminUserModerationResponse> moderateUser(
            @Header("Authorization") String bearerToken,
            @Path("userId") String userId,
            @Body AdminUserModerationRequest request
    );
}
