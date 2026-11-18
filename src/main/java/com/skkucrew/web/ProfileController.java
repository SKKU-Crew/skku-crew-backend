package com.skkucrew.web;

import com.skkucrew.service.AuthService;
import com.skkucrew.service.Dtos.ProfileResponse;
import com.skkucrew.service.Dtos.ProfileUpdateRequest;
import com.skkucrew.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "2. 프로필")
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final AuthService authService;
    private final ProfileService profileService;

    public ProfileController(AuthService authService, ProfileService profileService) {
        this.authService = authService;
        this.profileService = profileService;
    }

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    public ProfileResponse getMine(@RequestHeader(value = "Authorization", required = false) String auth) {
        return profileService.getMyProfile(authService.requirePersonal(auth));
    }

    @Operation(summary = "내 프로필 수정 (전체 교체)")
    @PutMapping("/me")
    public ProfileResponse updateMine(@RequestHeader(value = "Authorization", required = false) String auth,
                                      @RequestBody ProfileUpdateRequest req) {
        return profileService.updateMyProfile(authService.requirePersonal(auth), req);
    }
}
