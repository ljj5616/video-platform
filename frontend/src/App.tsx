import { useState } from 'react'
import type { FormEvent } from 'react'
import { useApi, fromSearch, fromRecommendation, formatDuration } from './api/videos'
import type { Video, SearchPage, RecommendationPage, Category } from './api/videos'
import './App.css'

const viewFormatter = new Intl.NumberFormat('ko-KR', { notation: 'compact', maximumFractionDigits: 1 })

function VideoCard({ video }: { video: Video }) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null)
  return <article className="video-card">
    <div className="thumbnail" aria-label={video.title + ' 썸네일'}>
      {video.thumbnailUrl && failedUrl !== video.thumbnailUrl ? <img src={video.thumbnailUrl} alt="" loading="lazy" onError={() => setFailedUrl(video.thumbnailUrl)} /> : <span className="play-symbol" aria-hidden="true">▶</span>}
      {video.duration != null && <span className="duration">{formatDuration(video.duration)}</span>}
    </div>
    <h3>{video.title}</h3>
    <p className="author">{video.author}</p>
    <p className="views">조회수 {viewFormatter.format(video.views)}회</p>
  </article>
}

function App() {
  const [input, setInput] = useState('')
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('')
  const [sort, setSort] = useState('latest')
  const [page, setPage] = useState(1)
  const [notice, setNotice] = useState('')
  const params = new URLSearchParams({ sort: sort.toUpperCase(), page: String(page - 1), size: '12' })
  if (query) params.set('keyword', query)
  if (category) params.set('categoryId', category)
  const listing = useApi<SearchPage>('/api/v1/videos?' + params)
  const recommendations = useApi<RecommendationPage>('/api/v1/videos/recommendations?page=0&size=3')
  const categoryList = useApi<Category[]>('/api/v1/categories')
  const filtered = listing.data?.content.map(fromSearch) ?? []
  const pageCount = listing.data?.totalPages ?? 0
  const categories = [{ id: '', name: '전체' }, ...(categoryList.data ?? []).map(item => ({ id: String(item.id), name: item.name }))]
  const firstPage = Math.max(1, Math.min(page - 2, pageCount - 4))
  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setQuery(input.trim())
    setPage(1)
  }

  return <>
    <a className="skip-link" href="#main">본문으로 건너뛰기</a>
    <header className="site-header"><div className="header-inner">
      <a className="brand" href="/" aria-label="VIDSHARE 홈"><span className="brand-mark" aria-hidden="true" />VIDSHARE</a>
      <form className="search" role="search" onSubmit={search}>
        <button type="submit" aria-label="검색"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><circle cx="10" cy="10" r="6" /><path d="m15 15 5 5" /></svg></button>
        <input aria-label="영상 제목 또는 작성자 검색" placeholder="영상 제목, 작성자 검색..." value={input} onChange={event => setInput(event.target.value)} />
      </form>
      <div className="header-actions">
        <button className="button" onClick={() => setNotice('영상 업로드 화면은 준비 중입니다.')}><span aria-hidden="true">↥</span> 영상 업로드</button>
        <button className="button primary" onClick={() => setNotice('로그인 화면은 준비 중입니다.')}>로그인</button>
      </div>
    </div></header>
    {notice && <div className="notice" role="status">{notice}<button aria-label="안내 닫기" onClick={() => setNotice('')}>×</button></div>}
    <main id="main" className="main-content">
      <section aria-labelledby="recommended-heading">
        <h1 id="recommended-heading" className="section-heading">추천 영상</h1>
        {recommendations.loading ? <p className="request-state" role="status">추천 영상을 불러오는 중입니다…</p> :
          recommendations.error ? <div className="request-state" role="alert">{recommendations.error} <button className="button" onClick={recommendations.retry}>다시 시도</button></div> :
          recommendations.data?.content.length ? <div className="video-grid recommended">{recommendations.data.content.map(fromRecommendation).map(video => <VideoCard key={video.id} video={video} />)}</div> :
          <p className="request-state">아직 추천할 영상이 없습니다.</p>}
      </section>
      <section className="browse" aria-labelledby="browse-heading">
        <h2 id="browse-heading" className="section-heading">영상 탐색</h2>
        <div className="toolbar">
          <div className="categories" role="group" aria-label="카테고리">
            {categories.map(item => <button key={item.id} className={category === item.id ? 'category selected' : 'category'} aria-pressed={category === item.id} onClick={() => { setCategory(item.id); setPage(1) }}>{item.name}</button>)}
          </div>
          <select aria-label="영상 정렬" value={sort} onChange={event => { setSort(event.target.value); setPage(1) }}><option value="latest">최신순</option><option value="popular">조회수순</option></select>
        </div>
        {categoryList.error && <div className="request-state" role="alert">카테고리를 불러오지 못했습니다. <button className="button" onClick={categoryList.retry}>다시 시도</button></div>}
        {query && listing.data && <div className="search-summary" role="status">“{query}” 검색 결과 {listing.data.totalElements}개 <button onClick={() => { setQuery(''); setInput(''); setPage(1) }}>검색 해제</button></div>}
        {listing.loading ? <p className="request-state" role="status">영상을 불러오는 중입니다…</p> : listing.error ? <div className="request-state" role="alert">{listing.error} <button className="button" onClick={listing.retry}>다시 시도</button></div> : filtered.length > 0 ? <div className="video-grid">{filtered.map(video => <VideoCard key={video.id} video={video} />)}</div> :
          <div className="empty-state"><h3>{query || category ? '검색 결과가 없습니다' : '아직 등록된 영상이 없습니다'}</h3><p>다른 검색어나 카테고리로 영상을 찾아보세요.</p><button className="button" onClick={() => { setInput(''); setQuery(''); setCategory(''); setPage(1) }}>전체 영상 보기</button></div>}
        {pageCount > 1 && <nav className="pagination" aria-label="영상 목록 페이지">
          <button aria-label="이전 페이지" disabled={page === 1} onClick={() => setPage(page - 1)}>‹</button>
          {Array.from({ length: Math.min(5, pageCount) }, (_, index) => firstPage + index).map(number => <button key={number} aria-label={number + '페이지'} aria-current={page === number ? 'page' : undefined} onClick={() => setPage(number)}>{number}</button>)}
          <button aria-label="다음 페이지" disabled={page === pageCount} onClick={() => setPage(page + 1)}>›</button>
        </nav>}
      </section>
    </main>
  </>
}

export default App
