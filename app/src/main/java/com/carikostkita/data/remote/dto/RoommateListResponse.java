package com.carikostkita.data.remote.dto;

import com.carikostkita.data.model.RoommatePost;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RoommateListResponse {
    @SerializedName("data")
    public List<RoommatePost> data;

    @SerializedName("total")
    public int total;

    @SerializedName("pagination")
    public Pagination pagination;

    public static class Pagination {
        @SerializedName("page")
        public int page;

        @SerializedName("page_size")
        public int pageSize;

        @SerializedName("has_more")
        public boolean hasMore;
    }
}
