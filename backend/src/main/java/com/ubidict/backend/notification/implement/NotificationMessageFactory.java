package com.ubidict.backend.notification.implement;

import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import org.springframework.stereotype.Component;

/**
 * 알림 문구를 만든다.
 *
 * <p><b>버튼 문구와 이동 경로는 만들지 않는다</b>(D-46). 그것은 {@code type}·{@code targetType}에서 화면이 파생할 값이고, 저장해 두면
 * 문구를 고칠 때 과거 알림 행까지 손대야 한다.
 *
 * <p>제목의 대상 이름은 리뷰 요청 제목을 그대로 쓴다. 프론트가 요청자 화면에서 지은 제목(「{문서명} 개정 반영」)이 이미 사람이 읽을 문장이다.
 */
@Component
public class NotificationMessageFactory {

    public NotificationContent reviewRequestReceived(String requestTitle, ReviewRequestType type) {
        return new NotificationContent("%s 리뷰를 요청받았습니다".formatted(requestTitle), targetLabel(type) + " 개정안");
    }

    public NotificationContent approved(String requestTitle) {
        return new NotificationContent("%s 리뷰가 승인되었습니다".formatted(requestTitle), "리뷰어가 승인 판정을 남겼습니다");
    }

    public NotificationContent changesRequested(String requestTitle) {
        return new NotificationContent("%s에 변경이 요청되었습니다".formatted(requestTitle), "재교정 후 다시 제출해야 합니다");
    }

    public NotificationContent revised(String requestTitle, ReviewRequestType type, int resultVersionNo) {
        return new NotificationContent(
                "%s 개정안이 반영되었습니다".formatted(requestTitle), "%s r%d 발행".formatted(targetLabel(type), resultVersionNo));
    }

    public NotificationContent canceled(String requestTitle) {
        return new NotificationContent("%s 리뷰 요청이 취소되었습니다".formatted(requestTitle), "더 이상 검토가 진행되지 않습니다");
    }

    /**
     * 대상을 못 찾았을 때 쓸 문구는 두지 않는다 — 그런 경우 알림 자체를 만들지 않는다.
     */
    private static String targetLabel(ReviewRequestType type) {
        return type == ReviewRequestType.DOCUMENT ? "문서" : "사전집";
    }
}
