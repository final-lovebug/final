package com.ubidict.backend.draftdictionary.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.draftdictionary.domain.ExtractionJobStatus;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryExtractionService;
import com.ubidict.backend.draftdictionary.service.model.ExtractionJobResult;
import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.support.WithLoginMember;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithLoginMember(2L)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(DraftDictionaryExtractionController.class)
class DraftDictionaryExtractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DraftDictionaryExtractionService extractionService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("용어 추출을 요청하면 202와 대기 작업을 응답한다.")
    @Test
    void request() {
        given(extractionService.request(any())).willReturn(result());

        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"workspaceId":1,"sourceDocumentIds":[10,20]}
                        """)
                .post("/api/draft-dictionaries/extractions")
                .then()
                .statusCode(202)
                .body("extractionJobId", equalTo(30))
                .body("status", equalTo("PENDING"));
    }

    @DisplayName("용어 추출 작업 상태를 조회하면 200을 응답한다.")
    @Test
    void read() {
        given(extractionService.read(30L, 2L)).willReturn(result());

        RestAssuredMockMvc.given()
                .get("/api/draft-dictionaries/extractions/30")
                .then()
                .statusCode(200)
                .body("sourceDocumentIds[0]", equalTo(10));
    }

    private ExtractionJobResult result() {
        OffsetDateTime now = OffsetDateTime.now();
        return new ExtractionJobResult(
                30L, 1L, null, List.of(10L, 20L), ExtractionJobStatus.PENDING, null, null, 2L, now, now);
    }
}
