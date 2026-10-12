package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Staff queue response. The application ID is distinct from its owner's user ID. */
public class OwnerVerificationQueueResponse {
    @SerializedName("data") public List<Application> data;
    @SerializedName("pagination") public Pagination pagination;

    public static class Application {
        @SerializedName("id") public String id;
        @SerializedName("submission_note") public String submissionNote;
        @SerializedName("status") public String status;
        @SerializedName("created_at") public String createdAt;
        @SerializedName("owner") public AdminUserDto owner;
    }

    public static class Pagination {
        @SerializedName("has_more") public boolean hasMore;
    }
}
