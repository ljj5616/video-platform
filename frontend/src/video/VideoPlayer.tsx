import { useEffect, useRef, useState } from 'react'
import Hls from 'hls.js'
import { useApi } from '../api/videos'
import { authRequest, getSession } from '../auth/session'

interface Playback { playbackUrl: string; mediaType: string; duration: number }

export function VideoPlayer({ id, poster }: { id: string; poster?: string | null }) {
  const playback = useApi<Playback>(`/api/v1/videos/${id}/playback`)
  const [attempt, setAttempt] = useState(0)
  return <section className="player-shell" aria-label="영상 재생">
    {playback.loading ? <p className="request-state">재생 정보를 불러오는 중…</p> : playback.error ?
      <p className="request-state" role="alert">{playback.error} <button className="button" onClick={playback.retry}>다시 시도</button></p> :
      playback.data && <Player key={playback.data.playbackUrl + attempt} id={id} source={playback.data} poster={poster} retry={() => { setAttempt(value => value + 1); playback.retry() }} />}
  </section>
}

function Player({ id, source, poster, retry }: { id: string; source: Playback; poster?: string | null; retry: () => void }) {
  const ref = useRef<HTMLVideoElement>(null)
  const recorded = useRef(false)
  const [error, setError] = useState('')
  useEffect(() => {
    const video = ref.current!
    let hls: Hls | undefined
    if (video.canPlayType(source.mediaType)) video.src = source.playbackUrl
    else if (Hls.isSupported()) {
      hls = new Hls()
      hls.loadSource(source.playbackUrl)
      hls.attachMedia(video)
      hls.on(Hls.Events.ERROR, (_event, data) => {
        if (data.fatal) { setError('영상을 재생하지 못했습니다. 다시 시도해 주세요.'); hls?.destroy() }
      })
    } else video.src = source.playbackUrl
    return () => { hls?.destroy(); video.removeAttribute('src'); video.load() }
  }, [source])
  async function recordView() {
    const session = getSession()
    if (!session || recorded.current) return
    recorded.current = true
    try { await authRequest(`/videos/${id}/views`, 'POST', undefined, session.accessToken) }
    catch { recorded.current = false }
  }
  return <>
    <video ref={ref} controls playsInline preload="metadata" poster={poster ?? undefined} onPlaying={recordView}
      onError={() => setError('영상을 재생하지 못했습니다. 다시 시도해 주세요.')} />
    {error && <p className="request-state" role="alert">{error} <button className="button" onClick={retry}>다시 시도</button></p>}
  </>
}
