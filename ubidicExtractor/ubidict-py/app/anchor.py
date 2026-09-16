"""콜백 앵커의 오프셋 단위 변환.

`docs/AI_CONTRACT.md` 6-2 — 대조 콜백의 `anchor`는 **UTF-16 code unit** 기준이다.
백엔드가 `body.substring(startOffset, endOffset)`으로 잘라 `originTerm`과 대조하고,
Java `String`의 인덱스가 UTF-16이기 때문이다.

파이프라인 쪽 오프셋(`app/pipeline/normalize.py`)은 파이썬 문자열 인덱스, 즉
**코드포인트** 기준이다. 한글·영문만 있는 본문에서는 두 값이 같지만, 이모지처럼
BMP 밖 문자가 앞에 하나라도 있으면 그 뒤가 한 칸씩 밀린다 — 어긋난 앵커는 예외
없이 `DRAFT_DOCUMENT_CHECK_INVALID_RESULT`로 거절되고, 그것도 그 한 건만이 아니라
**결과 전체**가 거절된다. 그래서 변환을 한 곳에 모아 둔다.
"""

from __future__ import annotations


def utf16_length(text: str) -> int:
    """`text`의 UTF-16 code unit 길이 — Java `String.length()`와 같은 값이다."""
    return len(text.encode("utf-16-le")) // 2


def anchor_for(body: str, start: int, end: int) -> dict:
    """본문의 코드포인트 구간 `[start, end)`를 콜백 앵커(UTF-16)로 바꾼다."""
    start_offset = utf16_length(body[:start])
    return {"startOffset": start_offset, "endOffset": start_offset + utf16_length(body[start:end])}
