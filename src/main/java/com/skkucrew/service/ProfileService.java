package com.skkucrew.service;

import com.skkucrew.common.ApiException;
import com.skkucrew.common.ErrorResponse.FieldError;
import com.skkucrew.domain.Member;
import com.skkucrew.domain.Profile;
import com.skkucrew.service.Dtos.MeResponse;
import com.skkucrew.service.Dtos.ProfileResponse;
import com.skkucrew.service.Dtos.ProfileUpdateRequest;
import com.skkucrew.store.DataStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ProfileService {

    private static final Pattern PHONE = Pattern.compile("^010-\\d{4}-\\d{4}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final DataStore store;

    public ProfileService(DataStore store) {
        this.store = store;
    }

    public MeResponse me(Member m) {
        Profile p = store.profile(m.id).orElse(null);
        return new MeResponse(m.id, m.name, m.type, m.schoolEmail,
                p == null ? null : p.isRequiredFilled(),
                p == null ? null : p.completeness());
    }

    public Profile profileOf(Member m) {
        return store.profile(m.id).orElseGet(() -> {
            Profile p = new Profile();
            p.memberId = m.id;
            store.saveProfile(p);
            return p;
        });
    }

    public ProfileResponse getMyProfile(Member m) {
        return toResponse(m, profileOf(m));
    }

    /** 프로필 전체 교체 (FR-PROF-01). 검증은 화면에 보이는 핵심 규칙만. */
    public ProfileResponse updateMyProfile(Member m, ProfileUpdateRequest req) {
        List<FieldError> errors = new ArrayList<>();
        if (req.name() != null && (req.name().length() < 2 || req.name().length() > 20)) {
            errors.add(new FieldError("name", "이름은 2~20자입니다."));
        }
        if (req.fields() != null && req.fields().size() > 5) {
            errors.add(new FieldError("fields", "관심 분야는 최대 5개입니다."));
        }
        if (req.bio() != null && req.bio().length() > 50) {
            errors.add(new FieldError("bio", "한 줄 소개는 50자 이하입니다."));
        }
        if (req.contacts() != null) {
            Profile.Contacts c = req.contacts();
            if (notBlank(c.phone) && !PHONE.matcher(c.phone).matches()) {
                errors.add(new FieldError("contacts.phone", "010-0000-0000 형식으로 입력해 주세요."));
            }
            if (notBlank(c.kakaoId) && (c.kakaoId.length() < 4 || c.kakaoId.length() > 20)) {
                errors.add(new FieldError("contacts.kakaoId", "카카오톡 ID는 4~20자입니다."));
            }
            if (notBlank(c.personalEmail) && !EMAIL.matcher(c.personalEmail).matches()) {
                errors.add(new FieldError("contacts.personalEmail", "이메일 형식이 올바르지 않습니다."));
            }
        }
        if (!errors.isEmpty()) {
            throw ApiException.badRequest("입력값을 확인해 주세요.", errors);
        }

        Profile p = profileOf(m);
        if (req.name() != null) m.name = req.name();
        p.profileImageUrl = req.profileImageUrl();
        p.majorPrimary = req.majorPrimary();
        p.majorsSecondary = nz(req.majorsSecondary());
        p.academicStatus = req.academicStatus();
        p.bio = req.bio();
        p.fields = nz(req.fields());
        p.skills = nz(req.skills());
        p.skills.forEach(s -> { if (s.id == null) s.id = store.nextItemId(); });
        p.experiences = nz(req.experiences());
        p.experiences.forEach(e -> { if (e.id == null) e.id = store.nextItemId(); });
        p.portfolioLinks = nz(req.portfolioLinks());
        p.portfolioLinks.forEach(l -> { if (l.id == null) l.id = store.nextItemId(); });
        p.portfolioFiles = nz(req.portfolioFiles());
        p.portfolioFiles.forEach(f -> { if (f.id == null) f.id = store.nextItemId(); });
        p.contacts = req.contacts() == null ? new Profile.Contacts() : req.contacts();
        if (req.defaultShare() != null) {
            p.defaultShare = new LinkedHashMap<>(req.defaultShare());
        }
        store.saveProfile(p);
        return toResponse(m, p);
    }

    public ProfileResponse toResponse(Member m, Profile p) {
        return new ProfileResponse(
                m.name, m.schoolEmail, p.profileImageUrl, p.majorPrimary, p.majorsSecondary, p.academicStatus,
                p.bio, p.fields, p.skills, p.experiences, p.portfolioLinks, p.portfolioFiles, p.contacts,
                p.defaultShare, p.activityHistory, p.completeness());
    }

    private static <T> List<T> nz(List<T> list) {
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
