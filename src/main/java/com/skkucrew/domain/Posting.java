package com.skkucrew.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.skkucrew.domain.Codes.ActivityMode;
import com.skkucrew.domain.Codes.Category;
import com.skkucrew.domain.Codes.ContactType;
import com.skkucrew.domain.Codes.Field;
import com.skkucrew.domain.Codes.PostingStatus;
import com.skkucrew.domain.Codes.QuestionType;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** 모집 공고 (SRS 표 4-15) */
public class Posting {
    public Long id;
    public Long recruiterId;
    public Category category;
    public String title;
    public List<Field> fields = new ArrayList<>();
    public int capacity;
    public OffsetDateTime deadline;
    public String activityStart;
    public String activityEnd;
    public ActivityMode activityMode;
    public String place;
    public List<String> skills = new ArrayList<>();
    public String description;
    public String imageUrl;
    public List<Question> questions = new ArrayList<>();
    public List<ContactType> requestedContacts = new ArrayList<>();
    public PostingStatus status;
    public String acceptMessage;
    public String rejectMessage;
    public OffsetDateTime announcedAt;
    public OffsetDateTime createdAt;

    // ---- 더미 데이터 전용: 서버 시작 시각 기준 상대 시간으로 날짜를 만든다 ----
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Double deadlineDaysFromNow;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Double createdDaysAgo;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Double announcedDaysAgo;

    public static class Question {
        public Long id;
        public int order;
        public QuestionType type;
        public String content;
        public boolean required;
        /** 장문형 최대 글자 수 */
        public Integer maxLength;
        /** 선택형 선택지 */
        public List<String> options;
    }
}
