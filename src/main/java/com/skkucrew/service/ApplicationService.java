package com.skkucrew.service;

import com.skkucrew.common.ApiException;
import com.skkucrew.common.ErrorResponse.FieldError;
import com.skkucrew.common.Times;
import com.skkucrew.domain.Application;
import com.skkucrew.domain.ApplicationDraft;
import com.skkucrew.domain.Codes.DraftSource;
import com.skkucrew.domain.Codes.NotificationType;
import com.skkucrew.domain.Codes.QuestionType;
import com.skkucrew.domain.Codes.ContactType;
import com.skkucrew.domain.Codes.Decision;
import com.skkucrew.domain.Codes.DisplayStatus;
import com.skkucrew.domain.Codes.MemberType;
import com.skkucrew.domain.Codes.PostingStatus;
import com.skkucrew.domain.Member;
import com.skkucrew.domain.Posting;
import com.skkucrew.domain.Profile;
import com.skkucrew.service.Dtos.AcceptedContact;
import com.skkucrew.service.Dtos.AnnounceRequest;
import com.skkucrew.service.Dtos.AnnounceResponse;
import com.skkucrew.service.Dtos.AnswerInput;
import com.skkucrew.service.Dtos.AnswerView;
import com.skkucrew.service.Dtos.ApplicantItem;
import com.skkucrew.service.Dtos.ApplicationDetail;
import com.skkucrew.service.Dtos.ApplicationForm;
import com.skkucrew.service.Dtos.ApplyRequest;
import com.skkucrew.service.Dtos.ApplyResponse;
import com.skkucrew.service.Dtos.ContactInput;
import com.skkucrew.service.Dtos.ContactOption;
import com.skkucrew.service.Dtos.ContactValue;
import com.skkucrew.service.Dtos.DecisionRequest;
import com.skkucrew.service.Dtos.DecisionResponse;
import com.skkucrew.service.Dtos.DisclosedContact;
import com.skkucrew.service.Dtos.DraftRequest;
import com.skkucrew.service.Dtos.DraftResponse;
import com.skkucrew.service.Dtos.MyApplicationItem;
import com.skkucrew.service.Dtos.ResultView;
import com.skkucrew.service.Dtos.ReviewDetail;
import com.skkucrew.service.Dtos.ShareSelection;
import com.skkucrew.service.Dtos.WithdrawResponse;
import com.skkucrew.store.DataStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 지원(4.5)과 검토·선발(4.6). 빈 깡통 데모라 화면에 드러나는 규칙만 지킨다. */
@Service
public class ApplicationService {

    private static final String DEFAULT_REJECT_MESSAGE = "지원해 주셔서 감사합니다. 아쉽게도 이번 모집에서는 함께하지 못하게 되었습니다.";

    private final DataStore store;
    private final PostingService postingService;
    private final ProfileService profileService;

    public ApplicationService(DataStore store, PostingService postingService, ProfileService profileService) {
        this.store = store;
        this.postingService = postingService;
        this.profileService = profileService;
    }

    // =====================================================================
    // 지원자
    // =====================================================================

    /** 지원서 작성 1단계(공유 항목 선택) 화면 데이터 */
    public ApplicationForm form(Member applicant, Long postingId) {
        Posting p = postingService.get(postingId);
        checkCanApply(applicant, p);
        Profile pr = profileService.profileOf(applicant);

        Map<String, Object> fixed = new LinkedHashMap<>();
        fixed.put("name", applicant.name);
        fixed.put("majorPrimary", pr.majorPrimary);
        fixed.put("academicStatus", pr.academicStatus);
        fixed.put("fields", pr.fields);

        Map<String, Object> selectable = new LinkedHashMap<>();
        selectable.put("profileImage", option(notBlank(pr.profileImageUrl), share(pr, "profileImage")));
        selectable.put("bio", option(notBlank(pr.bio), share(pr, "bio")));
        selectable.put("majorsSecondary", option(!pr.majorsSecondary.isEmpty(), share(pr, "majorsSecondary")));
        selectable.put("skills", option(!pr.skills.isEmpty(), share(pr, "skills")));
        selectable.put("experiences", pr.experiences.stream()
                .map(e -> item(e.id, e.title, share(pr, "experiences"))).toList());
        selectable.put("portfolioLinks", pr.portfolioLinks.stream()
                .map(l -> item(l.id, l.title, share(pr, "portfolioLinks"))).toList());
        selectable.put("portfolioFiles", pr.portfolioFiles.stream()
                .map(f -> item(f.id, f.fileName, share(pr, "portfolioFiles"))).toList());

        List<ContactOption> contacts = new ArrayList<>();
        for (ContactType t : ContactType.values()) {
            String v = contactValue(applicant, pr, t);
            contacts.add(new ContactOption(t, p.requestedContacts.contains(t), notBlank(v), mask(t, v)));
        }
        return new ApplicationForm(p.id, p.title, fixed, selectable, contacts, p.questions, draftView(applicant, p));
    }

