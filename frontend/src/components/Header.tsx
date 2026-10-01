import { Link, useNavigate } from 'react-router-dom';
import Button from './Button';
import { buttonClassName } from './buttonStyles';
import { useLogout } from '../hooks/useLogout';
import { useAuthStore } from '../stores/authStore';

function Header() {
  const navigate = useNavigate();
  const { isLoggedIn, user } = useAuthStore();
  const logout = useLogout();

  // onSuccess 가 아니라 onSettled 임
  // useLogout 은 서버가 실패해도 로컬 상태를 비움(onSettled). 그런데 이동을 onSuccess 에 두면
  // "로그아웃은 됐는데 화면은 그대로" 가 되어 둘이 어긋남
  const handleLogout = () => {
    logout.mutate(undefined, { onSettled: () => navigate('/') });
  };

  return (
    <header className="border-b border-stone-200 bg-white">
      <div className="mx-auto flex h-16 max-w-4xl items-center justify-between px-4">
        <Link to="/" className="text-lg font-bold text-emerald-700">
          나누미
        </Link>

        <nav className="flex items-center gap-2">
          {isLoggedIn ? (
            <>
              <Link
                to="/mypage"
                className="rounded-lg px-3 py-2 text-sm font-medium text-stone-700 hover:bg-stone-100"
              >
                {/* 닉네임이 없을 때 "{빈칸} 마이페이지" 가 되지 않게 분기함 */}
                {user ? `${user.nickname} 마이페이지` : '마이페이지'}
              </Link>
              <Button
                variant="outline"
                size="sm"
                onClick={handleLogout}
                isLoading={logout.isPending}
              >
                로그아웃
              </Button>
            </>
          ) : (
            // <Link><Button> 로 감싸면 <a> 안에 <button> 이 들어가 HTML 명세 위반이 됨
            // 링크를 버튼 모양으로 꾸며서 요소 하나로 둠(새 탭으로 열기도 그대로 동작함)
            <Link to="/login" className={buttonClassName({ size: 'sm' })}>
              로그인
            </Link>
          )}
        </nav>
      </div>
    </header>
  );
}

export default Header;
