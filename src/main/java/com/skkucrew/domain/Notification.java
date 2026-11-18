package com.skkucrew.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.skkucrew.domain.Codes.NotificationType;

import java.time.OffsetDateTime;

/** 서비스 내 알림 (FR-NOTI-01, 03, 06). 웹 푸시는 데모에서 생략한다. */
public class Notification {
    public Long id;
    public Long memberId;
    public NotificationType type;
    public String title;
    public String body;
    /** 누르면 이동할 화면 (SRS 표 4-34: SCR-06 공고 상세, SCR-08 내 지원 현황, SCR-12 지원자 목록) */
    public String targetScreen;
    public Long postingId;
    public Long applicationId;
    public boolean read;
    public OffsetDateTime createdAt;

    /** 더미 데이터 전용: 서버 시작 시각 기준 며칠 전에 생겼는지 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Double createdDaysAgo;
}
