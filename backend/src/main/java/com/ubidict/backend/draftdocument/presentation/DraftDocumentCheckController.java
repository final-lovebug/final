package com.ubidict.backend.draftdocument.presentation;

import com.ubidict.backend.draftdocument.presentation.dto.CheckJobResponse;
import com.ubidict.backend.draftdocument.presentation.dto.CreateCheckJobRequest;
import com.ubidict.backend.draftdocument.service.DraftDocumentCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** TODO(NFR-USR-001): 인증이 들어오면 memberId 파라미터를 걷어내고 인증 주체에서 해석한다. */
@RestController
@RequestMapping("/api/draft-documents/checks")
@RequiredArgsConstructor
public class DraftDocumentCheckController {

    private final DraftDocumentCheckService draftDocumentCheckService;

    @PostMapping
    public ResponseEntity<CheckJobResponse> request(
            @RequestParam Long memberId, @Valid @RequestBody CreateCheckJobRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(CheckJobResponse.from(draftDocumentCheckService.request(request.toCommand(memberId))));
    }

    @GetMapping("/{checkJobId}")
    public ResponseEntity<CheckJobResponse> read(@PathVariable Long checkJobId, @RequestParam Long memberId) {
        return ResponseEntity.ok(CheckJobResponse.from(draftDocumentCheckService.read(checkJobId, memberId)));
    }
}
