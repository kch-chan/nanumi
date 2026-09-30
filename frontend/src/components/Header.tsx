import { Link, useNavigate } from 'react-router-dom';
import Button from './Button';
import { useLogout } from '../hooks/useLogout';
import { useAuthStore } from '../stores/authStore';

function Header() {
  const navigate = useNavigate();
  const { isLoggedIn, user } = useAuthStore();
  const logout = useLogout();

  const handleLogout = () => {
    logout.mutate(undefined, { onSuccess: () => navigate('/') });
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
                {user?.nickname ?? ''} 마이페이지
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
            <Link to="/login">
              <Button size="sm">로그인</Button>
            </Link>
          )}
        </nav>
      </div>
    </header>
  );
}

export default Header;
