package com.skkucrew.service;

import com.skkucrew.common.ApiException;
import com.skkucrew.common.ErrorResponse.FieldError;
import com.skkucrew.common.PageResponse;
import com.skkucrew.common.Times;
import com.skkucrew.domain.Application;
import com.skkucrew.domain.Codes.ActivityMode;
import com.skkucrew.domain.Codes.BookmarkTarget;
import com.skkucrew.domain.Codes.Category;
import com.skkucrew.domain.Codes.Decision;
import com.skkucrew.domain.Codes.Field;
import com.skkucrew.domain.Codes.MemberType;
import com.skkucrew.domain.Codes.PostingStatus;
import com.skkucrew.domain.Codes.SortType;
import com.skkucrew.domain.Codes.ViewerAction;
import com.skkucrew.domain.Member;
import com.skkucrew.domain.Posting;
import com.skkucrew.domain.Profile;
import com.skkucrew.service.Dtos.ApplicantCounts;
import com.skkucrew.service.Dtos.HomeResponse;
import com.skkucrew.service.Dtos.MyPostingItem;
import com.skkucrew.service.Dtos.PostingCard;
import com.skkucrew.service.Dtos.PostingCreateRequest;
import com.skkucrew.service.Dtos.PostingDetail;
import com.skkucrew.service.Dtos.PostingRef;
import com.skkucrew.service.Dtos.PostingStatusResponse;
import com.skkucrew.service.Dtos.QuestionInput;
import com.skkucrew.service.Dtos.RecruiterView;
import com.skkucrew.store.DataStore;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PostingService {

    /** 개인회원이 게시할 수 있는 카테고리 (BR-01) */
    private static final Set<Category> PERSONAL_CATEGORIES = Set.of(Category.PROJECT, Category.CONTEST, Category.STUDY);

    private final DataStore store;

    public PostingService(DataStore store) {
        this.store = store;
    }

    // ---------------- 탐색 (4.3) ----------------

    /** 공고 검색 (FR-SRCH-02~05). 같은 항목 안은 OR, 다른 항목끼리는 AND. */
    public PageResponse<PostingCard> search(String keyword, List<Category> categories, List<Field> fields,
                                            List<ActivityMode> modes, MemberType recruiterType,
                                            boolean includeClosed, SortType sort, int page, int size,
                                            Member viewer) {
        String kw = keyword == null ? null : keyword.trim().toLowerCase(Locale.ROOT);
        if (kw != null && kw.length() < 2) {
            kw = null;
        }
        final String k = kw;
        Comparator<Posting> order = (sort == SortType.DEADLINE)
                ? Comparator.comparing((Posting p) -> p.deadline)
                : Comparator.comparing((Posting p) -> p.createdAt).reversed();

        List<PostingCard> cards = store.allPostings().stream()
                .filter(p -> p.status == PostingStatus.OPEN
                        || (includeClosed && (p.status == PostingStatus.CLOSED || p.status == PostingStatus.ANNOUNCED)))
                .filter(p -> categories == null || categories.isEmpty() || categories.contains(p.category))
                .filter(p -> fields == null || fields.isEmpty() || p.fields.stream().anyMatch(fields::contains))
                .filter(p -> modes == null || modes.isEmpty() || modes.contains(p.activityMode))
                .filter(p -> recruiterType == null || recruiterOf(p).type == recruiterType)
                .filter(p -> k == null || matches(p, k))
                .sorted(order)
                .map(p -> toCard(p, viewer))
                .toList();
        return PageResponse.of(cards, page, size);
    }

    /** 홈 화면: 맞춤 공고(FR-SRCH-07) + 마감 임박(FR-SRCH-08) */
    public HomeResponse home(Member viewer) {
        List<Posting> open = store.allPostings().stream()
                .filter(p -> p.status == PostingStatus.OPEN)
                .toList();

        Profile profile = viewer == null ? null : store.profile(viewer.id).orElse(null);
        List<Posting> recommended;
        if (profile == null || profile.fields.isEmpty()) {
            recommended = open.stream()
                    .sorted(Comparator.comparing((Posting p) -> p.createdAt).reversed())
                    .limit(10).toList();
        } else {
            Set<String> mySkills = new HashSet<>();
            profile.skills.forEach(s -> mySkills.add(s.name.toLowerCase(Locale.ROOT)));
            Long viewerId = viewer.id;
            recommended = open.stream()
                    .filter(p -> !p.recruiterId.equals(viewerId))
                    .sorted(Comparator.comparingLong((Posting p) -> score(p, profile, mySkills)).reversed()
                            .thenComparing((Posting p) -> p.createdAt, Comparator.reverseOrder()))
                    .limit(10).toList();
        }

        OffsetDateTime limit = Times.now().plusDays(3);
        List<Posting> closingSoon = open.stream()
                .filter(p -> p.deadline != null && p.deadline.isBefore(limit))
                .sorted(Comparator.comparing((Posting p) -> p.deadline))
                .limit(10).toList();

        return new HomeResponse(recommended.stream().map(p -> toCard(p, viewer)).toList(),
                closingSoon.stream().map(p -> toCard(p, viewer)).toList());
    }

    public PostingDetail detail(Long postingId, Member viewer) {
        Posting p = get(postingId);
        boolean owner = viewer != null && p.recruiterId.equals(viewer.id);
        if (p.status == PostingStatus.DRAFT && !owner) {
            throw ApiException.notFound("공고");
        }
        Integer applicantCount = owner
                ? (int) store.applicationsOfPosting(p.id).stream().filter(a -> !a.withdrawn).count()
                : null;
        return new PostingDetail(p.id, p.category, p.title, recruiterView(p), p.fields, p.capacity, p.deadline,
                dDay(p), p.activityStart, p.activityEnd, p.activityMode, p.place, p.skills, p.description,
                p.imageUrl, p.questions, p.requestedContacts, p.status, applicantCount,
                viewerAction(p, viewer), bookmarked(p, viewer), p.createdAt);
    }

    // ---------------- 공고 관리 (4.4) ----------------

    public PostingDetail create(Member recruiter, PostingCreateRequest req) {
        if (recruiter.type == MemberType.ADMIN) {
            throw ApiException.forbidden("관리자는 공고를 작성할 수 없습니다.");
        }
        List<FieldError> errors = new ArrayList<>();
        if (req.title() == null || req.title().isBlank()) errors.add(new FieldError("title", "제목을 입력해 주세요."));
        if (req.category() == null) errors.add(new FieldError("category", "카테고리를 선택해 주세요."));
        if (req.category() != null && recruiter.type == MemberType.PERSONAL && !PERSONAL_CATEGORIES.contains(req.category())) {
            errors.add(new FieldError("category", "개인회원은 프로젝트·공모전·스터디 공고만 올릴 수 있습니다."));
        }
        if (req.fields() == null || req.fields().isEmpty()) errors.add(new FieldError("fields", "분야를 1개 이상 선택해 주세요."));
        if (req.deadline() == null || !req.deadline().isAfter(Times.now())) {
            errors.add(new FieldError("deadline", "마감 일시는 현재 이후여야 합니다."));
        }
        if (req.requestedContacts() != null && req.requestedContacts().size() > 2) {
            errors.add(new FieldError("requestedContacts", "요청 연락처는 최대 2개입니다."));
        }
        if (req.questions() != null && req.questions().size() > 10) {
            errors.add(new FieldError("questions", "추가 질문은 최대 10개입니다."));
        }
        if (!errors.isEmpty()) {
            throw ApiException.badRequest("입력값을 확인해 주세요.", errors);
        }

        Posting p = new Posting();
        p.recruiterId = recruiter.id;
        p.category = req.category();
        p.title = req.title();
        p.fields = new ArrayList<>(req.fields());
        p.capacity = req.capacity() == null ? 1 : req.capacity();
        p.deadline = req.deadline();
        p.activityStart = req.activityStart();
        p.activityEnd = req.activityEnd();
        p.activityMode = req.activityMode() == null ? ActivityMode.ONLINE : req.activityMode();
        p.place = req.place();
        p.skills = req.skills() == null ? new ArrayList<>() : new ArrayList<>(req.skills());
        p.description = req.description();
        p.imageUrl = req.imageUrl();
        p.requestedContacts = req.requestedContacts() == null ? new ArrayList<>() : new ArrayList<>(req.requestedContacts());
        p.questions = new ArrayList<>();
        if (req.questions() != null) {
            int order = 1;
            for (QuestionInput qi : req.questions()) {
                Posting.Question q = new Posting.Question();
                q.order = order++;
                q.type = qi.type();
                q.content = qi.content();
                q.required = qi.required();
                q.maxLength = qi.maxLength();
                q.options = qi.options();
                p.questions.add(q);
            }
        }
        p.status = Boolean.FALSE.equals(req.publish()) ? PostingStatus.DRAFT : PostingStatus.OPEN;
        p.createdAt = Times.now();
        store.savePosting(p);
        return detail(p.id, recruiter);
    }

    /** 조기 마감 (FR-POST-07) */
    public PostingStatusResponse close(Member recruiter, Long postingId) {
        Posting p = getOwned(recruiter, postingId);
        if (p.status != PostingStatus.OPEN) {
            throw ApiException.conflict("INVALID_STATUS", "모집중인 공고만 마감할 수 있습니다.");
        }
        p.status = PostingStatus.CLOSED;
        store.onPostingClosed(p);
        return new PostingStatusResponse(p.id, p.status);
    }

    /** 내 공고 목록과 지원자 수 (FR-POST-12) */
    public List<MyPostingItem> mine(Member recruiter, PostingStatus status) {
        return store.allPostings().stream()
                .filter(p -> p.recruiterId.equals(recruiter.id))
                .filter(p -> status == null || p.status == status)
                .sorted(Comparator.comparing((Posting p) -> p.createdAt).reversed())
                .map(p -> new MyPostingItem(p.id, p.title, p.category, p.status, p.deadline, counts(p.id)))
                .toList();
    }

    public ApplicantCounts counts(Long postingId) {
        List<Application> apps = store.applicationsOfPosting(postingId);
        int withdrawn = (int) apps.stream().filter(a -> a.withdrawn).count();
        int pending = (int) apps.stream().filter(a -> !a.withdrawn && a.decision == Decision.PENDING).count();
        int accepted = (int) apps.stream().filter(a -> !a.withdrawn && a.decision == Decision.ACCEPTED).count();
        int rejected = (int) apps.stream().filter(a -> !a.withdrawn && a.decision == Decision.REJECTED).count();
        return new ApplicantCounts(apps.size() - withdrawn, pending, accepted, rejected, withdrawn);
    }

    // ---------------- 공통 ----------------

    public Posting get(Long postingId) {
        return store.posting(postingId).orElseThrow(() -> ApiException.notFound("공고"));
    }

    public Posting getOwned(Member recruiter, Long postingId) {
        Posting p = get(postingId);
        if (!p.recruiterId.equals(recruiter.id)) {
            throw ApiException.forbidden("본인 공고만 관리할 수 있습니다.");
        }
        return p;
    }

    public Member recruiterOf(Posting p) {
        return store.member(p.recruiterId).orElseThrow(() -> ApiException.notFound("모집자"));
    }

    public RecruiterView recruiterView(Posting p) {
        Member m = recruiterOf(p);
        String dept = null;
        if (m.type == MemberType.PERSONAL) {
            dept = store.profile(m.id).map(pr -> pr.majorPrimary).orElse(null);
        }
        return new RecruiterView(m.type, m.id, m.name, dept);
    }

    public PostingCard toCard(Posting p) {
        return toCard(p, null);
    }

    /** viewer가 있으면 관심 저장 여부(bookmarked)를 채운다 */
    public PostingCard toCard(Posting p, Member viewer) {
        return new PostingCard(p.id, p.category, p.title, recruiterView(p), p.fields, p.skills, p.capacity,
                p.deadline, dDay(p), p.status, !p.requestedContacts.isEmpty(), bookmarked(p, viewer));
    }

    private boolean bookmarked(Posting p, Member viewer) {
        return viewer != null && store.isBookmarked(viewer.id, BookmarkTarget.POSTING, p.id);
    }

    public PostingRef toRef(Posting p) {
        return new PostingRef(p.id, p.title, p.category, p.deadline, p.status, recruiterOf(p).name);
    }

    private Long dDay(Posting p) {
        if (p.deadline == null) return null;
        long days = ChronoUnit.DAYS.between(Times.now().toLocalDate(), p.deadline.atZoneSameInstant(Times.KST).toLocalDate());
        return Math.max(days, 0);
    }

    private ViewerAction viewerAction(Posting p, Member viewer) {
        if (viewer == null) return ViewerAction.LOGIN_REQUIRED;
        if (p.recruiterId.equals(viewer.id)) return ViewerAction.OWNER;
        if (p.status != PostingStatus.OPEN) return ViewerAction.CLOSED;
        if (viewer.type != MemberType.PERSONAL) return ViewerAction.NOT_ALLOWED;
        boolean applied = store.applicationsOfPosting(p.id).stream()
                .anyMatch(a -> a.applicantId.equals(viewer.id) && !a.withdrawn);
        return applied ? ViewerAction.APPLIED : ViewerAction.APPLY;
    }

    private boolean matches(Posting p, String kw) {
        if (contains(p.title, kw) || contains(p.description, kw)) return true;
        if (p.skills.stream().anyMatch(s -> contains(s, kw))) return true;
        return contains(recruiterOf(p).name, kw);
    }

    private static boolean contains(String text, String kw) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(kw);
    }

    private static long score(Posting p, Profile profile, Set<String> mySkills) {
        long fieldHits = p.fields.stream().filter(profile.fields::contains).count();
        long skillHits = p.skills.stream().filter(s -> mySkills.contains(s.toLowerCase(Locale.ROOT))).count();
        return fieldHits + skillHits;
    }
}
