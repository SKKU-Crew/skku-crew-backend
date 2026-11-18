package com.skkucrew.service;

import com.skkucrew.common.ApiException;
import com.skkucrew.common.PageResponse;
import com.skkucrew.common.Times;
import com.skkucrew.domain.Codes.BookmarkTarget;
import com.skkucrew.domain.Codes.ExternalType;
import com.skkucrew.domain.Codes.Field;
import com.skkucrew.domain.Codes.SortType;
import com.skkucrew.domain.ExternalActivity;
import com.skkucrew.domain.Member;
import com.skkucrew.service.Dtos.ExternalActivityItem;
import com.skkucrew.store.DataStore;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** 외부 활동 (4.8). 수집기 없이 더미 데이터만 조회한다. */
@Service
public class ExternalActivityService {

    private final DataStore store;

    public ExternalActivityService(DataStore store) {
        this.store = store;
    }

    /** 유형·분야·키워드 검색, 기본 정렬은 마감 임박순 (FR-EXT-07). 마감 지난 항목은 제외 (FR-EXT-06). */
    public PageResponse<ExternalActivityItem> search(String keyword, ExternalType type, List<Field> fields,
                                                     String source, SortType sort, int page, int size, Member viewer) {
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim().toLowerCase(Locale.ROOT);
        Comparator<ExternalActivity> order = (sort == SortType.LATEST)
                ? Comparator.comparing((ExternalActivity e) -> e.postedAt).reversed()
                : Comparator.comparing((ExternalActivity e) -> e.deadline, Comparator.nullsLast(Comparator.naturalOrder()));
        List<ExternalActivityItem> list = store.allExternalActivities().stream()
                .filter(this::isVisible)
                .filter(e -> type == null || e.type == type)
                .filter(e -> source == null || source.isBlank() || source.equalsIgnoreCase(e.source))
                .filter(e -> fields == null || fields.isEmpty() || e.fields.stream().anyMatch(fields::contains))
                .filter(e -> kw == null
                        || (e.title != null && e.title.toLowerCase(Locale.ROOT).contains(kw))
                        || (e.organizer != null && e.organizer.toLowerCase(Locale.ROOT).contains(kw)))
                .sorted(order)
                .map(e -> toItem(e, viewer))
                .toList();
        return PageResponse.of(list, page, size);
    }

    public ExternalActivity get(Long id) {
        return store.allExternalActivities().stream()
                .filter(e -> e.id.equals(id))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("외부 활동"));
    }

    /** 마감일이 지나지 않은 항목만 보인다 (FR-EXT-06) */
    public boolean isVisible(ExternalActivity e) {
        return e.deadline == null || !e.deadline.isBefore(Times.now().toLocalDate());
    }

    public ExternalActivityItem toItem(ExternalActivity e, Member viewer) {
        LocalDate today = Times.now().toLocalDate();
        Long dDay = e.deadline == null ? null : Math.max(ChronoUnit.DAYS.between(today, e.deadline), 0);
        boolean bookmarked = viewer != null && store.isBookmarked(viewer.id, BookmarkTarget.EXTERNAL_ACTIVITY, e.id);
        return new ExternalActivityItem(e.id, e.type, e.title, e.organizer, e.fields, e.source, e.postedAt,
                e.deadline, dDay, e.url, bookmarked);
    }
}
