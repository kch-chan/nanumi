import { Link } from 'react-router-dom';
import Button from '../../../components/Button';

interface StepCompleteProps {
  nickname: string;
}

function StepComplete({ nickname }: StepCompleteProps) {
  return (
    <div className="flex flex-col items-center gap-6 py-4 text-center">
      <div className="flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100">
        <svg
          className="h-8 w-8 text-emerald-600"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M5 13l4 4L19 7"
          />
        </svg>
      </div>

      <div>
        <h2 className="text-xl font-semibold text-stone-900">회원가입 완료</h2>
        <p className="mt-2 text-sm text-stone-500">
          <span className="font-medium text-emerald-600">{nickname}</span>님,
          환영합니다!
          <br />
          이제 로그인 후 나눔을 시작해 보세요.
        </p>
      </div>

      <Link to="/login" className="w-full">
        <Button fullWidth>로그인하러 가기</Button>
      </Link>
    </div>
  );
}

export default StepComplete;
