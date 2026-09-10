import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Button, Card } from '../../shared/ui'
import { useRuleSet } from '../../features/workspace/hooks/useRuleSet'
import { useUpdateRuleSet } from '../../features/workspace/hooks/useUpdateRuleSet'
import { WORKSPACE_MEMBER_CAPACITY } from '../../features/member/model/fixtures'

function Stepper({
  value,
  onChange,
  max,
}: {
  value: number
  onChange: (next: number) => void
  max: number
}) {
  return (
    <div className="flex overflow-hidden rounded-sm border border-border-strong">
      <button
        type="button"
        onClick={() => onChange(Math.max(0, value - 1))}
        className="px-3 py-[6px] text-text-secondary"
      >
        -
      </button>
      <span className="border-x border-border-strong px-4 py-[6px] font-bold">
        {value}
      </span>
      <button
        type="button"
        onClick={() => onChange(Math.min(max, value + 1))}
        className="px-3 py-[6px] text-text-secondary"
      >
        +
      </button>
    </div>
  )
}

function Toggle({ on, onToggle }: { on: boolean; onToggle: () => void }) {
  return (
    <button
      type="button"
      onClick={onToggle}
      className={`relative h-[22px] w-[38px] shrink-0 rounded-[11px] ${
        on ? 'bg-accent' : 'bg-border-strong'
      }`}
    >
      <span
        className={`absolute top-[2px] h-[18px] w-[18px] rounded-full bg-white transition-[left] ${
          on ? 'left-[18px]' : 'left-[2px]'
        }`}
      />
    </button>
  )
}

// ui/main.js renderSettingsRuleset() 이식. "승인 후 내용이 바뀌면 기존 승인 무효화"·
// "작성자 본인도 리뷰어로 셀 수 있음" 두 토글은 docs/DOMAIN.md RuleSet에 없는 값이라
// 저장하지 않는 화면 로컬 상태로만 둔다 (features/workspace/model/fixtures.ts 주석 참고).
export function SettingsRulesetPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const { data: ruleSet } = useRuleSet(workspaceId)
  const updateRuleSet = useUpdateRuleSet(workspaceId)

  const [documentReviewerCount, setDocumentReviewerCount] = useState(0)
  const [dictionaryReviewerCount, setDictionaryReviewerCount] = useState(0)
  const [invalidateOnChange, setInvalidateOnChange] = useState(true)
  const [authorCanReview, setAuthorCanReview] = useState(false)

  useEffect(() => {
    if (!ruleSet) return
    setDocumentReviewerCount(ruleSet.requiredDocumentReviewerCount)
    setDictionaryReviewerCount(ruleSet.requiredDictionaryReviewerCount)
  }, [ruleSet])

  const maxReviewers = WORKSPACE_MEMBER_CAPACITY - 1

  return (
    <div className="flex max-w-xl flex-col gap-[22px]">
      <div className="rounded-sm border border-accent-border bg-accent-bg-strong p-3 text-[12.5px] leading-[1.7] text-text">
        ADMIN 이상만 변경할 수 있습니다. 현재 멤버는 {WORKSPACE_MEMBER_CAPACITY}명이므로
        최대 {maxReviewers}명까지 설정할 수 있습니다. 후보 승인은 사전집 개정안 화면에서
        이뤄집니다.
      </div>

      <Card className="flex flex-col gap-4 p-[22px]">
        <p className="text-sm font-bold">용어 승인</p>
        <div className="flex items-center gap-[14px]">
          <span className="text-[13px] text-text-secondary">승인에 필요한 인원</span>
          <Stepper
            value={dictionaryReviewerCount}
            onChange={setDictionaryReviewerCount}
            max={maxReviewers}
          />
        </div>
        <p className="text-[11.5px] text-text-tertiary">
          본인이 올린 요청은 본인이 승인할 수 없습니다 (변경 불가)
        </p>
        <div className="flex items-center justify-between border-t border-border-soft pt-[14px]">
          <span className="text-[13px] text-text-secondary">
            승인 후 내용이 바뀌면 기존 승인을 무효화
          </span>
          <Toggle on={invalidateOnChange} onToggle={() => setInvalidateOnChange((v) => !v)} />
        </div>
      </Card>

      <Card className="flex flex-col gap-4 p-[22px]">
        <p className="text-sm font-bold">문서 검토</p>
        <div className="flex items-center gap-[14px]">
          <span className="text-[13px] text-text-secondary">필요한 리뷰어 수</span>
          <Stepper
            value={documentReviewerCount}
            onChange={setDocumentReviewerCount}
            max={maxReviewers}
          />
        </div>
        <div className="flex items-center justify-between border-t border-border-soft pt-[14px]">
          <span className="text-[13px] text-text-secondary">
            작성자 본인도 리뷰어로 셀 수 있음
          </span>
          <Toggle on={authorCanReview} onToggle={() => setAuthorCanReview((v) => !v)} />
        </div>
      </Card>

      <div className="flex justify-end">
        <Button
          variant="primary"
          disabled={updateRuleSet.isPending}
          onClick={() =>
            updateRuleSet.mutate({
              requiredDocumentReviewerCount: documentReviewerCount,
              requiredDictionaryReviewerCount: dictionaryReviewerCount,
            })
          }
        >
          {updateRuleSet.isPending ? '저장 중…' : '저장'}
        </Button>
      </div>
    </div>
  )
}
