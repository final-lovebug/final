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
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CandidateTermController {
    private final CandidateTermService service;

    @PostMapping("/draft-dictionaries/{id}/candidate-terms")
    public ResponseEntity<CandidateTermResponse> add(
            @PathVariable Long id, @RequestParam Long memberId, @Valid @RequestBody AddCandidateTermRequest request) {
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
            @RequestParam(defaultValue = "occurrenceCount,desc") String sort) {
        return ResponseEntity.ok(PageResponse.from(
                service.search(new CandidateTermSearchQuery(id, status, form, minOccurrenceCount, page, size, sort))
                        .map(CandidateTermResponse::from)));
    }

    @GetMapping("/candidate-terms/{id}")
    public ResponseEntity<CandidateTermResponse> read(@PathVariable Long id) {
        return ResponseEntity.ok(CandidateTermResponse.from(service.read(id)));
    }

    @PatchMapping("/candidate-terms/{id}")
    public ResponseEntity<CandidateTermResponse> edit(
            @PathVariable Long id, @RequestParam Long memberId, @RequestBody EditCandidateTermRequest request) {
        return ResponseEntity.ok(CandidateTermResponse.from(service.edit(request.toCommand(id, memberId))));
    }

    @DeleteMapping("/candidate-terms/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/candidate-terms/{id}/registration-approval")
    public ResponseEntity<CandidateTermResponse> approve(@PathVariable Long id, @RequestParam Long memberId) {
        return ResponseEntity.ok(CandidateTermResponse.from(service.decide(
                new DecideCandidateTermCommand(id, memberId, CandidateTermStatus.REGISTRATION_APPROVED, null, null))));
    }

    @PostMapping("/candidate-terms/{id}/synonym-merge")
    public ResponseEntity<CandidateTermResponse> merge(
            @PathVariable Long id, @RequestParam Long memberId, @Valid @RequestBody MergeCandidateTermRequest request) {
        return ResponseEntity.ok(CandidateTermResponse.from(service.decide(new DecideCandidateTermCommand(
                id, memberId, CandidateTermStatus.MERGED_AS_SYNONYM, null, request.mergeTargetTermId()))));
    }

    @PostMapping("/candidate-terms/{id}/rejection")
    public ResponseEntity<CandidateTermResponse> reject(
            @PathVariable Long id,
            @RequestParam Long memberId,
            @Valid @RequestBody RejectCandidateTermRequest request) {
        return ResponseEntity.ok(CandidateTermResponse.from(service.decide(new DecideCandidateTermCommand(
                id, memberId, CandidateTermStatus.REJECTED, request.rejectReason(), null))));
    }

    @PostMapping("/candidate-terms/{id}/hold")
    public ResponseEntity<CandidateTermResponse> hold(@PathVariable Long id, @RequestParam Long memberId) {
        return ResponseEntity.ok(CandidateTermResponse.from(
                service.decide(new DecideCandidateTermCommand(id, memberId, CandidateTermStatus.ON_HOLD, null, null))));
    }

    @PostMapping("/draft-dictionaries/{id}/candidate-terms/bulk-decision")
    public ResponseEntity<BulkDecisionResponse> bulk(
            @PathVariable Long id,
            @RequestParam Long memberId,
            @Valid @RequestBody BulkDecideCandidateTermsRequest request) {
        return ResponseEntity.ok(BulkDecisionResponse.from(service.bulkDecide(request.toCommand(id, memberId))));
    }
}
