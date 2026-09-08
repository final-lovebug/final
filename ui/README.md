# 유비쿼터스 언어 사전 — frontend

Real (non-prototype) implementation of the Claude Design handoff in
`../project/유비쿼터스 언어 사전 UI.dc.html` — a calm enterprise-SaaS UI for a
team glossary tool: AI finds where documents disagree on terminology, humans
review and approve a shared dictionary ("사전집"), and new documents get
checked against it automatically.

Plain HTML/CSS/JS, no build step, no backend — all data in `data.js` is
in-memory mock data. Per the design bundle's `chats/chat1.md`, the flow is a
document review pass, "초안" (draft), and PR-style "개정안" (revision review
with comments / change-request / approve).

## Run it

Any static file server works, e.g.:

```
npx http-server .
# or
python3 -m http.server 8000
```

Then open the printed URL. Opening `index.html` directly via `file://` also
works (no ES modules, no CORS-sensitive fetches).

## Files

- `index.html` — shell: loads Google Fonts, `style.css`, `data.js`, `main.js`.
- `style.css` — design tokens (colors/type/radii extracted from the mockup)
  and component classes (`.card`, `.pill`, `.btn`, `.dtable`, sidebar/topbar,
  comment threads, etc.).
- `data.js` — all mock data (workspaces, documents, members, dictionary
  revisions, terms, glossary candidates, review comments, notifications…).
- `main.js` — a small hand-rolled state/render loop: one `state` object, one
  `render()` that rebuilds `#app` from it, and one delegated `click` listener
  dispatching on `data-action` attributes. No framework.

## Screens implemented

Login → workspace picker → 문서 목록/업로드/상세/버전이력 → 용어 추출 →
사전집 초안(후보 작성, master-detail) → 사전집 → 사전집 리비전 이력·비교 →
문서 검토(치환 제안, with the inline suggestion popover) → 문서 개정안 리뷰
(PR-style) → 사전집 개정안(PR-style) → 설정(멤버/승인 규칙/알림/라벨).

## Notes / judgment calls

- This is frontend-only with mock data, by design (confirmed with the user)
  — nothing here persists, there's no auth or API.
- The source `.dc.html` had a small inconsistency in the 문서 검토 screen: the
  document body markup underlines 7 replaceable terms, but the suggestions
  side-list only showed 6 (missing "요금제 변경 → 플랜 업그레이드"). Fixed
  here by including all 7 in `REVIEW_SUGGESTIONS` and the side list.
- Interactivity is scoped to what the source prototype actually wired up
  (navigation between screens, sidebar accordions, the notification/version
  dropdowns, candidate selection on the 사전집 초안 screen, the review-request
  modal, and resolving a suggestion in 문서 검토). Filters, search boxes, and
  most buttons that were decorative in the original (e.g. "Change request",
  "내보내기", per-row 담당자 지정) are left visually in place but inert, same
  as the design handoff.
