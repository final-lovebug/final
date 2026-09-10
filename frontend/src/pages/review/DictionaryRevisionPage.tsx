import { useNavigate, useParams } from 'react-router-dom'
import { Avatar, Button, Card, Pill } from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { useDictionaryRevision } from '../../features/review/hooks/useDictionaryRevision'
import type { PillTone } from '../../shared/ui'

export function DictionaryRevisionPage() {
  const { workspaceId = '', revisionId = '' } = useParams<{
    workspaceId: string
    revisionId: string
  }>()
  const navigate = useNavigate()
  const { data } = useDictionaryRevision(revisionId)

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="font-display text-lg font-bold text-text">
          사전집 개정안 — r7 → r8
        </h1>
        <div className="flex gap-[10px]">
          <Button variant="outline">Change request</Button>
          <Button
            variant="primary"
            onClick={() => navigate(routes.dictionary(workspaceId))}
          >
            Approve · r8 발행
          </Button>
        </div>
      </div>

      <div className="flex items-start gap-5">
        <Card className="flex-1 overflow-hidden">
          <table className="w-full border-collapse text-[12.5px]">
            <thead>
              <tr>
                {['용어', '변경', '코멘트'].map((h) => (
                  <th
                    key={h}
                    className="whitespace-nowrap border-b border-border-soft px-4 py-[11px] text-left text-[11px] font-semibold text-text-quaternary"
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {data?.rows.map((row) => (
                <tr key={row.term} className={row.highlighted ? 'bg-[#FFFBF3]' : undefined}>
                  <td className="border-b border-border-faint px-4 py-3 font-bold text-text">
                    {row.term}
                  </td>
                  <td className="border-b border-border-faint px-4 py-3">
                    <Pill tone={row.changeTone as PillTone}>{row.change}</Pill>
                  </td>
                  <td
                    className={`border-b border-border-faint px-4 py-3 ${
                      row.comments ? 'font-semibold text-warn' : 'text-text-quaternary'
                    }`}
                  >
                    {row.comments ? `💬 ${row.comments}개 — ${row.commentNote}` : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>

        <div className="flex w-[340px] shrink-0 flex-col gap-3">
          <p className="text-[12.5px] font-bold">주문 · 코멘트</p>
          {data?.thread.map((comment, idx) => (
            <Card key={idx} className="p-[14px]">
              <div className="mb-2 flex items-center gap-2">
                <Avatar initial={comment.initial} tone={comment.tone} size={22} />
                <span className="text-[12.5px] font-bold">{comment.name}</span>
                <span className="text-[10.5px] text-text-quaternary">{comment.time}</span>
              </div>
              <p className="text-[12.5px] leading-[1.6] text-text-secondary">
                {comment.text}
              </p>
              {comment.canConvert && (
                <p className="mt-2 cursor-pointer text-[11px] font-semibold text-accent-strong">
                  Change request로 전환
                </p>
              )}
            </Card>
          ))}
          <div className="rounded-[10px] border border-border-strong bg-surface-muted px-3 py-[10px] text-[12.5px] text-text-quaternary">
            댓글 남기기…
          </div>
        </div>
      </div>
    </div>
  )
}
