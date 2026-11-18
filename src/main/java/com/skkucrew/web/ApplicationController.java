package com.skkucrew.web;

import com.skkucrew.domain.Codes.ApplicantSort;
import com.skkucrew.domain.Codes.Decision;
import com.skkucrew.service.ApplicationService;
import com.skkucrew.service.AuthService;
import com.skkucrew.service.Dtos.AcceptedContact;
import com.skkucrew.service.Dtos.AnnounceRequest;
import com.skkucrew.service.Dtos.AnnounceResponse;
import com.skkucrew.service.Dtos.ApplicantItem;
import com.skkucrew.service.Dtos.ApplicationDetail;
import com.skkucrew.service.Dtos.ApplicationForm;
import com.skkucrew.service.Dtos.ApplyRequest;
import com.skkucrew.service.Dtos.ApplyResponse;
import com.skkucrew.service.Dtos.DecisionRequest;
import com.skkucrew.service.Dtos.DecisionResponse;
import com.skkucrew.service.Dtos.DraftRequest;
import com.skkucrew.service.Dtos.DraftResponse;
import com.skkucrew.service.Dtos.MyApplicationItem;
import com.skkucrew.service.Dtos.ReviewDetail;
import com.skkucrew.service.Dtos.WithdrawResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApplicationController {

    private final AuthService authService;
    private final ApplicationService applicationService;

    public ApplicationController(AuthService authService, ApplicationService applicationService) {
        this.authService = authService;
        this.applicationService = applicationService;
    }

    // ---------------- 지원자 ----------------

    @Tag(name = "4. 지원 (지원자)")
    @Operation(summary = "지원서 작성 화면 데이터", description = "필수 공유 항목, 선택 항목(기본 공유 여부), 연락처(요청 여부·보유 여부), 추가 질문, 저장된 초안(draft). 지원 불가면 409")
    @GetMapping("/postings/{postingId}/application-form")
    public ApplicationForm form(@RequestHeader(value = "Authorization", required = false) String auth,
                                @PathVariable Long postingId) {
        return applicationService.form(authService.require(auth), postingId);
    }

    @Tag(name = "4. 지원 (지원자)")
    @Operation(summary = "지원서 제출", description = "Idempotency-Key 헤더가 같으면 지원서를 다시 만들지 않는다. 요청 연락처는 consent=true 필수")
    @PostMapping("/postings/{postingId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplyResponse apply(@RequestHeader(value = "Authorization", required = false) String auth,
                               @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                               @PathVariable Long postingId,
                               @RequestBody ApplyRequest req) {
        return applicationService.apply(authService.require(auth), postingId, req, idempotencyKey);
    }

    @Tag(name = "4. 지원 (지원자)")
    @Operation(summary = "내 지원 현황", description = "최신 제출순. displayStatus: SUBMITTED/UNDER_REVIEW/ACCEPTED/REJECTED/WITHDRAWN")
    @GetMapping("/applications/me")
    public List<MyApplicationItem> mine(@RequestHeader(value = "Authorization", required = false) String auth) {
        return applicationService.myApplications(authService.require(auth));
    }

    @Tag(name = "4. 지원 (지원자)")
    @Operation(summary = "내 지원서 상세·결과", description = "발표 후 합격이면 모집자 연락처와 공개된 내 연락처 포함")
    @GetMapping("/applications/{applicationId}")
    public ApplicationDetail detail(@RequestHeader(value = "Authorization", required = false) String auth,
                                    @PathVariable Long applicationId) {
        return applicationService.myApplicationDetail(authService.require(auth), applicationId);
    }

    @Tag(name = "4. 지원 (지원자)")
    @Operation(summary = "지원 취소", description = "마감 전에만 가능")
    @PostMapping("/applications/{applicationId}/withdraw")
    public WithdrawResponse withdraw(@RequestHeader(value = "Authorization", required = false) String auth,
                                     @PathVariable Long applicationId) {
        return applicationService.withdraw(authService.require(auth), applicationId);
    }

    @Tag(name = "4. 지원 (지원자)")
    @Operation(summary = "답변 초안 조회", description = "지원서 작성 화면의 \"초안 불러오기\"용. 없으면 exists=false. 공고가 마감되면 초안은 삭제된다")
    @GetMapping("/postings/{postingId}/application-draft")
    public DraftResponse getDraft(@RequestHeader(value = "Authorization", required = false) String auth,
                                  @PathVariable Long postingId) {
        return applicationService.getDraft(authService.requirePersonal(auth), postingId);
    }

    @Tag(name = "4. 지원 (지원자)")
    @Operation(summary = "답변 초안 저장 (제출 아님)",
            description = "추가 질문 답변을 초안으로 덮어써서 저장한다. source=MCP면 GPT가 저장한 초안. 지원 가능한 모집중 공고만 (FR-MCP-07)")
    @PutMapping("/postings/{postingId}/application-draft")
    public DraftResponse saveDraft(@RequestHeader(value = "Authorization", required = false) String auth,
                                   @PathVariable Long postingId,
                                   @RequestBody DraftRequest req) {
        return applicationService.saveDraft(authService.requirePersonal(auth), postingId, req);
    }

    @Tag(name = "4. 지원 (지원자)")
    @Operation(summary = "답변 초안 삭제")
    @DeleteMapping("/postings/{postingId}/application-draft")
    public DraftResponse deleteDraft(@RequestHeader(value = "Authorization", required = false) String auth,
                                     @PathVariable Long postingId) {
        return applicationService.deleteDraft(authService.requirePersonal(auth), postingId);
    }

    // ---------------- 모집자 ----------------

    @Tag(name = "5. 검토·선발 (모집자)")
    @Operation(summary = "지원자 목록",
            description = "?decision=PENDING 필터, sort=SUBMITTED(제출순, 기본)|NAME(이름순), keyword=이름·기술 검색. 취소된 지원서는 withdrawn=true")
    @GetMapping("/postings/{postingId}/applications")
    public List<ApplicantItem> applicants(@RequestHeader(value = "Authorization", required = false) String auth,
                                          @PathVariable Long postingId,
                                          @RequestParam(required = false) Decision decision,
                                          @RequestParam(defaultValue = "SUBMITTED") ApplicantSort sort,
                                          @RequestParam(required = false) String keyword) {
        return applicationService.applicants(authService.require(auth), postingId, decision, sort, keyword);
    }

    @Tag(name = "5. 검토·선발 (모집자)")
    @Operation(summary = "지원서 상세 (모집자용)", description = "contacts는 발표 전 항상 null")
    @GetMapping("/applications/{applicationId}/review")
    public ReviewDetail review(@RequestHeader(value = "Authorization", required = false) String auth,
                               @PathVariable Long applicationId) {
        return applicationService.review(authService.require(auth), applicationId);
    }

    @Tag(name = "5. 검토·선발 (모집자)")
    @Operation(summary = "결정·메모 저장", description = "decision: PENDING/ACCEPTED/REJECTED. 발표 전까지 변경 가능")
    @PatchMapping("/applications/{applicationId}/decision")
    public DecisionResponse decide(@RequestHeader(value = "Authorization", required = false) String auth,
                                   @PathVariable Long applicationId,
                                   @RequestBody DecisionRequest req) {
        return applicationService.decide(authService.require(auth), applicationId, req);
    }

    @Tag(name = "5. 검토·선발 (모집자)")
    @Operation(summary = "결과 일괄 발표", description = "마감(CLOSED) 상태이고 미정 지원서가 없어야 함. acceptMessage 필수")
    @PostMapping("/postings/{postingId}/announce")
    public AnnounceResponse announce(@RequestHeader(value = "Authorization", required = false) String auth,
                                     @PathVariable Long postingId,
                                     @RequestBody AnnounceRequest req) {
        return applicationService.announce(authService.require(auth), postingId, req);
    }

    @Tag(name = "5. 검토·선발 (모집자)")
    @Operation(summary = "합격자 연락처 (발표 후)", description = "합격자가 제공에 동의한 연락처만")
    @GetMapping("/postings/{postingId}/accepted-contacts")
    public List<AcceptedContact> acceptedContacts(@RequestHeader(value = "Authorization", required = false) String auth,
                                                  @PathVariable Long postingId) {
        return applicationService.acceptedContacts(authService.require(auth), postingId);
    }
}
