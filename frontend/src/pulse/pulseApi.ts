export type PulseItem = {
  id: string
  category: string
  title: string
  summary: string
  verificationStatus: string
  publishedAt: string
  readAt: string | null
}

export async function listPulseItems(): Promise<PulseItem[]> {
  const response = await fetch('/api/pulse-items')
  if (!response.ok) throw new Error('动态加载失败，请稍后重试。')
  return response.json() as Promise<PulseItem[]>
}
