import { useState } from 'react';
import Button from '../../components/Button';
import Input from '../../components/Input';
import PasswordInput from '../../components/PasswordInput';
import { useLogin } from '../../hooks/useLogin';
import { useLogout } from '../../hooks/useLogout';
import { useWithdrawal } from '../../hooks/useWithdrawal';
import { useAuthStore } from '../../stores/authStore';


function SessionTestPanel() {
  const { isLoggedIn } = useAuthStore();

  const loginM = useLogin();
  const logoutM = useLogout();
  const withdrawM = useWithdrawal();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [wPassword, setWPassword] = useState('');
  const [wReason, setWReason] = useState('');

  return (
    <div className="flex flex-col gap-5 rounded-xl border border-stone-200 p-5">
      <div className="flex flex-col gap-3">
        <h4 className="text-sm font-semibold text-stone-800">
          1) 로그인 <span className="text-stone-400">POST /api/auth/login</span>
        </h4>
        <Input
          label="이메일"
          type="email"
          placeholder="example@apt.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <PasswordInput
          label="비밀번호"
          placeholder="Test1234!"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <Button
          onClick={() => loginM.mutate({ email, password })}
          isLoading={loginM.isPending}
        >
          로그인 (토큰 저장)
        </Button>
        <ResultBox
          ok={loginM.isSuccess}
          error={loginM.isError}
          data={loginM.data}
          err={loginM.error?.response?.data ?? loginM.error?.message}
        />
      </div>

      <hr className="border-stone-200" />

      <div className="flex flex-col gap-3">
        <h4 className="text-sm font-semibold text-stone-800">
          2) 로그아웃{' '}
          <span className="text-stone-400">POST /api/auth/logout</span>
        </h4>
        <p className="text-xs text-stone-500">
          로그인 후 눌러야 동작
        </p>
        <Button
          variant="outline"
          onClick={() => logoutM.mutate()}
          isLoading={logoutM.isPending}
          disabled={!isLoggedIn}
        >
          로그아웃
        </Button>
        <ResultBox
          ok={logoutM.isSuccess}
          error={logoutM.isError}
          data={logoutM.data}
          err={logoutM.error?.response?.data ?? logoutM.error?.message}
        />
      </div>

      <hr className="border-stone-200" />

      <div className="flex flex-col gap-3">
        <h4 className="text-sm font-semibold text-stone-800">
          3) 회원탈퇴{' '}
          <span className="text-stone-400">POST /api/auth/withdrawal</span>
        </h4>
        <PasswordInput
          label="비밀번호 재확인"
          placeholder="Test1234!"
          value={wPassword}
          onChange={(e) => setWPassword(e.target.value)}
        />
        <Input
          label="탈퇴 사유 (선택)"
          placeholder="선택 입력, 255자 이하"
          value={wReason}
          onChange={(e) => setWReason(e.target.value)}
        />
        <Button
          variant="danger"
          onClick={() =>
            withdrawM.mutate({
              password: wPassword,
              reason: wReason || undefined,
            })
          }
          isLoading={withdrawM.isPending}
          disabled={!isLoggedIn}
        >
          회원탈퇴
        </Button>
        <ResultBox
          ok={withdrawM.isSuccess}
          error={withdrawM.isError}
          data={withdrawM.data}
          err={withdrawM.error?.response?.data ?? withdrawM.error?.message}
        />
      </div>
    </div>
  );
}

interface ResultBoxProps {
  ok: boolean;
  error: boolean;
  data: unknown;
  err: unknown;
}

function ResultBox({ ok, error, data, err }: ResultBoxProps) {
  if (!ok && !error) return null;
  return (
    <div className="flex flex-col gap-1">
      <span
        className={`text-xs font-semibold ${
          ok ? 'text-emerald-600' : 'text-red-500'
        }`}
      >
        {ok ? '성공 응답' : '에러 응답'}
      </span>
      <pre className="max-h-48 overflow-auto rounded-lg bg-stone-900 p-3 text-xs text-stone-100">
        {JSON.stringify(ok ? data : err, null, 2)}
      </pre>
    </div>
  );
}

export default SessionTestPanel;
