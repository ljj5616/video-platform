import { useEffect, useState } from 'react'
import { getSession, useSession, setSession } from '../auth/session'

export interface Video {
  id: number
  title: string
  thumbnailUrl: string | null
  author: string
  views: number
  duration?: number | null
}
interface Page<T> {
  content: T[]
  page: number
  totalElements: number
  totalPages: number
}
interface SearchVideo {
  videoId: number
  title: string
  thumbnailUrl: string | null
  channelName: string
  viewCount: number
}
interface RecommendedVideo {
  id: number
  title: string
  thumbnailUrl: string | null
  uploaderNickname: string
  viewCount: number
  duration: number | null
}
export interface Category { id: number; name: string }

export async function getJson<T>(url: string, signal: AbortSignal): Promise<T> {
  const current = getSession()
  const response = await fetch(url, { signal, headers: { Accept: 'application/json', ...(current ? { Authorization: 'Bearer ' + current.accessToken } : {}) } })
  if (response.status === 401 && current && getSession()?.accessToken === current.accessToken) setSession(null)
  if (!response.ok) {
    throw new Error(response.status === 401
      ? '조회 권한이 없습니다. 로그인 상태를 확인해 주세요.'
      : '데이터를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.')
  }
  return response.json() as Promise<T>
}

export function useApi<T>(url: string) {
  const [attempt, setAttempt] = useState(0)
  const session = useSession()
  const key = url + '#' + attempt + '#' + (session?.accessToken ?? '')
  const [result, setResult] = useState<{ key: string; data?: T; error?: string }>()
  useEffect(() => {
    const controller = new AbortController()
    const timeout = window.setTimeout(() => controller.abort(), 15000)
    let active = true
    getJson<T>(url, controller.signal).then(data => {
      if (active) setResult({ key, data })
    }).catch((error: unknown) => {
      if (active) setResult({ key, error: error instanceof Error && error.name !== 'AbortError'
        ? error.message : '서버 응답이 지연되고 있습니다. 다시 시도해 주세요.' })
    }).finally(() => window.clearTimeout(timeout))
    return () => { active = false; window.clearTimeout(timeout); controller.abort() }
  }, [url, key])
  return {
    data: result?.key === key ? result.data : undefined,
    error: result?.key === key ? result.error : undefined,
    loading: result?.key !== key,
    retry: () => setAttempt(value => value + 1),
  }
}

export type SearchPage = Page<SearchVideo>
export type RecommendationPage = Page<RecommendedVideo>
export const fromSearch = (video: SearchVideo): Video => ({
  id: video.videoId, title: video.title, thumbnailUrl: video.thumbnailUrl,
  author: video.channelName, views: video.viewCount,
})
export const fromRecommendation = (video: RecommendedVideo): Video => ({
  id: video.id, title: video.title, thumbnailUrl: video.thumbnailUrl,
  author: video.uploaderNickname, views: video.viewCount, duration: video.duration,
})
export function formatDuration(seconds: number) {
  const value = Math.max(0, Math.floor(seconds))
  const minutes = Math.floor(value / 60)
  return minutes >= 60
    ? Math.floor(minutes / 60) + ':' + String(minutes % 60).padStart(2, '0') + ':' + String(value % 60).padStart(2, '0')
    : minutes + ':' + String(value % 60).padStart(2, '0')
}

