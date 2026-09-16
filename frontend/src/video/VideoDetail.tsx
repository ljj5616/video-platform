import { useEffect, useRef, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { fromRecommendation, formatDuration, useApi } from '../api/videos'
import type { RecommendationPage } from '../api/videos'
import { authRequest, getSession, sessionUserId, useSession } from '../auth/session'
import { VideoPlayer } from './VideoPlayer'
import './detail.css'

interface Author { userId: number; nickname: string; profileImageUrl: string | null }
interface Detail {
  videoId: number; title: string; description: string | null; thumbnailUrl: string | null
  viewCount: number; categoryName: string; author: Author; createdAt: string
  likeCount: number; liked: boolean; bookmarked: boolean
}
interface Comment { commentId: number; content: string; author: Author; createdAt: string }
interface Comments { content: Comment[]; totalElements: number; totalPages: number }
const number = new Intl.NumberFormat('ko-KR')
const date = (value: string) => new Date(value).toLocaleDateString('ko-KR')
const reasons = { SPAM: '스팸', INAPPROPRIATE: '부적절한 콘텐츠', HATE_SPEECH: '혐오 표현', VIOLENCE: '폭력', COPYRIGHT: '저작권 침해', OTHER: '기타' }

function Avatar({ author }: { author?: Author }) {
  return <span className="avatar">{author?.profileImageUrl ? <img src={author.profileImageUrl} alt="" onError={event => { event.currentTarget.style.display = 'none' }} /> : null}</span>
}
function State({ error, loading, retry, children }: { error?: string; loading: boolean; retry: () => void; children: ReactNode }) {
  return loading ? <p className="request-state" role="status">불러오는 중…</p> : error ? <p className="request-state" role="alert">{error} <button className="button" onClick={retry}>다시 시도</button></p> : children
}

export function VideoDetail({ id, onDeleted }: { id: string; onDeleted: () => void }) {
  const session = useSession()
  const detail = useApi<Detail>(`/api/v1/videos/${id}`, true)
  const related = useApi<RecommendationPage>('/api/v1/videos/recommendations?page=0&size=6')
  const [page, setPage] = useState(0)
  const comments = useApi<Comments>(`/api/v1/videos/${id}/comments?page=${page}&size=10`)
  const [content, setContent] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const lock = useRef(false)
  const [report, setReport] = useState<string | null>(null)
  const [editing, setEditing] = useState<number | null>(null)
  const [draft, setDraft] = useState('')
  const [deleting, setDeleting] = useState<number | null>(null)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const mounted = useRef(true)
  useEffect(() => { mounted.current = true; return () => { mounted.current = false } }, [])
  const userId = sessionUserId(session)
  async function mutate(path: string, method: string, body?: unknown, done?: () => void) {
    if (lock.current) return
    const current = getSession()
    if (!current) { setMessage('로그인이 필요한 기능입니다.'); return }
    lock.current = true
    setBusy(true)
    setMessage('')
    try {
      await authRequest(path, method, body, current.accessToken)
      if (mounted.current) done?.()
    } catch (cause) {
      if (mounted.current) setMessage(cause instanceof Error && !['TypeError', 'TimeoutError'].includes(cause.name) ? cause.message : '서버에 연결하지 못했습니다. 다시 시도해 주세요.')
    } finally { lock.current = false; if (mounted.current) setBusy(false) }
  }
  function submitComment(event: FormEvent) {
    event.preventDefault()
    if (!content.trim()) return
    void mutate(`/videos/${id}/comments`, 'POST', { content: content.trim() }, () => {
      setContent(''); setPage(0); comments.retry(); setMessage('댓글을 등록했습니다.')
    })
  }
  const video = detail.data
  return <main id="main" className="detail-layout">
    <div className="detail-primary">
      <a className="back-link" href="#/">← 영상 목록</a>
      <State {...detail}>
        {video && <>
          <VideoPlayer id={id} poster={video.thumbnailUrl} />
          <h1 className="video-title">{video.title}</h1>
          {session && userId === video.author.userId && <div className="video-owner-actions">
            <button className="button" disabled={busy} onClick={() => { window.location.hash = `/videos/${id}/edit` }}>영상 수정</button>
            <button className="button danger" disabled={busy} onClick={() => setConfirmDelete(true)}>영상 삭제</button>
          </div>}
          {session && userId === video.author.userId && confirmDelete && <section className="video-delete-confirm" aria-label="영상 삭제 확인">
            <h2>이 영상을 삭제하시겠습니까?</h2><p>“{video.title}” 영상이 목록에서 제거되고 더 이상 시청할 수 없게 됩니다.</p>
            <div className="form-actions"><button className="button" disabled={busy} onClick={() => setConfirmDelete(false)}>취소</button><button className="button danger" disabled={busy} onClick={() => void mutate(`/videos/${id}`, 'DELETE', undefined, () => { onDeleted(); window.location.hash = '/' })}>{busy ? '삭제 중…' : '삭제 확인'}</button></div>
          </section>}
          <div className="video-meta">
            <div className="channel"><Avatar author={video.author} /><div><strong>{video.author.nickname}</strong><p>{video.categoryName}</p></div></div>
            <div className="video-actions">
              <button className="button" aria-pressed={video.liked} disabled={busy || detail.refreshing} onClick={() => void mutate(`/videos/${id}/likes`, video.liked ? 'DELETE' : 'POST', undefined, detail.retry)}>♡ 좋아요 {number.format(video.likeCount ?? 0)}</button>
              <button className="button" aria-pressed={video.bookmarked} disabled={busy || detail.refreshing} onClick={() => void mutate(`/videos/${id}/bookmarks`, video.bookmarked ? 'DELETE' : 'POST', undefined, detail.retry)}>{video.bookmarked ? '▣ 북마크됨' : '▢ 북마크'}</button>
              <button className="button" onClick={() => session ? setReport(`/videos/${id}/reports`) : setMessage('로그인이 필요한 기능입니다.')}>⚑ 신고</button>
            </div>
          </div>
          <section className="video-description" aria-label="영상 정보"><p><strong>조회수 {number.format(video.viewCount)}회</strong> <span>{date(video.createdAt)} 공개</span></p><p>{video.description || '등록된 영상 설명이 없습니다.'}</p></section>
        </>}
      </State>
      {message && <p className="detail-message" role="status">{message} {!session && <a href="#/login">로그인</a>}</p>}
      {report && <section className="report-panel" aria-label="신고 작성"><h2>콘텐츠 신고</h2><form onSubmit={event => {
        event.preventDefault()
        const fields = new FormData(event.currentTarget)
        void mutate(report, 'POST', { reason: fields.get('reason'), description: fields.get('description') }, () => { setReport(null); setMessage('신고가 접수되었습니다.') })
      }}><label>신고 사유<select name="reason" required>{Object.entries(reasons).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></label><label>상세 내용 (기타 선택 시 필수)<textarea name="description" maxLength={1000} /></label><div className="form-actions"><button type="button" className="button" disabled={busy} onClick={() => setReport(null)}>취소</button><button className="button primary" disabled={busy}>신고 제출</button></div></form></section>}
      {video && <section className="comments" aria-label="댓글">
        <h2>댓글 {comments.data ? number.format(comments.data.totalElements) + '개' : ''}</h2>
        <form className="comment-compose" onSubmit={submitComment}><Avatar /><div><label className="sr-only" htmlFor="new-comment">댓글</label><textarea id="new-comment" value={content} maxLength={1000} required disabled={!session || busy} placeholder={session ? '댓글을 입력하세요…' : '로그인 후 댓글을 작성할 수 있습니다.'} onChange={event => setContent(event.target.value)} /><div className="form-actions">{!session && <a href="#/login">로그인</a>}<button className="button primary" disabled={!session || busy || !content.trim()}>댓글 등록</button></div></div></form>
        <State {...comments}>
          {comments.data?.content.length === 0 && <p className="request-state">첫 댓글을 남겨보세요.</p>}
          {comments.data?.content.map(comment => <article className="comment-row" key={comment.commentId}><Avatar author={comment.author} /><div className="comment-body"><header><strong>{comment.author.nickname}</strong><time>{date(comment.createdAt)}</time>
            {session && <details className="comment-menu"><summary aria-label={`${comment.author.nickname} 댓글 메뉴`}>⋯</summary><div>{userId === comment.author.userId ? <><button disabled={busy} onClick={() => { setEditing(comment.commentId); setDraft(comment.content) }}>수정</button><button disabled={busy} onClick={() => setDeleting(comment.commentId)}>삭제</button></> : <button onClick={() => setReport(`/comments/${comment.commentId}/reports`)}>신고</button>}</div></details>}
          </header>{editing === comment.commentId ? <form onSubmit={event => { event.preventDefault(); if (draft.trim()) void mutate(`/comments/${comment.commentId}`, 'PATCH', { content: draft.trim() }, () => { setEditing(null); comments.retry() }) }}><textarea aria-label="댓글 수정" value={draft} maxLength={1000} required onChange={event => setDraft(event.target.value)} /><div className="form-actions"><button type="button" className="button" disabled={busy} onClick={() => setEditing(null)}>취소</button><button className="button primary" disabled={busy || !draft.trim()}>저장</button></div></form> : <p>{comment.content}</p>}
          {deleting === comment.commentId && <div className="delete-confirm">댓글을 삭제하시겠습니까? <button className="button" disabled={busy} onClick={() => setDeleting(null)}>취소</button> <button className="button primary" disabled={busy} onClick={() => void mutate(`/comments/${comment.commentId}`, 'DELETE', undefined, () => { setDeleting(null); if (comments.data?.content.length === 1 && page > 0) setPage(page - 1); comments.retry() })}>삭제</button></div>}
          </div></article>)}
        </State>
        {comments.data && comments.data.totalPages > 1 && <nav className="pagination" aria-label="댓글 페이지"><button disabled={page === 0} onClick={() => setPage(page - 1)}>이전</button><span>{page + 1} / {comments.data.totalPages}</span><button disabled={page + 1 >= comments.data.totalPages} onClick={() => setPage(page + 1)}>다음</button></nav>}
      </section>}
    </div>
    <aside className="related"><h2>추천 영상</h2><State {...related}>{related.data?.content.filter(item => String(item.id) !== id).slice(0, 5).map(fromRecommendation).map(item => <a className="related-card" key={item.id} href={`#/videos/${item.id}`}><div className="thumbnail">{item.thumbnailUrl ? <img src={item.thumbnailUrl} alt="" onError={event => { event.currentTarget.style.display = 'none' }} /> : <span className="play-symbol">▶</span>}{item.duration != null && <span className="duration">{formatDuration(item.duration)}</span>}</div><div><h3>{item.title}</h3><p>{item.author}</p><p>조회수 {number.format(item.views)}회</p></div></a>)}{related.data && !related.data.content.some(item => String(item.id) !== id) && <p className="request-state">추천 영상이 없습니다.</p>}</State></aside>
  </main>
}
