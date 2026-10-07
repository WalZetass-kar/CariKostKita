package com.carikostkita.data.remote;

import android.content.Context;
import com.carikostkita.BuildConfig;
import com.carikostkita.util.SessionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.concurrent.TimeUnit;
import okhttp3.Authenticator;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
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
    private final OkHttpClient okHttpClient;
    private final OkHttpClient refreshClient;
    private volatile String accessToken = null;
    private final Object refreshLock = new Object();

    private SupabaseClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

        refreshClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request origRequest = chain.request();
                    if (!isAuthEndpoint(origRequest)) {
                        refreshIfExpired();
                    }
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

    /** Helper: bangun URL publik file di Supabase Storage */
    public static String getStoragePublicUrl(String bucket, String path) {
        return BuildConfig.SUPABASE_URL + "/storage/v1/object/public/" + bucket + "/" + path;
    }

    private static boolean isAuthEndpoint(Request request) {
        return request.url().encodedPath().contains("/auth/v1/token");
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

            JsonObject body = new JsonObject();
            body.addProperty("refresh_token", refreshToken);
            Request request = new Request.Builder()
                    .url(BuildConfig.SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token")
                    .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();
            try (Response response = refreshClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return null;
                JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
                String newAccess = json.has("access_token") ? json.get("access_token").getAsString() : null;
                String newRefresh = json.has("refresh_token") ? json.get("refresh_token").getAsString() : null;
                long expiresIn = json.has("expires_in") ? json.get("expires_in").getAsLong() : 3600;
                if (newAccess == null) return null;
                session.saveTokens(newAccess, newRefresh, expiresIn);
                accessToken = newAccess;
                return newAccess;
            } catch (Exception e) {
                return null;
            }
        }
    }

    /** Ulangi request satu kali dengan token baru saat server membalas 401. */
    private class TokenAuthenticator implements Authenticator {
        @Override
        public Request authenticate(okhttp3.Route route, Response response) {
            if (isAuthEndpoint(response.request())) return null;
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
