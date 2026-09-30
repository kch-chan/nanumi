import { describe, expect, it } from 'vitest';
import { loginSchema, signupSchema } from '../authSchema';

// 가입 폼의 기본값. 각 테스트에서 검사할 칸만 바꿔 씀
const validSignup = {
  email: 'nanumi@example.com',
  password: 'Ab3!efgh',
  passwordConfirm: 'Ab3!efgh',
  nickname: '나눔이',
  aptName: '행복아파트',
  dong: '101',
  ho: '1502',
};

// 어느 칸에서 어떤 메시지가 나왔는지 꺼내는 도우미
const errorsOf = (input: Record<string, unknown>) => {
  const result = signupSchema.safeParse(input);
  if (result.success) return {};
  return result.error.issues.reduce<Record<string, string>>((acc, issue) => {
    acc[issue.path.join('.')] = issue.message;
    return acc;
  }, {});
};

describe('signupSchema', () => {
  it('모든 칸이 올바르면 통과한다', () => {
    expect(signupSchema.safeParse(validSignup).success).toBe(true);
  });

  it('동·호는 비워도 통과한다', () => {
    const rest = { ...validSignup, dong: undefined, ho: undefined };
    expect(signupSchema.safeParse(rest).success).toBe(true);
  });

  // 백엔드 PasswordValidator 와 같은 8~20자 규칙이어야 함
  // 프런트가 더 느슨하면 가입 버튼을 눌러야 서버가 막는 꼴이 됨
  it.each([
    ['Ab3!efg', '8자 미만'],
    [`Ab3!${'a'.repeat(17)}`, '20자 초과'],
  ])('비밀번호가 %s 이면 길이를 알려 준다 (%s)', (password) => {
    expect(errorsOf({ ...validSignup, password, passwordConfirm: password }).password).toBe(
      '비밀번호는 8~20자여야 합니다.',
    );
  });

  it('딱 20자 비밀번호는 통과한다', () => {
    const password = `Ab3!${'a'.repeat(16)}`;
    expect(errorsOf({ ...validSignup, password, passwordConfirm: password })).toEqual({});
  });

  it('영문·숫자·특수문자 중 하나라도 빠지면 알려 준다', () => {
    expect(errorsOf({ ...validSignup, password: 'abcdefgh!', passwordConfirm: 'abcdefgh!' }).password).toBe(
      '비밀번호는 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.',
    );
  });

  it('비밀번호 확인이 다르면 확인 칸에서 알려 준다', () => {
    expect(errorsOf({ ...validSignup, passwordConfirm: 'Ab3!efgi' }).passwordConfirm).toBe(
      '비밀번호가 일치하지 않습니다.',
    );
  });

  it('이메일 형식이 아니면 알려 준다', () => {
    expect(errorsOf({ ...validSignup, email: 'nanumi.example.com' }).email).toBeTruthy();
  });

  it('닉네임이 1자면 알려 준다', () => {
    expect(errorsOf({ ...validSignup, nickname: '나' }).nickname).toBe(
      '닉네임은 한글/영문/숫자 2~10자여야 합니다.',
    );
  });
});

describe('loginSchema', () => {
  // 로그인은 형식을 따지지 않음. 예전 규칙으로 가입한 회원도 들어와야 하고,
  // 형식을 알려 주면 공격자에게 규칙을 알려 주는 셈이 됨
  it('형식이 어긋난 값이어도 비어 있지만 않으면 통과한다', () => {
    expect(loginSchema.safeParse({ email: '옛날계정', password: 'a' }).success).toBe(true);
  });

  it('비어 있으면 알려 준다', () => {
    const result = loginSchema.safeParse({ email: '', password: '' });
    expect(result.success).toBe(false);
  });
});
