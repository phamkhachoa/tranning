package com.onemount.news.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewsListResponse {

    private List<NewsSummaryResponse> newsContent;
    private int pageNo;
    private int pageSize;
    private long totalElements;
    private int totalPages;

    @JsonProperty("isLast")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean last;

    @JsonProperty("isLast")
    public boolean isLast() {
        return last;
    }

    @JsonProperty("isLast")
    public void setLast(boolean last) {
        this.last = last;
    }
}
