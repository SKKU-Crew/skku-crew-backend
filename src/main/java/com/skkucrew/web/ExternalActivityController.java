package com.skkucrew.web;

import com.skkucrew.common.PageResponse;
import com.skkucrew.domain.Codes.ExternalType;
import com.skkucrew.domain.Codes.Field;
import com.skkucrew.domain.Codes.SortType;
import com.skkucrew.service.AuthService;
import com.skkucrew.service.Dtos.ExternalActivityItem;
import com.skkucrew.service.ExternalActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "6. 외부 활동")
@RestController
@RequestMapping("/api/external-activities")
public class ExternalActivityController {

    private final AuthService authService;
    private final ExternalActivityService service;

    public ExternalActivityController(AuthService authService, ExternalActivityService service) {
        this.authService = authService;
        this.service = service;
    }

    @Operation(summary = "외부 활동 목록", description = "기본 정렬은 마감 임박순(DEADLINE). 마감 지난 항목은 제외. source=SKKU_NOTICE(성대 공지)|PUBLIC_API(공공기관) 정보원 필터. 로그인하면 bookmarked 표시")
    @GetMapping
    public PageResponse<ExternalActivityItem> search(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ExternalType type,
            @RequestParam(required = false) List<Field> field,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "DEADLINE") SortType sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(keyword, type, field, source, sort, page, size, authService.optional(auth).orElse(null));
    }
}
