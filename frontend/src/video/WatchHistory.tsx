import { useRef, useState } from 'react'
import { formatDuration, useApi } from '../api/videos'
import { authRequest, getSession, useSession } from '../auth/session'
import { VideoCard } from './VideoCard'

interface HistoryPage {
  content: { videoId: number; title: string; thumbnailUrl: string | null; duration: number; positionSeconds: number; progressPercent: number; lastWatchedAt: string; author: { nickname: string } }[]
  totalElements: number
  totalPages: number
}
export function WatchHistory() {
  const session = useSession()
  return <main id="main" className="main-content"><h1 className="section-heading">시청 기록</h1>
    {session ? <HistoryList key={session.accessToken} /> : <div className="empty-state"><p>로그인 후 시청 기록을 확인할 수 있습니다.</p><a className="button primary" href="#/login">로그인</a></div>}
  </main>
}
function HistoryList() {
  const [page, setPage] = useState(0)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const lock = useRef(false)
  const listing = useApi<HistoryPage>(`/api/v1/users/me/watch-history?page=${page}&size=12`)
  const data = listing.data
  async function remove(id: number) {
    const current = getSession()
    if (!current || lock.current) return
    lock.current = true
    setBusy(true)
    setMessage('')
    try {
      await authRequest(`/users/me/watch-history/${id}`, 'DELETE', undefined, current.accessToken)
      setMessage('시청 기록을 삭제했습니다.')
      if (data?.content.length === 1 && page > 0) setPage(value => value - 1)
      else listing.retry()
    } catch (cause) {
      setMessage(cause instanceof Error && !['TypeError', 'TimeoutError'].includes(cause.name) ? cause.message : '시청 기록을 삭제하지 못했습니다. 다시 시도해 주세요.')
    } finally { lock.current = false; setBusy(false) }
  }
  return <>
    {message && <p className="request-state" role="status">{message}</p>}
    {listing.loading ? <p className="request-state" role="status">시청 기록을 불러오는 중입니다…</p> : listing.error ? <div className="request-state" role="alert">{listing.error}<button className="button" onClick={listing.retry}>다시 시도</button></div> : data && <>
      <p className="saved-count">총 {data.totalElements.toLocaleString('ko-KR')}개</p>
      {data.content.length ? <div className="video-grid">{data.content.map(video => <div key={video.videoId}>
        <VideoCard video={{ id: video.videoId, title: video.title, thumbnailUrl: video.thumbnailUrl, author: video.author.nickname, duration: video.duration }} />
        <div className="history-info">
          <progress aria-label={`${video.title} 시청 진행률`} max={100} value={video.progressPercent} />
          <p>{formatDuration(video.positionSeconds)} / {formatDuration(video.duration)} · {video.progressPercent}% 시청</p>
          <p>최근 시청 <time dateTime={video.lastWatchedAt}>{new Date(video.lastWatchedAt).toLocaleString('ko-KR')}</time></p>
          <button className="button" disabled={busy} aria-label={`${video.title} 시청 기록 삭제`} onClick={() => void remove(video.videoId)}>기록 삭제</button>
        </div>
      </div>)}</div> : <div className="empty-state"><h2>{page > 0 ? '이 페이지에 시청 기록이 없습니다.' : '아직 시청 기록이 없습니다.'}</h2><p>로그인 후 시청한 영상이 여기에 표시됩니다.</p><a className="button" href="#/">영상 둘러보기</a></div>}
    </>}
    {(page > 0 || (data?.totalPages ?? 0) > 1) && <nav className="pagination" aria-label="시청 기록 페이지">
      <button disabled={busy || listing.loading || page === 0} onClick={() => setPage(value => value - 1)}>이전</button>
      <span className="saved-page">{page + 1}{data && data.totalPages >= page + 1 ? ` / ${data.totalPages}` : ''}</span>
      <button disabled={busy || listing.loading || !data || page + 1 >= data.totalPages} onClick={() => setPage(value => value + 1)}>다음</button>
    </nav>}
  </>
}
