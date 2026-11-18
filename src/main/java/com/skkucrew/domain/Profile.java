package com.skkucrew.domain;

import com.skkucrew.domain.Codes.AcademicStatus;
import com.skkucrew.domain.Codes.ExperienceType;
import com.skkucrew.domain.Codes.Field;
import com.skkucrew.domain.Codes.SkillLevel;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 개인회원 프로필 (SRS 표 4-7). 이름은 Member.name을 쓴다. */
public class Profile {
    public Long memberId;
    public String profileImageUrl;
    public String majorPrimary;
    public List<String> majorsSecondary = new ArrayList<>();
    public AcademicStatus academicStatus;
    public String bio;
    public List<Field> fields = new ArrayList<>();
    public List<Skill> skills = new ArrayList<>();
    public List<Experience> experiences = new ArrayList<>();
    public List<PortfolioLink> portfolioLinks = new ArrayList<>();
    public List<PortfolioFile> portfolioFiles = new ArrayList<>();
    public Contacts contacts = new Contacts();
    /** 항목별 "지원 시 기본 공유" 여부 (FR-PROF-11). 키: profileImage, bio, majorsSecondary, skills, experiences, portfolioLinks, portfolioFiles, phone, kakaoId, personalEmail */
    public Map<String, Boolean> defaultShare = new LinkedHashMap<>();
    /** SKKU Crew 활동 이력 (합격 발표 시 자동 추가, FR-PROF-09) */
    public List<ActivityHistory> activityHistory = new ArrayList<>();

    public static class Skill {
        public Long id;
        public String name;
        public SkillLevel level;
    }

    public static class Experience {
        public Long id;
        public ExperienceType type;
        public String title;
        public String role;
        public String startDate;
        public String endDate;
        public String description;
    }

    public static class PortfolioLink {
        public Long id;
        public String title;
        public String url;
    }

    public static class PortfolioFile {
        public Long id;
        public String fileName;
        public String url;
    }

    public static class Contacts {
        public String phone;
        public String kakaoId;
        public String personalEmail;
    }

    public static class ActivityHistory {
        public Long applicationId;
        public Long postingId;
        public String postingTitle;
        public String recruiterName;
        public OffsetDateTime announcedAt;
        public boolean visible = true;
    }

    /** 필수 항목(주전공, 학년·학적 상태, 관심 분야) 입력 여부 (FR-PROF-02) */
    public boolean isRequiredFilled() {
        return majorPrimary != null && !majorPrimary.isBlank()
                && academicStatus != null
                && fields != null && !fields.isEmpty();
    }

    /** 완성도: 필수 40% + 선택 6개 각 10% (FR-PROF-03) */
    public int completeness() {
        int score = isRequiredFilled() ? 40 : 0;
        if (profileImageUrl != null && !profileImageUrl.isBlank()) score += 10;
        if (bio != null && !bio.isBlank()) score += 10;
        if (skills != null && !skills.isEmpty()) score += 10;
        if (experiences != null && !experiences.isEmpty()) score += 10;
        boolean hasPortfolio = (portfolioLinks != null && !portfolioLinks.isEmpty())
                || (portfolioFiles != null && !portfolioFiles.isEmpty());
        if (hasPortfolio) score += 10;
        boolean hasContact = contacts != null && (notBlank(contacts.phone) || notBlank(contacts.kakaoId) || notBlank(contacts.personalEmail));
        if (hasContact) score += 10;
        return score;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