    /** 지원서 제출 (UC-09) */
    public ApplyResponse apply(Member applicant, Long postingId, ApplyRequest req, String idempotencyKey) {
        var existing = store.applicationIdByIdempotencyKey(idempotencyKey).flatMap(store::application);
        if (existing.isPresent()) {
            Application a = existing.get();
            return new ApplyResponse(a.id, a.postingId, displayStatus(a, postingService.get(a.postingId)), a.submittedAt);
        }

        Posting p = postingService.get(postingId);
        checkCanApply(applicant, p);
        Profile pr = profileService.profileOf(applicant);

        if (req == null || !Boolean.TRUE.equals(req.agreeToProvide())) {
            throw ApiException.badRequest("정보 제공에 동의해야 제출할 수 있습니다.",
                    List.of(new FieldError("agreeToProvide", "동의가 필요합니다.")));
        }

        // 추가 질문: 필수 답변만 확인 (FR-APPL-03 단순화)
        Map<Long, Object> answerMap = new LinkedHashMap<>();
        if (req.answers() != null) {
            for (AnswerInput ai : req.answers()) {
                if (ai.questionId() != null) answerMap.put(ai.questionId(), ai.value());
            }
        }
        List<FieldError> errors = new ArrayList<>();
        for (Posting.Question q : p.questions) {
            if (q.required && isEmptyAnswer(answerMap.get(q.id))) {
                errors.add(new FieldError("answers[" + q.id + "]", "필수 질문입니다: " + q.content));
            }
        }

        // 연락처: 요청 연락처는 값 + 동의 필수 (FR-APPL-15~17, BR-09)
        Map<ContactType, ContactInput> contactInputs = new LinkedHashMap<>();
        if (req.contacts() != null) {
            for (ContactInput ci : req.contacts()) {
                if (ci.type() != null) contactInputs.put(ci.type(), ci);
            }
        }
        List<Application.SharedContact> shared = new ArrayList<>();
        for (ContactType t : ContactType.values()) {
            ContactInput ci = contactInputs.get(t);
            boolean requested = p.requestedContacts.contains(t);
            if (ci == null || !ci.consent()) {
                if (requested) {
                    errors.add(new FieldError("contacts." + t, "요청 연락처는 제공에 동의해야 지원할 수 있습니다."));
                }
                continue;
            }
            String value = notBlank(ci.value()) ? ci.value().trim() : contactValue(applicant, pr, t);
            if (!notBlank(value)) {
                errors.add(new FieldError("contacts." + t, "연락처 값을 입력해 주세요."));
                continue;
            }
            Application.SharedContact sc = new Application.SharedContact();
            sc.type = t;
            sc.value = value;
            sc.consent = true;
            shared.add(sc);
            if (notBlank(ci.value()) && Boolean.TRUE.equals(ci.saveToProfile())) {
                saveContactToProfile(pr, t, value);
            }
        }
        if (!errors.isEmpty()) {
            boolean contactProblem = errors.stream().anyMatch(e -> e.field().startsWith("contacts."));
            throw new ApiException(contactProblem ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST,
                    contactProblem ? "REQUESTED_CONTACT_MISSING" : "VALIDATION_FAILED",
                    contactProblem ? "요청 연락처를 입력하고 제공에 동의해 주세요." : "필수 질문에 답해 주세요.",
                    errors);
        }

        Application a = new Application();
        a.postingId = p.id;
        a.applicantId = applicant.id;
        ShareSelection share = req.share() == null
                ? new ShareSelection(null, null, null, null, null, null, null) : req.share();
        a.snapshot = buildSnapshot(applicant, pr, share, a.sharedItems);
        a.contacts = shared;
        shared.forEach(c -> a.sharedItems.add("contact:" + c.type));
        for (Map.Entry<Long, Object> e : answerMap.entrySet()) {
            Application.Answer ans = new Application.Answer();
            ans.questionId = e.getKey();
            ans.value = e.getValue();
            a.answers.add(ans);
        }
        a.submittedAt = Times.now();
        store.saveApplication(a);
        store.rememberIdempotencyKey(idempotencyKey, a.id);
        store.removeDraft(applicant.id, p.id); // 제출하면 초안은 지운다
        store.notify(p.recruiterId, NotificationType.NEW_APPLICATION, "새 지원서가 도착했어요",
                "'" + p.title + "'에 " + applicant.name + "님이 지원했어요.", "SCR-12", p.id, a.id); // N-01
        return new ApplyResponse(a.id, p.id, displayStatus(a, p), a.submittedAt);
    }

