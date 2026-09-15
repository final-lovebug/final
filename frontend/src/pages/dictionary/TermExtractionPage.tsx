import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  Button,
  Card,
  Checkbox,
  Pill,
  ScreenSubtitle,
  ScreenTitle,
} from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { ApiError } from '../../shared/api/httpClient'
import { useExtractionEligibleDocuments } from '../../features/dictionary/hooks/useExtractionEligibleDocuments'
import { useCreateExtractionJob } from '../../features/dictionary/hooks/useCreateExtractionJob'
import { useExtractionJob } from '../../features/dictionary/hooks/useExtractionJob'
import { useDictionary } from '../../features/dictionary/hooks/useDictionary'

// ui/main.js renderExtractScreen() 이식 + 실제 작업 접수·폴링(T-INT-17).
//
// 실행 주체는 외부 FastAPI 워커다(D-66). 백엔드는 접수 후 큐로 요청을 발행하고 완료를
// HTTP 콜백으로 받으므로, 화면이 할 수 있는 것은 상태 폴링뿐이다(D-34).
// **워커가 없는 환경(app.ai.dispatch.mode=in-process, 로컬 기본값)에서는 대역이 빈 결과로
// 작업을 끝낸다** — 즉 SUCCEEDED인데 후보어가 0건인 초안이 생기는 것이 정상이다.
//
// 대상 문서는 **자격 있는 최종본 중에서 사용자가 고른다.** 자격 판정(G-12·D-31 — 사전집
// 기준과 정렬됐고 직접 편집되지 않은 문서)은 서버 몫이라 자격 없는 문서는 선택할 수 없고,
// 제출한 목록도 서버가 한 번 더 거른다(DraftDictionaryExtractionService.request).
// 기본값은 자격 있는 문서 전체 선택이다.
export function TermExtractionPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const { data: documents, isLoading } = useExtractionEligibleDocuments(workspaceId)
  // 사전집이 아직 없는 워크스페이스는 404 — 첫 회차라 dictionaryId가 null인 정상 상황이다.
  const { data: activeDictionary } = useDictionary(workspaceId)

  const [extractionJobId, setExtractionJobId] = useState<string | null>(null)
  // null이면 "아직 사용자가 손대지 않음" — 목록이 도착하면 자격 있는 문서 전체가 기본값이다.
  // 목록 로딩이 비동기라 useState 초기값으로는 담을 수 없어 파생 계산으로 둔다.
  const [pickedIds, setPickedIds] = useState<ReadonlySet<string> | null>(null)
  const createExtractionJob = useCreateExtractionJob(workspaceId)
  const { job, isPollingExhausted } = useExtractionJob(extractionJobId)

  const eligibleDocuments = useMemo(
    () => documents?.filter((doc) => doc.eligible) ?? [],
    [documents],
  )
  const selectedIds = useMemo(
    () => pickedIds ?? new Set(eligibleDocuments.map((doc) => doc.documentId)),
    [pickedIds, eligibleDocuments],
  )
  const selectedCount = eligibleDocuments.filter((doc) => selectedIds.has(doc.documentId)).length
  const isAllSelected = eligibleDocuments.length > 0 && selectedCount === eligibleDocuments.length
  const isRunning = job?.status === 'PENDING' || job?.status === 'RUNNING'
  const isAccepting = createExtractionJob.isPending
  const isLocked = isAccepting || isRunning

  function toggleDocument(documentId: string) {
    const next = new Set(selectedIds)
    if (next.has(documentId)) next.delete(documentId)
    else next.add(documentId)
    setPickedIds(next)
  }

  function toggleAll() {
    setPickedIds(
      isAllSelected ? new Set<string>() : new Set(eligibleDocuments.map((doc) => doc.documentId)),
    )
  }

  function handleExtract() {
    createExtractionJob.mutate(
      {
        workspaceId,
        dictionaryId: activeDictionary?.dictionary.id ?? null,
        sourceDocumentIds: eligibleDocuments
          .filter((doc) => selectedIds.has(doc.documentId))
          .map((doc) => doc.documentId),
      },
      { onSuccess: (created) => setExtractionJobId(created.id) },
    )
  }

  return (
    <div className="max-w-[560px]">
      <ScreenTitle>후보 작성용 용어 추출</ScreenTitle>
      <ScreenSubtitle>
        {activeDictionary
          ? `최신 사전집(r${activeDictionary.dictionary.currentVersionNo})까지 갱신·대조를 마치고 최종본으로 확정된 문서에서 후보를 뽑습니다.`
          : '아직 사전집이 없습니다 — 이번이 첫 회차이며, 추출 결과가 첫 사전집의 후보가 됩니다.'}
      </ScreenSubtitle>

      {isLoading && <p className="text-sm text-text-tertiary">불러오는 중…</p>}

      {documents && (
        <Card className="p-5">
          <div className="mb-3 flex items-center justify-between">
            <p className="text-[13px] font-bold text-text">
              최종본 {eligibleDocuments.length}건 중 {selectedCount}건 선택됨
            </p>
            {eligibleDocuments.length > 0 && (
              <button
                type="button"
                disabled={isLocked}
                onClick={toggleAll}
                className="text-[11.5px] font-semibold text-accent-strong underline disabled:cursor-not-allowed disabled:text-text-disabled disabled:no-underline"
              >
                {isAllSelected ? '전체 해제' : '전체 선택'}
              </button>
            )}
          </div>

          <div className="flex flex-col gap-[9px]">
            {documents.length === 0 && (
              <p className="text-[13px] text-text-tertiary">이 워크스페이스에 문서가 없습니다.</p>
            )}
            {documents.map((doc) =>
              doc.eligible ? (
                <button
                  key={doc.documentId}
                  type="button"
                  aria-pressed={selectedIds.has(doc.documentId)}
                  disabled={isLocked}
                  onClick={() => toggleDocument(doc.documentId)}
                  className="flex w-full cursor-pointer items-center gap-[9px] text-left text-[13px] font-medium text-text disabled:cursor-not-allowed disabled:text-text-tertiary"
                >
                  <Checkbox checked={selectedIds.has(doc.documentId)} />
                  {doc.title}
                </button>
              ) : (
                // 자격 없는 문서는 서버가 어차피 걸러낸다(G-12) — 고를 수 없게 두고 이유만 보여준다.
                <div
                  key={doc.documentId}
                  className="flex items-center gap-[9px] text-[13px] text-text-faint"
                >
                  <Checkbox checked={false} />
                  {doc.title}
                  {doc.reason && (
                    <span className="ml-[6px] text-[10.5px] text-text-faint">— {doc.reason}</span>
                  )}
                </div>
              ),
            )}
          </div>

          <div className="mt-5 flex items-center justify-between border-t border-border-soft pt-4">
            <span className="text-xs text-text-quaternary">
              예상 소요 약 2~3분 · ADMIN 이상만 실행 가능
            </span>
            <Button
              variant="primary"
              disabled={selectedCount === 0 || isLocked}
              onClick={handleExtract}
            >
              {isAccepting ? '접수 중…' : isRunning ? '추출 중…' : '추출 실행'}
            </Button>
          </div>

          {eligibleDocuments.length === 0 && documents.length > 0 && (
            <p className="mt-3 text-xs text-text-tertiary">
              추출할 수 있는 최종본이 없습니다. 문서를 사전집 기준으로 갱신한 뒤 다시
              시도하세요.
            </p>
          )}

          {eligibleDocuments.length > 0 && selectedCount === 0 && (
            <p className="mt-3 text-xs text-text-tertiary">
              추출에 사용할 문서를 하나 이상 선택하세요.
            </p>
          )}
        </Card>
      )}

      {createExtractionJob.isError && (
        <Card className="mt-4 border-danger-border bg-danger-bg p-4">
          <p className="text-[12.5px] font-semibold text-danger">
            추출 작업을 접수하지 못했습니다.
          </p>
          <p className="mt-1 text-xs text-text-secondary">
            {createExtractionJob.error instanceof ApiError
              ? createExtractionJob.error.message
              : '알 수 없는 오류가 발생했습니다.'}
          </p>
        </Card>
      )}

      {job && (
        <Card className="mt-4 p-5">
          <div className="mb-3 flex items-center gap-2">
            <Pill
              tone={
                job.status === 'SUCCEEDED'
                  ? 'success'
                  : job.status === 'FAILED'
                    ? 'danger'
                    : 'warn'
              }
            >
              {job.status === 'PENDING'
                ? '대기 중'
                : job.status === 'RUNNING'
                  ? '추출 중'
                  : job.status === 'SUCCEEDED'
                    ? '완료'
                    : '실패'}
            </Pill>
            <span className="text-[11.5px] text-text-quaternary">
              작업 #{job.id} · 대상 문서 {job.sourceDocumentIds.length}건
            </span>
          </div>

          {isRunning && (
            <p className="text-[12.5px] text-text-tertiary">
              AI 워커가 용어를 뽑고 있습니다. 이 화면을 열어 두면 완료 시 자동으로 갱신됩니다.
            </p>
          )}

          {isPollingExhausted && (
            <p className="text-[12.5px] text-warn">
              5분 동안 끝나지 않아 상태 확인을 중단했습니다. 작업은 서버에서 계속 진행되거나
              제한 시간 뒤 실패로 회수됩니다 — 사전 초안 화면에서 결과를 확인하세요.
            </p>
          )}

          {job.status === 'SUCCEEDED' && (
            <div>
              <p className="text-[12.5px] text-text-secondary">
                추출이 끝나 사전 초안이 만들어졌습니다. 후보어를 검토해 등재 여부를 판정하세요.
              </p>
              <Link
                to={routes.dictionaryDraft(workspaceId)}
                className="mt-3 inline-block text-[12.5px] font-semibold text-accent-strong underline"
              >
                사전 초안 교정 화면으로 이동 →
              </Link>
            </div>
          )}

          {job.status === 'FAILED' && (
            <p className="text-[12.5px] text-danger">
              추출에 실패했습니다 — {job.failureReason ?? '원인이 기록되지 않았습니다.'}
            </p>
          )}
        </Card>
      )}
    </div>
  )
}
