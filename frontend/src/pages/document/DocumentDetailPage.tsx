import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Button, Card, ColFlex, Pill, Toolbar, ToolbarSpacer, TwoCol } from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { ApiError } from '../../shared/api/httpClient'
import { dictionaryVersionLabel } from '../../features/document/model/dictionaryVersionLabel'
import { useDocument } from '../../features/document/hooks/useDocument'
import { useCreateCheckJob } from '../../features/document/hooks/useCreateCheckJob'
import { useDeleteDocument } from '../../features/document/hooks/useDeleteDocument'

// ui/main.js renderDocDetailScreen() 이식 — 본문(넓게) + 속성 패널(320px) 2단.
//
// 프로토타입에서 장식이던 「최신 사전집으로 갱신」을 실제 대조 작업 접수(DD-5)로 연결했다.
// 접수만 여기서 하고 진행 상태·결과는 검토 화면이 폴링한다 — 작업 id를 쿼리 파라미터로
// 넘겨 그 화면이 이어받는다(같은 폴링 UI를 두 벌 두지 않기 위한 것이다).
export function DocumentDetailPage() {
  const { workspaceId = '', documentId = '' } = useParams<{
    workspaceId: string
    documentId: string
  }>()
  const navigate = useNavigate()
  const { data: document, isLoading, isError } = useDocument(workspaceId, documentId)
  const createCheckJob = useCreateCheckJob(workspaceId, documentId)
  const deleteDocument = useDeleteDocument(workspaceId, documentId)
  // 대조 접수와 삭제가 같은 자리에 오류를 띄운다 — 툴바 동작은 한 번에 하나만 실행된다.
  const [actionError, setActionError] = useState<string | null>(null)

  if (isLoading) return <p className="text-sm text-text-tertiary">불러오는 중…</p>
  if (isError || !document) {
    return <p className="text-sm text-danger">문서를 찾을 수 없습니다.</p>
  }

  function handleRunCheck() {
    setActionError(null)
    createCheckJob.mutate(undefined, {
      onSuccess: (created) =>
        navigate(`${routes.documentDraft(workspaceId, documentId)}?checkJob=${created.id}`),
      onError: (error) =>
        setActionError(
          error instanceof ApiError ? error.message : '대조 작업을 접수하지 못했습니다.',
        ),
    })
  }

  // 삭제 버튼은 목록이 아니라 이 툴바에만 둔다(`CONFLICTS.md` D-92) — 목록은 행·카드 전체가
  // 상세로 가는 클릭 영역이고, 되돌리기 어려운 동작은 내용을 확인한 자리에서 누르는 편이 안전하다.
  // 권한으로 감추지 않는다 — 참여자면 누구나 지울 수 있고, 최종 판정은 서버가 한다.
  const title = document.title

  function handleDelete() {
    if (!window.confirm(`「${title}」 문서를 삭제하시겠습니까? 되돌릴 수 없습니다.`)) return
    setActionError(null)
    deleteDocument.mutate(undefined, {
      onSuccess: () => navigate(routes.documents(workspaceId)),
      onError: (error) =>
        setActionError(
          error instanceof ApiError ? error.message : '문서를 삭제하지 못했습니다.',
        ),
    })
  }

  return (
    <div>
      <Toolbar className="mb-[18px]">
        <div className="font-display text-[17px] font-bold">{document.title}</div>
        {document.badge === 'danger' ? (
          <Pill tone="danger" size="lg">
            재검사 필요
          </Pill>
        ) : document.badge === 'warn' ? (
          <Pill tone="neutral" size="lg">
            뒤처짐
          </Pill>
        ) : (
          <Pill tone="success" size="lg">
            최신
          </Pill>
        )}
        <ToolbarSpacer />
        <Button variant="outline" disabled={createCheckJob.isPending} onClick={handleRunCheck}>
          {createCheckJob.isPending ? '접수 중…' : '최신 사전집으로 갱신'}
        </Button>
        <Button variant="dangerText" disabled={deleteDocument.isPending} onClick={handleDelete}>
          {deleteDocument.isPending ? '삭제 중…' : '삭제'}
        </Button>
      </Toolbar>

      {actionError && (
        <Card className="mb-4 border-danger-border bg-danger-bg p-4 text-[12.5px] text-danger">
          {actionError}
        </Card>
      )}

      <TwoCol>
        <ColFlex>
          <Card className="px-[30px] py-[26px]">
            <div className="mb-4 font-display text-base font-bold">{document.title}</div>
            <p className="whitespace-pre-wrap wrap-break-word text-[13.5px] leading-[1.9] text-text-secondary">
              {document.content}
            </p>
          </Card>
        </ColFlex>

        <Card className="w-[320px] shrink-0">
          <div className="flex flex-col gap-[14px] p-5 text-[12.5px]">
            <Property label="적용 사전집">
              <span className="flex items-center gap-[6px]">
                <Pill tone="outline">
                  {dictionaryVersionLabel(document.dictionaryVersionNo)}
                </Pill>
                {document.badge && (
                  <Pill tone={document.badge === 'danger' ? 'danger' : 'neutral'}>
                    {document.badge === 'danger' ? '재검사 필요' : '뒤처짐'}
                  </Pill>
                )}
              </span>
            </Property>

            <Property label="라벨">
              {document.labels && document.labels.length > 0 ? (
                <span className="flex flex-wrap gap-[6px]">
                  {document.labels.map((label) => (
                    <Pill key={label} tone="outline">
                      {label}
                    </Pill>
                  ))}
                </span>
              ) : (
                '—'
              )}
            </Property>

            <Property label="작성자">{document.ownerName ?? '—'}</Property>

            <div className="flex gap-5">
              <Property label="생성">
                {new Date(document.createdAt).toLocaleDateString('ko-KR')}
              </Property>
              <Property label="수정">
                {new Date(document.updatedAt).toLocaleDateString('ko-KR')}
              </Property>
            </div>

            <Link
              to={routes.documentHistory(workspaceId, documentId)}
              className="flex items-center justify-between rounded-sm border border-border-strong bg-surface-muted px-3 py-[10px]"
            >
              <span>
                <span className="block text-[11px] font-semibold text-text-quaternary">버전</span>
                <span className="font-semibold text-text">v{document.currentVersionNo}</span>
              </span>
              <span className="text-xs font-semibold text-accent-strong">문서 버전 확인 →</span>
            </Link>
          </div>
        </Card>
      </TwoCol>
    </div>
  )
}

function Property({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <div className="mb-1 text-[11px] font-semibold text-text-quaternary">{label}</div>
      <div className="text-text-secondary">{children}</div>
    </div>
  )
}
