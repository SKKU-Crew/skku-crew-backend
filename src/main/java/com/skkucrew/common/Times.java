package com.skkucrew.common;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** 서버 시각(KST) 기준 시간 유틸. 마감 판단은 항상 이 시각을 따른다(BR-13). */
public final class Times {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private Times() {}

    public static OffsetDateTime now() {
        return OffsetDateTime.now(KST).truncatedTo(ChronoUnit.SECONDS);
    }
}
