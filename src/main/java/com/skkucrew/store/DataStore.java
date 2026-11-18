package com.skkucrew.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skkucrew.common.Times;
import com.skkucrew.domain.Application;
import com.skkucrew.domain.ApplicationDraft;
import com.skkucrew.domain.Bookmark;
import com.skkucrew.domain.Codes.BookmarkTarget;
import com.skkucrew.domain.Codes.NotificationType;
import com.skkucrew.domain.Codes.PostingStatus;
import com.skkucrew.domain.ExternalActivity;
import com.skkucrew.domain.Member;
import com.skkucrew.domain.Notification;
import com.skkucrew.domain.Posting;
import com.skkucrew.domain.Profile;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 데모용 인메모리 저장소.
 * 서버가 시작될 때 resources/seed/*.json 더미 데이터를 읽어 들이고, /api/dev/reset 으로 처음 상태로 되돌린다.
 * DB로 바꾸려면 이 클래스만 JPA 리포지토리로 교체하면 된다(서비스 코드는 그대로).
 */
@Component
public class DataStore {

    private static final Logger log = LoggerFactory.getLogger(DataStore.class);

    private final ObjectMapper objectMapper;
    private final String seedLocation;

    private final Map<Long, Member> members = new ConcurrentHashMap<>();
    private final Map<Long, Profile> profiles = new ConcurrentHashMap<>();
    private final Map<Long, Posting> postings = new ConcurrentHashMap<>();
    private final Map<Long, Application> applications = new ConcurrentHashMap<>();
    private final Map<Long, ExternalActivity> externalActivities = new ConcurrentHashMap<>();
    /** key: memberId:targetType:targetId */
    private final Map<String, Bookmark> bookmarks = new ConcurrentHashMap<>();
    private final Map<Long, Notification> notifications = new ConcurrentHashMap<>();
    /** key: memberId:postingId */
    private final Map<String, ApplicationDraft> drafts = new ConcurrentHashMap<>();
    /** 지원서 중복 제출 방지 (FR-APPL-07): Idempotency-Key -> applicationId */
    private final Map<String, Long> idempotencyKeys = new ConcurrentHashMap<>();

    private final AtomicLong postingSeq = new AtomicLong(1000);
    private final AtomicLong questionSeq = new AtomicLong(10000);
    private final AtomicLong applicationSeq = new AtomicLong(5000);
    private final AtomicLong itemSeq = new AtomicLong(100000);
    private final AtomicLong notificationSeq = new AtomicLong(20000);

    public DataStore(ObjectMapper objectMapper, @Value("${app.seed-location:seed}") String seedLocation) {
        this.objectMapper = objectMapper;
        this.seedLocation = seedLocation;
    }

    @PostConstruct
    public void init() {
        reset();
    }

