// 유비쿼터스 언어 사전 — vanilla JS app shell.
// No framework, no build step: a single state object + full re-render on every
// state change, with one delegated click listener. Good enough for a mock-data
// clickable frontend of this size; would move to a real view layer if this grew
// server-backed state or per-keystroke inputs.

const state = {
  screen: 'login', // login | workspaces | docs | upload | extract | draft | dictionary
                    // | reviewDoc | docDetail | docHistory | dictHistory | reviewThread
                    // | revision | settings
  settingsTab: 'members', // members | ruleset | notif | labels
  notifOpen: false,
  versionMenuOpen: false,
  docsGroupOpen: true,
  dictGroupOpen: true,

  selectedCandidateId: 1,
  standardMode: 'word', // word | direct
  standardWordIdx: 0,

  reviewModalOpen: false,
  reviewerNames: ['김개발', '이백엔드', '박기획', '최마케팅'],

  // 07 문서 검토 — mutable copy so applying/ignoring a suggestion can update it.
  suggestions: REVIEW_SUGGESTIONS.map((s) => ({ ...s })),
  activeSuggestionId: null,
  reviewSideTab: 'suggestions', // suggestions | history
};

const SCREEN_TITLES = {
  docs: '문서', upload: '문서', extract: '사전집 초안', draft: '사전집 초안',
  dictionary: '사전집', reviewDoc: '문서 검토', docDetail: '문서', docHistory: '문서 버전 이력',
  dictHistory: '사전집 리비전 이력', reviewThread: '문서 개정안 리뷰',
  revision: '사전집 개정안', settings: '설정',
};

const DOC_GROUP_SCREENS = ['docs', 'upload', 'docDetail', 'docHistory'];
const DICT_GROUP_SCREENS = ['dictionary', 'extract', 'dictHistory'];

/* ---------------------------------------------------------------------------
   Small render helpers
   ------------------------------------------------------------------------- */

function pill(tone, text, extraClass) {
  return `<span class="pill ${extraClass || ''} tone-${tone}">${text}</span>`;
}

function pillLg(tone, text) {
  return `<span class="pill pill-lg tone-${tone}">${text}</span>`;
}

function docBadge(doc) {
  if (doc.badge === 'danger') return pill('danger', '재검사 필요');
  if (doc.badge === 'warn') return pill('neutral', '뒤처짐');
  return '';
}

function avatar(cls, initial, size) {
  const s = size ? `style="width:${size}px;height:${size}px;font-size:${Math.round(size * 0.42)}px"` : '';
  return `<div class="avatar ${cls}" ${s}>${initial || ''}</div>`;
}

function checkbox(checked) {
  return checked
    ? `<div class="checkbox checked">✓</div>`
    : `<div class="checkbox"></div>`;
}

function radioDot(checked) {
  return `<div class="radio-dot ${checked ? 'checked' : ''}"></div>`;
}

/* ---------------------------------------------------------------------------
   Screen: 08 로그인
   ------------------------------------------------------------------------- */

function renderLogin() {
  return `
    <div class="screen-center">
      <div class="login-card">
        <div class="logo-mark" style="width:44px;height:44px;border-radius:11px;font-size:18px;">U</div>
        <div>
          <div class="display-font" style="font-size:20px;margin-bottom:6px;">유비쿼터스</div>
          <div style="font-size:13.5px;color:var(--text-tertiary);">팀이 쓰는 말을 하나로 맞춥니다</div>
        </div>
        <div style="display:flex;flex-direction:column;gap:10px;width:100%;margin-top:6px;">
          <div class="social-btn" data-action="go" data-screen="workspaces">
            <span style="width:16px;height:16px;border-radius:4px;background:#4285F4;display:inline-block;"></span>
            Google로 계속하기
          </div>
        </div>
        <div style="font-size:11.5px;color:var(--text-quaternary);">처음이시면 자동으로 가입됩니다</div>
        <div style="font-size:11px;color:var(--text-faint);margin-top:8px;display:flex;gap:10px;">
          <span style="text-decoration:underline;cursor:pointer;">이용약관</span>
          <span style="text-decoration:underline;cursor:pointer;">개인정보처리방침</span>
        </div>
      </div>
    </div>`;
}

/* ---------------------------------------------------------------------------
   Screen: 09 워크스페이스 목록
   ------------------------------------------------------------------------- */

function renderWorkspaces() {
  const cards = WORKSPACES.map((w) => `
    <div class="workspace-card ${w.clickable ? 'clickable' : ''}"
         ${w.clickable ? `data-action="go" data-screen="docs"` : ''}>
      <div class="display-font" style="font-size:15px;">${w.name}</div>
      ${pill(w.dictBadgeTone, w.dictBadge)}
      <div class="wc-stats"><span>멤버 ${w.members}</span><span>문서 ${w.docs}</span></div>
      <div style="font-size:11px;color:var(--text-quaternary);">${w.activity}</div>
    </div>`).join('');

  return `
    <div style="background:var(--bg);min-height:100vh;">
      <div class="workspaces-topbar">
        <div class="display-font" style="font-size:16px;display:flex;align-items:center;gap:8px;">
          ${avatar('logo-mark', 'U', 26)}유비쿼터스
        </div>
        <div style="width:32px;height:32px;border-radius:50%;background:var(--border-strong);"></div>
      </div>
      <div class="workspaces-body">
        <div class="display-font" style="font-size:26px;margin-bottom:28px;">워크스페이스를 선택하세요</div>
        <div class="workspace-grid">
          ${cards}
          <div class="workspace-card-new">
            <div style="font-size:22px;">+</div>
            <div style="font-size:12.5px;font-weight:600;">새 워크스페이스 만들기</div>
          </div>
        </div>
      </div>
    </div>`;
}

/* ---------------------------------------------------------------------------
   App shell: sidebar + topbar + content router
   ------------------------------------------------------------------------- */

function sidebarItem(label, screen, active, extraHtml) {
  return `<div class="sidebar-item ${active ? 'active' : ''}" data-action="go" data-screen="${screen}">${label}${extraHtml || ''}</div>`;
}

function renderSidebar() {
  const s = state.screen;
  const docsGroupActive = DOC_GROUP_SCREENS.includes(s);
  const dictGroupActive = DICT_GROUP_SCREENS.includes(s);

  return `
    <div class="sidebar">
      <div class="sidebar-header">
        <div style="display:flex;align-items:center;gap:8px;">
          ${avatar('logo-mark', 'P', 24)}
          <span class="display-font" style="font-size:13.5px;">POTENUP_BE</span>
        </div>
        <span style="font-size:11px;color:var(--text-quaternary);">▾</span>
      </div>
      <div class="sidebar-nav">
        <div>
          <div class="sidebar-group-header" data-action="toggle-docs-group">
            <span class="sidebar-group-title">문서</span>
            <span class="sidebar-chevron ${state.docsGroupOpen ? '' : 'closed'}">▾</span>
          </div>
          ${state.docsGroupOpen ? `
            ${sidebarItem('문서', 'docs', docsGroupActive)}
            ${sidebarItem('초안', 'reviewDoc', s === 'reviewDoc')}
            ${sidebarItem('개정안', 'reviewThread', s === 'reviewThread')}
          ` : ''}
        </div>
        <div>
          <div class="sidebar-group-header" data-action="toggle-dict-group">
            <span class="sidebar-group-title">사전집</span>
            <span class="sidebar-chevron ${state.dictGroupOpen ? '' : 'closed'}">▾</span>
          </div>
          ${state.dictGroupOpen ? `
            ${sidebarItem('사전집', 'dictionary', dictGroupActive)}
            ${sidebarItem('초안', 'draft', s === 'draft')}
            ${sidebarItem('개정안', 'revision', s === 'revision')}
          ` : ''}
        </div>
        <div class="sidebar-settings">
          ${sidebarItem('설정', 'settings', s === 'settings', '<span class="badge-hint">ADMIN+</span>')}
        </div>
      </div>
      <div class="sidebar-footer">
        ${avatar('avatar-accent', '민', 30)}
        <div style="font-size:12.5px;font-weight:600;">민뱅</div>
        <div style="margin-left:auto;font-size:10px;font-weight:600;color:var(--accent-strong);background:var(--accent-bg);padding:2px 7px;border-radius:5px;">OWNER</div>
      </div>
    </div>`;
}

