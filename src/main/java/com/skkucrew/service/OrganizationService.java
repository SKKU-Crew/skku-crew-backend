package com.skkucrew.service;

import com.skkucrew.common.ApiException;
import com.skkucrew.domain.Codes.MemberType;
import com.skkucrew.domain.Codes.PostingStatus;
import com.skkucrew.domain.Member;
import com.skkucrew.domain.Posting;
import com.skkucrew.service.Dtos.FollowResponse;
import com.skkucrew.service.Dtos.OrgPage;
import com.skkucrew.service.Dtos.PostingCard;
import com.skkucrew.store.DataStore;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** 조직 페이지와 관심 조직 (FR-ORG-01, 표 4-34 N-05). 조직 정보 수정·인증 심사는 데모에서 생략한다. */
@Service
public class OrganizationService {

    private final DataStore store;
    private final PostingService postingService;

    public OrganizationService(DataStore store, PostingService postingService) {
        this.store = store;
        this.postingService = postingService;
    }

    public OrgPage page(Long orgId, Member viewer) {
        Member org = getOrg(orgId);
        List<Posting> mine = store.allPostings().stream()
                .filter(p -> p.recruiterId.equals(orgId))
                .sorted(Comparator.comparing((Posting p) -> p.createdAt).reversed())
                .toList();
        List<PostingCard> open = mine.stream()
                .filter(p -> p.status == PostingStatus.OPEN)
                .map(p -> postingService.toCard(p, viewer)).toList();
        List<PostingCard> past = mine.stream()
                .filter(p -> p.status == PostingStatus.CLOSED || p.status == PostingStatus.ANNOUNCED)
                .map(p -> postingService.toCard(p, viewer)).toList();
        boolean followed = viewer != null && store.isFollowing(viewer.id, orgId);
        return new OrgPage(org.id, org.name, org.orgType, org.verified, org.logoUrl, org.orgIntro, org.orgFields,
                org.orgLinks, followed, store.followersOf(orgId).size(), open, past);
    }

    public FollowResponse follow(Member member, Long orgId) {
        getOrg(orgId);
        store.follow(member.id, orgId);
        return new FollowResponse(orgId, true, store.followersOf(orgId).size());
    }

    public FollowResponse unfollow(Member member, Long orgId) {
        getOrg(orgId);
        store.unfollow(member.id, orgId);
        return new FollowResponse(orgId, false, store.followersOf(orgId).size());
    }

    private Member getOrg(Long orgId) {
        Member m = store.member(orgId).orElseThrow(() -> ApiException.notFound("조직"));
        if (m.type != MemberType.ORGANIZATION) {
            throw ApiException.notFound("조직");
        }
        return m;
    }
}
