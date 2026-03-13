import { toPng } from 'html-to-image'

export interface PngExportOptions {
  backgroundColor?: string
  pixelRatio?: number
  filename?: string
}

export async function exportCanvasToPng(
  element: HTMLElement,
  options: PngExportOptions = {},
): Promise<void> {
  const {
    backgroundColor = '#ffffff',
    pixelRatio = 2,
    filename,
  } = options

  const dataUrl = await toPng(element, { backgroundColor, pixelRatio })
  downloadDataUrl(dataUrl, filename ?? `erdsketch_export_${formatDate(new Date())}.png`)
}

export async function captureCanvasToPng(
  element: HTMLElement,
  options: PngExportOptions = {},
): Promise<string> {
  const { backgroundColor = '#ffffff', pixelRatio = 2 } = options
  return toPng(element, { backgroundColor, pixelRatio })
}

function downloadDataUrl(dataUrl: string, filename: string): void {
  const a = document.createElement('a')
  a.href = dataUrl
  a.download = filename
  a.click()
}

function formatDate(date: Date): string {
  return date.toISOString().slice(0, 10).replace(/-/g, '')
}
