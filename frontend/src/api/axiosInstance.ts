import axios from 'axios';
import { useAuthStore } from '../stores/authStore';

// 공통 axios 인스턴스. baseURL '/api' 는 vite dev 서버 프록시로 백엔드(8080)에 전달된다.
const axiosInstance = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

// 요청 인터셉터: 로그인 상태면 accessToken 을 Authorization 헤더에 주입
axiosInstance.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default axiosInstance;
