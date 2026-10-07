package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.AuthRequest;
import com.carikostkita.data.remote.dto.AuthResponse;
import com.carikostkita.data.remote.dto.IdTokenRequest;
import com.carikostkita.data.remote.dto.RefreshRequest;
import com.carikostkita.data.remote.dto.SignUpRequest;
import com.carikostkita.data.remote.dto.UpdatePasswordRequest;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;

/**
 * Retrofit interface untuk Supabase Auth (GoTrue) endpoints.
 */
public interface SupabaseAuthService {

    /** Login dengan email + password */
    @POST("auth/v1/token?grant_type=password")
    Call<AuthResponse> signInWithPassword(@Body AuthRequest body);

    /** Daftar akun baru */
    @POST("auth/v1/signup")
    Call<AuthResponse> signUp(@Body SignUpRequest body);

    /** Logout — invalidasi session di server */
    @POST("auth/v1/logout")
    Call<ResponseBody> signOut();

    /** Refresh access token menggunakan refresh token */
    @POST("auth/v1/token?grant_type=refresh_token")
    Call<AuthResponse> refreshToken(@Body RefreshRequest body);

    /** Ganti password (memerlukan access token aktif di header) */
    @PUT("auth/v1/user")
    Call<ResponseBody> updatePassword(@Body UpdatePasswordRequest body);

    /** Tukar Google ID token dengan Supabase session */
    @POST("auth/v1/token?grant_type=id_token")
    Call<AuthResponse> signInWithGoogleIdToken(@Body IdTokenRequest body);

    /** Kirim email reset kata sandi */
    @POST("auth/v1/recover")
    Call<ResponseBody> recoverPassword(
            @Body java.util.Map<String, String> body,
            @retrofit2.http.Query("redirect_to") String redirectTo
    );

    /** Ambil user profile dari Supabase Auth (/auth/v1/user) */
    @retrofit2.http.GET("auth/v1/user")
    Call<AuthResponse.AuthUser> getCurrentAuthUser();
}
