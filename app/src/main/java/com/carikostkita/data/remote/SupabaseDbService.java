package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.ActivityLogDto;
import com.carikostkita.data.remote.dto.ChatDto;
import com.carikostkita.data.remote.dto.ChatUpdateDto;
import com.carikostkita.data.remote.dto.FavoriteDto;
import com.carikostkita.data.remote.dto.KostDto;
import com.carikostkita.data.remote.dto.KostUpdateDto;
import com.carikostkita.data.remote.dto.MessageDto;
import com.carikostkita.data.remote.dto.ReadUpdateDto;
import com.carikostkita.data.remote.dto.ReportDto;
import com.carikostkita.data.remote.dto.ReportUpdateDto;
import com.carikostkita.data.remote.dto.UserDto;
import com.carikostkita.data.remote.dto.UserUpdateDto;
import java.util.List;
import java.util.Map;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Retrofit interface untuk PostgREST Supabase Database endpoints (/rest/v1/*).
 */
public interface SupabaseDbService {

    // ==================== USERS ====================

    @GET("rest/v1/users")
    Call<List<UserDto>> getUsers(
            @QueryMap(encoded = true) Map<String, String> filters,
            @Query("select") String select,
            @Query("order") String order
    );

    @GET("rest/v1/users")
    Call<List<UserDto>> getUserById(
            @Query("id") String idFilter,
            @Query("select") String select
    );

    @PATCH("rest/v1/users")
    Call<List<UserDto>> updateUser(
            @Query("id") String idFilter,
            @Body UserUpdateDto body
    );

    @POST("rest/v1/users")
    Call<List<UserDto>> createUser(
            @Body UserDto body
    );

    @DELETE("rest/v1/users")
    Call<Void> deleteUser(
            @Query("id") String idFilter
    );

    // ==================== KOSTS ====================

    @GET("rest/v1/kosts")
    Call<List<KostDto>> getKosts(
            @QueryMap(encoded = true) Map<String, String> filters,
            @Query("select") String select,
            @Query("order") String order
    );

    @GET("rest/v1/kosts")
    Call<List<KostDto>> getKostById(
            @Query("id") String idFilter,
            @Query("select") String select
    );

    @POST("rest/v1/kosts")
    Call<List<KostDto>> insertKost(@Body KostDto body);

    @PATCH("rest/v1/kosts")
    Call<List<KostDto>> updateKost(
            @Query("id") String idFilter,
            @Body KostUpdateDto body
    );

    @DELETE("rest/v1/kosts")
    Call<ResponseBody> deleteKost(@Query("id") String idFilter);

    // ==================== FAVORITES ====================

    @GET("rest/v1/favorites")
    Call<List<FavoriteDto>> getFavorites(
            @QueryMap(encoded = true) Map<String, String> filters,
            @Query("select") String select
    );

    @POST("rest/v1/favorites")
    Call<List<FavoriteDto>> addFavorite(@Body FavoriteDto body);

    @DELETE("rest/v1/favorites")
    Call<ResponseBody> removeFavorite(
            @Query("user_id") String userIdFilter,
            @Query("kost_id") String kostIdFilter
    );

    // ==================== CHATS ====================

    @GET("rest/v1/chats")
    Call<List<ChatDto>> getChats(
            @QueryMap(encoded = true) Map<String, String> filters,
            @Query("select") String select,
            @Query("order") String order
    );

    @POST("rest/v1/chats")
    Call<List<ChatDto>> insertChat(@Body ChatDto body);

    @PATCH("rest/v1/chats")
    Call<List<ChatDto>> updateChat(
            @Query("id") String idFilter,
            @Body ChatUpdateDto body
    );

    // ==================== MESSAGES ====================

    @GET("rest/v1/messages")
    Call<List<MessageDto>> getMessages(
            @Query("chat_id") String chatIdFilter,
            @Query("select") String select,
            @Query("order") String order
    );

    @POST("rest/v1/messages")
    Call<List<MessageDto>> insertMessage(@Body MessageDto body);

    @PATCH("rest/v1/messages")
    Call<ResponseBody> markMessagesRead(
            @Query("chat_id") String chatIdFilter,
            @Query("sender_id") String senderIdFilter,
            @Body ReadUpdateDto body
    );

    // ==================== REPORTS ====================

    @GET("rest/v1/reports")
    Call<List<ReportDto>> getReports(
            @QueryMap(encoded = true) Map<String, String> filters,
            @Query("select") String select,
            @Query("order") String order
    );

    @POST("rest/v1/reports")
    Call<List<ReportDto>> insertReport(@Body ReportDto body);

    @PATCH("rest/v1/reports")
    Call<List<ReportDto>> updateReport(
            @Query("id") String idFilter,
            @Body ReportUpdateDto body
    );

    // ==================== ACTIVITY LOGS ====================

    @GET("rest/v1/activity_logs")
    Call<List<ActivityLogDto>> getLogs(
            @Query("select") String select,
            @Query("order") String order
    );

    @POST("rest/v1/activity_logs")
    Call<ResponseBody> insertLog(@Body ActivityLogDto body);

    // ==================== TABEL GENERIK (fitur baru: survei, ulasan, blokir, event) ====================

    @GET("rest/v1/{table}")
    Call<List<com.google.gson.JsonObject>> select(
            @retrofit2.http.Path("table") String table,
            @QueryMap(encoded = true) Map<String, String> filters,
            @Query("select") String select,
            @Query("order") String order
    );

    @POST("rest/v1/{table}")
    Call<List<com.google.gson.JsonObject>> insert(
            @retrofit2.http.Path("table") String table,
            @Body Object body
    );

    @PATCH("rest/v1/{table}")
    Call<List<com.google.gson.JsonObject>> patch(
            @retrofit2.http.Path("table") String table,
            @QueryMap(encoded = true) Map<String, String> filters,
            @Body Object body
    );

    @DELETE("rest/v1/{table}")
    Call<ResponseBody> remove(
            @retrofit2.http.Path("table") String table,
            @QueryMap(encoded = true) Map<String, String> filters
    );

    // ==================== RPC (fungsi SQL di supabase/migrations) ====================

    @POST("rest/v1/rpc/{fn}")
    Call<List<com.google.gson.JsonObject>> rpcRows(
            @retrofit2.http.Path("fn") String functionName,
            @Body Map<String, Object> params
    );

    @POST("rest/v1/rpc/{fn}")
    Call<ResponseBody> rpc(
            @retrofit2.http.Path("fn") String functionName,
            @Body Map<String, Object> params
    );
}
