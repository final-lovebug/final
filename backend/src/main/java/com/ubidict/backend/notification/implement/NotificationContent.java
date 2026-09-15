package com.ubidict.backend.notification.implement;

/**
 * 알림 패널에 들어갈 두 줄. 제목은 완결된 문장이고 메타는 부연이다.
 */
public record NotificationContent(String title, String message) {}