    public List<MyApplicationItem> myApplications(Member applicant) {
        return store.applicationsOfMember(applicant.id).stream()
                .map(a -> {
                    Posting p = postingService.get(a.postingId);
                    return new MyApplicationItem(a.id, postingService.toRef(p), displayStatus(a, p), a.submittedAt,
                            p.status == PostingStatus.ANNOUNCED ? p.announcedAt : null);
                })
                .toList();
    }

    /** 지원자 본인용 상세. 결과는 발표 후에만 (FR-APPL-12, 표 4-22) */
    public ApplicationDetail myApplicationDetail(Member applicant, Long applicationId) {
        Application a = getApplication(applicationId);
        if (!a.applicantId.equals(applicant.id)) {
            throw ApiException.forbidden("본인 지원서만 볼 수 있습니다.");
        }
        Posting p = postingService.get(a.postingId);
        DisplayStatus status = displayStatus(a, p);
        if (a.withdrawn) {
            return new ApplicationDetail(a.id, postingService.toRef(p), status, a.submittedAt, Map.of(), List.of(), List.of(), null);
        }
        ResultView result = null;
        if (status == DisplayStatus.ACCEPTED) {
            Member recruiter = postingService.recruiterOf(p);
            ContactValue recruiterContact = recruiter.type == MemberType.ORGANIZATION
                    ? new ContactValue(ContactType.PERSONAL_EMAIL, recruiter.contactEmail)
                    : new ContactValue(ContactType.SCHOOL_EMAIL, recruiter.schoolEmail);
            List<DisclosedContact> disclosed = a.contacts.stream()
                    .map(c -> new DisclosedContact(c.type, c.value, a.disclosedAt)).toList();
            result = new ResultView(status, p.acceptMessage, recruiterContact, disclosed);
        } else if (status == DisplayStatus.REJECTED) {
            String msg = notBlank(p.rejectMessage) ? p.rejectMessage : DEFAULT_REJECT_MESSAGE;
            result = new ResultView(status, msg, null, null);
        }
        return new ApplicationDetail(a.id, postingService.toRef(p), status, a.submittedAt, a.snapshot,
                a.sharedItems, answerViews(a, p), result);
    }

    /** 지원 취소: 마감 전만 (BR-04) */
    public WithdrawResponse withdraw(Member applicant, Long applicationId) {
        Application a = getApplication(applicationId);
        if (!a.applicantId.equals(applicant.id)) {
            throw ApiException.forbidden("본인 지원서만 취소할 수 있습니다.");
        }
        Posting p = postingService.get(a.postingId);
        if (p.status != PostingStatus.OPEN) {
            throw ApiException.conflict("WITHDRAW_NOT_ALLOWED", "마감 이후에는 지원을 취소할 수 없습니다.");
        }
        a.withdrawn = true;
        a.withdrawnAt = Times.now();
        return new WithdrawResponse(a.id, DisplayStatus.WITHDRAWN);
    }

    // =====================================================================
    // 모집자
    // =====================================================================

