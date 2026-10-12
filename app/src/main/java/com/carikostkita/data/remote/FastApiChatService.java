package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.ChatListResponse;
import com.carikostkita.data.remote.dto.ChatResponse;
import com.carikostkita.data.remote.dto.CreateChatRequest;
import com.carikostkita.data.remote.dto.MessageListResponse;
import com.carikostkita.data.remote.dto.MessageResponse;
import com.carikostkita.data.remote.dto.SendMessageRequest;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.POST;
import retrofit2.http.GET;

/** Participant-scoped chat operations backed by FastAPI and Supabase RLS. */
public interface FastApiChatService {
    @GET("api/v1/chats")
    Call<ChatListResponse> listChats(
            @Header("Authorization") String bearerToken,
            @Query("page") int page,
            @Query("page_size") int pageSize,
            @Query("kost_id") String kostId
    );

    @POST("api/v1/chats")
    Call<ChatResponse> createChat(
            @Header("Authorization") String bearerToken,
            @Body CreateChatRequest body
    );

    @GET("api/v1/chats/{chatId}")
    Call<ChatResponse> readChat(
            @Header("Authorization") String bearerToken,
            @Path("chatId") String chatId
    );

    @GET("api/v1/chats/{chatId}/messages")
    Call<MessageListResponse> listMessages(
            @Header("Authorization") String bearerToken,
            @Path("chatId") String chatId,
            @Query("page") int page,
            @Query("page_size") int pageSize
    );

    @POST("api/v1/chats/{chatId}/messages")
    Call<MessageResponse> sendMessage(
            @Header("Authorization") String bearerToken,
            @Path("chatId") String chatId,
            @Body SendMessageRequest body
    );

    @PATCH("api/v1/chats/{chatId}/read")
    Call<ResponseBody> markRead(
            @Header("Authorization") String bearerToken,
            @Path("chatId") String chatId
    );
}
