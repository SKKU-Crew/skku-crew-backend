package com.skkucrew.web;

import com.skkucrew.domain.Member;
import com.skkucrew.service.AuthService;
import com.skkucrew.service.Dtos.DemoAccount;
import com.skkucrew.service.Dtos.LoginRequest;
import com.skkucrew.service.Dtos.LoginResponse;
import com.skkucrew.service.Dtos.MeResponse;
import com.skkucrew.service.ProfileService;
import com.skkucrew.store.DataStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "1. 인증 (데모 로그인)")
@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final ProfileService profileService;
    private final DataStore store;

    public AuthController(AuthService authService, ProfileService profileService, DataStore store) {
        this.authService = authService;
        this.profileService = profileService;
        this.store = store;
    }

    @Operation(summary = "데모 계정 목록", description = "로그인 화면에서 고를 더미 회원 목록")
    @GetMapping("/auth/demo-accounts")
    public List<DemoAccount> demoAccounts() {
        return store.allMembers().stream()
                .map(m -> new DemoAccount(m.id, m.name, m.type, m.demoDescription))
                .toList();
    }

    @Operation(summary = "데모 로그인", description = "회원 ID를 받아 토큰(demo-{id})을 발급. 이후 요청에 Authorization: Bearer demo-{id}")
    @PostMapping("/auth/demo-login")
    public LoginResponse demoLogin(@RequestBody LoginRequest req) {
        String token = authService.issueToken(req.memberId());
        Member m = authService.require(token);
        return new LoginResponse(token, profileService.me(m));
    }

    @Operation(summary = "내 계정 정보")
    @GetMapping("/me")
    public MeResponse me(@RequestHeader(value = "Authorization", required = false) String auth) {
        return profileService.me(authService.require(auth));
    }
}
