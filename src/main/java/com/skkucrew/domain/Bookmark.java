package com.skkucrew.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.skkucrew.domain.Codes.BookmarkTarget;

import java.time.OffsetDateTime;

/** 관심 저장 (FR-SRCH-11, FR-EXT-11). 개인회원만 사용한다. */
public class Bookmark {
    public Long memberId;
    public BookmarkTarget targetType;
    public Long targetId;
    public OffsetDateTime createdAt;

    /** 더미 데이터 전용: 서버 시작 시각 기준 며칠 전에 저장했는지 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Double createdDaysAgo;

    public String key() {
        return key(memberId, targetType, targetId);
    }

    public static String key(Long memberId, BookmarkTarget type, Long targetId) {
        return memberId + ":" + type + ":" + targetId;
    }
}
