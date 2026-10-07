package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.StorageUploadResponse;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

/**
 * Retrofit interface untuk Supabase Storage API (/storage/v1/*).
 */
public interface SupabaseStorageService {

    @Multipart
    @POST("storage/v1/object/{bucket}/{path}")
    Call<StorageUploadResponse> uploadFile(
            @Path("bucket") String bucket,
            @Path(value = "path", encoded = true) String path,
            @Part MultipartBody.Part file
    );

    @POST("storage/v1/object/{bucket}/{path}")
    Call<StorageUploadResponse> uploadFileBinary(
            @Path("bucket") String bucket,
            @Path(value = "path", encoded = true) String path,
            @retrofit2.http.Header("Content-Type") String mimeType,
            @retrofit2.http.Body okhttp3.RequestBody file
    );

    /** URL bertanda tangan sementara untuk file di bucket privat. */
    @POST("storage/v1/object/sign/{bucket}/{path}")
    Call<com.google.gson.JsonObject> createSignedUrl(
            @Path("bucket") String bucket,
            @Path(value = "path", encoded = true) String path,
            @retrofit2.http.Body java.util.Map<String, Object> body
    );

    @DELETE("storage/v1/object/{bucket}/{path}")
    Call<ResponseBody> deleteFile(
            @Path("bucket") String bucket,
            @Path(value = "path", encoded = true) String path
    );
}
