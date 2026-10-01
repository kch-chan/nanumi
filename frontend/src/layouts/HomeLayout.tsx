import { Outlet } from 'react-router-dom';
import Header from '../components/Header';

// 세로 flex 로 둬서 본문이 남은 높이를 차지하게 함
//
// 전에는 Home.tsx 가 min-h-[calc(100vh-4rem)] 로 "화면 높이 - 헤더 높이" 를 직접 계산했음.
// 그 4rem 은 Header 의 h-16 을 손으로 옮겨 적은 값이라, 헤더 높이를 바꾸면 조용히 어긋남
// flex-1 은 남은 공간을 차지하라는 뜻이라 헤더 높이를 몰라도 됨
function HomeLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-stone-50">
      <Header />
      <div className="flex flex-1 flex-col">
        <Outlet />
      </div>
    </div>
  );
}

export default HomeLayout;
