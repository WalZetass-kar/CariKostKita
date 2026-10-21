package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Response wrapper for GET /api/v1/kost. */
public class KostListResponse {
    @SerializedName("data")
    public List<KostDto> data;

    /**
     * Pagination pointer. The API caps one page at 100 rows, so the client has
     * to walk the pages to see the whole catalogue. Without it the app showed
     * only the first page (50 rows) while the admin dashboard showed all 153.
     */
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
