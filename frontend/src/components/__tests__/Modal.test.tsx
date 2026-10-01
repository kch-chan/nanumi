import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import Modal from '../Modal';
import { resetScrollLock } from '../../utils/scrollLock';

// 모달은 눈으로 보면 아는 부분(색, 둥근 정도)과 눈으로는 모르는 부분이 섞여 있음
// 여기서는 뒤쪽만 확인함: 닫히는 세 경로, 스크롤 락, 포커스, 접근성 속성
//
// @testing-library/user-event 를 쓰지 않고 fireEvent 로만 씀
// 의존성을 더 넣지 않고도 키 입력과 클릭을 흉내 낼 수 있기 때문임
describe('Modal', () => {
  beforeEach(() => {
    resetScrollLock();
  });

  afterEach(() => {
    // vitest 설정이 globals: true 가 아니라서 테스트 라이브러리의 자동 정리가 등록되지 않음
    // 모달은 createPortal 로 document.body 에 붙으므로, 정리하지 않으면 앞 테스트의 모달이
    // 그대로 남아 다음 테스트에서 "같은 것이 두 개 보인다" 로 실패함
    cleanup();
    resetScrollLock();
    vi.restoreAllMocks();
  });

  it('isOpen 이 false 면 아무것도 그리지 않는다', () => {
    render(
      <Modal isOpen={false} onClose={() => {}}>
        내용
      </Modal>,
    );

    expect(screen.queryByText('내용')).toBeNull();
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('isOpen 이 true 면 제목과 내용을 그린다', () => {
    render(
      <Modal isOpen onClose={() => {}} title="제목">
        내용
      </Modal>,
    );

    expect(screen.getByRole('dialog')).toBeTruthy();
    expect(screen.getByText('제목')).toBeTruthy();
    expect(screen.getByText('내용')).toBeTruthy();
  });

  // createPortal 로 body 직속에 그림
  // 부모에 overflow:hidden 이나 transform 이 있으면 모달이 잘리거나 가려지기 때문임
  it('모달을 body 직속에 붙인다', () => {
    const { container } = render(
      <Modal isOpen onClose={() => {}}>
        내용
      </Modal>,
    );

    const dialog = screen.getByRole('dialog');
    expect(dialog.parentElement).toBe(document.body);
    // 렌더링한 컴포넌트가 들어가는 자리에는 남지 않음
    expect(container.contains(dialog)).toBe(false);
  });

  describe('접근성 속성', () => {
    it('aria-modal 과 aria-labelledby 를 붙인다', () => {
      render(
        <Modal isOpen onClose={() => {}} title="회원탈퇴">
          내용
        </Modal>,
      );

      const dialog = screen.getByRole('dialog');
      expect(dialog.getAttribute('aria-modal')).toBe('true');

      // 제목 요소의 id 와 짝이 맞아야 "회원탈퇴 대화상자" 로 읽힘
      const labelledBy = dialog.getAttribute('aria-labelledby');
      expect(labelledBy).toBeTruthy();
      expect(document.getElementById(labelledBy as string)?.textContent).toBe(
        '회원탈퇴',
      );
    });

    // 고정 문자열 id 를 쓰면 모달이 두 개 열렸을 때 중복됨
    it('모달이 두 개여도 제목 id 가 겹치지 않는다', () => {
      render(
        <>
          <Modal isOpen onClose={() => {}} title="첫째">
            내용 1
          </Modal>
          <Modal isOpen onClose={() => {}} title="둘째">
            내용 2
          </Modal>
        </>,
      );

      const ids = screen
        .getAllByRole('dialog')
        .map((dialog) => dialog.getAttribute('aria-labelledby'));

      expect(ids).toHaveLength(2);
      expect(new Set(ids).size).toBe(2);
    });

    it('제목이 없으면 aria-labelledby 를 붙이지 않는다', () => {
      render(
        <Modal isOpen onClose={() => {}}>
          내용
        </Modal>,
      );

      expect(screen.getByRole('dialog').getAttribute('aria-labelledby')).toBeNull();
    });

    // SVG 는 읽히지 않으므로 이게 없으면 그냥 "버튼" 으로만 들림
    it('닫기 버튼에 이름을 준다', () => {
      render(
        <Modal isOpen onClose={() => {}}>
          내용
        </Modal>,
      );

      expect(screen.getByRole('button', { name: '닫기' })).toBeTruthy();
    });
  });

  describe('닫히는 경로', () => {
    it('Escape 키를 누르면 onClose 를 부른다', () => {
      const onClose = vi.fn();
      render(
        <Modal isOpen onClose={onClose}>
          내용
        </Modal>,
      );

      fireEvent.keyDown(document, { key: 'Escape' });

      expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('닫기 버튼을 누르면 onClose 를 부른다', () => {
      const onClose = vi.fn();
      render(
        <Modal isOpen onClose={onClose}>
          내용
        </Modal>,
      );

      fireEvent.click(screen.getByRole('button', { name: '닫기' }));

      expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('오버레이를 누르면 onClose 를 부른다', () => {
      const onClose = vi.fn();
      render(
        <Modal isOpen onClose={onClose}>
          내용
        </Modal>,
      );

      fireEvent.click(screen.getByTestId('modal-overlay'));

      expect(onClose).toHaveBeenCalledTimes(1);
    });

    // 오버레이를 모달의 부모가 아니라 형제로 둔 이유임
    // 부모로 두면 모달 안을 클릭해도 버블링으로 닫혀서 stopPropagation 을 써야 함
    it('모달 안을 누르면 닫히지 않는다', () => {
      const onClose = vi.fn();
      render(
        <Modal isOpen onClose={onClose}>
          <p>내용</p>
        </Modal>,
      );

      fireEvent.click(screen.getByText('내용'));

      expect(onClose).not.toHaveBeenCalled();
    });

    it('closeOnOverlayClick 이 false 면 오버레이를 눌러도 닫히지 않는다', () => {
      const onClose = vi.fn();
      render(
        <Modal isOpen onClose={onClose} closeOnOverlayClick={false}>
          내용
        </Modal>,
      );

      fireEvent.click(screen.getByTestId('modal-overlay'));

      expect(onClose).not.toHaveBeenCalled();
    });
  });

  describe('스크롤 락', () => {
    it('열려 있는 동안 body 스크롤을 막고 닫으면 되돌린다', () => {
      const { unmount } = render(
        <Modal isOpen onClose={() => {}}>
          내용
        </Modal>,
      );

      expect(document.body.style.overflow).toBe('hidden');

      unmount();

      expect(document.body.style.overflow).toBe('');
    });

    // 전에는 Modal 이 직접 overflow 를 넣고 뺐음
    // 그러면 겹쳐 열었을 때 안쪽을 닫는 순간 바깥 모달이 열려 있는데도 스크롤이 풀렸음
    it('모달을 겹쳐 열면 안쪽을 닫아도 스크롤이 풀리지 않는다', () => {
      const outer = render(
        <Modal isOpen onClose={() => {}}>
          바깥
        </Modal>,
      );
      const inner = render(
        <Modal isOpen onClose={() => {}}>
          안쪽
        </Modal>,
      );

      expect(document.body.style.overflow).toBe('hidden');

      inner.unmount();
      expect(document.body.style.overflow).toBe('hidden');

      outer.unmount();
      expect(document.body.style.overflow).toBe('');
    });
  });

  describe('포커스', () => {
    // 열렸는데 포커스가 바깥에 남아 있으면 키보드만 쓰는 이용자는 모달을 찾을 수 없음
    it('열면 모달 안으로 포커스를 넣는다', () => {
      render(
        <Modal isOpen onClose={() => {}}>
          <button type="button">확인</button>
        </Modal>,
      );

      const dialog = screen.getByRole('dialog');
      expect(dialog.contains(document.activeElement)).toBe(true);
    });

    it('닫으면 열기 전에 포커스가 있던 곳으로 돌려보낸다', () => {
      const opener = document.createElement('button');
      opener.textContent = '열기';
      document.body.appendChild(opener);
      opener.focus();
      expect(document.activeElement).toBe(opener);

      const { unmount } = render(
        <Modal isOpen onClose={() => {}}>
          <button type="button">확인</button>
        </Modal>,
      );
      expect(document.activeElement).not.toBe(opener);

      unmount();

      expect(document.activeElement).toBe(opener);
      opener.remove();
    });

    // Tab 이 모달을 빠져나가면 뒤에 깔린 화면의 버튼으로 넘어감
    it('마지막 요소에서 Tab 을 누르면 첫 요소로 돌아온다', () => {
      render(
        <Modal isOpen onClose={() => {}} title="제목">
          <button type="button">확인</button>
        </Modal>,
      );

      const dialog = screen.getByRole('dialog');
      const closeButton = screen.getByRole('button', { name: '닫기' });
      const confirmButton = screen.getByRole('button', { name: '확인' });

      confirmButton.focus();
      expect(document.activeElement).toBe(confirmButton);

      fireEvent.keyDown(document, { key: 'Tab' });

      expect(document.activeElement).toBe(closeButton);
      expect(dialog.contains(document.activeElement)).toBe(true);
    });

    it('첫 요소에서 Shift+Tab 을 누르면 마지막 요소로 간다', () => {
      render(
        <Modal isOpen onClose={() => {}} title="제목">
          <button type="button">확인</button>
        </Modal>,
      );

      const closeButton = screen.getByRole('button', { name: '닫기' });
      const confirmButton = screen.getByRole('button', { name: '확인' });

      closeButton.focus();

      fireEvent.keyDown(document, { key: 'Tab', shiftKey: true });

      expect(document.activeElement).toBe(confirmButton);
    });
  });
});
