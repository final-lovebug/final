package com.ubidict.backend.document.implement;

import com.ubidict.backend.document.domain.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 본문을 바꾸는 메서드가 없다. 본문은 확정 버전에만 있고, 바뀌는 경로는 대조 → 초안 → 리뷰 → 반영뿐이다.
 */
@Component
@RequiredArgsConstructor
public class DocumentUpdater {

    public void rename(Document document, String title, Long memberId) {
        document.rename(title, memberId);
    }

    public void touch(Document document, Long memberId) {
        document.touch(memberId);
    }
}
