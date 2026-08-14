import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Button from '../../components/Button';
import Input from '../../components/Input';
import Modal from '../../components/Modal';
import PasswordInput from '../../components/PasswordInput';
import { useWithdrawal } from '../../hooks/useWithdrawal';
import { useAuthStore } from '../../stores/authStore';
import { getErrorMessage } from '../../utils/errorMessage';

function MyPage() {
  const navigate = useNavigate();
  const { isLoggedIn, user } = useAuthStore();
  const withdrawal = useWithdrawal();

  const [password, setPassword] = useState('');
  const [reason, setReason] = useState('');
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);

  if (!isLoggedIn || !user) {
    return (
      <main className="mx-auto flex max-w-4xl flex-col items-center gap-3 px-4 py-16 text-center">
        <p className="text-stone-600">로그인이 필요한 페이지입니다.</p>
        <Link to="/login" className="text-sm font-medium text-emerald-700">
          로그인하러 가기
        </Link>
      </main>
    );
  }

  const handleWithdraw = () => {
    withdrawal.mutate(
      { password, reason: reason || undefined },
      { onSuccess: () => navigate('/') },
    );
  };

  return (
    <main className="mx-auto flex max-w-4xl flex-col gap-8 px-4 py-10">
      <section className="flex flex-col gap-4 rounded-2xl border border-stone-200 bg-white p-6">
        <h2 className="text-lg font-semibold text-stone-900">내 정보</h2>
        <dl className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
          <div>
            <dt className="text-stone-400">닉네임</dt>
            <dd className="text-stone-800">{user.nickname}</dd>
          </div>
          <div>
            <dt className="text-stone-400">아파트</dt>
            <dd className="text-stone-800">{user.aptName}</dd>
          </div>
          <div>
            <dt className="text-stone-400">동</dt>
            <dd className="text-stone-800">{user.dong ?? '-'}</dd>
          </div>
          <div>
            <dt className="text-stone-400">호</dt>
            <dd className="text-stone-800">{user.ho ?? '-'}</dd>
          </div>
        </dl>
      </section>

      <section className="flex flex-col gap-4 rounded-2xl border border-red-200 bg-white p-6">
        <div>
          <h2 className="text-lg font-semibold text-red-700">회원 탈퇴</h2>
          <p className="mt-1 text-sm text-stone-500">
            탈퇴 시 계정이 비활성화되며 로그인할 수 없습니다.
          </p>
        </div>

        <PasswordInput
          label="비밀번호 확인"
          placeholder="현재 비밀번호를 입력하세요"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <Input
          label="탈퇴 사유 (선택)"
          placeholder="선택 입력, 255자 이하"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
        />

        {withdrawal.isError && (
          <p className="text-sm text-red-500">
            {getErrorMessage(withdrawal.error)}
          </p>
        )}

        <Button
          variant="danger"
          disabled={!password}
          onClick={() => setIsConfirmOpen(true)}
        >
          회원 탈퇴
        </Button>
      </section>

      <Modal
        isOpen={isConfirmOpen}
        onClose={() => setIsConfirmOpen(false)}
        title="정말 탈퇴하시겠어요?"
        footer={
          <>
            <Button variant="outline" onClick={() => setIsConfirmOpen(false)}>
              취소
            </Button>
            <Button
              variant="danger"
              isLoading={withdrawal.isPending}
              onClick={handleWithdraw}
            >
              탈퇴하기
            </Button>
          </>
        }
      >
        <p className="text-sm text-stone-600">
          탈퇴한 계정은 되돌릴 수 없습니다. 계속하시겠습니까?
        </p>
      </Modal>
    </main>
  );
}

export default MyPage;
