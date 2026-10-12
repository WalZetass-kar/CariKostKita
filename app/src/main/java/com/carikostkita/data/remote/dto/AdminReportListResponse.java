package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AdminReportListResponse {
    @SerializedName("data") public List<AdminReportDto> data;
    @SerializedName("pagination") public Pagination pagination;

    public static class Pagination {
        @SerializedName("has_more") public boolean hasMore;
    }
}
