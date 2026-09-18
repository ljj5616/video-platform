import { useState } from 'react'
import { useApi } from '../api/videos'
import { useSession } from '../auth/session'
import { VideoCard } from './VideoCard'

type Kind = 'likes' | 'bookmarks'
interface SavedPage {
  content: { videoId: number; title: string; thumbnailUrl: string | null; duration: number; viewCount: number; author: { userId: number; nickname: string } }[]
  page: number
  totalElements: number
  totalPages: number
}

export function SavedVideos({ kind }: { kind: Kind }) {
  const session = useSession()
  const title = kind === 'likes' ? '좋아요한 영상' : '북마크한 영상'
  return <main id="main" className="main-content">
    <h1 className="section-heading">{title}</h1>
    <nav className="categories saved-navigation" aria-label="내 영상 목록">
      <a className={`category${kind === 'likes' ? ' selected' : ''}`} href="#/me/likes" aria-current={kind === 'likes' ? 'page' : undefined}>좋아요한 영상</a>
      <a className={`category${kind === 'bookmarks' ? ' selected' : ''}`} href="#/me/bookmarks" aria-current={kind === 'bookmarks' ? 'page' : undefined}>북마크한 영상</a>
    </nav>
    {session ? <SavedList key={kind + session.accessToken} kind={kind} /> : <div className="empty-state"><p>로그인 후 {title}을 확인할 수 있습니다.</p><a className="button primary" href="#/login">로그인</a></div>}
  </main>
}

function SavedList({ kind }: { kind: Kind }) {
  const [page, setPage] = useState(0)
  const listing = useApi<SavedPage>(`/api/v1/users/me/${kind}?page=${page}&size=12`)
  const data = listing.data
  return <>
    {listing.loading ? <p className="request-state" role="status">목록을 불러오는 중입니다…</p> : listing.error ?
      <div className="request-state" role="alert">{listing.error}<button className="button" onClick={listing.retry}>다시 시도</button></div> : data && <>
        <p className="saved-count">총 {data.totalElements.toLocaleString('ko-KR')}개</p>
        {data.content.length ? <div className="video-grid">{data.content.map(video => <VideoCard key={video.videoId} video={{
          id: video.videoId, title: video.title, thumbnailUrl: video.thumbnailUrl,
          author: video.author.nickname, views: video.viewCount, duration: video.duration,
        }} />)}</div> : <div className="empty-state"><h2>{page > 0 ? '이 페이지에 영상이 없습니다.' : kind === 'likes' ? '아직 좋아요한 영상이 없습니다.' : '아직 북마크한 영상이 없습니다.'}</h2><p>영상 상세 화면에서 {kind === 'likes' ? '좋아요를 누르면' : '북마크하면'} 여기에 표시됩니다.</p><a className="button" href="#/">영상 둘러보기</a></div>}
      </>}
    {(page > 0 || (data?.totalPages ?? 0) > 1) && <nav className="pagination" aria-label="내 영상 목록 페이지">
      <button aria-label="이전 페이지" disabled={page === 0 || listing.loading} onClick={() => setPage(value => value - 1)}>이전</button>
      <span className="saved-page">{page + 1}{data && data.totalPages >= page + 1 ? ` / ${data.totalPages}` : ''}</span>
      <button aria-label="다음 페이지" disabled={listing.loading || !data || page + 1 >= data.totalPages} onClick={() => setPage(value => value + 1)}>다음</button>
    </nav>}
  </>
}