    public List<ApplicantItem> applicants(Member recruiter, Long postingId, Decision decision) {
        postingService.getOwned(recruiter, postingId);
        return store.applicationsOfPosting(postingId).stream()
                .filter(a -> decision == null || (!a.withdrawn && a.decision == decision))
                .map(a -> {
                    Member m = store.member(a.applicantId).orElse(null);
                    Profile pr = store.profile(a.applicantId).orElse(null);
                    String name = m == null ? "(알 수 없음)" : m.name;
                    if (a.withdrawn) {
                        return new ApplicantItem(a.id, name, null, null, List.of(), a.submittedAt, null, false, true);
                    }
                    List<String> topSkills = pr == null ? List.of()
                            : pr.skills.stream().limit(3).map(s -> s.name).toList();
                    return new ApplicantItem(a.id, name, pr == null ? null : pr.majorPrimary,
                            pr == null ? null : pr.academicStatus, topSkills, a.submittedAt, a.decision,
                            notBlank(a.memo), false);
                })
                .toList();
    }

    /** 모집자용 지원서 상세. 연락처는 발표 후 합격자만 (FR-SEL-03) */
    public ReviewDetail review(Member recruiter, Long applicationId) {
        Application a = getApplication(applicationId);
        Posting p = postingService.getOwned(recruiter, a.postingId);
        if (a.withdrawn) {
            return new ReviewDetail(a.id, postingService.toRef(p), Map.of(), List.of(), List.of(), null,
                    null, null, a.submittedAt, true);
        }
        List<ContactValue> contacts = null;
        if (p.status == PostingStatus.ANNOUNCED && a.decision == Decision.ACCEPTED) {
            contacts = a.contacts.stream().map(c -> new ContactValue(c.type, c.value)).toList();
        }
        List<ContactType> sharedTypes = a.contacts.stream().map(c -> c.type).toList();
        return new ReviewDetail(a.id, postingService.toRef(p), a.snapshot, answerViews(a, p), sharedTypes,
                contacts, a.decision, a.memo, a.submittedAt, false);
    }

    /** 결정·메모 (FR-SEL-04, 05) */
    public DecisionResponse decide(Member recruiter, Long applicationId, DecisionRequest req) {
        Application a = getApplication(applicationId);
        Posting p = postingService.getOwned(recruiter, a.postingId);
        if (p.status == PostingStatus.ANNOUNCED) {
            throw ApiException.conflict("ALREADY_ANNOUNCED", "결과 발표 후에는 결정을 바꿀 수 없습니다.");
        }
        if (a.withdrawn) {
            throw ApiException.conflict("WITHDRAWN", "취소된 지원서입니다.");
        }
        if (req != null && req.decision() != null) a.decision = req.decision();
        if (req != null && req.memo() != null) a.memo = req.memo();
        return new DecisionResponse(a.id, a.decision, a.memo);
    }

    /** 결과 일괄 발표 (FR-SEL-06~10, 13) */
    public AnnounceResponse announce(Member recruiter, Long postingId, AnnounceRequest req) {
        Posting p = postingService.getOwned(recruiter, postingId);
        if (p.status == PostingStatus.ANNOUNCED) {
            throw ApiException.conflict("ALREADY_ANNOUNCED", "이미 결과를 발표했습니다.");
        }
        if (p.status != PostingStatus.CLOSED) {
            throw ApiException.conflict("POSTING_NOT_CLOSED", "마감된 공고만 결과를 발표할 수 있습니다.");
        }
        List<Application> valid = store.applicationsOfPosting(postingId).stream().filter(a -> !a.withdrawn).toList();
        long pending = valid.stream().filter(a -> a.decision == Decision.PENDING).count();
        if (pending > 0) {
            throw ApiException.conflict("UNDECIDED_APPLICATIONS", "결정하지 않은 지원서가 " + pending + "건 있습니다.");
        }
        if (req == null || !notBlank(req.acceptMessage())) {
            throw ApiException.badRequest("합격자 안내 메시지를 입력해 주세요.",
                    List.of(new FieldError("acceptMessage", "필수 항목입니다.")));
        }

        OffsetDateTime now = Times.now();
        p.acceptMessage = req.acceptMessage();
        p.rejectMessage = req.rejectMessage();
        p.status = PostingStatus.ANNOUNCED;
        p.announcedAt = now;

        Member recruiterMember = postingService.recruiterOf(p);
        int accepted = 0;
        for (Application a : valid) {
            if (a.decision == Decision.ACCEPTED) {
                accepted++;
                a.disclosedAt = now;
                store.profile(a.applicantId).ifPresent(pr -> {
                    Profile.ActivityHistory h = new Profile.ActivityHistory();
                    h.applicationId = a.id;
                    h.postingId = p.id;
                    h.postingTitle = p.title;
                    h.recruiterName = recruiterMember.name;
                    h.announcedAt = now;
                    pr.activityHistory.add(h);
                });
            }
        }
        for (Application a : valid) { // N-03: 취소하지 않은 모든 지원자에게 (FR-SEL-10)
            store.notify(a.applicantId, NotificationType.RESULT_ANNOUNCED, "선발 결과가 발표되었어요",
                    "'" + p.title + "' 지원 결과를 확인해 보세요.", "SCR-08", p.id, a.id);
        }
        int rejected = valid.size() - accepted;
        String warning = accepted > p.capacity
                ? "합격 인원(" + accepted + "명)이 모집 인원(" + p.capacity + "명)을 넘었습니다." : null;
        return new AnnounceResponse(p.id, p.status, accepted, rejected, now, warning);
    }