function renderNotifPanel() {
  if (!state.notifOpen) return '';
  const items = NOTIFICATIONS.map((n) => `
    <div class="notif-item ${n.read ? 'read' : ''}" data-action="go" data-screen="${n.go}">
      <div class="notif-dot"></div>
      <div style="flex:1;">
        <div class="notif-title">${n.title}</div>
        <div class="notif-meta">${n.meta}</div>
        <div class="notif-action">${n.action} →</div>
      </div>
    </div>`).join('');

  return `
    <div class="notif-panel">
      <div class="notif-panel-header">
        <div style="display:flex;align-items:center;gap:8px;">
          <span class="display-font" style="font-size:14px;">알림</span>
          ${pill('neutral', '인앱')}
        </div>
        <div style="font-size:11.5px;color:var(--accent-strong);font-weight:600;cursor:pointer;">모두 읽음</div>
      </div>
      <div>${items}</div>
      <div class="notif-panel-footer">용어 추가만 있는 리비전은 알림을 보내지 않습니다 · <span style="text-decoration:underline;cursor:pointer;">알림 설정</span></div>
    </div>`;
}

function renderTopbar() {
  return `
    <div class="topbar">
      <div class="topbar-title">${SCREEN_TITLES[state.screen] || '문서'}</div>
      <div class="topbar-right">
        ${pill('accent', '사전집 r7')}
        <div class="avatar-stack">
          ${avatar('avatar-accent')}${avatar('avatar-warn')}${avatar('avatar-success')}${avatar('avatar-danger')}${avatar('avatar-neutral')}
        </div>
        <div class="bell-btn" data-action="toggle-notif">
          <span class="bell-icon"></span>
          <span class="bell-dot"></span>
        </div>
      </div>
      ${renderNotifPanel()}
    </div>`;
}

const CONTENT_RENDERERS = {
  docs: renderDocsScreen,
  upload: renderUploadScreen,
  extract: renderExtractScreen,
  draft: renderDraftScreen,
  dictionary: renderDictionaryScreen,
  reviewDoc: renderReviewDocScreen,
  docDetail: renderDocDetailScreen,
  docHistory: renderDocHistoryScreen,
  dictHistory: renderDictHistoryScreen,
  reviewThread: renderReviewThreadScreen,
  revision: renderRevisionScreen,
  settings: renderSettingsScreen,
};

function renderAppShell() {
  const contentFn = CONTENT_RENDERERS[state.screen] || renderDocsScreen;
  return `
    <div class="app-shell">
      ${renderSidebar()}
      <div class="main-col">
        ${renderTopbar()}
        <div class="content">${contentFn()}</div>
      </div>
    </div>`;
}

/* ---------------------------------------------------------------------------
   Screen: 01 문서 목록
   ------------------------------------------------------------------------- */

function renderDocsScreen() {
  const rows = DOCUMENTS.map((doc) => `
    <tr class="clickable" data-action="go" data-screen="docDetail">
      <td>
        <div style="font-weight:700;color:var(--text);">${doc.name}</div>
        <div style="font-size:11.5px;color:var(--text-quaternary);margin-top:2px;">${doc.desc}</div>
      </td>
      <td><div style="display:inline-flex;gap:6px;align-items:center;">
        <span class="pill-outline pill" style="border-radius:6px;">${doc.ver}</span>
        ${docBadge(doc)}
      </div></td>
      <td><span class="pill" style="border:1px solid var(--border-strong);border-radius:12px;color:var(--text-secondary);">${doc.label}</span></td>
      <td>${doc.author}</td>
      <td>${doc.editor}</td>
      <td style="color:var(--text-quaternary);">${doc.modified}</td>
    </tr>`).join('');

  return `
    <div class="toolbar">
      <div class="segmented"><div class="active">목록</div><div>카드</div></div>
      <div class="filter-chip">라벨 전체 ▾</div>
      <div class="filter-chip filter-search">문서 검색</div>
      <div class="spacer"></div>
      <div class="btn btn-outline" data-action="go" data-screen="upload">문서 업로드</div>
      <div class="btn btn-primary" data-action="go" data-screen="extract">용어 추출 실행</div>
    </div>
    <div class="sync-line">sync · 마지막 갱신 10분 전</div>
    <div class="card" style="overflow:hidden;">
      <table class="dtable">
        <tr>
          <th>문서명</th><th>사전집 버전</th><th>라벨</th><th>작성자</th><th>최종 수정자</th><th>수정일시</th>
        </tr>
        ${rows}
      </table>
    </div>`;
}

/* ---------------------------------------------------------------------------
   Screen: 02 문서 업로드
   ------------------------------------------------------------------------- */

function renderUploadScreen() {
  return `
    <div style="max-width:640px;">
      <div class="screen-title">문서 업로드</div>
      <div style="border:1.5px dashed var(--text-disabled);border-radius:var(--r-md);padding:40px;text-align:center;background:var(--surface);margin-bottom:22px;">
        <div style="font-size:13.5px;color:var(--text-secondary);font-weight:600;margin-bottom:6px;">파일을 끌어다 놓거나 클릭해 선택하세요</div>
        <div style="font-size:11.5px;color:var(--text-quaternary);">.md, .txt · 최대 10MB</div>
      </div>
      <div class="input-box" style="margin-bottom:22px;">회원 도메인 설계 노트.md</div>
      <div style="display:flex;flex-direction:column;gap:16px;">
        <div>
          <div class="field-label">라벨 <span class="req-mark">*</span></div>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <div style="border:1.5px solid var(--accent);color:var(--accent-strong);background:var(--accent-bg);border-radius:16px;padding:6px 14px;font-size:12.5px;font-weight:600;">도메인</div>
            <div style="border:1px solid var(--border-strong);border-radius:16px;padding:6px 14px;font-size:12.5px;color:var(--text-tertiary);">정책</div>
            <div style="border:1px solid var(--border-strong);border-radius:16px;padding:6px 14px;font-size:12.5px;color:var(--text-tertiary);">결제</div>
          </div>
        </div>
        <div>
          <div class="field-label">문서명</div>
          <div class="input-box">회원 도메인 설계 노트</div>
        </div>
      </div>
      <div style="display:flex;gap:10px;justify-content:flex-end;margin-top:26px;">
        <div class="btn btn-outline" data-action="go" data-screen="docs">취소</div>
        <div class="btn btn-primary" data-action="go" data-screen="docs">업로드</div>
      </div>
    </div>`;
}

/* ---------------------------------------------------------------------------
   Screen: 03 용어 추출 실행 (최종본 일괄 추출) — lives under 사전집
   ------------------------------------------------------------------------- */

