package com.ubidict.backend.dev;

import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.infra.CandidateTermRepository;
import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.infra.MemberRepository;
import com.ubidict.backend.reviewrequest.domain.Comment;
import com.ubidict.backend.reviewrequest.domain.Review;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestStatus;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.reviewrequest.domain.Reviewer;
import com.ubidict.backend.reviewrequest.domain.RevisionDictionary;
import com.ubidict.backend.reviewrequest.infra.CommentRepository;
import com.ubidict.backend.reviewrequest.infra.ReviewRepository;
import com.ubidict.backend.reviewrequest.infra.ReviewRequestRepository;
import com.ubidict.backend.reviewrequest.infra.ReviewerRepository;
import com.ubidict.backend.reviewrequest.infra.RevisionDictionaryRepository;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개발 프로필에서만 기존 사전집 개정안에 리뷰 화면용 데이터를 한 번 주입한다.
 *
 * <p>소셜 로그인 계정은 하나만 있어도 되도록 실제 로그인 회원을 요청자로 그대로 두고,
 * 별도의 목업 회원을 리뷰어로 만든다. 목업 회원이 이미 있으면 다시 넣지 않으므로 앱을
 * 재시작해도 리뷰 이력이 중복되지 않는다.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevReviewMockDataInitializer implements ApplicationRunner {

    private static final String MOCK_PROVIDER_PREFIX = "dev-review-mock-";

    private final MemberRepository memberRepository;
    private final ParticipantRepository participantRepository;
    private final ReviewRequestRepository reviewRequestRepository;
    private final RevisionDictionaryRepository revisionDictionaryRepository;
    private final CandidateTermRepository candidateTermRepository;
    private final ReviewerRepository reviewerRepository;
    private final ReviewRepository reviewRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Optional<ReviewRequest> request = findActiveDictionaryReviewRequest();
        if (request.isEmpty()) {
            log.info("[DevReviewMockDataInitializer] Active dictionary review request not found; skip.");
            return;
        }

        ReviewRequest reviewRequest = request.get();
        if (memberRepository
                .findByProviderAndProviderId(OAuthProvider.GOOGLE, MOCK_PROVIDER_PREFIX + "1")
                .isPresent()) {
            log.info(
                    "[DevReviewMockDataInitializer] Mock review data already exists. reviewRequestId={}",
                    reviewRequest.getId());
            return;
        }

        RevisionDictionary revision = revisionDictionaryRepository
                .findTopByReviewRequestIdOrderByReexamineRoundDesc(reviewRequest.getId())
                .orElse(null);
        if (revision == null) {
            log.info("[DevReviewMockDataInitializer] Dictionary revision not found; skip.");
            return;
        }

        List<CandidateTerm> terms = candidateTermRepository.findAllByDraftDictionaryIdAndDeletedAtIsNullOrderByFormAsc(
                revision.getDraftDictionaryId());
        if (terms.isEmpty()) {
            log.info("[DevReviewMockDataInitializer] Candidate terms not found; skip.");
            return;
        }

        Long requesterId = reviewRequest.getRequesterId();
        List<Member> mockMembers = createMockMembers();
        for (Member member : mockMembers) {
            participantRepository
                    .findByWorkspaceIdAndMemberId(reviewRequest.getWorkspaceId(), member.getId())
                    .orElseGet(() -> participantRepository.save(Participant.join(
                            reviewRequest.getWorkspaceId(), member.getId(), Permission.REGULAR, requesterId)));
            reviewerRepository.save(Reviewer.create(reviewRequest.getId(), member.getId(), requesterId));
        }

        Review oldReview = reviewRepository.save(Review.submit(
                reviewRequest.getId(),
                mockMembers.get(0).getId(),
                revision.getReexamineRound(),
                ReviewVerdict.CHANGES_REQUESTED,
                mockMembers.get(0).getId()));
        saveComment(oldReview, mockMembers.get(0).getId(), "이전 리뷰에서 변경을 요청했지만, 다음 리뷰에서 승인으로 바꿨습니다.", null);

        Review latestApproval = reviewRepository.save(Review.submit(
                reviewRequest.getId(),
                mockMembers.get(0).getId(),
                revision.getReexamineRound(),
                ReviewVerdict.APPROVED,
                mockMembers.get(0).getId()));
        saveComment(latestApproval, mockMembers.get(0).getId(), "최신 개정안은 전체적으로 확인했습니다. 발행해도 좋습니다.", null);
        saveComment(
                latestApproval,
                mockMembers.get(0).getId(),
                "대표어와 정의가 자연스럽게 정리되었습니다.",
                terms.get(0).getId());

        Review changes = reviewRepository.save(Review.submit(
                reviewRequest.getId(),
                mockMembers.get(1).getId(),
                revision.getReexamineRound(),
                ReviewVerdict.CHANGES_REQUESTED,
                mockMembers.get(1).getId()));
        saveComment(changes, mockMembers.get(1).getId(), "두 번째 용어의 정의를 조금 더 구체적으로 다듬어 주세요.", null);
        if (terms.size() > 1) {
            saveComment(
                    changes,
                    mockMembers.get(1).getId(),
                    "문서에서 쓰인 의미와 정의가 맞는지 확인이 필요합니다.",
                    terms.get(1).getId());
        }

        reviewRepository.save(Review.submit(
                reviewRequest.getId(),
                mockMembers.get(2).getId(),
                revision.getReexamineRound(),
                ReviewVerdict.APPROVED,
                mockMembers.get(2).getId()));
        // mockMembers.get(3)은 리뷰를 제출하지 않아 화면에서 「대기」로 보인다.
        makeChangesRequested(reviewRequest);

        log.info(
                "[DevReviewMockDataInitializer] Seeded mock reviews. reviewRequestId={}, reviewerCount={}",
                reviewRequest.getId(),
                mockMembers.size());
    }

    private List<Member> createMockMembers() {
        return List.of(
                createMockMember("1", "김리뷰"),
                createMockMember("2", "이검토"),
                createMockMember("3", "박승인"),
                createMockMember("4", "최대기"));
    }

    private Member createMockMember(String suffix, String displayName) {
        return memberRepository.save(Member.create(
                "dev-review-mock-" + suffix + "@example.local",
                displayName,
                OAuthProvider.GOOGLE,
                MOCK_PROVIDER_PREFIX + suffix));
    }

    private void saveComment(Review review, Long authorId, String content, Long targetItemId) {
        commentRepository.save(Comment.create(review.getId(), authorId, content, null, targetItemId, null, authorId));
    }

    private void makeChangesRequested(ReviewRequest reviewRequest) {
        if (reviewRequest.getStatus() == ReviewRequestStatus.PENDING_REVIEW) {
            reviewRequest.startReview();
        }
        if (reviewRequest.getStatus() == ReviewRequestStatus.IN_REVIEW
                || reviewRequest.getStatus() == ReviewRequestStatus.APPROVED) {
            reviewRequest.requestChanges();
        }
    }

    private Optional<ReviewRequest> findActiveDictionaryReviewRequest() {
        return reviewRequestRepository.findAll().stream()
                .filter(request -> !request.isDeleted())
                .filter(request -> request.getType() == ReviewRequestType.DICTIONARY)
                .filter(request -> request.getStatus() != ReviewRequestStatus.REVISED)
                .filter(request -> request.getStatus() != ReviewRequestStatus.CANCELED)
                .max((first, second) -> first.getCreatedAt().compareTo(second.getCreatedAt()));
    }
}
