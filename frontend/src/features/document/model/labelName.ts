// 라벨명은 대소문자를 구분하지 않는다(docs/plan/CONFLICTS.md `D-94`).
//
// 서버의 `Label.matchKey()`와 같은 기준이다 — 앞뒤 공백을 떼고 NFKC로 정규화한 뒤 소문자로 내린다.
// DB collation(`utf8mb4_0900_ai_ci`)이 최종 판정자라 악센트까지는 못 맞추지만, 화면이 서버와
// 다르게 판단해 「칩은 2개인데 응답은 1개」 같은 거짓말을 하지 않게 하는 것이 목적이다.

export function labelMatchKey(name: string): string {
  return name.trim().normalize('NFKC').toLowerCase()
}

export function isSameLabelName(a: string, b: string): boolean {
  return labelMatchKey(a) === labelMatchKey(b)
}

/** 이미 있는 표기를 찾아 돌려준다. 표시명은 최초 생성 시 입력값을 유지하므로 사용자가 새로 친 표기가 아니라 이쪽이 이긴다. */
export function resolveExistingLabelName(
  name: string,
  knownNames: readonly string[],
): string | undefined {
  return knownNames.find((known) => isSameLabelName(known, name))
}
