package com.carikostkita.data.remote;

import com.carikostkita.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton client untuk semua komunikasi dengan Supabase.
 * Menyediakan Retrofit instance yang dikonfigurasi dengan header Supabase
 * (apikey, Authorization, Content-Type) untuk setiap request.
 */
public class SupabaseClient {

    private static volatile SupabaseClient instance;
    private final Retrofit retrofit;
    private final OkHttpClient okHttpClient;
    private volatile String accessToken = null;

    private SupabaseClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

        okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request origRequest = chain.request();
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
}
