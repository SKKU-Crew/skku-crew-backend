package com.skkucrew.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.skkucrew.domain.Codes.DraftSource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 추가 질문 답변 초안 (FR-APPL-14, FR-MCP-07).
 * 회원·공고마다 1개. 저장만으로는 제출되지 않고, 제출하거나 공고가 마감되면 삭제한다.
 */
public class ApplicationDraft {
    public Long memberId;
    public Long postingId;
    public List<Application.Answer> answers = new ArrayList<>();
    public DraftSource source;
    public OffsetDateTime updatedAt;

    /** 더미 데이터 전용: 서버 시작 시각 기준 며칠 전에 저장했는지 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Double updatedDaysAgo;

    public String key() {
        return key(memberId, postingId);
    }

    public static String key(Long memberId, Long postingId) {
        return memberId + ":" + postingId;
    }
}
