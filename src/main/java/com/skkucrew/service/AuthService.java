package com.skkucrew.service;

import com.skkucrew.common.ApiException;
import com.skkucrew.domain.Codes.MemberType;
import com.skkucrew.domain.Member;
import com.skkucrew.store.DataStore;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 데모 로그인.
 * Google 학교 계정 로그인(OIDC) 대신, 더미 회원을 골라 "demo-{회원ID}" 토큰을 발급한다.
 * 요청 헤더: Authorization: Bearer demo-1
 */
@Service
public class AuthService {

    private static final String TOKEN_PREFIX = "demo-";

    private final DataStore store;

    public AuthService(DataStore store) {
        this.store = store;
    }

    public String issueToken(Long memberId) {
        Member m = store.member(memberId).orElseThrow(() -> ApiException.notFound("회원"));
        return TOKEN_PREFIX + m.id;
    }

    /** 로그인 회원 (없으면 401) */
    public Member require(String authorization) {
        return optional(authorization).orElseThrow(ApiException::unauthorized);
    }

    /** 로그인 회원 (비로그인 허용 API용) */
    public Optional<Member> optional(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return Optional.empty();
        }
        String token = authorization.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        if (!token.startsWith(TOKEN_PREFIX)) {
            return Optional.empty();
        }
        try {
            long id = Long.parseLong(token.substring(TOKEN_PREFIX.length()));
            return store.member(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public Member requirePersonal(String authorization) {
        Member m = require(authorization);
        if (m.type != MemberType.PERSONAL) {
            throw ApiException.forbidden("개인회원만 사용할 수 있는 기능입니다.");
        }
        return m;
    }
}
