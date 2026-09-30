import axios, { AxiosError, AxiosHeaders } from 'axios';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import axiosInstance from '../axiosInstance';
import { AUTH_STORAGE_KEY, useAuthStore } from '../../stores/authStore';
import type { UserResponse } from '../../types/auth';

const user: UserResponse = {
  id: 1,
  nickname: '나눔이',
  aptName: '행복아파트',
  dong: '101',
  ho: '1502',
  role: 'USER',
};

// 인터셉터가 붙어 있는 인스턴스 대신, 바닥의 어댑터를 바꿔 치워서 응답을 흉내 냄
// 이렇게 하면 요청·응답 인터셉터가 실제로 도는 상태에서 확인할 수 있음
type Responder = (config: { url?: string; headers: AxiosHeaders }) => {
  status: number;
  data?: unknown;
};

function useAdapter(responder: Responder) {
  const seen: { url?: string; authorization?: unknown }[] = [];

  axiosInstance.defaults.adapter = async (config) => {
    const headers = config.headers as AxiosHeaders;
    seen.push({ url: config.url, authorization: headers.get('Authorization') });

    const result = responder({ url: config.url, headers });
    if (result.status >= 400) {
      const error = new AxiosError('실패', undefined, config as never);
      error.response = {
        data: result.data,
        status: result.status,
        statusText: '',
        headers: new AxiosHeaders(),
        config: config as never,
      };
      throw error;
    }
    return {
      data: result.data,
      status: result.status,
      statusText: 'OK',
      headers: new AxiosHeaders(),
      config: config as never,
    };
  };

  return seen;
}