function renderExtractScreen() {
  const finals = [
    { name: '기획(Planning) 정책 정의서', eligible: false, reason: 'AI 검토, 최종본 아님' },
    { name: '구독 상품 요금제 개편안', eligible: true },
    { name: '2026 상반기 캠페인 실행안', eligible: false, reason: '초안 단계' },
    { name: '리텐션 지표 정의서', eligible: true },
    { name: '결제 API 명세 v2', eligible: false, reason: '리뷰 단계, 최종본 아님' },
    { name: '회원 도메인 설계 노트', eligible: false, reason: '임시 단계' },
  ];
  const rows = finals.map((f) => `
    <div style="display:flex;align-items:center;gap:9px;font-size:13px;color:${f.eligible ? 'var(--text)' : 'var(--text-faint)'};${f.eligible ? 'font-weight:500;' : ''}">
      ${f.eligible ? checkbox(true) : `<div class="checkbox"></div>`}
      ${f.name}
      ${f.reason ? `<span style="font-size:10.5px;margin-left:6px;color:var(--text-faint);">— ${f.reason}</span>` : ''}
    </div>`).join('');
  const selectedCount = finals.filter((f) => f.eligible).length;

  return `
    <div style="max-width:560px;">
      <div class="screen-title">후보 작성용 용어 추출</div>
      <div class="screen-subtitle">최신 사전집(r7)까지 갱신·대조를 마치고 최종본으로 확정된 문서에서 후보를 뽑습니다.</div>
      <div class="card" style="padding:20px;">
        <div style="font-size:13px;font-weight:700;margin-bottom:12px;">최종본 ${selectedCount}건 중 ${selectedCount}건 선택됨</div>
        <div style="display:flex;flex-direction:column;gap:9px;">${rows}</div>
        <div style="display:flex;align-items:center;justify-content:space-between;margin-top:20px;padding-top:16px;border-top:1px solid var(--border-soft);">
          <div style="font-size:12px;color:var(--text-quaternary);">예상 소요 약 2~3분 · ADMIN 이상만 실행 가능</div>
          <div class="btn btn-primary" data-action="go" data-screen="draft">추출 실행</div>
        </div>
      </div>
    </div>`;
}

/* ---------------------------------------------------------------------------
   Screen: 04 사전집 초안 — 후보 작성 (master-detail)
   ------------------------------------------------------------------------- */

function getSelectedCandidate() {
  return CANDIDATES.find((c) => c.id === state.selectedCandidateId) || CANDIDATES[0];
}

function renderDraftScreen() {
  const rows = CANDIDATES.map((c) => `
    <tr class="clickable" data-action="select-candidate" data-id="${c.id}"
        style="${c.id === state.selectedCandidateId ? 'background:var(--accent-bg-strong);box-shadow:inset 3px 0 0 var(--accent);' : ''}">
      <td style="width:14px;">${checkbox(false)}</td>
      <td style="font-weight:600;color:var(--text);">${c.words.join(', ')}</td>
      <td>${c.type}</td>
      <td>${c.occurrences} / ${c.docCount}</td>
      <td>${c.owner}</td>
    </tr>`).join('');

  const candidate = getSelectedCandidate();
  const wordOptions = candidate.words.map((w, idx) => {
    const active = state.standardMode === 'word' && state.standardWordIdx === idx;
    return `
      <div data-action="select-standard-word" data-idx="${idx}" style="display:flex;align-items:center;gap:8px;font-size:13px;cursor:pointer;color:${active ? 'var(--text)' : 'var(--text-secondary)'};">
        ${radioDot(active)}${w}
      </div>`;
  }).join('');
  const directActive = state.standardMode === 'direct';

  const quotes = candidate.quotes.map((q) => `
    <div class="quote-block ${q.split ? 'split' : ''}">
      <div class="quote-text">"${q.text}"</div>
      <div class="quote-source">${q.source}</div>
    </div>`).join('') + (candidate.splitNote ? `<div style="font-size:11px;color:var(--text-tertiary);margin-top:2px;">${candidate.splitNote}</div>` : '');

  return `
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:4px;">
      <div class="screen-title" style="margin-bottom:0;">사전집 초안 — 후보 작성</div>
      <div class="btn btn-primary btn-sm" data-action="go" data-screen="revision">개정안 제출</div>
    </div>
    <div style="font-size:12px;color:var(--text-quaternary);margin-bottom:18px;">담당자를 지정해 표준어와 정의를 채우면, 개정안 제출 후 다른 멤버가 사전집 개정안에서 승인합니다.</div>
    <div class="toolbar">
      <div class="filter-chip">유형 전체 ▾</div>
      <div class="filter-chip">담당자 전체 ▾</div>
      <div class="filter-chip filter-search" style="width:160px;">검색</div>
      <div class="spacer"></div>
      <div style="font-size:11px;color:var(--text-quaternary);">추출 실행 · 09-03 14:20 · 후보 41건</div>
    </div>
    <div class="two-col">
      <div class="col-flex card" style="overflow:hidden;">
        <table class="dtable">
          <tr><th style="width:14px;"></th><th>후보 단어</th><th>유형</th><th>출현/문서</th><th>작성자</th></tr>
          ${rows}
        </table>
        <div style="display:flex;align-items:center;justify-content:space-between;padding:12px 16px;border-top:1px solid var(--border-soft);">
          <div style="font-size:11px;color:var(--text-quaternary);">↑↓ 키로 후보를 이동하면 오른쪽 패널이 따라옵니다</div>
          <div class="btn btn-outline btn-sm">선택 항목 일괄 담당자 지정</div>
        </div>
      </div>
      <div class="detail-panel" style="width:420px;">
        <div style="display:flex;align-items:baseline;justify-content:space-between;">
          <div style="display:flex;align-items:center;gap:8px;"><span class="display-font" style="font-size:16px;">${candidate.title}</span>${pill('neutral', candidate.type)}</div>
          <span style="font-size:11px;color:var(--text-quaternary);">출현 ${candidate.occurrences}회 · 문서 ${candidate.docCount}건</span>
        </div>
        <div>
          <div class="field-label">표준어</div>
          <div style="display:flex;flex-direction:column;gap:7px;">
            ${wordOptions}
            <div data-action="select-standard-direct" style="display:flex;align-items:center;gap:8px;font-size:13px;color:${directActive ? 'var(--text)' : 'var(--text-secondary)'};cursor:pointer;">
              ${radioDot(directActive)}직접 입력<div style="flex:1;border:1px solid var(--border-strong);border-radius:6px;height:28px;background:var(--surface-muted);"></div>
            </div>
          </div>
        </div>
        <div>
          <div class="field-label">정의</div>
          <div style="border:1px solid var(--border-strong);border-radius:var(--r-sm);padding:10px 12px;font-size:12.5px;color:var(--text-secondary);line-height:1.6;background:var(--surface-muted);min-height:64px;">${candidate.def}</div>
          <div style="font-size:10.5px;color:var(--text-quaternary);margin-top:5px;">AI 초안 · 수정할 수 있습니다</div>
        </div>
        <div>
          <div class="field-label">근거 문장</div>
          <div style="display:flex;flex-direction:column;gap:8px;">${quotes}</div>
        </div>
        <div style="border-top:1px solid var(--border-soft);padding-top:14px;display:flex;flex-direction:column;gap:9px;">
          <div style="display:flex;align-items:center;gap:10px;">
            <div class="field-label" style="margin-bottom:0;">작성 담당자</div>
            <div class="input-box" style="padding:6px 10px;font-size:12px;">${candidate.owner} ▾</div>
          </div>
          <div style="display:flex;gap:8px;">
            <div class="btn btn-primary btn-sm">작성 완료로 표시</div>
            <div class="btn btn-outline btn-sm">보류</div>
          </div>
        </div>
      </div>
    </div>`;
}

