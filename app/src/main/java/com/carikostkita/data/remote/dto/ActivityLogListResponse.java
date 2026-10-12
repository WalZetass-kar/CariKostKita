package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ActivityLogListResponse {
    @SerializedName("data") public List<ActivityLogDto> data;
    @SerializedName("pagination") public Pagination pagination;

    public static class Pagination {
        @SerializedName("count") public Integer count;
        @SerializedName("has_more") public boolean hasMore;
    }
}