    /** 모든 데이터를 지우고 더미 데이터를 다시 읽는다. */
    public synchronized void reset() {
        members.clear();
        profiles.clear();
        postings.clear();
        applications.clear();
        externalActivities.clear();
        bookmarks.clear();
        notifications.clear();
        drafts.clear();
        idempotencyKeys.clear();

        OffsetDateTime now = Times.now();

        for (Member m : read("members.json", new TypeReference<List<Member>>() {})) {
            members.put(m.id, m);
        }
        for (Profile p : read("profiles.json", new TypeReference<List<Profile>>() {})) {
            profiles.put(p.memberId, p);
        }
        for (Posting p : read("postings.json", new TypeReference<List<Posting>>() {})) {
            if (p.deadline == null && p.deadlineDaysFromNow != null) {
                p.deadline = plusDays(now, p.deadlineDaysFromNow);
            }
            if (p.createdAt == null) {
                p.createdAt = plusDays(now, -(p.createdDaysAgo == null ? 1.0 : p.createdDaysAgo));
            }
            if (p.announcedAt == null && p.announcedDaysAgo != null) {
                p.announcedAt = plusDays(now, -p.announcedDaysAgo);
            }
            postings.put(p.id, p);
        }
        for (Application a : read("applications.json", new TypeReference<List<Application>>() {})) {
            if (a.submittedAt == null) {
                a.submittedAt = plusDays(now, -(a.submittedDaysAgo == null ? 1.0 : a.submittedDaysAgo));
            }
            if (a.disclosedAt == null && a.decision == com.skkucrew.domain.Codes.Decision.ACCEPTED) {
                Posting p = postings.get(a.postingId);
                if (p != null && p.status == PostingStatus.ANNOUNCED) {
                    a.disclosedAt = p.announcedAt;
                }
            }
            applications.put(a.id, a);
        }
        // 더미 활동 이력의 발표 시각은 공고의 발표 시각을 따른다
        for (Profile pr : profiles.values()) {
            for (Profile.ActivityHistory h : pr.activityHistory) {
                Posting p = postings.get(h.postingId);
                if (h.announcedAt == null && p != null) {
                    h.announcedAt = p.announcedAt;
                }
            }
        }
        for (ExternalActivity e : read("external-activities.json", new TypeReference<List<ExternalActivity>>() {})) {
            if (e.postedAt == null) {
                e.postedAt = now.toLocalDate().minusDays(e.postedDaysAgo == null ? 1 : e.postedDaysAgo);
            }
            if (e.deadline == null && e.deadlineDaysFromNow != null) {
                e.deadline = now.toLocalDate().plusDays(e.deadlineDaysFromNow);
            }
            externalActivities.put(e.id, e);
        }

        for (Bookmark b : read("bookmarks.json", new TypeReference<List<Bookmark>>() {})) {
            if (b.createdAt == null) {
                b.createdAt = plusDays(now, -(b.createdDaysAgo == null ? 1.0 : b.createdDaysAgo));
            }
            bookmarks.put(b.key(), b);
        }
        for (Notification n : read("notifications.json", new TypeReference<List<Notification>>() {})) {
            if (n.createdAt == null) {
                n.createdAt = plusDays(now, -(n.createdDaysAgo == null ? 1.0 : n.createdDaysAgo));
            }
            notifications.put(n.id, n);
        }
        for (ApplicationDraft d : read("drafts.json", new TypeReference<List<ApplicationDraft>>() {})) {
            if (d.updatedAt == null) {
                d.updatedAt = plusDays(now, -(d.updatedDaysAgo == null ? 1.0 : d.updatedDaysAgo));
            }
            drafts.put(d.key(), d);
        }

        postingSeq.set(Math.max(1000, postings.keySet().stream().mapToLong(Long::longValue).max().orElse(0)));
        applicationSeq.set(Math.max(5000, applications.keySet().stream().mapToLong(Long::longValue).max().orElse(0)));
        long maxQuestionId = postings.values().stream()
                .flatMap(p -> p.questions.stream())
                .mapToLong(q -> q.id == null ? 0 : q.id).max().orElse(0);
        questionSeq.set(Math.max(10000, maxQuestionId));
        notificationSeq.set(Math.max(20000, notifications.keySet().stream().mapToLong(Long::longValue).max().orElse(0)));

        log.info("Seed loaded: members={}, profiles={}, postings={}, applications={}, externalActivities={}, "
                        + "bookmarks={}, notifications={}, drafts={}",
                members.size(), profiles.size(), postings.size(), applications.size(), externalActivities.size(),
                bookmarks.size(), notifications.size(), drafts.size());
    }

