import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { useApi } from '../api/videos'
import type { Category } from '../api/videos'
import { authRequest, getSession, sessionUserId, useSession } from '../auth/session'
import { VideoPlayer } from './VideoPlayer'
import { validateFile } from './upload'
import './detail.css'
import './upload.css'
import './edit.css'

interface EditableVideo {
  videoId: number; title: string; description: string | null; thumbnailUrl: string | null
  categoryId: number; categoryName: string; visibility: 'PUBLIC' | 'UNLISTED' | 'PRIVATE'
  status: string; author: { userId: number }
}

export function VideoEdit({ id, onSaved }: { id: string; onSaved: () => void }) {
  const session = useSession()
  const detail = useApi<EditableVideo>(`/api/v1/videos/${id}`)
  if (!session) return <main id="main" className="upload-layout"><h1>영상 수정</h1><p className="request-state">영상 수정은 로그인이 필요합니다. <a className="button" href="#/login">로그인</a></p></main>
  if (detail.loading) return <main id="main" className="request-state" role="status">영상 정보를 불러오는 중…</main>
  if (detail.error || !detail.data) return <main id="main" className="request-state" role="alert">{detail.error || '영상 정보를 불러오지 못했습니다.'} <button className="button" onClick={detail.retry}>다시 시도</button></main>
  const video = detail.data
  if (sessionUserId(session) !== video.author.userId) return <main id="main" className="upload-layout"><h1>영상 수정</h1><p className="request-state" role="alert">본인이 업로드한 영상만 수정할 수 있습니다.</p><a className="button" href={`#/videos/${id}`}>영상으로 돌아가기</a></main>
  if (!['PUBLIC', 'UNLISTED', 'PRIVATE'].includes(video.visibility) || !['PUBLISHED', 'FAILED'].includes(video.status)) return <main id="main" className="upload-layout"><h1>영상 수정</h1><p className="request-state" role="status">{['UPLOADING', 'PROCESSING'].includes(video.status) ? '영상 변환 중에는 수정할 수 없습니다. 처리가 끝난 뒤 다시 시도해 주세요.' : '영상의 공개 범위 또는 처리 상태를 확인할 수 없습니다. 최신 백엔드가 실행 중인지 확인해 주세요.'}</p><div className="edit-actions"><button className="button" onClick={detail.retry}>다시 확인</button><a className="button" href={`#/videos/${id}`}>영상으로 돌아가기</a></div></main>
  return <EditForm key={id} video={video} onSaved={onSaved} />
}

function EditForm({ video, onSaved }: { video: EditableVideo; onSaved: () => void }) {
  const categories = useApi<Category[]>('/api/v1/categories')
  const [thumbnail, setThumbnail] = useState<File>()
  const [categoryId, setCategoryId] = useState(String(video.categoryId))
  const image = useRef<HTMLImageElement>(null)
  const [busy, setBusy] = useState(false)
  const lock = useRef(false)
  const mounted = useRef(true)
  const [error, setError] = useState('')
  const [fileError, setFileError] = useState('')
  useEffect(() => { mounted.current = true; return () => { mounted.current = false } }, [])
  useEffect(() => {
    const url = thumbnail ? URL.createObjectURL(thumbnail) : video.thumbnailUrl
    if (image.current) {
      if (url) image.current.src = url
      else image.current.removeAttribute('src')
    }
    return () => { if (thumbnail && url) URL.revokeObjectURL(url) }
  }, [thumbnail, video.thumbnailUrl])
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (lock.current) return
    const session = getSession()
    if (!session || sessionUserId(session) !== video.author.userId) { setError('로그인 상태와 수정 권한을 확인해 주세요.'); return }
    const body = new FormData(event.currentTarget)
    const title = String(body.get('title') ?? '').trim()
    if (!title) { setError('영상 제목을 입력해 주세요.'); return }
    body.set('title', title)
    body.set('description', String(body.get('description') ?? '').trim())
    if (thumbnail) body.set('thumbnailFile', thumbnail)
    lock.current = true; setBusy(true); setError('')
    try {
      await authRequest(`/videos/${video.videoId}`, 'PATCH', body, session.accessToken)
      if (mounted.current) { onSaved(); window.location.hash = `/videos/${video.videoId}` }
    } catch (cause) {
      if (mounted.current) setError(cause instanceof Error && !['TypeError', 'TimeoutError'].includes(cause.name) ? cause.message : '저장 결과를 확인하지 못했습니다. 연결 상태를 확인하고 다시 시도해 주세요.')
    } finally { lock.current = false; if (mounted.current) setBusy(false) }
  }
  return <main id="main" className="upload-layout video-edit"><h1>영상 수정</h1>
    <form onSubmit={save}><fieldset className="edit-form-fields" disabled={busy}>
      <div className="upload-grid">
        <section className="upload-panel edit-preview"><h2>영상 미리보기</h2><VideoPlayer id={String(video.videoId)} poster={video.thumbnailUrl} />
          <h2>썸네일 변경</h2><div className="edit-thumbnail-row"><div className="edit-thumbnail">{thumbnail || video.thumbnailUrl ? <img ref={image} alt="썸네일 미리보기" /> : <span>썸네일 없음</span>}</div>
            <label className="button edit-thumbnail-button">변경<input className="upload-file-input" aria-label="썸네일 변경" type="file" accept=".jpg,.jpeg,.png,.webp" onChange={event => {
              const file = event.target.files?.[0]
              if (file) try { setThumbnail(validateFile(file, true)); setFileError('') } catch (cause) { setFileError((cause as Error).message) }
              event.target.value = ''
            }} /></label>{thumbnail && <button className="button" type="button" onClick={() => { setThumbnail(undefined); setFileError('') }}>변경 취소</button>}
          </div><p className="upload-help">{thumbnail ? `선택됨: ${thumbnail.name} · ` : ''}JPG·PNG·WebP, 최대 10MB. 선택하지 않으면 기존 썸네일을 유지합니다.</p>{fileError && <p className="upload-error" role="alert">{fileError}</p>}
        </section>
        <section className="upload-panel upload-fields" aria-label="영상 정보">
          <label>제목<input name="title" maxLength={200} required defaultValue={video.title} /></label>
          <label>설명<textarea name="description" maxLength={5000} rows={5} defaultValue={video.description ?? ''} /></label>
          <label>카테고리<select aria-label="카테고리" name="categoryId" required value={categoryId} onChange={event => setCategoryId(event.target.value)}>
            {!categories.data?.some(item => item.id === video.categoryId) && <option value={video.categoryId}>{video.categoryName}</option>}
            {categories.data?.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
          </select></label>
          {categories.loading && <p className="upload-help" role="status">카테고리를 불러오는 중…</p>}
          {categories.error && <p className="upload-help" role="alert">카테고리 목록을 불러오지 못했습니다. 기존 카테고리는 유지됩니다. <button className="button" type="button" onClick={categories.retry}>다시 시도</button></p>}
          <fieldset className="upload-visibility"><legend>공개 범위</legend>{[['PUBLIC', '전체 공개'], ['UNLISTED', '링크 공개'], ['PRIVATE', '비공개']].map(([value, label]) => <label key={value}><input type="radio" name="visibility" value={value} defaultChecked={video.visibility === value} />{label}</label>)}</fieldset>
        </section>
      </div>
      {error && <p className="upload-error" role="alert">{error}</p>}
      <div className="edit-actions"><button className="button primary" type="submit">{busy ? '저장 중…' : '저장'}</button><button className="button" type="button" onClick={() => { window.location.hash = `/videos/${video.videoId}` }}>취소</button></div>
    </fieldset></form>
  </main>
}
