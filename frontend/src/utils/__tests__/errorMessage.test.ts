import { AxiosError, AxiosHeaders } from 'axios';
import { describe, expect, it } from 'vitest';
import { getErrorMessage } from '../errorMessage';

// 백엔드가 내려주는 오류 응답 모양을 흉내 냄
const axiosErrorWith = (data: unknown, status = 400) => {
  const error = new AxiosError('요청 실패');
  error.response = {
    data,
    status,
    statusText: '',
    headers: new AxiosHeaders(),
    config: { headers: new AxiosHeaders() },
  };
  return error;
};

describe('getErrorMessage', () => {
  it('서버가 내려준 message 를 꺼낸다', () => {
    const error = axiosErrorWith({ status: 409, message: '이미 사용 중인 이메일입니다.' }, 409);

    expect(getErrorMessage(error)).toBe('이미 사용 중인 이메일입니다.');
  });

  // 서버 형식이 갈리면(예: 스프링 기본 ProblemDetail) message 가 없을 수 있음
  it('message 가 없으면 기본 문구를 쓴다', () => {
    const error = axiosErrorWith({ title: 'Bad Request', detail: '어쩌고' });

    expect(getErrorMessage(error)).toBe('요청 처리 중 오류가 발생했습니다.');
  });

  it('응답 자체가 없으면(네트워크 끊김) 기본 문구를 쓴다', () => {
    expect(getErrorMessage(new AxiosError('Network Error'))).toBe(
      '요청 처리 중 오류가 발생했습니다.',
    );
  });

  it('axios 오류가 아니면 기본 문구를 쓴다', () => {
    expect(getErrorMessage(new Error('그냥 오류'))).toBe('요청 처리 중 오류가 발생했습니다.');
    expect(getErrorMessage(null)).toBe('요청 처리 중 오류가 발생했습니다.');
    expect(getErrorMessage('문자열')).toBe('요청 처리 중 오류가 발생했습니다.');
  });

  it('화면마다 다른 기본 문구를 줄 수 있다', () => {
    expect(getErrorMessage(new Error('x'), '로그인에 실패했습니다.')).toBe('로그인에 실패했습니다.');
  });
});
