package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Client-supplied log details; user_id is deliberately absent. */
public class ActivityLogCreateRequest {
    @SerializedName("action_type") public String actionType;
    @SerializedName("description") public String description;
    @SerializedName("target_type") public String targetType;
    @SerializedName("target_id") public String targetId;
    @SerializedName("actor_name") public String actorName;

    public ActivityLogCreateRequest(String actionType, String description,
                                    String targetType, String targetId, String actorName) {
        this.actionType = actionType;
        this.description = description;
        this.targetType = targetType;
        this.targetId = targetId;
        this.actorName = actorName;
    }
}
