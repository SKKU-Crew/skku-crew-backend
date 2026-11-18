package com.skkucrew.service;

import com.skkucrew.domain.Codes.AcademicStatus;
import com.skkucrew.domain.Codes.BookmarkTarget;
import com.skkucrew.domain.Codes.DraftSource;
import com.skkucrew.domain.Codes.ExternalType;
import com.skkucrew.domain.Codes.NotificationType;
import com.skkucrew.domain.Codes.ActivityMode;
import com.skkucrew.domain.Codes.Category;
import com.skkucrew.domain.Codes.ContactType;
import com.skkucrew.domain.Codes.Decision;
import com.skkucrew.domain.Codes.DisplayStatus;
import com.skkucrew.domain.Codes.Field;
import com.skkucrew.domain.Codes.MemberType;
import com.skkucrew.domain.Codes.PostingStatus;
import com.skkucrew.domain.Codes.QuestionType;
import com.skkucrew.domain.Codes.ViewerAction;
import com.skkucrew.domain.Posting;
import com.skkucrew.domain.Profile;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** API 요청·응답 형식 모음. 서비스 메서드가 이 타입을 주고받으므로 MCP 도구에서도 그대로 재사용하면 된다. */
public final class Dtos {

    private Dtos() {}

    // ---------------- 인증 ----------------

    public record DemoAccount(Long memberId, String name, MemberType memberType, String description) {}

    public record LoginRequest(Long memberId) {}

    public record LoginResponse(String accessToken, MeResponse member) {}

    public record MeResponse(Long memberId, String name, MemberType memberType, String schoolEmail,
                             Boolean profileCompleted, Integer profileCompleteness) {}

    // ---------------- 프로필 ----------------

    public record ProfileResponse(
            String name,
            String schoolEmail,
            String profileImageUrl,
            String majorPrimary,
            List<String> majorsSecondary,
            AcademicStatus academicStatus,
            String bio,
            List<Field> fields,
            List<Profile.Skill> skills,
            List<Profile.Experience> experiences,
            List<Profile.PortfolioLink> portfolioLinks,
            List<Profile.PortfolioFile> portfolioFiles,
            Profile.Contacts contacts,
            Map<String, Boolean> defaultShare,
            List<Profile.ActivityHistory> activityHistory,
            int completeness
    ) {}

    public record ProfileUpdateRequest(
            String name,
            String profileImageUrl,
            String majorPrimary,
            List<String> majorsSecondary,
            AcademicStatus academicStatus,
            String bio,
            List<Field> fields,
            List<Profile.Skill> skills,
            List<Profile.Experience> experiences,
            List<Profile.PortfolioLink> portfolioLinks,
            List<Profile.PortfolioFile> portfolioFiles,
            Profile.Contacts contacts,
            Map<String, Boolean> defaultShare
    ) {}

    // ---------------- 공고 ----------------

    /** department: 개인회원의 주전공 / orgType·verified: 단체회원의 유형과 인증 배지 */
    public record RecruiterView(MemberType type, Long memberId, String name, String department,
                                String orgType, boolean verified) {}

    public record PostingCard(
            Long postingId,
            Category category,
            String title,
            RecruiterView recruiter,
            List<Field> fields,
            List<String> skills,
            int capacity,
            OffsetDateTime deadline,
            Long dDay,
            PostingStatus status,
            boolean requestsContact,
            boolean bookmarked
    ) {}

    public record PostingDetail(
            Long postingId,
            Category category,
            String title,
            RecruiterView recruiter,
            List<Field> fields,
            int capacity,
            OffsetDateTime deadline,
            Long dDay,
            String activityStart,
            String activityEnd,
            ActivityMode activityMode,
            String place,
            List<String> skills,
            String description,
            String imageUrl,
            List<Posting.Question> questions,
            List<ContactType> requestedContacts,
            PostingStatus status,
            Integer applicantCount,
            ViewerAction viewerAction,
            boolean bookmarked,
            OffsetDateTime createdAt
    ) {}

    public record HomeResponse(List<PostingCard> recommended, List<PostingCard> closingSoon) {}

    public record QuestionInput(QuestionType type, String content, boolean required, Integer maxLength, List<String> options) {}

    public record PostingCreateRequest(
            Boolean publish,
            Category category,
            String title,
            List<Field> fields,
            Integer capacity,
            OffsetDateTime deadline,
            String activityStart,
            String activityEnd,
            ActivityMode activityMode,
            String place,
            List<String> skills,
            String description,
            String imageUrl,
            List<QuestionInput> questions,
            List<ContactType> requestedContacts
    ) {}

    public record PostingStatusResponse(Long postingId, PostingStatus status) {}

    public record ApplicantCounts(int total, int pending, int accepted, int rejected, int withdrawn) {}

    public record MyPostingItem(Long postingId, String title, Category category, PostingStatus status,
                                OffsetDateTime deadline, ApplicantCounts counts) {}

    // ---------------- 지원 ----------------

    public record ContactOption(ContactType type, boolean requested, boolean hasValue, String maskedValue) {}

    public record ApplicationForm(
            Long postingId,
            String postingTitle,
            Map<String, Object> fixedItems,
            Map<String, Object> selectableItems,
            List<ContactOption> contacts,
            List<Posting.Question> questions,
            /** 저장된 답변 초안. exists=false면 "초안 불러오기" 버튼을 숨긴다 (FR-APPL-14) */
            DraftResponse draft
    ) {}

