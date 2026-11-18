package com.skkucrew.domain;

/** API에서 쓰는 코드값 모음. 화면 표기는 docs/API.md 2절 참고. */
public final class Codes {

    private Codes() {}

    /** 개인회원, 단체회원, 관리자 */
    public enum MemberType { PERSONAL, ORGANIZATION, ADMIN }

    /** 프로젝트, 연구, 공모전, 스터디, 동아리·학회 */
    public enum Category { PROJECT, RESEARCH, CONTEST, STUDY, CLUB }

    /** 개발, AI, 데이터, 금융, 디자인, 기획·마케팅, 기타 */
    public enum Field { DEV, AI, DATA, FINANCE, DESIGN, PLANNING_MARKETING, ETC }

    /** 온라인, 오프라인, 혼합 */
    public enum ActivityMode { ONLINE, OFFLINE, HYBRID }

    /** 1~4학년, 5학년 이상, 휴학, 졸업유예, 석사, 박사, 기타 */
    public enum AcademicStatus { GRADE_1, GRADE_2, GRADE_3, GRADE_4, GRADE_5_PLUS, LEAVE, DEFERRED, MASTER, PHD, OTHER }

    public enum SkillLevel { HIGH, MID, LOW }

    /** 프로젝트, 대외활동, 수상, 경력, 기타 */
    public enum ExperienceType { PROJECT, EXTRACURRICULAR, AWARD, CAREER, ETC }

    /** 학교 Google 계정 이메일, 전화번호, 카카오톡 ID, 개인 이메일 */
    public enum ContactType { SCHOOL_EMAIL, PHONE, KAKAO_ID, PERSONAL_EMAIL }

    /** 임시저장, 모집중, 마감, 발표 완료, 숨김 */
    public enum PostingStatus { DRAFT, OPEN, CLOSED, ANNOUNCED, HIDDEN }

    /** 단답형, 장문형, 단일 선택, 복수 선택 */
    public enum QuestionType { SHORT_TEXT, LONG_TEXT, SINGLE_CHOICE, MULTI_CHOICE }

    /** 모집자가 지정하는 결정. 발표 전에는 지원자에게 비공개(BR-06) */
    public enum Decision { PENDING, ACCEPTED, REJECTED }

    /** 지원자에게 보이는 지원서 상태 (SRS 표 4-18) */
    public enum DisplayStatus { SUBMITTED, UNDER_REVIEW, ACCEPTED, REJECTED, WITHDRAWN }

    /** 공고 상세 하단 버튼: 지원하기, 지원 완료, 모집 마감, 내 공고 관리, 지원 불가 */
    public enum ViewerAction { APPLY, APPLIED, CLOSED, OWNER, NOT_ALLOWED, LOGIN_REQUIRED }

    /** 공모전·대회, 인턴·채용, 교육·부트캠프, 대외활동, 연구 프로그램, 기타 */
    public enum ExternalType { CONTEST, INTERN_JOB, EDUCATION, EXTRACURRICULAR, RESEARCH_PROGRAM, ETC }

    public enum SortType { LATEST, DEADLINE }

    /** 관심 저장 대상: 교내 공고, 외부 활동 (FR-SRCH-11, FR-EXT-11) */
    public enum BookmarkTarget { POSTING, EXTERNAL_ACTIVITY }

    /** 알림 이벤트 (SRS 표 4-34). 데모에서는 N-01~N-05만 사용 */
    public enum NotificationType {
        /** N-01 새 지원서 접수 → 모집자, SCR-12 */
        NEW_APPLICATION,
        /** N-02 모집 마감·발표 요청 → 모집자, SCR-12 */
        POSTING_CLOSED,
        /** N-03 선발 결과 발표 → 지원자, SCR-08 */
        RESULT_ANNOUNCED,
        /** N-04 관심 공고 마감 임박 → 관심 저장 후 미지원 회원, SCR-06 */
        BOOKMARK_CLOSING_SOON,
        /** N-05 관심 조직 새 공고 → 관심 조직 등록 회원, SCR-06 */
        FOLLOWED_ORG_NEW_POSTING
    }

    /** 지원자 목록 정렬: 제출순, 이름순 (FR-SEL-02) */
    public enum ApplicantSort { SUBMITTED, NAME }

    /** 초안을 저장한 곳: 웹 임시저장, MCP(GPT) 초안 저장 도구 (FR-APPL-14) */
    public enum DraftSource { WEB, MCP }
}
