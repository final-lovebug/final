package com.ubidict.backend.member.presentation;

import com.ubidict.backend.member.presentation.dto.CreateMemberRequest;
import com.ubidict.backend.member.presentation.dto.MemberResponse;
import com.ubidict.backend.member.presentation.dto.UpdateMemberRequest;
import com.ubidict.backend.member.service.MemberService;
import com.ubidict.backend.member.service.model.CreateMemberCommand;
import com.ubidict.backend.member.service.model.MemberResult;
import com.ubidict.backend.member.service.model.UpdateMemberCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Member 도메인 CRUD API. {@code /me}(인증 기반)가 아니라 {@code /{memberId}} 경로를 쓴다 —
 * 로그인(JWT/Security)이 아직 별도 티켓이라 "본인"을 판별할 인증 정보가 없기 때문이다.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public MemberResponse create(@Valid @RequestBody CreateMemberRequest request) {
        MemberResult result = memberService.create(new CreateMemberCommand(
                request.email(), request.displayName(), request.provider(), request.providerId()));
        return MemberResponse.from(result);
    }

    @GetMapping("/{memberId}")
    public MemberResponse getById(@PathVariable Long memberId) {
        return MemberResponse.from(memberService.getById(memberId));
    }

    @PatchMapping("/{memberId}")
    public MemberResponse update(@PathVariable Long memberId, @Valid @RequestBody UpdateMemberRequest request) {
        MemberResult result = memberService.update(new UpdateMemberCommand(memberId, request.displayName()));
        return MemberResponse.from(result);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{memberId}")
    public void withdraw(@PathVariable Long memberId) {
        memberService.withdraw(memberId);
    }
}
