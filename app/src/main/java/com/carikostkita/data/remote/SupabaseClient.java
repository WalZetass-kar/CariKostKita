package com.carikostkita.data.remote;

import android.content.Context;
import com.carikostkita.BuildConfig;
import com.carikostkita.data.remote.dto.AuthResponse;
import com.carikostkita.data.remote.dto.RefreshRequest;
import com.carikostkita.util.SessionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.concurrent.TimeUnit;
import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton client untuk semua komunikasi dengan Supabase.
 * Menyediakan Retrofit instance yang dikonfigurasi dengan header Supabase
 * (apikey, Authorization, Content-Type) untuk setiap request, serta
 * refresh token otomatis ketika access token kedaluwarsa.
 */
public class SupabaseClient {

    private static volatile SupabaseClient instance;
    private static Context appContext;

    private final Retrofit retrofit;
    private final Retrofit apiRetrofit;
    private final OkHttpClient okHttpClient;
    private volatile String accessToken = null;
    private final Object refreshLock = new Object();

    private SupabaseClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request origRequest = chain.request();
                    refreshIfExpired();
                    String token = accessToken != null ? accessToken : BuildConfig.SUPABASE_ANON_KEY;
                    Request.Builder builder = origRequest.newBuilder()
                            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                            .header("Authorization", "Bearer " + token);