/* ---------------------------------------------------------------------------
   Screen: 06 사전집
   ------------------------------------------------------------------------- */

function renderDictionaryScreen() {
  const versionMenu = state.versionMenuOpen ? `
    <div style="position:absolute;top:38px;left:0;width:180px;background:var(--surface);border:1px solid var(--border);border-radius:10px;box-shadow:var(--shadow-pop);font-size:12.5px;z-index:10;overflow:hidden;">
      <div style="padding:9px 13px;background:var(--accent-bg);color:var(--accent-strong);font-weight:700;">r7</div>
      <div style="padding:9px 13px;border-top:1px solid var(--border-soft);color:var(--text-tertiary);">r6</div>
      <div style="padding:9px 13px;border-top:1px solid var(--border-soft);color:var(--text-tertiary);">r5</div>
      <div style="padding:9px 13px;border-top:1px solid var(--border-soft);color:var(--text-tertiary);">r4</div>
      <div data-action="go" data-screen="dictHistory" style="padding:9px 13px;border-top:1px solid var(--border-strong);color:var(--accent-strong);font-weight:600;cursor:pointer;">all</div>
    </div>` : '';

  const rows = TERMS.map((t) => `
    <tr style="${t.name === TERM_IN_FOCUS ? 'background:var(--accent-bg-strong);' : ''}">
      <td style="font-weight:700;color:var(--text);">${t.name}</td>
      <td>${t.def}</td>
      <td style="color:var(--text-quaternary);">${t.modified}</td>
    </tr>`).join('');

  return `
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:6px;position:relative;">
      <div class="btn btn-outline btn-sm" data-action="toggle-version-menu">r7 ▾</div>
      ${pill('success', '공식')}
      ${versionMenu}
    </div>
    <div style="font-size:11.5px;color:var(--text-quaternary);margin-bottom:14px;">공식 리비전 · 대조 결과가 문서 검사에 그대로 적용됩니다</div>
    <div class="banner banner-accent" style="margin-bottom:8px;display:flex;justify-content:space-between;align-items:center;">
      <span>사전집 개정안에서 승인 대기 중인 변경 8건 — 용어 추가 5 · 정의 수정 3</span>
      <span class="btn-link" data-action="go" data-screen="revision">개정안 보기 →</span>
    </div>
    <div style="font-size:11.5px;color:var(--text-quaternary);margin-bottom:18px;">승인이 완료되면 리비전이 자동으로 발행됩니다.</div>
    <div class="two-col">
      <div class="col-flex card" style="overflow:hidden;">
        <table class="dtable">
          <tr><th>통일 용어</th><th>정의</th><th>최종 수정</th></tr>
          ${rows}
        </table>
      </div>
      <div style="width:280px;flex-shrink:0;background:var(--surface-muted);border:1px solid var(--border-soft);border-radius:var(--r-md);padding:16px;opacity:.6;">
        <div style="display:flex;align-items:center;gap:6px;margin-bottom:6px;">
          <span style="font-size:12.5px;font-weight:700;">역인덱스 · 변경 이력</span>
          ${pill('neutral', 'MVP2')}
        </div>
        <div style="font-size:11.5px;color:var(--text-tertiary);line-height:1.6;">이 용어가 쓰인 문서 목록과 변경 이력 타임라인은 추후 제공됩니다. 지금은 사전집 리비전 이력에서 확인할 수 있습니다.</div>
      </div>
    </div>
    <div style="display:flex;gap:10px;margin-top:16px;">
      <div class="btn btn-outline" data-action="go" data-screen="draft">직접 후보 등록</div>
      <div class="btn btn-outline">내보내기</div>
    </div>`;
}

/* ---------------------------------------------------------------------------
   Screen: 07 문서 검토 — 치환 제안
   ------------------------------------------------------------------------- */

function suggestionSpan(s) {
  if (s.status === 'resolved') {
    const title = `원래: ${s.original} · 근거: ${s.suggestion}`;
    return `<span class="term-resolved" title="${title}">${s.original}</span>`;
  }
  const isOpen = state.activeSuggestionId === s.id;
  return `<span class="term-pending" data-action="open-suggestion" data-id="${s.id}" style="${isOpen ? 'background:var(--accent-bg);' : ''}">${s.original}</span>`;
}

function renderReviewModal() {
  if (!state.reviewModalOpen) return '';
  const chips = state.reviewerNames.map((name) => `
    <div class="reviewer-chip">${name}<span class="x" data-action="remove-reviewer" data-name="${name}">×</span></div>`).join('');

  return `
    <div class="modal-overlay">
      <div class="modal-box">
        <div class="display-font" style="font-size:17px;">리뷰 요청 · 리뷰어 등록</div>
        <div>
          <div class="field-label">리뷰 타이틀</div>
          <div class="input-box">이용자→구독자 등 치환 12건 반영</div>
        </div>
        <div>
          <div class="field-label">리뷰어 <span style="color:var(--text-quaternary);font-weight:500;">워크스페이스 멤버가 기본으로 모두 포함됩니다</span></div>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">${chips}</div>
        </div>
        <div class="card" style="padding:12px 14px;font-size:12px;color:var(--text-tertiary);line-height:1.6;">처리한 치환 12건이 리뷰 대상입니다. 승인 규칙에 따라 리뷰어 2명의 승인이 필요합니다.</div>
        <div style="display:flex;gap:10px;justify-content:flex-end;">
          <div class="btn btn-outline" data-action="toggle-review-modal">취소</div>
          <div class="btn btn-primary" data-action="submit-review-request">리뷰 요청</div>
        </div>
      </div>
    </div>`;
}

function renderSuggestionPopover(s) {
  return `
    <div class="suggestion-popover" style="margin-top:8px;">
      <div class="sp-title">${s.original} → ${s.suggestion}</div>
      <div class="sp-actions">
        <div class="btn btn-primary" data-action="resolve-suggestion" data-id="${s.id}" data-mode="적용">적용</div>
        <div class="btn btn-outline" data-action="resolve-suggestion" data-id="${s.id}" data-mode="무시">무시</div>
      </div>
      <div class="sp-note">직접 입력</div>
      <div class="sp-input"></div>
      <div class="sp-checkline">${checkbox(false)}이 표현을 새 용어로 등록 요청</div>
      <div class="sp-note">무시 사유 (선택)</div>
      <div class="sp-input"></div>
    </div>`;
}

