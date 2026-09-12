package com.ubidict.backend.dictionary.domain;

/**
 * 사전집 버전의 상태.
 *
 * <p>ARCHIVED는 "삭제됨"이 아니라 "지나간 버전"을 뜻한다. 사전집은 삭제하지 않으며, 새 버전이 반영될 때 기존 ACTIVE가 이 상태로 내려간다.
 */
public enum DictionaryStatus {
    ACTIVE,
    ARCHIVED
}
