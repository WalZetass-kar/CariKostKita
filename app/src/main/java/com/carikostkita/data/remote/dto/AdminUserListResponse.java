package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AdminUserListResponse {
    @SerializedName("data") public List<AdminUserDto> data;
    @SerializedName("pagination") public Pagination pagination;

    public static class Pagination {
        @SerializedName("has_more") public boolean hasMore;
    }
}