    /** 발표 후 모집자에게 공개되는 합격자 연락처 (표 4-22) */
    public List<AcceptedContact> acceptedContacts(Member recruiter, Long postingId) {
        Posting p = postingService.getOwned(recruiter, postingId);
        if (p.status != PostingStatus.ANNOUNCED) {
            throw ApiException.conflict("NOT_ANNOUNCED", "결과 발표 후에 확인할 수 있습니다.");
        }
        return store.applicationsOfPosting(postingId).stream()
                .filter(a -> !a.withdrawn && a.decision == Decision.ACCEPTED)
                .map(a -> new AcceptedContact(a.id,
                        store.member(a.applicantId).map(m -> m.name).orElse("(알 수 없음)"),
                        a.contacts.stream().map(c -> new ContactValue(c.type, c.value)).toList(),
                        a.disclosedAt))
                .toList();
    }

    // =====================================================================
    // 답변 초안 (FR-APPL-14, FR-MCP-07)
    // =====================================================================

    /** 저장된 초안 조회. 공고가 모집중이 아니면 초안을 지우고 exists=false */
    public DraftResponse getDraft(Member applicant, Long postingId) {
        return draftView(applicant, postingService.get(postingId));
    }

    /** 초안 저장(덮어쓰기). 제출이 아니다. 지원 가능한 모집중 공고에만 허용 (FR-MCP-07) */
    public DraftResponse saveDraft(Member applicant, Long postingId, DraftRequest req) {
        Posting p = postingService.get(postingId);
        checkCanApply(applicant, p);

        Map<Long, Posting.Question> questions = new LinkedHashMap<>();
        p.questions.forEach(q -> questions.put(q.id, q));
        List<FieldError> errors = new ArrayList<>();
        List<Application.Answer> answers = new ArrayList<>();
        if (req != null && req.answers() != null) {
            for (AnswerInput ai : req.answers()) {
                Posting.Question q = ai.questionId() == null ? null : questions.get(ai.questionId());
                if (q == null) {
                    errors.add(new FieldError("answers[" + ai.questionId() + "]", "이 공고에 없는 질문입니다."));
                    continue;
                }
                if (ai.value() instanceof String text && q.maxLength != null && text.length() > q.maxLength) {
                    errors.add(new FieldError("answers[" + q.id + "]", q.maxLength + "자 이하로 입력해 주세요."));
                    continue;
                }
                if (q.type == QuestionType.MULTI_CHOICE && ai.value() != null && !(ai.value() instanceof List<?>)) {
                    errors.add(new FieldError("answers[" + q.id + "]", "복수 선택 질문은 배열로 보내 주세요."));
                    continue;
                }
                Application.Answer ans = new Application.Answer();
                ans.questionId = q.id;
                ans.value = ai.value();
                answers.add(ans);
            }
        }
        if (!errors.isEmpty()) {
            throw ApiException.badRequest("초안 내용을 확인해 주세요.", errors);
        }

        ApplicationDraft d = new ApplicationDraft();
        d.memberId = applicant.id;
        d.postingId = p.id;
        d.answers = answers;
        d.source = (req == null || req.source() == null) ? DraftSource.WEB : req.source();
        d.updatedAt = Times.now();
        store.saveDraft(d);
        return draftView(applicant, p);
    }

    public DraftResponse deleteDraft(Member applicant, Long postingId) {
        Posting p = postingService.get(postingId);
        store.removeDraft(applicant.id, p.id);
        return draftView(applicant, p);
    }

