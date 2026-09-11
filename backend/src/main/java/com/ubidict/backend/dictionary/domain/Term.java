package com.ubidict.backend.dictionary.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.dictionary.exception.TermErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사전집에 등재된 표준 용어. 소속 사전집 버전과 함께 얼어붙는다.
 *
 * <p>개별 용어를 고치거나 지우는 경로가 없어 전 필드를 updatable = false로 못 박는다. 공통 감사·삭제 시각 규약을 따르기 위해
 * {@link BaseEntity}를 상속하며, 현재 삭제 유스케이스는 제공하지 않는다.
 *
 * <p>동의어·비권장어는 두지 않는다. 문서 대조는 저장된 표기 목록을 훑는 방식이 아니라 LLM이 문맥을 파악해 표준어·정의와 비교하는 방식이므로, definition이 판단 근거를
 * 대신한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Term extends BaseEntity {

    private static final int FORM_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long dictionaryId;

    @Column(nullable = false, length = FORM_MAX_LENGTH, updatable = false)
    private String preferredForm;

    @Column(length = FORM_MAX_LENGTH, updatable = false)
    private String englishName;

    @Lob
    @Column(nullable = false, updatable = false)
    private String definition;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private Term(Long dictionaryId, String preferredForm, String englishName, String definition, Long createdBy) {
        this.dictionaryId = dictionaryId;
        this.preferredForm = preferredForm;
        this.englishName = englishName;
        this.definition = definition;
        this.createdBy = createdBy;
    }

    public static Term create(
            Long dictionaryId, String preferredForm, String englishName, String definition, Long createdBy) {
        String normalizedForm = normalize(preferredForm);
        String normalizedEnglishName = blankToNull(normalize(englishName));
        validate(normalizedForm, normalizedEnglishName, definition);

        return new Term(dictionaryId, normalizedForm, normalizedEnglishName, definition.strip(), createdBy);
    }

    /**
     * 표기 비교는 앞뒤 공백을 제거한 뒤 대소문자를 구분한다. 공백·하이픈 제거 같은 정규화는 표기 변형 검출(REQ-EXT-009)의 몫이라 여기서 흉내 내지 않는다.
     */
    public static String normalize(String form) {
        return form == null ? null : form.strip();
    }

    /**
     * 선택 항목인 영문명은 빈 문자열과 미입력을 구분할 이유가 없으므로 null로 모은다.
     */
    private static String blankToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static void validate(String preferredForm, String englishName, String definition) {
        if (preferredForm == null || preferredForm.isEmpty() || preferredForm.length() > FORM_MAX_LENGTH) {
            throw new BusinessException(TermErrorCode.TERM_INVALID_PREFERRED_FORM);
        }
        if (englishName != null && englishName.length() > FORM_MAX_LENGTH) {
            throw new BusinessException(TermErrorCode.TERM_INVALID_ENGLISH_NAME);
        }
        if (definition == null || definition.isBlank()) {
            throw new BusinessException(TermErrorCode.TERM_INVALID_DEFINITION);
        }
    }
}
