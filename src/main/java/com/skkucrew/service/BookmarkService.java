package com.skkucrew.service;

import com.skkucrew.common.ApiException;
import com.skkucrew.common.ErrorResponse.FieldError;
import com.skkucrew.common.Times;
import com.skkucrew.domain.Bookmark;
import com.skkucrew.domain.Codes.BookmarkTarget;
import com.skkucrew.domain.Codes.PostingStatus;
import com.skkucrew.domain.Member;
import com.skkucrew.domain.Posting;
import com.skkucrew.service.Dtos.BookmarkList;
import com.skkucrew.service.Dtos.BookmarkRequest;
import com.skkucrew.service.Dtos.BookmarkResponse;
import com.skkucrew.service.Dtos.ExternalActivityItem;
import com.skkucrew.service.Dtos.PostingCard;
import com.skkucrew.store.DataStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 관심 저장 (FR-SRCH-11, FR-EXT-11). MCP의 save_bookmark 도구도 이 서비스를 쓰면 된다. */
@Service
public class BookmarkService {

    private final DataStore store;
    private final PostingService postingService;
    private final ExternalActivityService externalActivityService;

    public BookmarkService(DataStore store, PostingService postingService, ExternalActivityService externalActivityService) {
        this.store = store;
        this.postingService = postingService;
        this.externalActivityService = externalActivityService;
    }

    /** "내 활동 > 관심" 목록. 마감·발표된 공고도 표시하고, 마감 지난 외부 활동은 뺀다. */
    public BookmarkList list(Member member) {
        List<PostingCard> postings = new ArrayList<>();
        List<ExternalActivityItem> externals = new ArrayList<>();
        for (Bookmark b : store.bookmarksOfMember(member.id)) {
            if (b.targetType == BookmarkTarget.POSTING) {
                store.posting(b.targetId)
                        .filter(p -> p.status != PostingStatus.DRAFT && p.status != PostingStatus.HIDDEN)
                        .ifPresent(p -> postings.add(postingService.toCard(p, member)));
            } else {
                store.allExternalActivities().stream()
                        .filter(e -> e.id.equals(b.targetId) && externalActivityService.isVisible(e))
                        .findFirst()
                        .ifPresent(e -> externals.add(externalActivityService.toItem(e, member)));
            }
        }
        return new BookmarkList(postings, externals);
    }

    public BookmarkResponse add(Member member, BookmarkRequest req) {
        validate(req);
        Bookmark b = new Bookmark();
        b.memberId = member.id;
        b.targetType = req.targetType();
        b.targetId = req.targetId();
        b.createdAt = Times.now();
        store.saveBookmark(b);
        return new BookmarkResponse(req.targetType(), req.targetId(), true);
    }

    public BookmarkResponse remove(Member member, BookmarkTarget type, Long targetId) {
        store.removeBookmark(member.id, type, targetId);
        return new BookmarkResponse(type, targetId, false);
    }

    private void validate(BookmarkRequest req) {
        if (req == null || req.targetType() == null || req.targetId() == null) {
            throw ApiException.badRequest("관심 저장 대상을 지정해 주세요.",
                    List.of(new FieldError("targetType", "POSTING 또는 EXTERNAL_ACTIVITY"),
                            new FieldError("targetId", "필수 항목입니다.")));
        }
        if (req.targetType() == BookmarkTarget.POSTING) {
            Posting p = postingService.get(req.targetId());
            if (p.status == PostingStatus.DRAFT || p.status == PostingStatus.HIDDEN) {
                throw ApiException.notFound("공고");
            }
        } else {
            externalActivityService.get(req.targetId());
        }
    }
}