    private <T> List<T> read(String file, TypeReference<List<T>> type) {
        ClassPathResource resource = new ClassPathResource(seedLocation + "/" + file);
        if (!resource.exists()) {
            log.warn("Seed file not found: {}", resource.getPath());
            return new ArrayList<>();
        }
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, type);
        } catch (IOException e) {
            throw new IllegalStateException("더미 데이터를 읽지 못했습니다: " + file, e);
        }
    }

    private static OffsetDateTime plusDays(OffsetDateTime base, double days) {
        long minutes = Math.round(days * 24 * 60);
        return base.plusMinutes(minutes);
    }

    /** 스케줄러 대신: 조회할 때 마감 일시가 지난 모집중 공고를 마감으로 바꾼다 (FR-POST-08 단순화). */
    public synchronized void closeExpiredPostings() {
        OffsetDateTime now = Times.now();
        for (Posting p : postings.values()) {
            if (p.status == PostingStatus.OPEN && p.deadline != null && !p.deadline.isAfter(now)) {
                p.status = PostingStatus.CLOSED;
                onPostingClosed(p);
            }
        }
    }

    // ---------------- Member / Profile ----------------

    public List<Member> allMembers() {
        return members.values().stream().sorted(Comparator.comparing(m -> m.id)).toList();
    }

    public Optional<Member> member(Long id) {
        return Optional.ofNullable(id == null ? null : members.get(id));
    }

    public Optional<Profile> profile(Long memberId) {
        return Optional.ofNullable(memberId == null ? null : profiles.get(memberId));
    }

    public void saveProfile(Profile profile) {
        profiles.put(profile.memberId, profile);
    }

    // ---------------- Posting ----------------

    public List<Posting> allPostings() {
        closeExpiredPostings();
        return new ArrayList<>(postings.values());
    }

    public Optional<Posting> posting(Long id) {
        closeExpiredPostings();
        return Optional.ofNullable(id == null ? null : postings.get(id));
    }

    public Posting savePosting(Posting posting) {
        if (posting.id == null) {
            posting.id = postingSeq.incrementAndGet();
        }
        for (Posting.Question q : posting.questions) {
            if (q.id == null) {
                q.id = questionSeq.incrementAndGet();
            }
        }
        postings.put(posting.id, posting);
        return posting;
    }

    // ---------------- Application ----------------

    public List<Application> allApplications() {
        return new ArrayList<>(applications.values());
    }

    public Optional<Application> application(Long id) {
        return Optional.ofNullable(id == null ? null : applications.get(id));
    }

    public List<Application> applicationsOfPosting(Long postingId) {
        return applications.values().stream()
                .filter(a -> a.postingId.equals(postingId))
                .sorted(Comparator.comparing((Application a) -> a.submittedAt))
                .toList();
    }

    public List<Application> applicationsOfMember(Long memberId) {
        return applications.values().stream()
                .filter(a -> a.applicantId.equals(memberId))
                .sorted(Comparator.comparing((Application a) -> a.submittedAt).reversed())
                .toList();
    }

    public Application saveApplication(Application application) {
        if (application.id == null) {
            application.id = applicationSeq.incrementAndGet();
        }
        applications.put(application.id, application);
        return application;
    }

    public Optional<Long> applicationIdByIdempotencyKey(String key) {
        return Optional.ofNullable(key == null ? null : idempotencyKeys.get(key));
    }

    public void rememberIdempotencyKey(String key, Long applicationId) {
        if (key != null && !key.isBlank()) {
            idempotencyKeys.put(key, applicationId);
        }
    }

    // ---------------- External activity ----------------

    public List<ExternalActivity> allExternalActivities() {
        return new ArrayList<>(externalActivities.values());
    }

    // ---------------- Bookmark ----------------

    public List<Bookmark> bookmarksOfMember(Long memberId) {
        return bookmarks.values().stream()
                .filter(b -> b.memberId.equals(memberId))
                .sorted(Comparator.comparing((Bookmark b) -> b.createdAt).reversed())
                .toList();
    }

    public boolean isBookmarked(Long memberId, BookmarkTarget type, Long targetId) {
        return memberId != null && bookmarks.containsKey(Bookmark.key(memberId, type, targetId));
    }

    public Bookmark saveBookmark(Bookmark b) {
        return bookmarks.computeIfAbsent(b.key(), k -> b);
    }

    public boolean removeBookmark(Long memberId, BookmarkTarget type, Long targetId) {
        return bookmarks.remove(Bookmark.key(memberId, type, targetId)) != null;
    }

    // ---------------- Notification ----------------

    public List<Notification> notificationsOf(Long memberId) {
        return notifications.values().stream()
                .filter(n -> n.memberId.equals(memberId))
                .sorted(Comparator.comparing((Notification n) -> n.createdAt)
                        .thenComparing(n -> n.id).reversed()) // 같은 초에 생긴 알림은 나중 것이 위로
                .toList();
    }

    public Optional<Notification> notification(Long id) {
        return Optional.ofNullable(id == null ? null : notifications.get(id));
    }

    /** 서비스 내 알림 생성 (FR-NOTI-01) */
    public Notification notify(Long memberId, NotificationType type, String title, String body,
                               String targetScreen, Long postingId, Long applicationId) {
        Notification n = new Notification();
        n.id = notificationSeq.incrementAndGet();
        n.memberId = memberId;
        n.type = type;
        n.title = title;
        n.body = body;
        n.targetScreen = targetScreen;
        n.postingId = postingId;
        n.applicationId = applicationId;
        n.read = false;
        n.createdAt = Times.now();
        notifications.put(n.id, n);
        return n;
    }

    /**
     * 공고가 마감될 때(조기 마감·마감 시각 도래) 공통 처리:
     * 모집자에게 N-02 알림, 해당 공고의 답변 초안 삭제 (FR-APPL-14).
     */
    public void onPostingClosed(Posting p) {
        long count = applications.values().stream()
                .filter(a -> a.postingId.equals(p.id) && !a.withdrawn).count();
        notify(p.recruiterId, NotificationType.POSTING_CLOSED, "모집이 마감되었어요",
                "'" + p.title + "' 지원서 " + count + "건을 검토하고 결과를 발표해 주세요.", "SCR-12", p.id, null);
        drafts.values().removeIf(d -> d.postingId.equals(p.id));
    }

    // ---------------- Application draft ----------------

    public Optional<ApplicationDraft> draft(Long memberId, Long postingId) {
        return Optional.ofNullable(drafts.get(ApplicationDraft.key(memberId, postingId)));
    }

    public ApplicationDraft saveDraft(ApplicationDraft d) {
        drafts.put(d.key(), d);
        return d;
    }

    public void removeDraft(Long memberId, Long postingId) {
        drafts.remove(ApplicationDraft.key(memberId, postingId));
    }

    public List<ApplicationDraft> draftsOfMember(Long memberId) {
        return drafts.values().stream()
                .filter(d -> d.memberId.equals(memberId))
                .sorted(Comparator.comparing((ApplicationDraft d) -> d.updatedAt).reversed())
                .toList();
    }

    // ---------------- ID for profile items ----------------

    public long nextItemId() {
        return itemSeq.incrementAndGet();
    }
}
