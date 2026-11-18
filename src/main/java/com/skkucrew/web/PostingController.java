package com.skkucrew.web;

import com.skkucrew.common.PageResponse;
import com.skkucrew.domain.Codes.ActivityMode;
import com.skkucrew.domain.Codes.Category;
import com.skkucrew.domain.Codes.Field;
import com.skkucrew.domain.Codes.MemberType;
import com.skkucrew.domain.Codes.PostingStatus;
import com.skkucrew.domain.Codes.SortType;
import com.skkucrew.domain.Member;
import com.skkucrew.service.AuthService;
import com.skkucrew.service.Dtos.HomeResponse;
import com.skkucrew.service.Dtos.MyPostingItem;
import com.skkucrew.service.Dtos.PostingCard;
import com.skkucrew.service.Dtos.PostingCreateRequest;
import com.skkucrew.service.Dtos.PostingDetail;
import com.skkucrew.service.Dtos.PostingStatusResponse;
import com.skkucrew.service.PostingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "3. 공고")
@RestController
@RequestMapping("/api/postings")
public class PostingController {

    private final AuthService authService;
    private final PostingService postingService;

    public PostingController(AuthService authService, PostingService postingService) {
        this.authService = authService;
        this.postingService = postingService;
    }

    @Operation(summary = "공고 검색·목록", description = "같은 항목의 복수 값은 OR, 다른 항목끼리는 AND. 예: ?category=PROJECT&category=CONTEST&field=DATA. 로그인하면 bookmarked 표시")
    @GetMapping
    public PageResponse<PostingCard> search(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<Category> category,
            @RequestParam(required = false) List<Field> field,
            @RequestParam(required = false) List<ActivityMode> mode,
            @RequestParam(required = false) MemberType recruiterType,
            @RequestParam(defaultValue = "false") boolean includeClosed,
            @RequestParam(defaultValue = "LATEST") SortType sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return postingService.search(keyword, category, field, mode, recruiterType, includeClosed, sort, page, size,
                authService.optional(auth).orElse(null));
    }

    @Operation(summary = "홈: 맞춤 공고 + 마감 임박 공고", description = "로그인하면 관심 분야·기술 기준 맞춤 공고, 비로그인이면 최신순")
    @GetMapping("/home")
    public HomeResponse home(@RequestHeader(value = "Authorization", required = false) String auth) {
        return postingService.home(authService.optional(auth).orElse(null));
    }

    @Operation(summary = "내 공고 목록 (모집자)", description = "공고별 지원자 수 포함. ?status=OPEN 필터")
    @GetMapping("/mine")
    public List<MyPostingItem> mine(@RequestHeader(value = "Authorization", required = false) String auth,
                                    @RequestParam(required = false) PostingStatus status) {
        return postingService.mine(authService.require(auth), status);
    }

    @Operation(summary = "공고 상세", description = "viewerAction으로 하단 버튼(지원하기/지원 완료/모집 마감/내 공고 관리)을 정한다")
    @GetMapping("/{postingId}")
    public PostingDetail detail(@RequestHeader(value = "Authorization", required = false) String auth,
                                @PathVariable Long postingId) {
        Member viewer = authService.optional(auth).orElse(null);
        return postingService.detail(postingId, viewer);
    }

    @Operation(summary = "공고 작성", description = "publish=false면 임시저장(DRAFT), 아니면 바로 모집중(OPEN)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostingDetail create(@RequestHeader(value = "Authorization", required = false) String auth,
                                @RequestBody PostingCreateRequest req) {
        return postingService.create(authService.require(auth), req);
    }

    @Operation(summary = "조기 마감", description = "모집중(OPEN) → 마감(CLOSED). 시연에서 마감 시각을 기다리지 않고 결과 발표로 넘어갈 때 사용")
    @PostMapping("/{postingId}/close")
    public PostingStatusResponse close(@RequestHeader(value = "Authorization", required = false) String auth,
                                       @PathVariable Long postingId) {
        return postingService.close(authService.require(auth), postingId);
    }
}