    public record ShareSelection(
            Boolean profileImage,
            Boolean bio,
            Boolean majorsSecondary,
            Boolean skills,
            List<Long> experienceIds,
            List<Long> portfolioLinkIds,
            List<Long> portfolioFileIds
    ) {}

    /** value: 프로필에 없는 연락처를 새로 입력할 때만. saveToProfile: 입력값을 프로필에도 저장할지 (FR-APPL-18) */
    public record ContactInput(ContactType type, boolean consent, String value, Boolean saveToProfile) {}

    /** value: 문자열, 복수 선택은 문자열 배열 */
    public record AnswerInput(Long questionId, Object value) {}

    public record ApplyRequest(ShareSelection share, List<ContactInput> contacts, List<AnswerInput> answers, Boolean agreeToProvide) {}

    public record ApplyResponse(Long applicationId, Long postingId, DisplayStatus displayStatus, OffsetDateTime submittedAt) {}

    public record PostingRef(Long postingId, String title, Category category, OffsetDateTime deadline,
                             PostingStatus status, String recruiterName) {}

    public record MyApplicationItem(Long applicationId, PostingRef posting, DisplayStatus displayStatus,
                                    OffsetDateTime submittedAt, OffsetDateTime announcedAt) {}

    public record AnswerView(Long questionId, String content, Object value) {}

    public record ContactValue(ContactType type, String value) {}

    public record DisclosedContact(ContactType type, String value, OffsetDateTime disclosedAt) {}

    public record ResultView(DisplayStatus result, String message, ContactValue recruiterContact,
                             List<DisclosedContact> disclosedContacts) {}

    public record ApplicationDetail(
            Long applicationId,
            PostingRef posting,
            DisplayStatus displayStatus,
            OffsetDateTime submittedAt,
            Map<String, Object> snapshot,
            List<String> sharedItems,
            List<AnswerView> answers,
            ResultView result
    ) {}

    public record WithdrawResponse(Long applicationId, DisplayStatus displayStatus) {}

    // ---------------- 검토·선발 ----------------

    public record ApplicantItem(
            Long applicationId,
            String name,
            String department,
            AcademicStatus academicStatus,
            List<String> topSkills,
            OffsetDateTime submittedAt,
            Decision decision,
            boolean hasMemo,
            boolean withdrawn
    ) {}

    public record ReviewDetail(
            Long applicationId,
            PostingRef posting,
            Map<String, Object> snapshot,
            List<AnswerView> answers,
            List<ContactType> sharedContactTypes,
            List<ContactValue> contacts,
            Decision decision,
            String memo,
            OffsetDateTime submittedAt,
            boolean withdrawn
    ) {}

    public record DecisionRequest(Decision decision, String memo) {}

    public record DecisionResponse(Long applicationId, Decision decision, String memo) {}

    public record AnnounceRequest(String acceptMessage, String rejectMessage) {}

    public record AnnounceResponse(Long postingId, PostingStatus status, int accepted, int rejected,
                                   OffsetDateTime announcedAt, String warning) {}

    public record AcceptedContact(Long applicationId, String name, List<ContactValue> contacts, OffsetDateTime disclosedAt) {}

    // ---------------- 답변 초안 (FR-APPL-14, FR-MCP-07) ----------------

    /** source: WEB(웹 임시저장) 또는 MCP(GPT 초안 저장 도구). 생략하면 WEB */
    public record DraftRequest(List<AnswerInput> answers, DraftSource source) {}

    public record DraftResponse(
            Long postingId,
            String postingTitle,
            boolean exists,
            List<AnswerView> answers,
            DraftSource source,
            OffsetDateTime updatedAt,
            String notice
    ) {}

    // ---------------- 관심 저장 (FR-SRCH-11, FR-EXT-11) ----------------

    public record BookmarkRequest(BookmarkTarget targetType, Long targetId) {}

    public record BookmarkResponse(BookmarkTarget targetType, Long targetId, boolean bookmarked) {}

    public record BookmarkList(List<PostingCard> postings, List<ExternalActivityItem> externalActivities) {}

    // ---------------- 외부 활동 ----------------

    public record ExternalActivityItem(
            Long id,
            ExternalType type,
            String title,
            String organizer,
            List<Field> fields,
            String source,
            LocalDate postedAt,
            LocalDate deadline,
            Long dDay,
            String url,
            boolean bookmarked
    ) {}

    // ---------------- 알림 (FR-NOTI-01, 03, 06) ----------------

    public record NotificationItem(
            Long id,
            NotificationType type,
            String title,
            String body,
            String targetScreen,
            Long postingId,
            Long applicationId,
            boolean read,
            OffsetDateTime createdAt
    ) {}

    public record NotificationList(int unreadCount, List<NotificationItem> items) {}

    public record UnreadCount(int unreadCount) {}

    // ---------------- 조직 페이지 (FR-ORG-01) ----------------

    public record OrgPage(
            Long orgId,
            String name,
            String orgType,
            boolean verified,
            String logoUrl,
            String intro,
            List<Field> fields,
            List<com.skkucrew.domain.Member.Link> links,
            boolean followed,
            int followerCount,
            List<PostingCard> openPostings,
            List<PostingCard> pastPostings
    ) {}

    public record FollowResponse(Long orgId, boolean followed, int followerCount) {}

    // ---------------- 프로필 보조 ----------------

    /** SKKU Crew 활동 이력 항목 공개 여부 (FR-PROF-09) */
    public record HistoryVisibilityRequest(Boolean visible) {}
}
