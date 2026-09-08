import { useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

const categories = ['전체', '음악', '게임', '교육', '요리', '여행', '기술', '일상']
const titles = [
  ['2024년 꼭 알아야 할 인공지능 트렌드 대정리', '테크 인사이트', '기술'],
  ['초보자를 위한 10분 완성 에스프레소 가이드', '커피 노트', '요리'],
  ['가장 현실적인 유럽 배낭여행 꿀팁 TOP 5', '여행의 기록', '여행'],
  ['하루 15분 완성 코어 강화 필라테스 루틴', '헬스데이', '일상'],
  ['초보 개발자를 위한 깃허브 실전 사용법 총정리', '코딩 마스터', '기술'],
  ['요즘 유행하는 캠핑 용품 내돈내산 솔직 리뷰', '아웃도어 라이프', '여행'],
  ['독서가 쉬워지는 미니멀 독서법과 책 추천', '지식의 샘', '교육'],
  ['직장인을 위한 초간단 5분 아침식사 레시피', '요리왕김셰프', '요리'],
  ['기타 솔로 초보자 전용 쉬운 연습곡 추천', '음악창고', '음악'],
  ['고양이의 마음을 읽는 행동 분석 백과사전', '펫케어 TV', '일상'],
  ['도심 속 힐링 공간, 서울 숨은 북카페 탐방기', '감성골목', '일상'],
  ['수익형 블로그 개설부터 첫 수익까지의 로드맵', '머니클래스', '교육'],
  ['세계에서 가장 아름다운 국립공원 자연 다큐멘터리', '에코뷰', '여행'],
  ['아이패드 프로 생산성 극대화 앱 추천 & 세팅', '디지털 라이프', '기술'],
  ['처음 시작하는 아늑한 농장 게임 가이드', '슬로우 플레이', '게임'],
]
// 화면 확인용 데이터입니다. API 연결 시 서버 응답으로 교체합니다.
const videos = titles.map(([title, author, category], index) => ({
  id: index + 1, title, author, category,
  views: [82000, 34000, 67000, 23000, 11000, 5600, 9200, 12000, 3400, 42000, 7800, 31000, 150000, 25000, 18000][index],
  duration: ['18:24', '10:08', '15:32', '15:02', '24:16'][index % 5],
  published: index < 3 ? index : 18 - index,
}))
type Video = typeof videos[number]
const viewFormatter = new Intl.NumberFormat('ko-KR', { notation: 'compact', maximumFractionDigits: 1 })

function VideoCard({ video }: { video: Video }) {
  return <article className="video-card">
    <div className="thumbnail" aria-label={video.title + ' 썸네일 준비 중'}>
      <span className="play-symbol" aria-hidden="true">▶</span>
      <span className="duration">{video.duration}</span>
    </div>
    <h3>{video.title}</h3>
    <p className="author">{video.author}</p>
    <p className="views">조회수 {viewFormatter.format(video.views)}회</p>
  </article>
}

function App() {
  const [input, setInput] = useState('')
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('전체')
  const [sort, setSort] = useState('latest')
  const [page, setPage] = useState(1)
  const [notice, setNotice] = useState('')
  const filtered = videos.filter(video =>
    (category === '전체' || video.category === category) &&
    (video.title + ' ' + video.author).toLowerCase().includes(query.toLowerCase()),
  ).sort((a, b) => sort === 'popular' ? b.views - a.views : b.published - a.published)
  const pageCount = Math.ceil(filtered.length / 12)

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
        <div className="video-grid recommended">{videos.slice(0, 3).map(video => <VideoCard key={video.id} video={video} />)}</div>
      </section>
      <section className="browse" aria-labelledby="browse-heading">
        <h2 id="browse-heading" className="section-heading">영상 탐색</h2>
        <div className="toolbar">
          <div className="categories" role="group" aria-label="카테고리">
            {categories.map(item => <button key={item} className={category === item ? 'category selected' : 'category'} aria-pressed={category === item} onClick={() => { setCategory(item); setPage(1) }}>{item}</button>)}
          </div>
          <select aria-label="영상 정렬" value={sort} onChange={event => { setSort(event.target.value); setPage(1) }}><option value="latest">최신순</option><option value="popular">조회수순</option></select>
        </div>
        {query && <div className="search-summary" role="status">“{query}” 검색 결과 {filtered.length}개 <button onClick={() => { setQuery(''); setInput(''); setPage(1) }}>검색 해제</button></div>}
        {filtered.length > 0 ? <div className="video-grid">{filtered.slice((page - 1) * 12, page * 12).map(video => <VideoCard key={video.id} video={video} />)}</div> :
          <div className="empty-state"><h3>검색 결과가 없습니다</h3><p>다른 검색어나 카테고리로 영상을 찾아보세요.</p><button className="button" onClick={() => { setInput(''); setQuery(''); setCategory('전체'); setPage(1) }}>전체 영상 보기</button></div>}
        {pageCount > 1 && <nav className="pagination" aria-label="영상 목록 페이지">
          <button aria-label="이전 페이지" disabled={page === 1} onClick={() => setPage(page - 1)}>‹</button>
          {Array.from({ length: pageCount }, (_, index) => index + 1).map(number => <button key={number} aria-label={number + '페이지'} aria-current={page === number ? 'page' : undefined} onClick={() => setPage(number)}>{number}</button>)}
          <button aria-label="다음 페이지" disabled={page === pageCount} onClick={() => setPage(page + 1)}>›</button>
        </nav>}
      </section>
    </main>
  </>
}

export default App