function renderReviewDocScreen() {
  const s = state.suggestions;
  const byId = Object.fromEntries(s.map((x) => [x.id, x]));
  const activeSuggestion = state.activeSuggestionId ? byId[state.activeSuggestionId] : null;

  const resolvedCount = s.filter((x) => x.status === 'resolved').length;

  const suggestionList = s.map((x) => `
    <div style="border:1.5px solid ${x.id === state.activeSuggestionId ? 'var(--accent)' : 'var(--border-strong)'};background:${x.id === state.activeSuggestionId ? 'var(--accent-bg-strong)' : 'transparent'};border-radius:7px;padding:8px 10px;color:${x.status === 'resolved' ? 'var(--text-quaternary)' : 'var(--text-secondary)'};cursor:pointer;"
         data-action="open-suggestion" data-id="${x.id}">
      ${x.original} → ${x.suggestion}${x.status === 'resolved' ? ' <span style="color:var(--success);font-weight:600;">✓</span>' : ''}
    </div>`).join('');

  const historyRows = REVIEW_HISTORY.map((h) => `
    <tr>
      <td style="display:flex;align-items:center;gap:5px;">${h.action === 'ignored' ? '<span style="width:5px;height:5px;border-radius:50%;background:var(--danger);display:inline-block;"></span>' : ''}${h.original}</td>
      <td>${h.result}</td>
      <td style="${h.action === 'applied' ? 'color:var(--success);font-weight:600;' : h.action === 'manual' ? 'color:var(--accent-strong);font-weight:600;' : 'color:var(--text-tertiary);'}">
        ${h.action === 'applied' ? '적용' : h.action === 'manual' ? `직접 입력 → "${h.manualValue}"` : `무시 — "${h.reason}"`}
      </td>
    </tr>`).join('');

  return `
    <div class="toolbar" style="margin-bottom:16px;">
      ${pillLg('warn', 'AI 검토')}
      <span class="pill-outline pill" style="border-radius:6px;">r4</span>
      ${pill('danger', '재검사 필요')}
      <span style="font-size:11.5px;color:var(--text-tertiary);">리뷰어 승인 1/2</span>
      <div class="spacer"></div>
      <div class="btn btn-primary" data-action="toggle-review-modal">검토 완료</div>
    </div>
    ${renderReviewModal()}
    <div class="two-col">
      <div class="col-flex card doc-body" style="padding:26px 30px;position:relative;">
        새로운 신규 획득 정책 2026에 따라
        ${suggestionSpan(byId.s1)}${activeSuggestion && activeSuggestion.id === 's1' ? renderSuggestionPopover(activeSuggestion) : ''}를 대상으로 다음과 같은 비즈니스 규칙을 적용합니다. 첫 번째 규칙은
        ${suggestionSpan(byId.s2)}${activeSuggestion && activeSuggestion.id === 's2' ? renderSuggestionPopover(activeSuggestion) : ''} 14일이 충분한 시점을 기준으로 합니다. 이때 고객의 상태는
        ${suggestionSpan(byId.s3)}로 자동 전환되어야 합니다. 만약 이 과정에서
        ${suggestionSpan(byId.s4)}${activeSuggestion && activeSuggestion.id === 's4' ? renderSuggestionPopover(activeSuggestion) : ''}가 발생할 경우, 시스템은 즉시
        ${suggestionSpan(byId.s5)}${activeSuggestion && activeSuggestion.id === 's5' ? renderSuggestionPopover(activeSuggestion) : ''} 처리를 진행하고 안내 메일을 발송해야 합니다. 두 번째 규칙은 기존 고객의 상향 가입을 유도하기 위한 정책입니다.
        ${suggestionSpan(byId.s6)}을 진행할 경우, 시스템은 혜택의 일환으로
        ${suggestionSpan(byId.s7)}${activeSuggestion && activeSuggestion.id === 's7' ? renderSuggestionPopover(activeSuggestion) : ''} 5,000원을 지급합니다.
        <div style="margin-top:24px;font-size:10.5px;color:var(--text-faint);">수락하면 새 버전으로 커밋됩니다 · 검토본은 따로 만들지 않습니다 · 리뷰어는 [처리 내역] 탭에서 이 기록을 봅니다</div>
      </div>
      <div class="pr-thread narrow card" style="padding:18px;">
        <div class="tab-row">
          <div class="tab-item ${state.reviewSideTab === 'suggestions' ? 'active' : ''}" data-action="review-side-tab" data-tab="suggestions">제안 12건</div>
          <div class="tab-item ${state.reviewSideTab === 'history' ? 'active' : ''}" data-action="review-side-tab" data-tab="history">처리 내역</div>
        </div>
        ${state.reviewSideTab === 'suggestions' ? `
          <div style="font-size:12px;font-weight:600;color:var(--text-tertiary);margin:12px 0;">처리 ${resolvedCount + 1}/12</div>
          <div style="display:flex;flex-direction:column;gap:7px;font-size:12px;">${suggestionList}</div>
        ` : `
          <div style="margin-top:12px;overflow:auto;">
            <table class="dtable" style="font-size:12px;">
              <tr><th style="font-size:10.5px;">원래</th><th style="font-size:10.5px;">결과</th><th style="font-size:10.5px;">처리</th></tr>
              ${historyRows}
            </table>
          </div>
        `}
      </div>
    </div>`;
}

/* ---------------------------------------------------------------------------
   Screen: 10 문서 상세 · 속성
   ------------------------------------------------------------------------- */

function renderDocDetailScreen() {
  return `
    <div class="toolbar" style="margin-bottom:18px;">
      <div class="display-font" style="font-size:17px;">${FOCUS_DOC.name}</div>
      ${pillLg('warn', 'AI 검토')}
      <div class="spacer"></div>
      <div class="btn btn-primary" data-action="go" data-screen="reviewDoc">검토 화면으로</div>
      <div class="btn btn-outline">최신 사전집으로 갱신</div>
      <div class="btn btn-outline">⋯</div>
    </div>
    <div class="two-col">
      <div class="col-flex card" style="padding:26px 30px;">
        <div class="display-font" style="font-size:16px;margin-bottom:16px;">${FOCUS_DOC.name}</div>
        <div style="font-size:13.5px;line-height:1.9;color:var(--text-secondary);">
          새로운 신규 획득 정책 2026에 따라 이용자를 대상으로 다음과 같은 비즈니스 규칙을 적용합니다. 첫 번째 규칙은 체험판 14일이 충분한 시점을 기준으로 합니다. 이때 고객의 상태는 가입완료로 자동 전환되어야 합니다. 만약 이 과정에서 오류가 발생할 경우, 시스템은 즉시 탈퇴 처리를 진행하고 안내 메일을 발송해야 합니다.
          <br><br>
          두 번째 규칙은 기존 고객의 상향 가입을 유도하기 위한 정책입니다. 요금제 변경을 진행할 경우, 시스템은 혜택의 일환으로 마일리지 5,000원을 지급합니다.
        </div>
      </div>
      <div style="width:320px;flex-shrink:0;" class="card">
        <div style="padding:20px;display:flex;flex-direction:column;gap:14px;font-size:12.5px;">
          <div>
            <div style="color:var(--text-quaternary);margin-bottom:4px;font-size:11px;font-weight:600;">적용 사전집</div>
            <div style="display:flex;gap:6px;align-items:center;">
              <span class="pill-outline pill" style="border-radius:6px;font-weight:700;">r4</span>
              ${pill('danger', '재검사 필요')}
            </div>
          </div>
          <div><div style="color:var(--text-quaternary);margin-bottom:4px;font-size:11px;font-weight:600;">작성자</div><div>${FOCUS_DOC.author}</div></div>
          <div style="display:flex;gap:20px;">
            <div><div style="color:var(--text-quaternary);margin-bottom:4px;font-size:11px;font-weight:600;">생성</div><div>08-28 14:20</div></div>
            <div><div style="color:var(--text-quaternary);margin-bottom:4px;font-size:11px;font-weight:600;">수정</div><div>08-31 09:05</div></div>
          </div>
          <div data-action="go" data-screen="docHistory" style="display:flex;align-items:center;justify-content:space-between;border:1px solid var(--border-strong);border-radius:var(--r-sm);padding:10px 12px;cursor:pointer;background:var(--surface-muted);">
            <div><div style="color:var(--text-quaternary);font-size:11px;font-weight:600;">버전</div><div style="font-weight:600;">v3</div></div>
            <div style="color:var(--accent-strong);font-size:12px;font-weight:600;">문서 버전 확인 →</div>
          </div>
        </div>
      </div>
    </div>`;
}

/* ---------------------------------------------------------------------------
   Screen: 14 문서 버전 이력
   ------------------------------------------------------------------------- */

