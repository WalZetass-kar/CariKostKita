package com.carikostkita.data.remote.dto;

import java.util.List;

public class MessageListResponse {
    public List<MessageDto> data;
    public Pagination pagination;

    public static class Pagination {
        public int page;
        public int page_size;
        public boolean has_more;
    }
}
