import { Link } from 'react-router-dom';
import LoginForm from '../features/auth/LoginForm';

function LoginPage() {
  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="text-xl font-semibold text-stone-900">로그인</h2>
        <p className="mt-1 text-sm text-stone-500">
          가입한 이메일로 로그인하세요.
        </p>
      </div>

      <LoginForm />

      <p className="text-center text-sm text-stone-500">
        아직 회원이 아니신가요?{' '}
        <Link
          to="/signup"
          className="font-medium text-emerald-600 hover:underline"
        >
          회원가입
        </Link>
      </p>
    </div>
  );
}

export default LoginPage;