describe('axiosInstance', () => {
  beforeEach(() => {
    localStorage.clear();
    useAuthStore.getState().clearAuth();
  });

  afterEach(() => {
    delete axiosInstance.defaults.adapter;
    vi.restoreAllMocks();
  });

  it('액세스 토큰이 있으면 Authorization 헤더에 붙인다', async () => {
    useAuthStore.getState().setAuth({ accessToken: 'access', refreshToken: 'refresh', user });
    const seen = useAdapter(() => ({ status: 200, data: { ok: true } }));

    await axiosInstance.get('/users/me');

    expect(seen[0].authorization).toBe('Bearer access');
  });

  it('로그인 전에는 Authorization 헤더를 붙이지 않는다', async () => {
    const seen = useAdapter(() => ({ status: 200, data: {} }));

    await axiosInstance.get('/health');

    expect(seen[0].authorization).toBeUndefined();
  });

  // 401 을 받으면 리프레시 토큰으로 새 액세스 토큰을 받아 원래 요청을 한 번 다시 보냄
  it('401 이 나면 토큰을 다시 받아 원래 요청을 한 번 더 보낸다', async () => {
    useAuthStore.getState().setAuth({ accessToken: 'old', refreshToken: 'refresh', user });

    const post = vi
      .spyOn(axios, 'post')
      .mockResolvedValue({ data: { accessToken: 'new', refreshToken: 'new-refresh' } });

    let first = true;
    const seen = useAdapter(() => {
      if (first) {
        first = false;
        return { status: 401 };
      }
      return { status: 200, data: { ok: true } };
    });

    const response = await axiosInstance.get('/users/me');

    expect(response.data).toEqual({ ok: true });
    // 개발 환경에서는 baseURL 이 /api 라서 이 값이 상대 경로와 글자까지 같음.
    // 그래서 이 단정만으로는 아래 '절대 주소' 테스트가 잡는 버그를 못 잡음
    expect(post).toHaveBeenCalledWith(`${axiosInstance.defaults.baseURL}/auth/refresh`, {
      refreshToken: 'refresh',
    });
    expect(seen[1].authorization).toBe('Bearer new');
    expect(useAuthStore.getState().accessToken).toBe('new');
    expect(useAuthStore.getState().refreshToken).toBe('new-refresh');
  });

  // 동시에 여러 요청이 401 을 받아도 리프레시는 한 번만 불러야 함
  it('여러 요청이 동시에 401 이어도 리프레시는 한 번만 부른다', async () => {
    useAuthStore.getState().setAuth({ accessToken: 'old', refreshToken: 'refresh', user });

    const post = vi
      .spyOn(axios, 'post')
      .mockResolvedValue({ data: { accessToken: 'new', refreshToken: 'new-refresh' } });

    const failed = new Set<string>();
    useAdapter((config) => {
      const key = String(config.url);
      if (!failed.has(key)) {
        failed.add(key);
        return { status: 401 };
      }
      return { status: 200, data: { url: key } };
    });

    await Promise.all([
      axiosInstance.get('/a'),
      axiosInstance.get('/b'),
      axiosInstance.get('/c'),
    ]);

    expect(post).toHaveBeenCalledTimes(1);
  });

  // 로그인 자체가 401 인데 또 리프레시를 부르면 끝없이 돌게 됨
  it('로그인·가입·리프레시 경로의 401 은 다시 시도하지 않는다', async () => {
    useAuthStore.getState().setAuth({ accessToken: 'old', refreshToken: 'refresh', user });
    const post = vi.spyOn(axios, 'post');
    useAdapter(() => ({ status: 401, data: { message: '이메일 또는 비밀번호가 올바르지 않습니다.' } }));

    await expect(axiosInstance.post('/auth/login', {})).rejects.toThrow();

    expect(post).not.toHaveBeenCalled();
    // 로그인 실패로 기존 세션을 지우면 안 됨
    expect(useAuthStore.getState().isLoggedIn).toBe(true);
  });

  it('리프레시까지 실패하면 로그아웃시키고 로그인 화면으로 보낸다', async () => {
    useAuthStore.getState().setAuth({ accessToken: 'old', refreshToken: 'refresh', user });
    vi.spyOn(axios, 'post').mockRejectedValue(new Error('리프레시 실패'));
    useAdapter(() => ({ status: 401 }));

    await expect(axiosInstance.get('/users/me')).rejects.toThrow();

    expect(useAuthStore.getState().isLoggedIn).toBe(false);
    expect(useAuthStore.getState().refreshToken).toBeNull();
  });

  it('리프레시 토큰이 아예 없으면 바로 로그아웃시킨다', async () => {
    const post = vi.spyOn(axios, 'post');
    useAdapter(() => ({ status: 401 }));

    await expect(axiosInstance.get('/users/me')).rejects.toThrow();

    expect(post).not.toHaveBeenCalled();
    expect(useAuthStore.getState().isLoggedIn).toBe(false);
  });

  // 리프레시만 baseURL 을 타지 않아서 배포 환경에서 재발급이 100% 실패한 적이 있음.
  // 요청이 백엔드가 아니라 프런트 도메인으로 가서 405 가 났고, 액세스 토큰은
  // localStorage 에 남기지 않으므로 새로고침 한 번에 로그아웃됐음.
  // 개발 환경에서는 baseURL 이 /api 라 상대 경로와 구분되지 않으므로,
  // 배포와 같은 절대 주소를 넣은 새 인스턴스로 확인함
  it('배포 설정(절대 주소)에서도 리프레시가 백엔드로 간다', async () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.example.test/api');
    vi.resetModules();

    const fresh = (await import('../axiosInstance')).default;
    const store = (await import('../../stores/authStore')).useAuthStore;
    store.getState().setAuth({ accessToken: 'old', refreshToken: 'refresh', user });

    const post = vi
      .spyOn(axios, 'post')
      .mockResolvedValue({ data: { accessToken: 'new', refreshToken: 'new-refresh' } });

    let first = true;
    fresh.defaults.adapter = async (config) => {
      if (first) {
        first = false;
        const error = new AxiosError('실패', undefined, config as never);
        error.response = {
          data: undefined,
          status: 401,
          statusText: '',
          headers: new AxiosHeaders(),
          config: config as never,
        };
        throw error;
      }
      return {
        data: { ok: true },
        status: 200,
        statusText: 'OK',
        headers: new AxiosHeaders(),
        config: config as never,
      };
    };

    await fresh.get('/users/me');

    expect(post).toHaveBeenCalledWith('https://api.example.test/api/auth/refresh', {
      refreshToken: 'refresh',
    });

    delete fresh.defaults.adapter;
    vi.unstubAllEnvs();
    vi.resetModules();
  });

  // 탭 두 개를 켜 두면 둘이 같은 리프레시 토큰으로 동시에 재발급을 부를 수 있음.
  // 서버는 쓰인 토큰 행을 지우므로, 늦게 도착한 쪽은 "없는 토큰" 이 되어 재사용 공격으로 판단되고
  // 그 계정의 모든 기기가 로그아웃됨. 그래서 부르기 직전에 저장소를 다시 읽어야 함
  it('다른 탭이 토큰을 갈아 끼웠으면 저장소의 새 토큰으로 재발급한다', async () => {
    // 이 탭의 메모리에는 옛 토큰이 들어 있음
    useAuthStore.getState().setAuth({ accessToken: 'access', refreshToken: '옛-토큰', user });

    // 다른 탭이 회전시킨 결과가 저장소에만 반영된 상태를 만듦
    localStorage.setItem(
      AUTH_STORAGE_KEY,
      JSON.stringify({ state: { refreshToken: '다른-탭이-받은-토큰', user, isLoggedIn: true }, version: 0 }),
    );

    const post = vi
      .spyOn(axios, 'post')
      .mockResolvedValue({ data: { accessToken: 'new', refreshToken: 'new-refresh' } });

    let first = true;
    useAdapter(() => {
      if (first) {
        first = false;
        return { status: 401 };
      }
      return { status: 200, data: { ok: true } };
    });

    await axiosInstance.get('/users/me');

    expect(post).toHaveBeenCalledWith('/api/auth/refresh', {
      refreshToken: '다른-탭이-받은-토큰',
    });
  });

  it('401 이 아닌 오류는 그대로 올려 보낸다', async () => {
    useAuthStore.getState().setAuth({ accessToken: 'access', refreshToken: 'refresh', user });
    const post = vi.spyOn(axios, 'post');
    useAdapter(() => ({ status: 409, data: { message: '이미 사용 중인 이메일입니다.' } }));

    await expect(axiosInstance.post('/auth/signup2', {})).rejects.toMatchObject({
      response: { status: 409 },
    });

    expect(post).not.toHaveBeenCalled();
    expect(useAuthStore.getState().isLoggedIn).toBe(true);
  });
});
