import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Banner, Button, Card, Toggle } from '../../shared/ui'
import { ApiError } from '../../shared/api/httpClient'
import { useRuleSet } from '../../features/workspace/hooks/useRuleSet'
import { useUpdateRuleSet } from '../../features/workspace/hooks/useUpdateRuleSet'
import { useWorkspace } from '../../features/workspace/hooks/useWorkspace'
import { useWorkspaceMembers } from '../../features/member/hooks/useWorkspaceMembers'

function Stepper({
  value,
  onChange,
  max,
  disabled,
  label,
}: {
  value: number
  onChange: (next: number) => void
  max: number
  disabled?: boolean
  label: string
}) {
  return (
    <div className="flex overflow-hidden rounded-sm border border-border-strong">
      <button
        type="button"
        aria-label={`${label} 줄이기`}
        disabled={disabled || value <= 0}
        onClick={() => onChange(Math.max(0, value - 1))}
        className="cursor-pointer px-3 py-[6px] text-text-secondary disabled:cursor-not-allowed disabled:text-text-disabled"
      >
        -
      </button>
      <span className="border-x border-border-strong px-4 py-[6px] font-bold">{value}</span>
      <button
        type="button"
        aria-label={`${label} 늘리기`}
        disabled={disabled || value >= max}
        onClick={() => onChange(Math.min(max, value + 1))}
        className="cursor-pointer px-3 py-[6px] text-text-secondary disabled:cursor-not-allowed disabled:text-text-disabled"
      >
        +
      </button>
    </div>
  )
}

// ui/main.js renderSettingsRuleset() 이식.
//
// **(2026-09-14) 저장되지 않던 토글 둘을 고정 정책 표시로 바꿨다.** 프로토타입의
// 「승인 후 내용이 바뀌면 기존 승인을 무효화」·「작성자 본인도 리뷰어로 셀 수 있음」은
// `RuleSet`에 없는 값이라 그동안 화면 로컬 상태로만 움직였다 — 켜고 꺼도 아무 일도
// 일어나지 않았다. 둘 다 실제로는 **바꿀 수 없는 서버 동작**이므로(재교정이 기존 승인을
// 무효화하고, 요청자는 리뷰어에서 제외된다) 잠근 채 사유를 적는다.
//
// 정원 상수 대신 **실제 참여자 수**로 상한을 잡는다 — 멤버가 3명인 워크스페이스에서
// 리뷰어 4명을 요구하면 영원히 정족수를 못 채운다.
export function SettingsRulesetPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const { data: ruleSet } = useRuleSet(workspaceId)
  const { data: workspace } = useWorkspace(workspaceId)
  const { data: members } = useWorkspaceMembers(workspaceId)
  const updateRuleSet = useUpdateRuleSet(workspaceId)

  // 서버 값이 기준이고, 사용자가 만진 것만 덮는다 — effect로 state를 채우지 않는다.
  const [documentOverride, setDocumentOverride] = useState<number | null>(null)
  const [dictionaryOverride, setDictionaryOverride] = useState<number | null>(null)
  const documentReviewerCount = documentOverride ?? ruleSet?.requiredDocumentReviewerCount ?? 0
  const dictionaryReviewerCount =
    dictionaryOverride ?? ruleSet?.requiredDictionaryReviewerCount ?? 0

  const memberCount = members?.length ?? 0
  // 요청자는 자기 요청을 승인할 수 없으므로 최대 정족수는 "참여자 - 1"이다.
  const maxReviewers = Math.max(0, memberCount - 1)
  const canEdit = workspace?.myPermission === 'OWNER' || workspace?.myPermission === 'ADMIN'
  const isDirty = documentOverride !== null || dictionaryOverride !== null

  return (
    <div className="flex max-w-[640px] flex-col gap-[22px]">
      <Banner className="leading-[1.7]">
        ADMIN 이상만 변경할 수 있습니다. 현재 멤버는 {memberCount}명이므로 최대{' '}
        {maxReviewers}명까지 설정할 수 있습니다. 후보 승인은 사전집 개정안 화면에서
        이뤄집니다.
      </Banner>

      <Card className="flex flex-col gap-4 p-[22px]">
        <p className="text-sm font-bold">용어 승인</p>
        <div className="flex items-center gap-[14px]">
          <span className="text-[13px] text-text-secondary">승인에 필요한 인원</span>
          <Stepper
            label="용어 승인 정족수"
            value={dictionaryReviewerCount}
            onChange={setDictionaryOverride}
            max={maxReviewers}
            disabled={!canEdit}
          />
        </div>
        <p className="text-[11.5px] text-text-tertiary">
          본인이 올린 요청은 본인이 승인할 수 없습니다 (변경 불가)
        </p>
        <div className="flex items-center justify-between border-t border-border-soft pt-[14px]">
          <span className="text-[13px] text-text-secondary">
            승인 후 내용이 바뀌면 기존 승인을 무효화
            <span className="ml-2 text-[11px] text-text-quaternary">
              재교정이 돌면 회차가 올라가 기존 승인이 무효화됩니다 · 변경 불가
            </span>
          </span>
          <Toggle on label="승인 무효화(고정)" />
        </div>
      </Card>

      <Card className="flex flex-col gap-4 p-[22px]">
        <p className="text-sm font-bold">문서 검토</p>
        <div className="flex items-center gap-[14px]">
          <span className="text-[13px] text-text-secondary">필요한 리뷰어 수</span>
          <Stepper
            label="문서 리뷰어 정족수"
            value={documentReviewerCount}
            onChange={setDocumentOverride}
            max={maxReviewers}
            disabled={!canEdit}
          />
        </div>
        <div className="flex items-center justify-between border-t border-border-soft pt-[14px]">
          <span className="text-[13px] text-text-secondary">
            작성자 본인도 리뷰어로 셀 수 있음
            <span className="ml-2 text-[11px] text-text-quaternary">
              요청자는 리뷰어에서 제외됩니다 · 변경 불가
            </span>
          </span>
          <Toggle on={false} label="작성자 리뷰어 포함(고정)" />
        </div>
      </Card>

      {updateRuleSet.isError && (
        <p className="text-xs text-danger">
          {updateRuleSet.error instanceof ApiError
            ? updateRuleSet.error.message
            : '저장하지 못했습니다.'}
        </p>
      )}

      <div className="flex items-center justify-end gap-3">
        {updateRuleSet.isSuccess && !isDirty && (
          <span className="text-[11.5px] text-success">저장했습니다.</span>
        )}
        <Button
          variant="primary"
          disabled={!canEdit || !isDirty || updateRuleSet.isPending}
          onClick={() =>
            updateRuleSet.mutate(
              {
                requiredDocumentReviewerCount: documentReviewerCount,
                requiredDictionaryReviewerCount: dictionaryReviewerCount,
              },
              {
                onSuccess: () => {
                  setDocumentOverride(null)
                  setDictionaryOverride(null)
                },
              },
            )
          }
        >
          {updateRuleSet.isPending ? '저장 중…' : '저장'}
        </Button>
      </div>
    </div>
  )
}