function renderDocHistoryScreen() {
  const timeline = DOC_HISTORY_VERSIONS.map((v) => `
    <div class="timeline-item" style="border:${v.current ? '2px solid var(--accent)' : '1px solid var(--border)'};${!v.current && v.id === 'v1' ? 'opacity:.7;' : ''}">
      <div class="ti-top" style="justify-content:space-between;">
        <span style="font-weight:700;">${v.id}</span>
        ${v.current ? '<span style="font-size:10.5px;font-weight:700;color:var(--accent-strong);background:var(--accent-bg);border-radius:5px;padding:2px 7px;">현재</span>' : ''}
      </div>
      <div class="ti-date">${v.date} · ${v.author}</div>
      <div class="ti-summary">${v.summary}</div>
      ${v.appliedDict !== '—' ? `<div style="font-size:10.5px;color:var(--accent-strong);font-weight:600;margin-top:6px;">적용 사전집 ${v.appliedDict}</div>` : ''}
    </div>`).join('');

  const historyRows = REVIEW_HISTORY.map((h) => `
    <tr>
      <td style="display:flex;align-items:center;gap:5px;">${h.action === 'ignored' ? '<span style="width:5px;height:5px;border-radius:50%;background:var(--danger);display:inline-block;"></span>' : ''}${h.original}</td>
      <td>${h.result}</td>
      <td style="${h.action === 'applied' ? 'color:var(--success);font-weight:600;' : h.action === 'manual' ? 'color:var(--accent-strong);font-weight:600;' : 'color:var(--text-tertiary);'}">
        ${h.action === 'applied' ? '적용' : h.action === 'manual' ? `직접 입력 → "${h.manualValue}"` : `무시 — "${h.reason}"`}
      </td>
    </tr>`).join('');

  return `
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:18px;">
      <div class="display-font" style="font-size:19px;">${FOCUS_DOC.name} · 버전 이력</div>
      <div class="btn btn-outline" data-action="go" data-screen="docDetail">문서로 돌아가기</div>
    </div>
    <div class="two-col">
      <div style="width:360px;flex-shrink:0;display:flex;flex-direction:column;gap:10px;">${timeline}</div>
      <div class="col-flex card" style="padding:22px;display:flex;flex-direction:column;gap:20px;">
        <div>
          <div class="display-font" style="font-size:14.5px;margin-bottom:12px;">v2 → v3</div>
          <div style="font-size:13.5px;line-height:1.9;color:var(--text-secondary);">
            새로운 신규 획득 정책 2026에 따라 <span style="text-decoration:line-through;color:var(--text-faint);">이용자</span> <strong>구독자(Subscriber)</strong>를 대상으로 다음과 같은 비즈니스 규칙을 적용합니다. 첫 번째 규칙은 <span style="text-decoration:line-through;color:var(--text-faint);">체험판</span> <strong>무료 체험(Free Trial)</strong> 14일이 충분한 시점을 기준으로 합니다. 이때 고객의 상태는 <span style="text-decoration:line-through;color:var(--text-faint);">가입완료</span> <strong>구독 활성화(Subscription Activation)</strong>로 자동 전환되어야 합니다.
          </div>
        </div>
        <div style="border-top:1px solid var(--border-soft);padding-top:16px;">
          <div style="font-size:12.5px;font-weight:700;margin-bottom:8px;">처리 내역</div>
          <div style="font-size:11.5px;color:var(--text-quaternary);margin-bottom:10px;">리뷰어는 이 기록으로 무엇이 왜 바뀌었는지 확인합니다</div>
          <table class="dtable" style="font-size:12px;">
            <tr><th style="font-size:10.5px;">원래</th><th style="font-size:10.5px;">결과</th><th style="font-size:10.5px;">처리</th></tr>
            ${historyRows}
          </table>
        </div>
        <div style="border-top:1px solid var(--border-soft);padding-top:16px;display:flex;align-items:center;gap:12px;">
          <div class="btn btn-disabled">v2로 되돌리기</div>
          ${pill('neutral', 'MVP2')}
          <div style="font-size:11px;color:var(--text-quaternary);">되돌리면 v2 내용이 새 버전(v4)으로 쌓이고 v3은 이력에 남습니다.</div>
        </div>
      </div>
    </div>`;
}

/* ---------------------------------------------------------------------------
   Screen: 11 사전집 리비전 이력 · 비교
   ------------------------------------------------------------------------- */

function renderDictHistoryScreen() {
  const timeline = REVISIONS.map((r) => `
    <div class="timeline-item" style="border:${r.emphasize ? '2px solid var(--danger)' : r.current ? '2px solid var(--accent)' : '1px solid var(--border)'};">
      <div class="ti-top">
        <span style="font-weight:700;">${r.id}</span>
        ${pill(r.tone, r.grade)}
        ${r.current ? '<span style="font-size:10px;font-weight:700;color:var(--accent-strong);">현재</span>' : ''}
      </div>
      <div class="ti-date">${r.date} · ${r.author}</div>
      <div class="ti-summary">${r.summary}</div>
      ${r.emphasize ? `<div style="font-size:10.5px;color:var(--danger);margin-top:4px;">문서 2건에 재검사 필요</div>` : ''}
    </div>`).join('');

  return `
    <div style="display:flex;align-items:center;gap:14px;margin-bottom:18px;">
      <div class="display-font" style="font-size:19px;">사전집 리비전 이력 · 비교</div>
      <div class="spacer"></div>
      <div class="filter-chip">r6 ▾</div>
      <span style="color:var(--text-quaternary);">→</span>
      <div class="filter-chip">r7 ▾</div>
      <div class="btn btn-outline" data-action="go" data-screen="dictionary">돌아가기</div>
    </div>
    <div class="two-col">
      <div style="width:380px;flex-shrink:0;display:flex;flex-direction:column;gap:9px;max-height:720px;overflow:auto;">${timeline}</div>
      <div class="col-flex card" style="padding:22px;">
        <div class="display-font" style="font-size:15px;margin-bottom:16px;">r6 → r7 변경 내역</div>
        <div style="display:flex;flex-direction:column;gap:16px;">
          <div>
            <div style="font-size:12.5px;font-weight:700;color:var(--success);margin-bottom:8px;">추가 ${DICT_DIFF_R6_R7.added.length}</div>
            <div style="font-size:13px;color:var(--text-secondary);line-height:1.8;">${DICT_DIFF_R6_R7.added.join(' · ')}</div>
          </div>
          <div>
            <div style="font-size:12.5px;font-weight:700;color:var(--warn);margin-bottom:8px;">변경 ${DICT_DIFF_R6_R7.changed.length}</div>
            <div style="font-size:13px;color:var(--text-secondary);line-height:1.8;">${DICT_DIFF_R6_R7.changed.join(' · ')}</div>
          </div>
          <div><div style="font-size:12.5px;font-weight:700;color:var(--text-quaternary);">삭제 ${DICT_DIFF_R6_R7.removed.length}</div></div>
        </div>
        <div class="banner banner-accent" style="margin-top:18px;line-height:1.7;">
          대표어 변경과 용어 삭제가 없어 '새 지적'입니다. 기존 문서를 다시 볼 필요는 없고, 새 용어에 대한 제안만 늘어납니다.<br>
          <strong>r6 이후 재검사 등급 없음</strong> → r6 태그를 단 문서는 '뒤처짐'으로만 표시됩니다.
        </div>
      </div>
    </div>`;
}

/* ---------------------------------------------------------------------------
   Screen: 19 문서 개정안 리뷰 (PR 스타일)
   ------------------------------------------------------------------------- */

