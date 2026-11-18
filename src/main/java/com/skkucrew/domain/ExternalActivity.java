package com.skkucrew.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.skkucrew.domain.Codes.ExternalType;
import com.skkucrew.domain.Codes.Field;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 외부 활동 정보. 데모에서는 수집기 없이 더미 데이터만 제공한다. */
public class ExternalActivity {
    public Long id;
    public ExternalType type;
    public String title;
    public String organizer;
    public List<Field> fields = new ArrayList<>();
    /** SKKU_NOTICE(학교 공지) / PUBLIC_API(공공기관 Open API) */
    public String source;
    public LocalDate postedAt;
    /** 원문에 마감일이 없으면 null (FR-EXT-12) */
    public LocalDate deadline;
    public String url;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Integer postedDaysAgo;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Integer deadlineDaysFromNow;
}
