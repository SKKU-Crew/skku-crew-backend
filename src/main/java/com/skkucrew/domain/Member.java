package com.skkucrew.domain;

import com.skkucrew.domain.Codes.MemberType;

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
    /** 단체회원 소개 (예: "데이터 분석 학회") */
    public String orgIntro;
    /** 데모 계정 선택 화면에 보여 줄 설명 */
    public String demoDescription;
}
