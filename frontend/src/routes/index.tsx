// routes/index.tsx
import {
  createBrowserRouter,
  createRoutesFromElements,
  Route,
} from 'react-router-dom';
import TestPage from '../pages/TestPage';
import HomeLayout from '../layouts/HomeLayout';
import HomePage from '../pages/HomePage';
import MyPagePage from '../pages/MyPagePage';
import AuthLayout from '../layouts/AuthLayout';
import LoginPage from '../pages/LoginPage';
import SignupPage from '../pages/SignupPage';
import NotFoundPage from '../pages/NotFoundPage';
import ErrorPage from '../pages/ErrorPage';

export const router = createBrowserRouter(
  createRoutesFromElements(
    // errorElement 는 그 라우트 아래에서 렌더링 중 예외가 났을 때 그 자리를 대신 채움
    // 없으면 React 가 트리 전체를 내려서 하얀 화면만 남음
    <>
      <Route path="/" element={<HomeLayout />} errorElement={<ErrorPage />}>
        <Route index element={<HomePage />} />
        <Route path="mypage" element={<MyPagePage />} />
      </Route>

      <Route element={<AuthLayout />} errorElement={<ErrorPage />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
      </Route>

      {/* API 를 임의로 호출하는 확인용 화면임. 배포 빌드에는 넣지 않음 */}
      {import.meta.env.DEV && <Route path="/test" element={<TestPage />} />}

      {/* 위에 걸리지 않은 모든 경로. 반드시 마지막에 있어야 함 */}
      <Route path="*" element={<NotFoundPage />} />
    </>,
  ),
);
