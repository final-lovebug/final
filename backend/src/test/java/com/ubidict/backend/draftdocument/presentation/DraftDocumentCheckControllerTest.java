package com.ubidict.backend.draftdocument.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import com.ubidict.backend.draftdocument.service.DraftDocumentCheckService;
import com.ubidict.backend.draftdocument.service.model.CheckJobResult;
import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.support.WithLoginMember;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithLoginMember(30L)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(DraftDocumentCheckController.class)
class DraftDocumentCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DraftDocumentCheckService draftDocumentCheckService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("문서 대조를 요청하면 202와 대기 작업을 응답한다.")
    @Test
    void request() {
        given(draftDocumentCheckService.request(any())).willReturn(result());

        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("{\"documentId\":10}")
                .post("/api/draft-documents/checks")
                .then()
                .statusCode(202)
                .body("checkJobId", equalTo(40))
                .body("status", equalTo("PENDING"));
    }

    @DisplayName("문서 대조 작업 상태를 조회하면 200을 응답한다.")
    @Test
    void read() {
        given(draftDocumentCheckService.read(40L, 30L)).willReturn(result());

        RestAssuredMockMvc.given()
                .get("/api/draft-documents/checks/40")
                .then()
                .statusCode(200)
                .body("documentId", equalTo(10));
    }

    private CheckJobResult result() {
        OffsetDateTime now = OffsetDateTime.now();
        return new CheckJobResult(40L, 10L, CheckJobStatus.PENDING, null, null, 30L, now, now);
    }
}
