import type { ReactNode } from 'react';
import { useEffect, useId, useRef } from 'react';
import { createPortal } from 'react-dom';
import { lockScroll, unlockScroll } from '../utils/scrollLock';

type ModalSize = 'sm' | 'md' | 'lg';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  children: ReactNode;
  footer?: ReactNode;
  size?: ModalSize;
  closeOnOverlayClick?: boolean;
}

const SIZE_STYLES: Record<ModalSize, string> = {
  sm: 'max-w-sm',
  md: 'max-w-md',
  lg: 'max-w-lg',
};

// 모달 안에서 Tab 으로 갈 수 있는 요소들임
// tabindex="-1" 인 것(PasswordInput 의 보기 버튼 등)은 일부러 뺌
const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

function Modal({
  isOpen,
  onClose,
  title,
  children,
  footer,
  size = 'md',
  closeOnOverlayClick = true,
}: ModalProps) {
  // 제목 id 를 고정 문자열("modal-title")로 두면 모달이 두 개 열렸을 때 중복됨
  const titleId = useId();
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isOpen) return;

    // 모달을 열기 전에 포커스가 있던 곳. 닫을 때 여기로 돌려보냄
    const previouslyFocused = document.activeElement as HTMLElement | null;

    // 숨겨진 요소는 뺌
    //
    // offsetParent 나 getClientRects() 로 보이는지 판단하지 않음.
    // 둘 다 실제 레이아웃을 봐야 알 수 있는 값이라 jsdom 에서는 언제나 "안 보임" 이 되고,
    // 그러면 포커스 트랩이 테스트 환경에서 통째로 동작하지 않음.
    // 이 모달 안에서 조건부로 사라지는 요소는 CSS 로 감추는 대신 아예 렌더링하지 않으므로
    // hidden / aria-hidden 만 걸러도 충분함
    const focusableIn = (panel: HTMLElement) =>
      Array.from(panel.querySelectorAll<HTMLElement>(FOCUSABLE)).filter(
        (element) =>
          !element.closest('[hidden]') &&
          element.closest('[aria-hidden="true"]') === null,
      );

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
        return;
      }

      if (e.key !== 'Tab') return;

      const panel = panelRef.current;
      if (!panel) return;

      // 목록을 미리 담아 두지 않고 누를 때마다 다시 구함
      // 모달 안 내용이 바뀌면(단계 이동, 조건부 버튼) 미리 담은 목록은 낡은 것이 됨
      const focusable = focusableIn(panel);
      if (focusable.length === 0) {
        // 누를 것이 하나도 없으면 포커스가 모달 밖으로 나가지 않게 막기만 함
        e.preventDefault();
        return;
      }

      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      const active = document.activeElement;

      // 끝에서 Tab, 처음에서 Shift+Tab 일 때 반대쪽으로 감
      // 이게 없으면 Tab 이 모달을 빠져나가 뒤에 깔린 화면의 버튼으로 넘어감
      if (!e.shiftKey && active === last) {
        e.preventDefault();
        first.focus();
      } else if (e.shiftKey && active === first) {
        e.preventDefault();
        last.focus();
      } else if (!panel.contains(active)) {
        // 바깥에 있다가 Tab 을 누른 경우임
        e.preventDefault();
        first.focus();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    lockScroll();

    // 열면 모달 안으로 포커스를 넣음
    // 이게 없으면 키보드만 쓰는 이용자는 모달이 떴다는 것도, 어디로 가야 하는지도 알 수 없음
    const panel = panelRef.current;
    if (panel) {
      const focusable = focusableIn(panel);
      // 첫 요소가 닫기(X) 버튼인 경우가 많은데, 위험한 동작을 묻는 모달에서
      // 엉뚱한 버튼에 포커스가 가는 것보다 낫음. 필요하면 쓰는 쪽에서 autoFocus 로 지정함
      (focusable[0] ?? panel).focus();
    }

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      unlockScroll();
      // 닫을 때 원래 자리로 돌려보냄
      previouslyFocused?.focus?.();
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      role="dialog"
      // 이 밖은 보지 않아도 된다는 표시임. 없으면 스크린 리더가 뒤에 깔린 내용도 함께 읽음
      aria-modal="true"
      // 제목을 모달의 이름으로 알려 줌. 없으면 그냥 "대화상자" 로만 들림
      aria-labelledby={title ? titleId : undefined}
    >
      <div
        data-testid="modal-overlay"
        className="absolute inset-0 bg-stone-900/40 animate-[overlay-in_150ms_ease-out]"
        onClick={closeOnOverlayClick ? onClose : undefined}
      />

      <div
        ref={panelRef}
        // 안에 누를 것이 하나도 없을 때 포커스를 받을 자리임
        tabIndex={-1}
        className={`
          relative w-full ${SIZE_STYLES[size]}
          rounded-xl bg-white shadow-xl
          animate-[modal-in_150ms_ease-out]
          focus:outline-none
        `.trim()}
      >
        <div className="flex items-center justify-between border-b border-stone-100 px-5 py-4">
          {title && (
            <h2 id={titleId} className="text-base font-semibold text-stone-900">
              {title}
            </h2>
          )}
          <button
            type="button"
            onClick={onClose}
            aria-label="닫기"
            className="ml-auto rounded-md p-1 text-stone-400 hover:bg-stone-100 hover:text-stone-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
          >
            <svg
              className="h-5 w-5"
              aria-hidden
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <div className="px-5 py-4">{children}</div>

        {footer && (
          <div className="flex justify-end gap-2 border-t border-stone-100 px-5 py-4">
            {footer}
          </div>
        )}
      </div>
    </div>,
    document.body,
  );
}

export default Modal;
