package com.skkucrew.domain;

import com.skkucrew.domain.Codes.Field;
import com.skkucrew.domain.Codes.MemberType;

import java.util.ArrayList;
import java.util.List;

/** 회원 (개인·단체·관리자 공통). 데모에서는 로그인 없이 회원을 골라 쓴다. */
public class Member {
    public Long id;
    public MemberType type;
    /** 개인회원: 이름 / 단체회원: 단체명 */
    public String name;
    /** 개인회원의 학교 Google 계정 이메일 */
    public String schoolEmail;
    /** 단체회원의 대표 연락 이메일 (합격자에게 공개) */
    public String contactEmail;
    /** 단체회원 소개 (FR-ORG-01, 1,000자 이하) */
    public String orgIntro;
    /** 데모 계정 선택 화면에 보여 줄 설명 */
    public String demoDescription;

    // ---------------- 단체회원 조직 페이지 (FR-ORG-01) ----------------

    /** 단체 유형 (예: 동아리, 학회, 연구실) */
    public String orgType;
    /** 단체 인증 완료 여부 (인증 배지) */
    public boolean verified;
    public String logoUrl;
    /** 활동 분야 */
    public List<Field> orgFields = new ArrayList<>();
    /** 공식 링크 (최대 3개) */
    public List<Link> orgLinks = new ArrayList<>();

    public static class Link {
        public String title;
        public String url;
    }
}
