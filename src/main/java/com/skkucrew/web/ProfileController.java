package com.skkucrew.web;

import com.skkucrew.service.AuthService;
import com.skkucrew.service.Dtos.HistoryVisibilityRequest;
import com.skkucrew.service.Dtos.ProfileResponse;
import com.skkucrew.service.Dtos.ProfileUpdateRequest;
import com.skkucrew.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "2. 프로필")
@RestController
@RequestMapping("/api")
public class ProfileController {

    private final AuthService authService;
    private final ProfileService profileService;

    public ProfileController(AuthService authService, ProfileService profileService) {
        this.authService = authService;
        this.profileService = profileService;
    }

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/profiles/me")
    public ProfileResponse getMine(@RequestHeader(value = "Authorization", required = false) String auth) {
        return profileService.getMyProfile(authService.requirePersonal(auth));
    }

    @Operation(summary = "내 프로필 수정 (전체 교체)")
    @PutMapping("/profiles/me")
    public ProfileResponse updateMine(@RequestHeader(value = "Authorization", required = false) String auth,
                                      @RequestBody ProfileUpdateRequest req) {
        return profileService.updateMyProfile(authService.requirePersonal(auth), req);
    }

    @Operation(summary = "활동 이력 공개 여부", description = "SKKU Crew 활동 이력 항목의 공개 토글 (FR-PROF-09). { \"visible\": false }")
    @PatchMapping("/profiles/me/activity-history/{applicationId}")
    public ProfileResponse setHistoryVisible(@RequestHeader(value = "Authorization", required = false) String auth,
                                             @PathVariable Long applicationId,
                                             @RequestBody HistoryVisibilityRequest req) {
        return profileService.setHistoryVisible(authService.requirePersonal(auth), applicationId,
                req == null ? null : req.visible());
    }

    @Operation(summary = "기술 스택 자동완성", description = "예: ?keyword=fl → Flask, Flutter. 앞글자가 일치하는 것부터 (FR-PROF-04)")
    @GetMapping("/skills")
    public List<String> skills(@RequestParam(required = false) String keyword,
                               @RequestParam(defaultValue = "10") int limit) {
        return profileService.skillSuggestions(keyword, limit);
    }
}
