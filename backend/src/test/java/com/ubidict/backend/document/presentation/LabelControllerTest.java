package com.ubidict.backend.document.presentation;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.document.service.DocumentService;
import com.ubidict.backend.document.service.model.LabelResult;
import com.ubidict.backend.member.infra.security.JwtProvider;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(LabelController.class)
class LabelControllerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long WORKSPACE_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    /**
     * addFilters=false로 Security 필터 체인은 우회하지만 SecurityConfig가 이 슬라이스에 함께
     * 로드되므로, JwtAuthenticationFilter가 요구하는 JwtProvider를 mock으로 채워 컨텍스트를 띄운다.
     */
    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("워크스페이스의 라벨 목록은 200을 응답한다.")
    @Test
    void readAll() {
        // given
        given(documentService.readLabels(WORKSPACE_ID, MEMBER_ID))
                .willReturn(List.of(new LabelResult(1L, "결제"), new LabelResult(2L, "설계")));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/workspaces/{workspaceId}/labels?memberId={memberId}", WORKSPACE_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("", hasSize(2))
                .body("name", contains("결제", "설계"));
    }
}
