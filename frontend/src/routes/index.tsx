// routes/index.tsx
import {
  createBrowserRouter,
  createRoutesFromElements,
  Route,
} from 'react-router-dom';
import TestPage from '../pages/TestPage';
import HomeLayout from '../layouts/HomeLayout';
import HomePage from '../pages/HomePage';
import AuthLayout from '../layouts/AuthLayout';
import LoginPage from '../pages/LoginPage';
import SignupPage from '../pages/SignupPage';

export const router = createBrowserRouter(
  createRoutesFromElements(
    <>
      <Route path="/" element={<HomeLayout />}>
        <Route index element={<HomePage />} />
      </Route>

      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
      </Route>

      <Route path="/test" element={<TestPage />} />
    </>,
  ),
);
