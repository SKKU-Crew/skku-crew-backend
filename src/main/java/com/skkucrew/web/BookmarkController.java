package com.skkucrew.web;

import com.skkucrew.domain.Codes.BookmarkTarget;
import com.skkucrew.service.AuthService;
import com.skkucrew.service.BookmarkService;
import com.skkucrew.service.Dtos.BookmarkList;
import com.skkucrew.service.Dtos.BookmarkRequest;
import com.skkucrew.service.Dtos.BookmarkResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "7. 관심 저장")
@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final AuthService authService;
    private final BookmarkService bookmarkService;

    public BookmarkController(AuthService authService, BookmarkService bookmarkService) {
        this.authService = authService;
        this.bookmarkService = bookmarkService;
    }

    @Operation(summary = "내 관심 목록", description = "\"내 활동 > 관심\" 화면. 교내 공고(postings)와 외부 활동(externalActivities)을 나눠서 최근 저장순")
    @GetMapping
    public BookmarkList list(@RequestHeader(value = "Authorization", required = false) String auth) {
        return bookmarkService.list(authService.requirePersonal(auth));
    }

    @Operation(summary = "관심 저장", description = "{ \"targetType\": \"POSTING\" | \"EXTERNAL_ACTIVITY\", \"targetId\": 101 }. 이미 저장돼 있어도 200")
    @PostMapping
    public BookmarkResponse add(@RequestHeader(value = "Authorization", required = false) String auth,
                                @RequestBody BookmarkRequest req) {
        return bookmarkService.add(authService.requirePersonal(auth), req);
    }

    @Operation(summary = "관심 해제", description = "예: DELETE /api/bookmarks/POSTING/101")
    @DeleteMapping("/{targetType}/{targetId}")
    public BookmarkResponse remove(@RequestHeader(value = "Authorization", required = false) String auth,
                                   @PathVariable BookmarkTarget targetType,
                                   @PathVariable Long targetId) {
        return bookmarkService.remove(authService.requirePersonal(auth), targetType, targetId);
    }
}
