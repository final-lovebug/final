package com.ubidict.backend.common.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.ErrorCode;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Import(GlobalExceptionHandlerTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("BusinessException이 발생하면 에러 코드가 가진 상태와 코드로 응답한다.")
    @Test
    void handleBusinessException() {
        RestAssuredMockMvc.given()
                .when()
                .get("/test/business")
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("code", equalTo("TEST_ALREADY_REGISTERED"))
                .body("message", equalTo("이미 등록된 대상이다."));
    }

    @DisplayName("검증 실패 정보가 없으면 errors 필드를 응답에 포함하지 않는다.")
    @Test
    void handleBusinessException_withoutValidationError() {
        RestAssuredMockMvc.given().when().get("/test/business").then().body("$", not(hasKey("errors")));
    }

    @DisplayName("요청 본문 검증에 실패하면 400과 필드 단위 검증 정보로 응답한다.")
    @Test
    void handleMethodArgumentNotValid() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new TestRequest(" "))
                .when()
                .post("/test/body")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"))
                .body("errors", hasSize(1))
                .body("errors[0].field", equalTo("name"))
                .body("errors[0].message", equalTo("이름은 필수다."));
    }

    @DisplayName("ConstraintViolationException이 발생하면 400과 필드 단위 검증 정보로 응답한다.")
    @Test
    void handleConstraintViolation() {
        RestAssuredMockMvc.given()
                .when()
                .get("/test/constraint")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"))
                .body("errors", hasSize(1))
                .body("errors[0].field", equalTo("name"))
                .body("errors[0].message", equalTo("이름은 필수다."));
    }

    @DisplayName("요청 파라미터 검증에 실패하면 400과 파라미터 단위 검증 정보로 응답한다.")
    @Test
    void handleHandlerMethodValidation() {
        RestAssuredMockMvc.given()
                .queryParam("name", " ")
                .when()
                .get("/test/parameter")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"))
                .body("errors", hasSize(1))
                .body("errors[0].field", equalTo("name"));
    }

    @DisplayName("인증에 실패하면 401로 응답한다.")
    @Test
    void handleAuthentication() {
        RestAssuredMockMvc.given()
                .when()
                .get("/test/authentication")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("code", equalTo("COMMON_UNAUTHORIZED"));
    }

    @DisplayName("권한이 부족하면 403으로 응답한다.")
    @Test
    void handleAccessDenied() {
        RestAssuredMockMvc.given()
                .when()
                .get("/test/access-denied")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", equalTo("COMMON_ACCESS_DENIED"));
    }

    @DisplayName("읽을 수 없는 요청 본문이면 400으로 응답한다.")
    @Test
    void handleMalformedRequest() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("{")
                .when()
                .post("/test/body")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @DisplayName("지원하지 않는 HTTP 메서드로 요청하면 405로 응답한다.")
    @Test
    void handleMethodNotSupported() {
        RestAssuredMockMvc.given()
                .when()
                .post("/test/business")
                .then()
                .statusCode(HttpStatus.METHOD_NOT_ALLOWED.value())
                .body("code", equalTo("COMMON_METHOD_NOT_ALLOWED"));
    }

    @DisplayName("예상하지 못한 예외가 발생하면 500으로 응답하고 내부 정보를 노출하지 않는다.")
    @Test
    void handleException() {
        RestAssuredMockMvc.given()
                .when()
                .get("/test/unexpected")
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("code", equalTo("COMMON_INTERNAL_ERROR"))
                .body("message", equalTo("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."));
    }

    @RequiredArgsConstructor
    enum TestErrorCode implements ErrorCode {
        TEST_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 등록된 대상이다.");

        private final HttpStatus status;
        private final String message;

        @Override
        public HttpStatus status() {
            return status;
        }

        @Override
        public String message() {
            return message;
        }
    }

    record TestRequest(@NotBlank(message = "이름은 필수다.") String name) {}

    @RequestMapping("/test")
    @RestController
    static class TestController {

        @GetMapping("/business")
        void business() {
            throw new BusinessException(TestErrorCode.TEST_ALREADY_REGISTERED);
        }

        @PostMapping("/body")
        void body(@Valid @RequestBody TestRequest request) {}

        @GetMapping("/parameter")
        void parameter(@NotBlank @RequestParam String name) {}

        @GetMapping("/constraint")
        void constraint() {
            try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
                Validator validator = factory.getValidator();
                Set<ConstraintViolation<TestRequest>> violations = validator.validate(new TestRequest(" "));

                throw new ConstraintViolationException(violations);
            }
        }

        @GetMapping("/authentication")
        void authentication() {
            throw new BadCredentialsException("invalid credentials");
        }

        @GetMapping("/access-denied")
        void accessDenied() {
            throw new AccessDeniedException("no authority");
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("internal detail");
        }
    }
}
