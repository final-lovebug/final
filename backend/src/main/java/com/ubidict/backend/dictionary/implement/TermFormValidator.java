package com.ubidict.backend.dictionary.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.dictionary.domain.NewTerm;
import com.ubidict.backend.dictionary.exception.TermErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 한 반영 요청 안의 표준어 중복을 저장 전에 잡는다(NFR-DIC-002).
 *
 * <p>uk_term_dictionary_preferred_form이 최후 방어선이지만, DB 제약에 닿으면 DataIntegrityViolationException이 되어 사용자에게
 * 줄 메시지를 만들 수 없다.
 */
@Component
public class TermFormValidator {

    public void validateUnique(List<NewTerm> newTerms) {
        Set<String> seen = new HashSet<>();
        for (NewTerm newTerm : newTerms) {
            String preferredForm = newTerm.preferredForm();
            if (isBlank(preferredForm)) {
                continue;
            }
            if (!seen.add(preferredForm)) {
                throw new BusinessException(TermErrorCode.TERM_DUPLICATE_PREFERRED_FORM);
            }
        }
    }

    /**
     * 비어 있는 표준어는 중복이 아니라 값 자체가 잘못된 것이므로 여기서 걸러 Term이 제 이유로 실패하게 둔다.
     */
    private static boolean isBlank(String preferredForm) {
        return preferredForm == null || preferredForm.isEmpty();
    }
}
