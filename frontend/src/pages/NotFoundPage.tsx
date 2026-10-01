import { Link } from 'react-router-dom';

// 없는 경로로 들어왔을 때 보여 줄 화면임
//
// 전에는 path="*" 라우트가 없어서 react-router 가 "No routes matched location" 경고만 남기고
// 아무것도 그리지 않았음. 이용자에게는 하얀 화면으로 보임
function NotFoundPage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 bg-stone-50 px-4 text-center">
      <p className="text-5xl font-bold text-stone-300">404</p>
      <p className="text-stone-600">찾으시는 화면이 없습니다.</p>
      <Link
        to="/"
        className="text-sm font-medium text-emerald-700 hover:underline"
      >
        처음으로 돌아가기
      </Link>
    </main>
  );
}

export default NotFoundPage;
