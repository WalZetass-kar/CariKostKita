package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** DTO untuk tabel activity_logs di Supabase */
public class ActivityLogDto {

    @SerializedName("id")
    public String id;

    @SerializedName("user_id")
    public String userId;

    @SerializedName("actor_name")
    public String actorName;

    @SerializedName("action_type")
    public String actionType;

    @SerializedName("description")
    public String description;

    @SerializedName("target_type")
    public String targetType;

    @SerializedName("target_id")
    public String targetId;

    @SerializedName("created_at")
    public String createdAt;

    public ActivityLogDto() {}

    public ActivityLogDto(String userId, String actorName, String actionType,
                          String description, String targetType, String targetId) {
        this.userId = userId;
        this.actorName = actorName;
        this.actionType = actionType;
        this.description = description;
        this.targetType = targetType;
        this.targetId = targetId;
    }
}