    private DraftResponse draftView(Member applicant, Posting p) {
        if (p.status != PostingStatus.OPEN) {
            store.removeDraft(applicant.id, p.id); // 공고가 마감되면 초안 삭제
        }
        ApplicationDraft d = store.draft(applicant.id, p.id).orElse(null);
        if (d == null) {
            return new DraftResponse(p.id, p.title, false, List.of(), null, null, null);
        }
        return new DraftResponse(p.id, p.title, true, answerViews(d.answers, p), d.source, d.updatedAt,
                "초안만 저장되었습니다. 아직 지원이 제출되지 않았으니 SKKU Crew에서 확인 후 제출하세요.");
    }

    // =====================================================================
    // 내부 도우미
    // =====================================================================

    /** 지원자에게 보이는 상태 (표 4-18). 발표 전에는 결정을 숨긴다 (BR-06). */
    public static DisplayStatus displayStatus(Application a, Posting p) {
        if (a.withdrawn) return DisplayStatus.WITHDRAWN;
        if (p.status == PostingStatus.ANNOUNCED) {
            return a.decision == Decision.ACCEPTED ? DisplayStatus.ACCEPTED : DisplayStatus.REJECTED;
        }
        if (p.status == PostingStatus.OPEN) return DisplayStatus.SUBMITTED;
        return DisplayStatus.UNDER_REVIEW;
    }

    private Application getApplication(Long id) {
        return store.application(id).orElseThrow(() -> ApiException.notFound("지원서"));
    }

    private void checkCanApply(Member applicant, Posting p) {
        if (applicant.type != MemberType.PERSONAL) {
            throw ApiException.forbidden("개인회원만 지원할 수 있습니다.");
        }
        if (p.recruiterId.equals(applicant.id)) {
            throw ApiException.conflict("OWN_POSTING", "본인 공고에는 지원할 수 없습니다.");
        }
        if (p.status != PostingStatus.OPEN || !p.deadline.isAfter(Times.now())) {
            throw ApiException.conflict("APPLICATION_CLOSED", "모집이 마감되어 지원할 수 없습니다.");
        }
        boolean already = store.applicationsOfPosting(p.id).stream()
                .anyMatch(a -> a.applicantId.equals(applicant.id) && !a.withdrawn);
        if (already) {
            throw ApiException.conflict("ALREADY_APPLIED", "이미 지원한 공고입니다.");
        }
        Profile pr = store.profile(applicant.id).orElse(null);
        if (pr == null || !pr.isRequiredFilled()) {
            throw ApiException.conflict("PROFILE_INCOMPLETE", "프로필 필수 항목(주전공, 학년, 관심 분야)을 먼저 입력해 주세요.");
        }
    }

    private Map<String, Object> buildSnapshot(Member m, Profile pr, ShareSelection s, List<String> sharedItems) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("name", m.name);
        snap.put("majorPrimary", pr.majorPrimary);
        snap.put("academicStatus", pr.academicStatus);
        snap.put("fields", new ArrayList<>(pr.fields));

