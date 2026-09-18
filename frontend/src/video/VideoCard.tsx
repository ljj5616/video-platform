import { useState } from 'react'
import { formatDuration } from '../api/videos'
import type { Video } from '../api/videos'

const viewFormatter = new Intl.NumberFormat('ko-KR', { notation: 'compact', maximumFractionDigits: 1 })

export function VideoCard({ video }: { video: Omit<Video, 'views'> & { views?: number } }) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null)
  return <article className="video-card"><a href={`#/videos/${video.id}`}>
    <div className="thumbnail" aria-label={video.title + ' 썸네일'}>
      {video.thumbnailUrl && failedUrl !== video.thumbnailUrl ? <img src={video.thumbnailUrl} alt="" loading="lazy" onError={() => setFailedUrl(video.thumbnailUrl)} /> : <span className="play-symbol" aria-hidden="true">▶</span>}
      {video.duration != null && <span className="duration">{formatDuration(video.duration)}</span>}
    </div>
    <h3>{video.title}</h3>
    <p className="author">{video.author}</p>
    {video.views !== undefined && <p className="views">조회수 {viewFormatter.format(video.views)}회</p>}
  </a></article>
}