function renderReviewThreadScreen() {
  const comments = REVIEW_THREAD_COMMENTS.map((c) => `
    <div class="comment-card ${c.mine ? 'mine' : ''}">
      <div class="comment-head">${avatar('avatar-' + c.tone, c.initial, 22)}<span class="comment-name">${c.name}</span><span class="comment-time">${c.time}</span></div>
      <div class="comment-text">${c.text}</div>
      ${c.mine ? '<div class="comment-action">삭제</div>' : ''}
    </div>`).join('');

  return `
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;">
      <div class="display-font" style="font-size:18px;">이용자→구독자 등 치환 12건 반영</div>
      <div style="display:flex;gap:10px;">
        <div class="btn btn-outline">Change request</div>
        <div class="btn btn-primary">Approve</div>
      </div>
    </div>
    <div class="two-col">
      <div class="col-flex card doc-body" style="padding:26px 30px;">
        새로운 신규 획득 정책 2026에 따라
        <span class="term-flag-open">이용자</span>를 대상으로 다음과 같은 비즈니스 규칙을 적용합니다. 첫 번째 규칙은
        <span class="term-flag-neutral">체험판</span> 14일이 충분한 시점을 기준으로 합니다. 이때 고객의 상태는
        <span class="term-flag-neutral">가입완료</span>로 자동 전환되어야 합니다. 만약 이 과정에서
        <span class="term-flag-danger">오류</span>가 발생할 경우, 시스템은 즉시
        <span class="term-flag-danger">탈퇴</span> 처리를 진행하고 안내 메일을 발송해야 합니다.
      </div>
      <div class="pr-thread">
        ${comments}
        <div class="comment-compose">댓글 남기기…</div>
      </div>
    </div>`;
}

/* ---------------------------------------------------------------------------
   Screen: 20 사전집 개정안 (PR 스타일)
   ------------------------------------------------------------------------- */

function renderRevisionScreen() {
  const rows = REVISION_ROWS.map((r) => `
    <tr style="${r.highlighted ? 'background:#FFFBF3;' : ''}">
      <td style="font-weight:700;color:var(--text);">${r.term}</td>
      <td>${pill(r.changeTone, r.change)}</td>
      <td style="${r.comments ? 'color:var(--warn);font-weight:600;' : 'color:var(--text-quaternary);'}">
        ${r.comments ? `💬 ${r.comments}개 — ${r.commentNote}` : '—'}
      </td>
    </tr>`).join('');

  const comments = REVISION_THREAD.map((c) => `
    <div class="comment-card">
      <div class="comment-head">${avatar('avatar-' + c.tone, c.initial, 22)}<span class="comment-name">${c.name}</span><span class="comment-time">${c.time}</span></div>
      <div class="comment-text">${c.text}</div>
      ${c.canConvert ? '<div style="display:flex;gap:12px;margin-top:8px;"><span class="comment-action">Change request로 전환</span></div>' : ''}
    </div>`).join('');

  return `
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;">
      <div class="display-font" style="font-size:18px;">사전집 개정안 — r7 → r8</div>
      <div style="display:flex;gap:10px;">
        <div class="btn btn-outline">Change request</div>
        <div class="btn btn-primary" data-action="go" data-screen="dictionary">Approve · r8 발행</div>
      </div>
    </div>
    <div class="two-col">
      <div class="col-flex card" style="overflow:hidden;">
        <table class="dtable">
          <tr><th>용어</th><th>변경</th><th>코멘트</th></tr>
          ${rows}
        </table>
      </div>
      <div class="pr-thread">
        <div style="font-size:12.5px;font-weight:700;">주문 · 코멘트</div>
        ${comments}
        <div class="comment-compose">댓글 남기기…</div>
      </div>
    </div>`;
}

/* ---------------------------------------------------------------------------
   Screen: 설정 (멤버 / 승인 규칙 / 알림 / 라벨)
   ------------------------------------------------------------------------- */

function renderSettingsMembers() {
  const rows = MEMBERS.map((m) => `
    <tr>
      <td style="display:flex;align-items:center;gap:9px;color:var(--text);font-weight:600;">${avatar('avatar-accent', m.initial, 26)}${m.name}</td>
      <td>${m.email}</td>
      <td><span class="pill" style="background:var(--neutral-bg);color:var(--text-secondary);border-radius:6px;">${m.role}</span></td>
      <td>${m.joined}</td>
      <td style="text-align:right;"><span style="font-size:11.5px;font-weight:600;color:${m.removable ? 'var(--danger)' : 'var(--text-disabled)'};">제외</span></td>
    </tr>`).join('');

  return `
    <div style="max-width:880px;">
      <div class="card" style="overflow:hidden;">
        <table class="dtable">
          <tr><th>이름</th><th>이메일</th><th>역할</th><th>가입일</th><th></th></tr>
          ${rows}
        </table>
      </div>
      <div class="banner banner-accent" style="margin-top:16px;">자리가 모두 찼습니다(5/5). 더 초대하려면 기존 멤버를 제외해야 합니다.</div>
    </div>`;
}

function renderSettingsRuleset() {
  return `
    <div style="max-width:640px;display:flex;flex-direction:column;gap:22px;">
      <div class="banner banner-accent" style="line-height:1.7;">ADMIN 이상만 변경할 수 있습니다. 현재 멤버는 5명이므로 최대 4명까지 설정할 수 있습니다. 후보 승인은 사전집 개정안 화면에서 이뤄집니다.</div>
      <div class="card" style="padding:22px;display:flex;flex-direction:column;gap:16px;">
        <div style="font-size:14px;font-weight:700;">용어 승인</div>
        <div style="display:flex;align-items:center;gap:14px;">
          <div style="font-size:13px;color:var(--text-secondary);">승인에 필요한 인원</div>
          <div style="display:flex;border:1px solid var(--border-strong);border-radius:var(--r-sm);overflow:hidden;">
            <div style="padding:6px 12px;color:var(--text-secondary);cursor:pointer;">-</div>
            <div style="padding:6px 16px;font-weight:700;border-left:1px solid var(--border-strong);border-right:1px solid var(--border-strong);">2</div>
            <div style="padding:6px 12px;color:var(--text-secondary);cursor:pointer;">+</div>
          </div>
        </div>
        <div style="font-size:11.5px;color:var(--text-tertiary);">본인이 올린 요청은 본인이 승인할 수 없습니다 (변경 불가)</div>
        <div style="display:flex;align-items:center;justify-content:space-between;border-top:1px solid var(--border-soft);padding-top:14px;">
          <div style="font-size:13px;color:var(--text-secondary);">승인 후 내용이 바뀌면 기존 승인을 무효화</div>
          <div class="toggle on"></div>
        </div>
      </div>
      <div class="card" style="padding:22px;display:flex;flex-direction:column;gap:16px;">
        <div style="font-size:14px;font-weight:700;">문서 검토</div>
        <div style="display:flex;align-items:center;gap:14px;">
          <div style="font-size:13px;color:var(--text-secondary);">필요한 리뷰어 수</div>
          <div style="display:flex;border:1px solid var(--border-strong);border-radius:var(--r-sm);overflow:hidden;">
            <div style="padding:6px 12px;color:var(--text-secondary);cursor:pointer;">-</div>
            <div style="padding:6px 16px;font-weight:700;border-left:1px solid var(--border-strong);border-right:1px solid var(--border-strong);">2</div>
            <div style="padding:6px 12px;color:var(--text-secondary);cursor:pointer;">+</div>
          </div>
        </div>
        <div style="display:flex;align-items:center;justify-content:space-between;border-top:1px solid var(--border-soft);padding-top:14px;">
          <div style="font-size:13px;color:var(--text-secondary);">작성자 본인도 리뷰어로 셀 수 있음</div>
          <div class="toggle"></div>
        </div>
      </div>
      <div style="display:flex;justify-content:flex-end;"><div class="btn btn-primary">저장</div></div>
    </div>`;
}

