package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Response for GET/POST/DELETE /api/v1/me/blocks/{other_user_id}. */
public class BlockStatusResponse {
    @SerializedName("blocked") public Boolean blocked;
    @SerializedName("created") public Boolean created;
    @SerializedName("blocked_by_me") public Boolean blockedByMe;
    @SerializedName("blocked_me") public Boolean blockedMe;
}
