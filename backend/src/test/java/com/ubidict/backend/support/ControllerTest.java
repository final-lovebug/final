package com.ubidict.backend.support;

import com.ubidict.backend.dictionary.service.DictionaryService;
import com.ubidict.backend.document.service.DocumentService;
import com.ubidict.backend.draftdictionary.service.CandidateTermService;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryExtractionCallbackService;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryExtractionService;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryService;
import com.ubidict.backend.draftdocument.service.DraftDocumentCheckCallbackService;
import com.ubidict.backend.draftdocument.service.DraftDocumentCheckService;
import com.ubidict.backend.draftdocument.service.DraftDocumentService;
import com.ubidict.backend.draftdocument.service.SuggestionTermService;
import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.member.infra.security.OAuthExchangeCodeRedisRepository;
import com.ubidict.backend.member.infra.security.RefreshTokenRedisRepository;
import com.ubidict.backend.member.infra.security.RegistrationTokenRedisRepository;
import com.ubidict.backend.member.presentation.RefreshTokenCookieProvider;
import com.ubidict.backend.member.service.DevLoginService;
import com.ubidict.backend.member.service.LogoutService;
import com.ubidict.backend.member.service.MemberDirectory;
import com.ubidict.backend.member.service.MemberOAuthLoginService;
import com.ubidict.backend.member.service.MemberService;
import com.ubidict.backend.member.service.TokenReissueService;
import com.ubidict.backend.notification.service.NotificationService;
import com.ubidict.backend.reviewrequest.service.CommentService;
import com.ubidict.backend.reviewrequest.service.DraftReviewRequestService;
import com.ubidict.backend.reviewrequest.service.ReexamineService;
import com.ubidict.backend.reviewrequest.service.ReviewRequestService;
import com.ubidict.backend.reviewrequest.service.ReviewService;
import com.ubidict.backend.reviewrequest.service.ReviewerService;
import com.ubidict.backend.reviewrequest.service.ReviseService;
import com.ubidict.backend.reviewrequest.service.RevisionService;
import com.ubidict.backend.workspace.service.InvitationService;
import com.ubidict.backend.workspace.service.ParticipantService;
import com.ubidict.backend.workspace.service.WorkspaceService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Presentation 테스트의 공통 MVC 컨텍스트와 인증 관련 대역. */
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest
@Import(ControllerTest.CookieProviderConfiguration.class)
public abstract class ControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected JwtProvider jwtProvider;

    @MockitoBean
    protected RefreshTokenRedisRepository refreshTokenRedisRepository;

    @MockitoBean
    protected OAuthExchangeCodeRedisRepository oAuthExchangeCodeRedisRepository;

    @MockitoBean
    protected RegistrationTokenRedisRepository registrationTokenRedisRepository;

    @MockitoBean
    protected DictionaryService dictionaryService;

    @MockitoBean
    protected DocumentService documentService;

    @MockitoBean
    protected CandidateTermService candidateTermService;

    @MockitoBean
    protected DraftDictionaryExtractionCallbackService draftDictionaryExtractionCallbackService;

    @MockitoBean
    protected DraftDictionaryExtractionService draftDictionaryExtractionService;

    @MockitoBean
    protected DraftDictionaryService draftDictionaryService;

    @MockitoBean
    protected DraftDocumentCheckCallbackService draftDocumentCheckCallbackService;

    @MockitoBean
    protected DraftDocumentCheckService draftDocumentCheckService;

    @MockitoBean
    protected DraftDocumentService draftDocumentService;

    @MockitoBean
    protected SuggestionTermService suggestionTermService;

    @MockitoBean
    protected DevLoginService devLoginService;

    @MockitoBean
    protected LogoutService logoutService;

    @MockitoBean
    protected MemberDirectory memberDirectory;

    @MockitoBean
    protected MemberOAuthLoginService memberOAuthLoginService;

    @MockitoBean
    protected MemberService memberService;

    @MockitoBean
    protected TokenReissueService tokenReissueService;

    @MockitoBean
    protected NotificationService notificationService;

    @MockitoBean
    protected CommentService commentService;

    @MockitoBean
    protected DraftReviewRequestService draftReviewRequestService;

    @MockitoBean
    protected ReexamineService reexamineService;

    @MockitoBean
    protected ReviewRequestService reviewRequestService;

    @MockitoBean
    protected ReviewService reviewService;

    @MockitoBean
    protected ReviewerService reviewerService;

    @MockitoBean
    protected ReviseService reviseService;

    @MockitoBean
    protected RevisionService revisionService;

    @MockitoBean
    protected InvitationService invitationService;

    @MockitoBean
    protected ParticipantService participantService;

    @MockitoBean
    protected WorkspaceService workspaceService;

    @BeforeEach
    void setUpControllerTest() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @Configuration(proxyBeanMethods = false)
    static class CookieProviderConfiguration {
        @Bean
        RefreshTokenCookieProvider refreshTokenCookieProvider() {
            return new RefreshTokenCookieProvider(java.time.Duration.ofDays(7), false);
        }
    }
}