function renderSettingsNotif() {
  const channels = NOTIF_CHANNELS.map((c) => `
    <div style="display:flex;align-items:center;justify-content:space-between;padding:16px 0;border-bottom:1px solid var(--border-soft);${c.on ? '' : 'opacity:.6;'}">
      <div>
        <div style="font-size:13.5px;font-weight:700;display:flex;align-items:center;gap:8px;">${c.name}${c.badge ? pill('neutral', c.badge) : ''}</div>
        ${c.desc ? `<div style="font-size:11.5px;color:var(--text-quaternary);margin-top:2px;">${c.desc}</div>` : ''}
      </div>
      <div class="toggle ${c.on ? 'on' : ''}"></div>
    </div>`).join('');

  const matrixRows = NOTIF_MATRIX.map((row) => `
    <tr>
      <td style="color:var(--text-secondary);">${row.trigger}</td>
      <td style="text-align:center;color:var(--success);font-weight:700;">✓</td>
      <td style="text-align:center;opacity:.3;">–</td>
      <td style="text-align:center;opacity:.3;">–</td>
      <td style="text-align:center;opacity:.3;">–</td>
    </tr>`).join('');

  return `
    <div style="max-width:720px;display:flex;flex-direction:column;gap:22px;">
      <div class="card" style="padding:6px 22px;">${channels}</div>
      <div class="banner banner-neutral">카카오톡은 지원하지 않습니다 — 알림톡 발신에 사업자등록증과 템플릿 심사가 필요합니다.</div>
      <div class="card" style="padding:20px;">
        <table class="dtable" style="font-size:12px;">
          <tr><th>트리거</th><th style="text-align:center;">인앱</th><th style="text-align:center;opacity:.5;">Slack</th><th style="text-align:center;opacity:.5;">메일</th><th style="text-align:center;opacity:.5;">웹push</th></tr>
          ${matrixRows}
        </table>
        <div style="font-size:11.5px;color:var(--text-tertiary);margin-top:10px;line-height:1.7;">재검사 등급 리비전에서, 그 용어를 실제로 쓰는 문서의 작성자에게만 보냅니다. 용어 추가만 있는 리비전은 보내지 않습니다.</div>
      </div>
      <div style="display:flex;justify-content:flex-end;"><div class="btn btn-primary">저장</div></div>
    </div>`;
}

function renderSettingsLabels() {
  const rows = LABELS.map((l) => `
    <div style="display:flex;align-items:center;gap:12px;padding:12px 0;border-bottom:1px solid var(--border-faint);font-size:13px;">
      <div style="color:var(--text-disabled);">⠿</div>
      <div style="flex:1;color:var(--text);font-weight:600;">${l.name}</div>
      <div style="color:var(--text-quaternary);font-size:12px;">문서 ${l.count}건</div>
      <div style="font-size:12px;color:var(--accent-strong);font-weight:600;cursor:pointer;">이름 변경</div>
      <div style="font-size:12px;color:var(--danger);font-weight:600;cursor:pointer;">삭제</div>
    </div>`).join('');

  return `
    <div style="max-width:640px;">
      <div class="banner banner-accent" style="margin-bottom:20px;line-height:1.7;">여기서 만든 라벨은 문서 목록의 라벨 컬럼과 필터, 후보 근거 문장의 출처 표시에 쓰입니다. 문서 하나에 라벨을 여러 개 지정할 수 있습니다.</div>
      <div class="card" style="padding:8px 20px;">
        ${rows}
        <div style="display:flex;gap:10px;padding:16px 0 8px;">
          <div class="input-box muted" style="flex:1;">새 라벨 이름</div>
          <div class="btn btn-primary">추가</div>
        </div>
      </div>
    </div>`;
}

const SETTINGS_TABS = [
  { key: 'members', label: '멤버', render: renderSettingsMembers },
  { key: 'ruleset', label: '승인 규칙', render: renderSettingsRuleset },
  { key: 'notif', label: '알림', render: renderSettingsNotif },
  { key: 'labels', label: '라벨', render: renderSettingsLabels },
];

function renderSettingsScreen() {
  const tabs = SETTINGS_TABS.map((t) => `
    <div class="tab-item ${state.settingsTab === t.key ? 'active' : ''}" data-action="settings-tab" data-tab="${t.key}">${t.label}</div>`).join('');
  const active = SETTINGS_TABS.find((t) => t.key === state.settingsTab) || SETTINGS_TABS[0];

  return `
    <div class="tab-row settings-tabs">${tabs}</div>
    ${active.render()}`;
}

/* ---------------------------------------------------------------------------
   Root render + event delegation
   ------------------------------------------------------------------------- */

function render() {
  const root = document.getElementById('app');
  if (state.screen === 'login') {
    root.innerHTML = renderLogin();
  } else if (state.screen === 'workspaces') {
    root.innerHTML = renderWorkspaces();
  } else {
    root.innerHTML = renderAppShell();
  }
}

function goScreen(screen) {
  state.screen = screen;
  state.notifOpen = false;
  state.versionMenuOpen = false;
  if (screen !== 'reviewDoc') {
    state.reviewModalOpen = false;
    state.activeSuggestionId = null;
  }
}

const ACTIONS = {
  'go': (ds) => goScreen(ds.screen),

  'toggle-notif': () => { state.notifOpen = !state.notifOpen; state.versionMenuOpen = false; },
  'toggle-version-menu': () => { state.versionMenuOpen = !state.versionMenuOpen; state.notifOpen = false; },
  'toggle-docs-group': () => { state.docsGroupOpen = !state.docsGroupOpen; },
  'toggle-dict-group': () => { state.dictGroupOpen = !state.dictGroupOpen; },

  'select-candidate': (ds) => {
    state.selectedCandidateId = Number(ds.id);
    state.standardMode = 'word';
    state.standardWordIdx = 0;
  },
  'select-standard-word': (ds) => {
    state.standardMode = 'word';
    state.standardWordIdx = Number(ds.idx);
  },
  'select-standard-direct': () => { state.standardMode = 'direct'; },

  'toggle-review-modal': () => { state.reviewModalOpen = !state.reviewModalOpen; },
  'remove-reviewer': (ds) => {
    state.reviewerNames = state.reviewerNames.filter((n) => n !== ds.name);
  },
  'submit-review-request': () => {
    state.reviewModalOpen = false;
    goScreen('reviewThread');
  },

  'settings-tab': (ds) => { state.settingsTab = ds.tab; },

  'open-suggestion': (ds) => {
    state.activeSuggestionId = state.activeSuggestionId === ds.id ? null : ds.id;
  },
  'resolve-suggestion': (ds) => {
    const item = state.suggestions.find((s) => s.id === ds.id);
    if (item) { item.status = 'resolved'; item.action = ds.mode; }
    state.activeSuggestionId = null;
  },
  'review-side-tab': (ds) => { state.reviewSideTab = ds.tab; },
};

document.addEventListener('click', (e) => {
  const el = e.target.closest('[data-action]');
  if (!el) return;
  const action = ACTIONS[el.dataset.action];
  if (!action) return;
  action(el.dataset, e);
  render();
});

render();