        if (Boolean.TRUE.equals(s.profileImage()) && notBlank(pr.profileImageUrl)) {
            snap.put("profileImageUrl", pr.profileImageUrl);
            sharedItems.add("profileImage");
        }
        if (Boolean.TRUE.equals(s.bio()) && notBlank(pr.bio)) {
            snap.put("bio", pr.bio);
            sharedItems.add("bio");
        }
        if (Boolean.TRUE.equals(s.majorsSecondary()) && !pr.majorsSecondary.isEmpty()) {
            snap.put("majorsSecondary", new ArrayList<>(pr.majorsSecondary));
            sharedItems.add("majorsSecondary");
        }
        if (Boolean.TRUE.equals(s.skills()) && !pr.skills.isEmpty()) {
            snap.put("skills", pr.skills.stream().map(sk -> pair("name", sk.name, "level", sk.level)).toList());
            sharedItems.add("skills");
        }
        if (s.experienceIds() != null && !s.experienceIds().isEmpty()) {
            List<Profile.Experience> exps = pr.experiences.stream().filter(e -> s.experienceIds().contains(e.id)).toList();
            if (!exps.isEmpty()) {
                snap.put("experiences", exps.stream().map(e -> {
                    Map<String, Object> x = new LinkedHashMap<>();
                    x.put("type", e.type);
                    x.put("title", e.title);
                    x.put("role", e.role);
                    x.put("startDate", e.startDate);
                    x.put("endDate", e.endDate);
                    x.put("description", e.description);
                    return x;
                }).toList());
                exps.forEach(e -> sharedItems.add("experience:" + e.id));
            }
        }
        if (s.portfolioLinkIds() != null && !s.portfolioLinkIds().isEmpty()) {
            List<Profile.PortfolioLink> links = pr.portfolioLinks.stream().filter(l -> s.portfolioLinkIds().contains(l.id)).toList();
            if (!links.isEmpty()) {
                snap.put("portfolioLinks", links.stream().map(l -> pair("title", l.title, "url", l.url)).toList());
                links.forEach(l -> sharedItems.add("portfolioLink:" + l.id));
            }
        }
        if (s.portfolioFileIds() != null && !s.portfolioFileIds().isEmpty()) {
            List<Profile.PortfolioFile> files = pr.portfolioFiles.stream().filter(f -> s.portfolioFileIds().contains(f.id)).toList();
            if (!files.isEmpty()) {
                snap.put("portfolioFiles", files.stream().map(f -> pair("fileName", f.fileName, "url", f.url)).toList());
                files.forEach(f -> sharedItems.add("portfolioFile:" + f.id));
            }
        }
        return snap;
    }

    private List<AnswerView> answerViews(Application a, Posting p) {
        return answerViews(a.answers, p);
    }

    private List<AnswerView> answerViews(List<Application.Answer> answers, Posting p) {
        Map<Long, Object> byId = answers.stream()
                .filter(x -> x.questionId != null)
                .collect(Collectors.toMap(x -> x.questionId, x -> x.value == null ? "" : x.value, (x, y) -> x, LinkedHashMap::new));
        return p.questions.stream()
                .sorted(Comparator.comparingInt(q -> q.order))
                .map(q -> new AnswerView(q.id, q.content, byId.get(q.id)))
                .toList();
    }

    static String contactValue(Member m, Profile pr, ContactType t) {
        return switch (t) {
            case SCHOOL_EMAIL -> m.schoolEmail;
            case PHONE -> pr.contacts == null ? null : pr.contacts.phone;
            case KAKAO_ID -> pr.contacts == null ? null : pr.contacts.kakaoId;
            case PERSONAL_EMAIL -> pr.contacts == null ? null : pr.contacts.personalEmail;
        };
    }

    private static void saveContactToProfile(Profile pr, ContactType t, String value) {
        if (pr.contacts == null) pr.contacts = new Profile.Contacts();
        switch (t) {
            case PHONE -> pr.contacts.phone = value;
            case KAKAO_ID -> pr.contacts.kakaoId = value;
            case PERSONAL_EMAIL -> pr.contacts.personalEmail = value;
            default -> { /* 학교 이메일은 계정 정보라 저장하지 않는다 */ }
        }
    }

    private static String mask(ContactType t, String v) {
        if (!notBlank(v)) return null;
        if (t == ContactType.PHONE && v.length() >= 13) {
            return v.substring(0, 4) + "****" + v.substring(8);
        }
        int at = v.indexOf('@');
        if (at > 0) {
            String local = v.substring(0, at);
            String shown = local.length() <= 3 ? local.substring(0, 1) : local.substring(0, 3);
            return shown + "***" + v.substring(at);
        }
        return v.length() <= 3 ? v.charAt(0) + "**" : v.substring(0, 3) + "***";
    }

    private static Map<String, Object> option(boolean available, boolean defaultShare) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("available", available);
        m.put("defaultShare", available && defaultShare);
        return m;
    }

    private static Map<String, Object> item(Long id, String title, boolean defaultShare) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("title", title);
        m.put("defaultShare", defaultShare);
        return m;
    }

    private static boolean share(Profile pr, String key) {
        return pr.defaultShare != null && Boolean.TRUE.equals(pr.defaultShare.get(key));
    }

    private static boolean isEmptyAnswer(Object v) {
        if (v == null) return true;
        if (v instanceof String s) return s.isBlank();
        if (v instanceof List<?> l) return l.isEmpty();
        return false;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** null 값을 허용하는 2쌍짜리 Map (Map.of는 null을 받지 않음) */
    private static Map<String, Object> pair(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }
}
