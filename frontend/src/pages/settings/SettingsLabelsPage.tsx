import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Button, Card } from '../../shared/ui'
import { useLabels } from '../../features/document/hooks/useLabels'
import { useCreateLabel } from '../../features/document/hooks/useCreateLabel'

// ui/main.js renderSettingsLabels() 이식. docs/DOMAIN.md가 Label 엔티티를 아직 "미확정"으로
// 표시하고 있어(467~469줄), 이름 변경·삭제·드래그 정렬 같은 편집 기능은 실제 모델이
// 정해진 뒤에 추가한다. 지금은 목록 조회 + 새 라벨 추가까지만 동작한다.
export function SettingsLabelsPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const { data: labels } = useLabels(workspaceId)
  const createLabel = useCreateLabel(workspaceId)
  const [newLabelName, setNewLabelName] = useState('')

  function handleAdd() {
    if (!newLabelName.trim()) return
    createLabel.mutate(newLabelName.trim(), { onSuccess: () => setNewLabelName('') })
  }

  return (
    <div className="max-w-xl">
      <div className="mb-5 rounded-sm border border-accent-border bg-accent-bg-strong p-3 text-[12.5px] leading-[1.7] text-text">
        여기서 만든 라벨은 문서 목록의 라벨 컬럼과 필터, 후보 근거 문장의 출처 표시에
        쓰입니다. 문서 하나에 라벨을 여러 개 지정할 수 있습니다.
      </div>

      <Card className="px-5 py-2">
        {labels?.map((label, idx) => (
          <div
            key={label.id}
            className={`flex items-center gap-3 py-3 text-[13px] ${
              idx < labels.length - 1 ? 'border-b border-border-faint' : ''
            }`}
          >
            <span className="text-text-disabled">⠿</span>
            <span className="flex-1 font-semibold text-text">{label.name}</span>
            <span className="text-xs text-text-quaternary">
              문서 {label.documentCount}건
            </span>
            <button type="button" className="text-xs font-semibold text-accent-strong">
              이름 변경
            </button>
            <button type="button" className="text-xs font-semibold text-danger">
              삭제
            </button>
          </div>
        ))}

        <div className="flex gap-[10px] py-4">
          <input
            value={newLabelName}
            onChange={(event) => setNewLabelName(event.target.value)}
            onKeyDown={(event) => event.key === 'Enter' && handleAdd()}
            placeholder="새 라벨 이름"
            className="flex-1 rounded-sm border border-border-strong bg-surface-muted px-3 py-[9px] text-[13px] text-text placeholder:text-text-quaternary"
          />
          <Button variant="primary" onClick={handleAdd} disabled={createLabel.isPending}>
            추가
          </Button>
        </div>
      </Card>
    </div>
  )
}
