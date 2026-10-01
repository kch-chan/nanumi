import { useRouteError } from 'react-router-dom';

// 화면을 그리는 중에 예외가 났을 때 그 자리를 대신 채움
//
// 전에는 errorElement 가 없어서, 어느 컴포넌트에서든 예외가 나면 React 가 트리 전체를
// 내려 버리고 완전히 빈 화면이 남았음. 콘솔에만 오류가 찍혀서 이용자는 "앱이 죽었다" 로만 봄
function ErrorPage() {
  const error = useRouteError();

  // 화면에는 내용을 보여 주지 않음. 스택에는 내부 구조가 드러나므로 콘솔에만 남김
  // (백엔드도 같은 이유로 server.error.include-stacktrace 를 never 로 둠)
  console.error(error);

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 bg-stone-50 px-4 text-center">
      <p className="text-stone-700">화면을 불러오는 중 문제가 생겼습니다.</p>
      <button
        type="button"
        onClick={() => window.location.reload()}
        className="text-sm font-medium text-emerald-700 hover:underline"
      >
        다시 시도
      </button>
    </main>
  );
}

export default ErrorPage;
