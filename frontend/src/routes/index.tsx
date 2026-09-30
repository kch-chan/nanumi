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

export const router = createBrowserRouter(
  createRoutesFromElements(
    <>
      <Route path="/" element={<HomeLayout />}>
        <Route index element={<HomePage />} />
        <Route path="mypage" element={<MyPagePage />} />
      </Route>

      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
      </Route>

      {/* API 를 임의로 호출하는 확인용 화면임. 배포 빌드에는 넣지 않음 */}
      {import.meta.env.DEV && <Route path="/test" element={<TestPage />} />}
    </>,
  ),
);
