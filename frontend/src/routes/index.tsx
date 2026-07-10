// routes/index.tsx
import {
  createBrowserRouter,
  createRoutesFromElements,
  Route,
} from 'react-router-dom';
import TestPage from '../pages/TestPage';
import HomeLayout from '../layouts/HomeLayout';
import HomePage from '../pages/HomePage';

export const router = createBrowserRouter(
  createRoutesFromElements(
    <>
      <Route path="/" element={<HomeLayout />}>
        <Route index element={<HomePage />} />
      </Route>

      <Route path="/test" element={<TestPage />} />
    </>,
  ),
);
