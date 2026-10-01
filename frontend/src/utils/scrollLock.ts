// 모달이 열려 있는 동안 뒤 페이지가 스크롤되지 않게 막음
//
// 왜 모듈로 뺐는가: 전에는 Modal 이 직접 document.body.style.overflow 를 넣고 뺐음.
// 그러면 모달을 겹쳐 열었을 때 깨짐
//   모달 A 열기 -> overflow: hidden
//   모달 B 열기 -> overflow: hidden
//   모달 B 닫기 -> overflow: ''   <- A 가 아직 열려 있는데 스크롤이 풀림
//
// 지금은 몇 개가 열려 있는지 세고, 0 이 될 때만 되돌림

let lockCount = 0;

// 잠그기 전의 값을 기억해 둠
// 빈 문자열로 되돌리는 것과 "원래 있던 인라인 스타일로 되돌리는 것" 은 다름
let previousOverflow = '';

export function lockScroll(): void {
  if (lockCount === 0) {
    previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
  }
  lockCount += 1;
}

export function unlockScroll(): void {
  // 음수로 내려가면 다음 lock 이 previousOverflow 를 'hidden' 으로 덮어씀
  lockCount = Math.max(0, lockCount - 1);
  if (lockCount === 0) {
    document.body.style.overflow = previousOverflow;
  }
}

// 테스트에서 상태를 초기화할 때만 씀
export function resetScrollLock(): void {
  lockCount = 0;
  previousOverflow = '';
  document.body.style.overflow = '';
}
