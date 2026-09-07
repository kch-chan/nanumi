import axios from 'axios';
import type { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '../stores/authStore';
import type { TokenResponse } from '../types/auth';

const LOGIN_PATH = '/login';

// 리프레시 자체가 401 이 났을 때 또 리프레시를 부르면 끝없이 돌게 되므로 이 경로들은 건너뜀
const SKIP_REFRESH_PATHS = ['/auth/login', '/auth/signup', '/auth/refresh'];

// 한 번 다시 보내 본 요청인지 표시해 두는 칸임
interface RetriableConfig extends InternalAxiosRequestConfig {
  retried?: boolean;
}

const axiosInstance = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

axiosInstance.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 동시에 여러 요청이 401 을 받아도 리프레시는 한 번만 부르도록 진행 중인 약속을 들고 있음
let refreshPromise: Promise<string> | null = null;

async function requestNewAccessToken(): Promise<string> {
  const { refreshToken } = useAuthStore.getState();
  if (!refreshToken) {
    throw new Error('리프레시 토큰이 없음');
  }

  // 인터셉터를 타지 않도록 기본 axios 로 부름
  const { data } = await axios.post<TokenResponse>('/api/auth/refresh', {
    refreshToken,
  });

  // 서버가 리프레시 토큰도 새로 주므로(회전) 둘 다 갈아 끼움
  useAuthStore.getState().setTokens(data);
  return data.accessToken;
}

function goToLogin() {
  if (window.location.pathname !== LOGIN_PATH) {
    window.location.href = LOGIN_PATH;
  }
}

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetriableConfig | undefined;
    const url = config?.url ?? '';

    const shouldRefresh =
      error.response?.status === 401 &&
      config !== undefined &&
      !config.retried &&
      !SKIP_REFRESH_PATHS.some((path) => url.startsWith(path));

    if (!shouldRefresh) {
      return Promise.reject(error);
    }

    config.retried = true;

    try {
      refreshPromise ??= requestNewAccessToken().finally(() => {
        refreshPromise = null;
      });

      const accessToken = await refreshPromise;
      config.headers.Authorization = `Bearer ${accessToken}`;
      return await axiosInstance(config);
    } catch {
      // 리프레시 토큰까지 만료됐거나 로그아웃된 상태임. 더 해 볼 수 있는 게 없음
      useAuthStore.getState().clearAuth();
      goToLogin();
      return Promise.reject(error);
    }
  },
);

export default axiosInstance;
