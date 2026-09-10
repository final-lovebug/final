package com.ubidict.backend.member.presentation;

import com.ubidict.backend.member.presentation.dto.CreateMemberRequest;
import com.ubidict.backend.member.presentation.dto.MemberResponse;
import com.ubidict.backend.member.presentation.dto.UpdateMemberRequest;
import com.ubidict.backend.member.service.MemberService;
import com.ubidict.backend.member.service.model.CreateMemberCommand;
import com.ubidict.backend.member.service.model.MemberResult;
import com.ubidict.backend.member.service.model.UpdateMemberCommand;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Member 도메인 CRUD API. {@code /me}는 JWT 인증 기반 본인 식별을 쓴다 —
 * {@code com.ubidict.backend.member.infra.security.JwtAuthenticationFilter}가 SecurityContext에
 * 채운 principal이 회원 id({@link Long})라서 {@link AuthenticationPrincipal}로 바로 꺼낸다.
 *
 * <p>{@code POST}(생성)만 예외다 — 아직 "본인"이 없는 신규 가입 시점이라 테스트·관리자용으로 남겨둔다.
 * 실제 가입은 Google 로그인 시 자동 생성된다({@code docs/API.md} Google 로그인 절).
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<MemberResponse> create(@Valid @RequestBody CreateMemberRequest request) {
        MemberResult result = memberService.create(new CreateMemberCommand(
                request.email(), request.displayName(), request.provider(), request.providerId()));
        MemberResponse response = MemberResponse.from(result);
        return ResponseEntity.created(URI.create("/api/members/" + response.memberId()))
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyProfile(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(MemberResponse.from(memberService.getById(memberId)));
    }

    @PatchMapping("/me")
    public ResponseEntity<MemberResponse> updateMyProfile(
            @AuthenticationPrincipal Long memberId, @Valid @RequestBody UpdateMemberRequest request) {
        MemberResult result = memberService.update(new UpdateMemberCommand(memberId, request.displayName()));
        return ResponseEntity.ok(MemberResponse.from(result));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdrawMyself(@AuthenticationPrincipal Long memberId) {
        memberService.withdraw(memberId);

        return ResponseEntity.noContent().build();
    }
}
