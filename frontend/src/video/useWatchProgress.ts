import { useEffect, useState } from 'react'
import type { RefObject } from 'react'
import { authRequest, getSession, useSession } from '../auth/session'

export function useWatchProgress(ref: RefObject<HTMLVideoElement | null>, id: string, duration: number) {
  const session = useSession()
  const [error, setError] = useState('')
  useEffect(() => {
    const video = ref.current
    if (!video || !session) return
    let started = false
    let active = true
    let sending = false
    let queued: number | undefined
    let saved = -1
    let lastAttempt = Date.now()
    async function flush() {
      if (sending || queued === undefined || getSession()?.accessToken !== session!.accessToken) return
      const position = queued
      queued = undefined
      if (position === saved) return
      sending = true
      lastAttempt = Date.now()
      try {
        await authRequest(`/videos/${id}/progress`, 'PUT', { positionSeconds: position }, session!.accessToken, true)
        saved = position
        if (active) setError('')
      } catch {
        if (active) setError('시청 위치를 저장하지 못했습니다. 다음 저장 시 다시 시도합니다.')
      } finally {
        sending = false
        if (queued !== undefined) void flush()
      }
    }
    function save() {
      if (!started || !Number.isFinite(video!.currentTime) || !Number.isFinite(duration) || duration < 0) return
      queued = Math.floor(Math.max(0, Math.min(video!.currentTime, duration)))
      void flush()
    }
    const playing = () => { started = true }
    const update = () => { if (Date.now() - lastAttempt >= 10000) save() }
    const hidden = () => { if (document.visibilityState === 'hidden') save() }
    video.addEventListener('playing', playing)
    video.addEventListener('timeupdate', update)
    video.addEventListener('pause', save)
    video.addEventListener('ended', save)
    document.addEventListener('visibilitychange', hidden)
    window.addEventListener('pagehide', save)
    return () => {
      active = false
      save()
      video.removeEventListener('playing', playing)
      video.removeEventListener('timeupdate', update)
      video.removeEventListener('pause', save)
      video.removeEventListener('ended', save)
      document.removeEventListener('visibilitychange', hidden)
      window.removeEventListener('pagehide', save)
    }
  }, [ref, id, duration, session])
  return error
}
