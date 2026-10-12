package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.AuthRequest;
import com.carikostkita.data.remote.dto.AuthResponse;
import com.carikostkita.data.remote.dto.IdTokenRequest;
import com.carikostkita.data.remote.dto.RefreshRequest;
import com.carikostkita.data.remote.dto.SignUpRequest;
import com.carikostkita.data.remote.dto.UpdatePasswordRequest;
import java.util.Map;
import okhttp3.ResponseBody;
import okhttp3.HttpUrl;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

/** FastAPI adapter for Supabase Auth; token issuance/verification remains with Supabase Auth. */
public interface FastApiAuthService {

    static String googleAuthorizationUrl() {
        HttpUrl base = HttpUrl.get(com.carikostkita.BuildConfig.API_BASE_URL);
        return base.newBuilder()
                .addPathSegments("api/v1/auth/authorize")
                .addQueryParameter("provider", "google")
                .addQueryParameter("redirect_to", "carikostkita://login-callback")
                .build()
                .toString();
    }

    @POST("api/v1/auth/login")
    Call<AuthResponse> signInWithPassword(@Body AuthRequest body);

    @POST("api/v1/auth/signup")
    Call<AuthResponse> signUp(@Body SignUpRequest body);

    @POST("api/v1/auth/logout")
    Call<ResponseBody> signOut(
            @Header("Authorization") String bearerToken,
            @Query("scope") String scope
    );

    @POST("api/v1/auth/refresh")
    Call<AuthResponse> refreshToken(@Body RefreshRequest body);

    @PUT("api/v1/auth/password")
    Call<ResponseBody> updatePassword(
            @Header("Authorization") String bearerToken,
            @Body UpdatePasswordRequest body
    );

    @POST("api/v1/auth/google")
    Call<AuthResponse> signInWithGoogleIdToken(@Body IdTokenRequest body);

    @POST("api/v1/auth/recover")
    Call<ResponseBody> recoverPassword(
            @Body Map<String, String> body,
            @Query("redirect_to") String redirectTo
    );

    @GET("api/v1/auth/user")
    Call<AuthResponse.AuthUser> getCurrentAuthUser(
            @Header("Authorization") String bearerToken
    );
}
