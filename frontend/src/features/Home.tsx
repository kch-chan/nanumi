// flex-1 은 HomeLayout 이 만든 세로 flex 안에서 남은 높이를 차지하라는 뜻임
// 전에는 min-h-[calc(100vh-4rem)] 로 헤더 높이(h-16)를 손으로 빼고 있었음
function Home() {
  return (
    <main className="mx-auto flex w-full max-w-4xl flex-1 items-center justify-center px-4">
      <p className="text-stone-400">빈 내용</p>
    </main>
  );
}

export default Home;
