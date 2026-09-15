package com.ubidict.backend.draftdictionary.presentation;

import com.ubidict.backend.common.presentation.PageResponse;
import com.ubidict.backend.draftdictionary.domain.CandidateTermStatus;
import com.ubidict.backend.draftdictionary.presentation.dto.*;
import com.ubidict.backend.draftdictionary.service.CandidateTermService;
import com.ubidict.backend.draftdictionary.service.model.CandidateTermSearchQuery;
import com.ubidict.backend.draftdictionary.service.model.DecideCandidateTermCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CandidateTermController {
    // 판정 엔드포인트의 현재 상태는 아래 「사용 안 함」 주석 블록을 읽는다.

    private final CandidateTermService service;

    @PostMapping("/draft-dictionaries/{id}/candidate-terms")
    public ResponseEntity<CandidateTermResponse> add(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody AddCandidateTermRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CandidateTermResponse.from(service.add(request.toCommand(id, memberId))));
    }

    @GetMapping("/draft-dictionaries/{id}/candidate-terms")
    public ResponseEntity<PageResponse<CandidateTermResponse>> search(
            @PathVariable Long id,
            @RequestParam(required = false) CandidateTermStatus status,
            @RequestParam(required = false) String form,
            @RequestParam(required = false) Integer minOccurrenceCount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurrenceCount,desc") String sort,
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(PageResponse.from(service.search(
                        new CandidateTermSearchQuery(id, status, form, minOccurrenceCount, page, size, sort, memberId))
                .map(CandidateTermResponse::from)));
    }

    @GetMapping("/candidate-terms/{id}")
    public ResponseEntity<CandidateTermResponse> read(@PathVariable Long id, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(CandidateTermResponse.from(service.read(id, memberId)));
    }

    @PatchMapping("/candidate-terms/{id}")
    public ResponseEntity<CandidateTermResponse> edit(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId,
            @RequestBody EditCandidateTermRequest request) {
        return ResponseEntity.ok(CandidateTermResponse.from(service.edit(request.toCommand(id, memberId))));
    }

    @DeleteMapping("/candidate-terms/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Long memberId) {
        service.delete(id, memberId);
        return ResponseEntity.noContent().build();
    }

    // ────────────────────────────────────────────────────────────────────────
    // 아래 판정 5종(등재 승인·동의어 편입·거절·보류·일괄)은 **사용 안 함**이다.
    //
    // `docs/plan/DRAFT_PLAN.md` 가 사전집 초안을 단일 페이지로 바꾸면서 후보어별 판정을
    // 화면에서 걷어냈다. 준비 여부는 이제 판정 상태가 아니라 「모든 후보어가 대표어와 정의를
    // 가졌는가」로 판단하고(`DraftDictionaryReviewReadinessValidator`), 발행 목록도 상태로
    // 거르지 않는다(`DraftDictionaryQueryAdapter.readFinalTerms`).
    //
    // **지우지 않고 남기는 이유** — 이미 판정이 기록된 초안의 데이터가 DB에 있고,
    // `CandidateTermStatus` 를 함께 걷어내면 그 행들을 읽을 수 없게 된다. 새 초안·리뷰 요청
    // 경로는 이 엔드포인트들을 호출하지 않으며, 프런트에도 호출부가 없다.
    // ────────────────────────────────────────────────────────────────────────

    @PostMapping("/candidate-terms/{id}/registration-approval")
    public ResponseEntity<CandidateTermResponse> approve(
            @PathVariable Long id, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(CandidateTermResponse.from(service.decide(
                new DecideCandidateTermCommand(id, memberId, CandidateTermStatus.REGISTRATION_APPROVED, null, null))));
    }

    @PostMapping("/candidate-terms/{id}/synonym-merge")
    public ResponseEntity<CandidateTermResponse> merge(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody MergeCandidateTermRequest request) {
        return ResponseEntity.ok(CandidateTermResponse.from(service.decide(new DecideCandidateTermCommand(
                id, memberId, CandidateTermStatus.MERGED_AS_SYNONYM, null, request.mergeTargetTermId()))));
    }

    @PostMapping("/candidate-terms/{id}/rejection")
    public ResponseEntity<CandidateTermResponse> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody RejectCandidateTermRequest request) {
        return ResponseEntity.ok(CandidateTermResponse.from(service.decide(new DecideCandidateTermCommand(
                id, memberId, CandidateTermStatus.REJECTED, request.rejectReason(), null))));
    }

    @PostMapping("/candidate-terms/{id}/hold")
    public ResponseEntity<CandidateTermResponse> hold(@PathVariable Long id, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(CandidateTermResponse.from(
                service.decide(new DecideCandidateTermCommand(id, memberId, CandidateTermStatus.ON_HOLD, null, null))));
    }

    @PostMapping("/draft-dictionaries/{id}/candidate-terms/bulk-decision")
    public ResponseEntity<BulkDecisionResponse> bulk(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody BulkDecideCandidateTermsRequest request) {
        return ResponseEntity.ok(BulkDecisionResponse.from(service.bulkDecide(request.toCommand(id, memberId))));
    }
}
