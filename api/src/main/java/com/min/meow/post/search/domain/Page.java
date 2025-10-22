package com.min.meow.post.search.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true, value = {"pageable"})
public class Page<T> extends PageImpl<T> {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Page(@JsonProperty("contents") List<T> content,
                @JsonProperty("number") int page,
                @JsonProperty("size") int size,
                @JsonProperty("totalElements") long total) {
        super(content, PageRequest.of(page, size), total);
    }

    public Page(org.springframework.data.domain.Page<T> page) {
        super(page.getContent(), page.getPageable(), page.getTotalElements());
    }
}
