package com.skkucrew.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.skkucrew.domain.Codes.ContactType;
import com.skkucrew.domain.Codes.Decision;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 지원서. 제출 시점 프로필 스냅샷을 그대로 보존한다 (BR-08, BR-15). */
public class Application {
    public Long id;
    public Long postingId;
    public Long applicantId;
    /** 필수 공유 항목 + 지원자가 고른 항목만으로 만든 프로필 스냅샷 */
    public Map<String, Object> snapshot = new LinkedHashMap<>();
    /** 공유한 항목 목록 (예: bio, skills, experience:21, contact:PHONE) */
    public List<String> sharedItems = new ArrayList<>();
    /** 공유에 동의한 연락처와 그 값 (발표 전에는 모집자에게 비공개) */
    public List<SharedContact> contacts = new ArrayList<>();
    public List<Answer> answers = new ArrayList<>();
    public boolean withdrawn;
    public Decision decision = Decision.PENDING;
    public String memo;
    public OffsetDateTime submittedAt;
    public OffsetDateTime withdrawnAt;
    /** 결과 발표 시 합격자 연락처가 모집자에게 공개된 시각 (FR-SEL-13) */
    public OffsetDateTime disclosedAt;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Double submittedDaysAgo;

    public static class SharedContact {
        public ContactType type;
        public String value;
        public boolean consent;
    }

    public static class Answer {
        public Long questionId;
        /** 단답·장문·단일 선택은 문자열, 복수 선택은 문자열 배열 */
        public Object value;
    }
}
