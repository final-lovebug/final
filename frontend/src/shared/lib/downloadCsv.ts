// 클라이언트에서 바로 CSV 파일을 만들어 내려받게 하는 유틸리티. 실제 백엔드가 생기면
// 서버가 파일을 만들어 내려줄 수도 있지만, 지금은 목데이터뿐이라 브라우저에서 직접 만든다.
export function downloadCsv(filename: string, rows: string[][]): void {
  const csvBody = rows.map((row) => row.map(escapeCsvCell).join(',')).join('\r\n')
  // 엑셀에서 한글이 깨지지 않도록 UTF-8 BOM을 붙인다.
  const blob = new Blob(['﻿' + csvBody], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)

  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function escapeCsvCell(value: string): string {
  if (/[",\r\n]/.test(value)) {
    return `"${value.replace(/"/g, '""')}"`
  }
  return value
}
