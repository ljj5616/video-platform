import { getSession, setSession } from '../auth/session'

export interface UploadResult { videoId: number; status: string; createdAt: string }
export interface UploadProgress { percent: number; secondsLeft?: number }

export function uploadVideo(body: FormData, progress: (value: UploadProgress) => void) {
  const session = getSession()
  if (!session) throw new Error('로그인이 만료되었습니다. 다시 로그인해 주세요.')
  const request = new XMLHttpRequest()
  const started = Date.now()
  const promise = new Promise<UploadResult>((resolve, reject) => {
    request.open('POST', '/api/v1/videos')
    request.setRequestHeader('Authorization', `Bearer ${session.accessToken}`)
    request.setRequestHeader('Accept', 'application/json')
    request.upload.onprogress = event => {
      if (!event.lengthComputable) return
      const seconds = (Date.now() - started) / 1000
      progress({ percent: Math.floor(event.loaded / event.total * 100),
        secondsLeft: event.loaded > 0 && seconds > 1 ? Math.ceil((event.total - event.loaded) * seconds / event.loaded) : undefined })
    }
    request.onload = () => {
      let result
      try { result = JSON.parse(request.responseText) } catch { /* Proxy errors can be non-JSON. */ }
      if (request.status === 401 && getSession()?.accessToken === session.accessToken) setSession(null)
      if (request.status >= 200 && request.status < 300 && Number.isSafeInteger(result?.videoId) && result.videoId > 0) resolve(result)
      else reject(new Error(result?.message ?? (request.status === 413 ? '파일 용량이 서버의 업로드 제한을 초과했습니다.' : '업로드 결과를 확인하지 못했습니다. 잠시 후 다시 확인해 주세요.')))
    }
    request.onerror = () => reject(new Error('서버 연결이 끊겼습니다. 업로드 결과를 확인한 후 다시 시도해 주세요.'))
    request.onabort = () => reject(new DOMException('업로드 전송을 취소했습니다.', 'AbortError'))
    request.send(body)
  })
  return { promise, cancel: () => request.abort() }
}

export function validateFile(file: File, thumbnail = false) {
  const extension = file.name.split('.').pop()?.toLowerCase() ?? ''
  const types: Record<string, string> = thumbnail
    ? { jpg: 'image/jpeg', jpeg: 'image/jpeg', png: 'image/png', webp: 'image/webp' }
    : { mp4: 'video/mp4', mov: 'video/quicktime', webm: 'video/webm' }
  if (!file.size || file.size > (thumbnail ? 10 * 1024 ** 2 : 1024 ** 3) || !types[extension] || (file.type && file.type !== types[extension])) {
    throw new Error(thumbnail ? '썸네일은 최대 10MB의 JPG·PNG·WebP 파일을 선택해 주세요.' : '영상은 최대 1GB의 MP4·MOV·WebM 파일을 선택해 주세요.')
  }
  return file.type ? file : new File([file], file.name, { type: types[extension] })
}

// Decode locally; no video leaves the browser until the upload button is pressed.
export async function thumbnailCandidates(file: File, signal: AbortSignal): Promise<File[]> {
  const video = document.createElement('video')
  const url = URL.createObjectURL(file)
  video.muted = true
  video.preload = 'auto'
  function wait(event: string, action: () => void) {
    return new Promise<void>((resolve, reject) => {
      const clean = () => { clearTimeout(timer); video.removeEventListener(event, ready); video.removeEventListener('error', fail); signal.removeEventListener('abort', fail) }
      const ready = () => { clean(); resolve() }
      const fail = () => { clean(); reject(new Error('썸네일을 자동 생성할 수 없습니다. 직접 이미지를 선택할 수 있습니다.')) }
      const timer = window.setTimeout(fail, 12000)
      video.addEventListener(event, ready, { once: true })
      video.addEventListener('error', fail, { once: true })
      signal.addEventListener('abort', fail, { once: true })
      if (signal.aborted) fail()
      else action()
    })
  }
  try {
    await wait('loadeddata', () => { video.src = url })
    if (!Number.isFinite(video.duration) || video.duration <= 0) throw new Error('영상 길이를 확인할 수 없습니다. 썸네일을 직접 선택해 주세요.')
    const canvas = document.createElement('canvas')
    canvas.width = Math.min(640, video.videoWidth)
    canvas.height = Math.max(1, Math.round(canvas.width * video.videoHeight / video.videoWidth))
    const context = canvas.getContext('2d')!
    const files: File[] = []
    for (const fraction of [0.2, 0.5, 0.8]) {
      await wait('seeked', () => { video.currentTime = video.duration * fraction })
      context.drawImage(video, 0, 0, canvas.width, canvas.height)
      const blob = await new Promise<Blob | null>(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.85))
      if (!blob || signal.aborted) throw new Error('썸네일 생성을 중단했습니다.')
      files.push(new File([blob], `thumbnail-${files.length + 1}.jpg`, { type: 'image/jpeg' }))
    }
    return files
  } finally {
    video.removeAttribute('src'); video.load(); URL.revokeObjectURL(url)
  }
}
