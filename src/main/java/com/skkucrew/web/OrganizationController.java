package com.skkucrew.web;

import com.skkucrew.service.AuthService;
import com.skkucrew.service.Dtos.FollowResponse;
import com.skkucrew.service.Dtos.OrgPage;
import com.skkucrew.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "7-1. 조직 페이지")
@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final AuthService authService;
    private final OrganizationService organizationService;

    public OrganizationController(AuthService authService, OrganizationService organizationService) {
        this.authService = authService;
        this.organizationService = organizationService;
    }

    @Operation(summary = "조직 페이지", description = "단체명·유형·인증 배지·소개·분야·공식 링크, 모집중 공고와 지난 공고 (FR-ORG-01). 로그인하면 followed 표시")
    @GetMapping("/{orgId}")
    public OrgPage page(@RequestHeader(value = "Authorization", required = false) String auth,
                        @PathVariable Long orgId) {
        return organizationService.page(orgId, authService.optional(auth).orElse(null));
    }

    @Operation(summary = "관심 조직 등록", description = "등록하면 이 조직이 새 공고를 올릴 때 N-05 알림을 받는다")
    @PostMapping("/{orgId}/follow")
    public FollowResponse follow(@RequestHeader(value = "Authorization", required = false) String auth,
                                 @PathVariable Long orgId) {
        return organizationService.follow(authService.requirePersonal(auth), orgId);
    }

    @Operation(summary = "관심 조직 해제")
    @DeleteMapping("/{orgId}/follow")
    public FollowResponse unfollow(@RequestHeader(value = "Authorization", required = false) String auth,
                                   @PathVariable Long orgId) {
        return organizationService.unfollow(authService.requirePersonal(auth), orgId);
    }
}