                    if (origRequest.header("Content-Type") == null) {
                        builder.header("Content-Type", "application/json");
                    }
                    if (origRequest.header("Prefer") == null) {
                        builder.header("Prefer", "return=representation");
                    }
                    return chain.proceed(builder.build());
                })
                .authenticator(new TokenAuthenticator())
                .addInterceptor(loggingInterceptor)
                .build();

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.SUPABASE_URL + "/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        OkHttpClient apiHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .build();
        String apiBaseUrl = BuildConfig.API_BASE_URL.endsWith("/")
                ? BuildConfig.API_BASE_URL
                : BuildConfig.API_BASE_URL + "/";
        apiRetrofit = new Retrofit.Builder()
                .baseUrl(apiBaseUrl)
                .client(apiHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    /** Dipanggil sekali dari Application agar refresh token dapat mengakses sesi. */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
        getInstance();
    }

    public static SupabaseClient getInstance() {
        if (instance == null) {
            synchronized (SupabaseClient.class) {
                if (instance == null) {
                    instance = new SupabaseClient();
                }
            }
        }
        return instance;
    }

    public void setAccessToken(String token) {
        this.accessToken = token;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void clearToken() {
        this.accessToken = null;
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public <T> T createService(Class<T> serviceClass) {
        return retrofit.create(serviceClass);
    }

    public FastApiAuthService createAuthService() {
        return apiRetrofit.create(FastApiAuthService.class);
    }

    public FastApiAnalyticsService createAnalyticsService() {
        return apiRetrofit.create(FastApiAnalyticsService.class);
    }

    public FastApiCrashReportService createCrashReportService() {
        return apiRetrofit.create(FastApiCrashReportService.class);
    }

    public FastApiProfileService createProfileService() {
        return apiRetrofit.create(FastApiProfileService.class);
    }

    public FastApiOwnerKostService createOwnerKostService() {
        return apiRetrofit.create(FastApiOwnerKostService.class);
    }

    public FastApiOwnerStatsService createOwnerStatsService() {
        return apiRetrofit.create(FastApiOwnerStatsService.class);
    }

    public FastApiUnreadMessagesService createUnreadMessagesService() {
        return apiRetrofit.create(FastApiUnreadMessagesService.class);
    }

    public FastApiFavoriteService createFavoriteService() {
        return apiRetrofit.create(FastApiFavoriteService.class);
    }

    public FastApiChatService createChatService() {
        return apiRetrofit.create(FastApiChatService.class);
    }

    public FastApiReportService createReportService() {
        return apiRetrofit.create(FastApiReportService.class);
    }

    public FastApiAdminReportService createAdminReportService() {
        return apiRetrofit.create(FastApiAdminReportService.class);
    }

    public FastApiAdminKostService createAdminKostService() {
        return apiRetrofit.create(FastApiAdminKostService.class);
    }

    public FastApiActivityLogService createActivityLogService() {
        return apiRetrofit.create(FastApiActivityLogService.class);
    }

    public FastApiAvatarService createAvatarService() {
        return apiRetrofit.create(FastApiAvatarService.class);
    }

    public FastApiBlockService createBlockService() {
        return apiRetrofit.create(FastApiBlockService.class);
    }

    public FastApiSafetyService createSafetyService() {
        return apiRetrofit.create(FastApiSafetyService.class);
    }

    public FastApiSurveyService createSurveyService() {
        return apiRetrofit.create(FastApiSurveyService.class);
    }

    public FastApiReviewService createReviewService() {
        return apiRetrofit.create(FastApiReviewService.class);
    }

    public FastApiOwnerVerificationService createOwnerVerificationService() {
        return apiRetrofit.create(FastApiOwnerVerificationService.class);
    }

    public FastApiUserLookupService createUserLookupService() {
        return apiRetrofit.create(FastApiUserLookupService.class);
    }

    public FastApiAdminUserService createAdminUserService() {
        return apiRetrofit.create(FastApiAdminUserService.class);
    }

    public FastApiKostService createKostService() {
        return apiRetrofit.create(FastApiKostService.class);
    }

    /** Helper: bangun URL publik file di Supabase Storage */
    public static String getStoragePublicUrl(String bucket, String path) {
        return BuildConfig.SUPABASE_URL + "/storage/v1/object/public/" + bucket + "/" + path;
    }

    /** Refresh proaktif sebelum request jika token tersimpan sudah kedaluwarsa. */
    private void refreshIfExpired() {
        if (appContext == null) return;
        SessionManager session = new SessionManager(appContext);
        if (session.isLoggedIn() && session.isTokenExpired()) {
            refreshTokenBlocking(accessToken);
        }
    }

    /**
     * Tukar refresh token dengan access token baru. Aman dipanggil dari banyak thread:
     * jika thread lain sudah memperbarui token, hasilnya langsung dipakai.
     *
     * @return access token baru, atau null bila refresh gagal.
     */
    public String refreshTokenBlocking(String staleToken) {
        if (appContext == null) return null;
        synchronized (refreshLock) {
            if (accessToken != null && staleToken != null && !accessToken.equals(staleToken)) {
                return accessToken;
            }
            SessionManager session = new SessionManager(appContext);
            String refreshToken = session.getRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) return null;

            try {
                retrofit2.Response<AuthResponse> response = apiRetrofit
                        .create(FastApiAuthService.class)
                        .refreshToken(new RefreshRequest(refreshToken))
                        .execute();
                if (!response.isSuccessful() || response.body() == null) return null;
                AuthResponse refreshed = response.body();
                if (refreshed.accessToken == null || refreshed.accessToken.isEmpty()) return null;
                session.saveTokens(refreshed.accessToken, refreshed.refreshToken, refreshed.expiresIn);
                accessToken = refreshed.accessToken;
                return refreshed.accessToken;
            } catch (Exception e) {
                return null;
            }
        }
    }

    /** Ulangi request satu kali dengan token baru saat server membalas 401. */
    private class TokenAuthenticator implements Authenticator {
        @Override
        public Request authenticate(okhttp3.Route route, Response response) {
            if (response.priorResponse() != null) return null;

            String header = response.request().header("Authorization");
            String usedToken = header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
            if (usedToken == null || usedToken.equals(BuildConfig.SUPABASE_ANON_KEY)) return null;

            String newToken = refreshTokenBlocking(usedToken);
            if (newToken == null) return null;
            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + newToken)
                    .build();
        }
    }
}
