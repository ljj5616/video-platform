import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { useApi } from '../api/videos'
import type { Category } from '../api/videos'
import { useSession } from '../auth/session'
import { thumbnailCandidates, uploadVideo, validateFile } from './upload'
import type { UploadProgress, UploadResult } from './upload'
import './upload.css'

function Preview({ file }: { file: File }) {
  const image = useRef<HTMLImageElement>(null)
  useEffect(() => {
    const value = URL.createObjectURL(file)
    if (image.current) image.current.src = value
    return () => URL.revokeObjectURL(value)
  }, [file])
  return <img ref={image} alt="썸네일 미리보기" />
}

export function VideoUpload({ onUploaded }: { onUploaded: () => void }) {
  const session = useSession()
  const categories = useApi<Category[]>('/api/v1/categories')
  const [video, setVideo] = useState<File>()
  const [thumbnail, setThumbnail] = useState<File>()
  const [candidates, setCandidates] = useState<File[]>([])
  const [generating, setGenerating] = useState(false)
  const [thumbnailMessage, setThumbnailMessage] = useState('영상을 선택하면 썸네일 후보를 생성합니다.')
  const [error, setError] = useState('')
  const [dragging, setDragging] = useState(false)
  const [progress, setProgress] = useState<UploadProgress>({ percent: 0 })
  const [busy, setBusy] = useState(false)
  const [result, setResult] = useState<UploadResult>()
  const active = useRef<ReturnType<typeof uploadVideo> | null>(null)
  const generation = useRef<AbortController | null>(null)

  useEffect(() => () => { generation.current?.abort(); active.current?.cancel(); active.current = null }, [])
  useEffect(() => {
    if (!busy) return
    const warn = (event: BeforeUnloadEvent) => { event.preventDefault(); event.returnValue = '' }
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [busy])

  async function chooseVideo(file?: File) {
    if (!file || active.current) return
    let valid: File
    try { valid = validateFile(file) } catch (cause) { setError((cause as Error).message); return }
    generation.current?.abort()
    const controller = new AbortController()
    generation.current = controller
    setVideo(valid); setThumbnail(undefined); setCandidates([]); setError(''); setGenerating(true)
    setThumbnailMessage('썸네일 후보를 생성하고 있습니다…')
    try {
      const files = await thumbnailCandidates(valid, controller.signal)
      if (!controller.signal.aborted) { setCandidates(files); setThumbnailMessage('후보를 선택하거나 이미지를 직접 업로드하세요. 선택하지 않으면 기본 썸네일을 사용합니다.') }
    } catch (cause) {
      if (!controller.signal.aborted) setThumbnailMessage((cause as Error).message)
    } finally { if (!controller.signal.aborted) setGenerating(false) }
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (active.current) return
    if (!video) { setError('업로드할 영상 파일을 선택해 주세요.'); return }
    const body = new FormData(event.currentTarget)
    const title = String(body.get('title') ?? '').trim()
    if (!title) { setError('영상 제목을 입력해 주세요.'); return }
    body.set('title', title)
    body.set('description', String(body.get('description') ?? '').trim())
    body.set('videoFile', video)
    if (thumbnail) body.set('thumbnailFile', thumbnail)
    setError(''); setProgress({ percent: 0 })
    try {
      const task = uploadVideo(body, setProgress)
      active.current = task
      setBusy(true)
      try {
        const response = await task.promise
        if (active.current !== task) return
        setResult(response); onUploaded()
      } catch (cause) {
        if (active.current !== task) return
        setError(cause instanceof Error ? cause.message : '업로드하지 못했습니다.')
      } finally {
        if (active.current === task) { active.current = null; setBusy(false) }
      }
    } catch (cause) { setError((cause as Error).message) }
  }

  if (result) return <main id="main" className="upload-status-layout"><section className="upload-panel upload-status" role="status">
    <h1>{result.status === 'PUBLISHED' ? '영상 업로드가 완료되었습니다' : result.status === 'FAILED' ? '영상 변환에 실패했습니다' : '업로드 완료! 영상 변환을 요청했습니다'}</h1>
    <p>{result.status === 'FAILED' ? '영상 상세에서 상태를 확인해 주세요.' : '변환이 완료되면 선택한 공개 범위로 시청할 수 있습니다. 페이지를 떠나도 서버의 변환 작업은 계속됩니다.'}</p>
    <div className="upload-status-actions"><a className="button primary" href={`#/videos/${result.videoId}`}>업로드한 영상 보기</a><a className="button" href="#/">홈으로 이동</a></div>
  </section></main>

  if (!session && !busy) return <main id="main" className="upload-status-layout"><section className="upload-panel upload-status"><h1>영상 업로드</h1><p role="status">영상 업로드는 로그인이 필요합니다.</p><a className="button primary" href="#/login">로그인</a></section></main>

  return <main id="main" className={busy ? 'upload-status-layout' : 'upload-layout'}>
    {busy && <section className="upload-panel upload-status" aria-live="polite"><h1>{progress.percent === 100 ? '서버에서 업로드를 접수하고 있습니다…' : '파일 업로드 중…'}</h1>
      <p className="upload-filename">파일명: {video?.name}</p><progress aria-label="영상 업로드 진행률" max={100} value={progress.percent} />
      <div className="upload-progress-label"><strong>{progress.percent}% 전송</strong><span>{progress.percent === 100 ? '서버 응답 대기 중' : progress.secondsLeft !== undefined ? `남은 시간: 약 ${progress.secondsLeft}초` : '남은 시간 계산 중…'}</span></div>
      <button className="button upload-wide" onClick={() => active.current?.cancel()}>취소</button><p className="upload-help">전송 중 페이지를 떠나면 업로드가 중단됩니다. 전송이 끝난 뒤에는 취소해도 서버 처리가 계속될 수 있습니다.</p>
    </section>}
    <form onSubmit={submit} hidden={busy}>
      <h1>영상 업로드</h1>
      <div className="upload-grid">
        <label className={`upload-drop ${dragging ? 'dragging' : ''}`} onDragOver={event => { event.preventDefault(); setDragging(true) }} onDragLeave={() => setDragging(false)} onDrop={event => {
          event.preventDefault(); setDragging(false)
          if (event.dataTransfer.files.length !== 1) { setError('영상 파일을 한 개씩 선택해 주세요.'); return }
          void chooseVideo(event.dataTransfer.files[0])
        }}>
          <input className="upload-file-input" aria-label="영상 파일" type="file" accept=".mp4,.mov,.webm" onChange={event => { void chooseVideo(event.target.files?.[0]); event.target.value = '' }} />
          <span className="upload-icon" aria-hidden="true">↥</span><strong>{video ? video.name : '영상 파일을 드래그하거나 클릭하여 선택'}</strong>
          <span>{video ? `${(video.size / 1024 ** 2).toFixed(1)}MB · 클릭하여 변경` : '최대 1GB, MP4/MOV/WebM 지원'}</span>
        </label>
        <section className="upload-panel upload-fields" aria-label="영상 정보">
          <label>제목<input name="title" maxLength={200} required placeholder="영상의 제목을 입력해 주세요" /></label>
          <label>설명<textarea name="description" maxLength={5000} rows={4} placeholder="영상에 대한 상세한 설명을 적어 주세요" /></label>
          <label>카테고리<select aria-label="카테고리" name="categoryId" required defaultValue="" disabled={categories.loading || !!categories.error}><option value="" disabled>{categories.loading ? '불러오는 중…' : '카테고리를 선택하세요'}</option>{categories.data?.map(item => <option value={item.id} key={item.id}>{item.name}</option>)}</select></label>
          {categories.error && <p role="alert">{categories.error} <button type="button" className="button" onClick={categories.retry}>다시 시도</button></p>}
          {categories.data?.length === 0 && <p role="status">등록된 카테고리가 없습니다.</p>}
          <fieldset className="upload-visibility"><legend>공개 범위</legend><label><input type="radio" name="visibility" value="PUBLIC" defaultChecked />전체 공개</label><label><input type="radio" name="visibility" value="PRIVATE" />비공개</label></fieldset>
        </section>
      </div>
      <section className="upload-panel upload-thumbnails"><h2>썸네일 설정</h2><div className="upload-thumbnail-options">
        {[0, 1, 2].map(index => <button className="upload-thumbnail" type="button" key={index} disabled={!candidates[index]} aria-pressed={!!candidates[index] && thumbnail === candidates[index]} aria-label={`자동 생성 후보 ${index + 1}`} onClick={() => setThumbnail(candidates[index])}>{candidates[index] ? <Preview file={candidates[index]} /> : <span>{generating ? '생성 중…' : `자동 생성 후보 ${index + 1}`}</span>}</button>)}
        <label className="upload-thumbnail upload-custom"><input className="upload-file-input" aria-label="썸네일 직접 업로드" type="file" accept=".jpg,.jpeg,.png,.webp" onChange={event => {
          const file = event.target.files?.[0]
          if (file) try { setThumbnail(validateFile(file, true)); setError('') } catch (cause) { setError((cause as Error).message) }
          event.target.value = ''
        }} />{thumbnail && !candidates.includes(thumbnail) ? <Preview file={thumbnail} /> : <span>＋<br />직접 업로드</span>}</label>
      </div><p className="upload-help" role="status">{thumbnailMessage} 직접 업로드: JPG·PNG·WebP, 최대 10MB.</p>
        {thumbnail && <div className="upload-selection">선택됨: {thumbnail.name} <button type="button" className="button" onClick={() => setThumbnail(undefined)}>선택 해제</button></div>}
      </section>
      {error && <p className="upload-error" role="alert">{error}</p>}
      <button className="button primary upload-wide" type="submit" disabled={categories.loading || !!categories.error || !categories.data?.length}>업로드</button>
    </form>
  </main>
}
