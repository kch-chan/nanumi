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

  it('딱 8자 비밀번호는 통과한다', () => {
    const password = 'Ab3!efgh';

    expect(password).toHaveLength(8);
    expect(errorsOf({ ...validSignup, password, passwordConfirm: password })).toEqual({});
  });
});

// SafeTextValidator 가 막는 것들임
// 프런트가 통과시키면 가입 버튼을 눌러야 서버가 막는 꼴이 되고,
// 프런트가 더 막으면 서버는 허용하는 값을 화면에서 못 쓰게 됨
describe('safeText 검사', () => {
  it.each([
    ['<script>', 'HTML 태그는 사용할 수 없습니다.'],
    ['행복>아파트', 'HTML 태그는 사용할 수 없습니다.'],
    ['&lt;아파트', 'HTML 문자 참조는 사용할 수 없습니다.'],
    ['&#60;아파트', 'HTML 문자 참조는 사용할 수 없습니다.'],
    ['&#x3c;아파트', 'HTML 문자 참조는 사용할 수 없습니다.'],
    ['javascript:alert(1)', '스크립트 주소는 사용할 수 없습니다.'],
    ['data:text/html,x', '스크립트 주소는 사용할 수 없습니다.'],
    ['vbscript:x', '스크립트 주소는 사용할 수 없습니다.'],
  ])('아파트명이 %s 이면 거부한다', (aptName, message) => {
    expect(errorsOf({ ...validSignup, aptName }).aptName).toBe(message);
  });

  // 공백을 걷어 내고 봐야 잡힘. "java script:" 로 적어서 우회하는 것을 막음
  it('공백을 끼워 넣은 스크립트 주소도 거부한다', () => {
    expect(errorsOf({ ...validSignup, aptName: 'Java Script:alert(1)' }).aptName).toBe(
      '스크립트 주소는 사용할 수 없습니다.',
    );
  });

  // 제로 폭 공백(U+200B). 화면에는 "행복아파트" 로 보이지만 값이 다름
  // 코드 포인트로 적어 둠. 문자를 직접 붙여 넣으면 편집기가 지울 수 있음
  it('보이지 않는 문자를 거부한다', () => {
    expect(errorsOf({ ...validSignup, aptName: '행복​아파트' }).aptName).toBe(
      '보이지 않는 문자는 사용할 수 없습니다.',
    );
  });

  // 방향 재정의(U+202E). 글자가 거꾸로 보이게 만들어 사람을 속일 수 있음
  it('방향 뒤집기 문자를 거부한다', () => {
    expect(errorsOf({ ...validSignup, nickname: '나눔‮이' }).nickname).toBeTruthy();
  });
});

// 백엔드 EmailValidator 와 같은 순서여야 함
//
// 순서가 다르면 프런트에서 지적한 것을 고쳐도 서버가 다른 것을 지적해서 두 번 고치게 됨
// 예: 'a@b.c' 는 형식(TLD 1자)도 틀렸지만 길이(5자)에서 먼저 걸려야 함
describe('이메일 검증 순서', () => {
  it.each([
    ['', '이메일을 입력해 주세요.'],
    ['a b@example.com', '이메일에는 공백을 포함할 수 없습니다.'],
    ['a@@example.com', '이메일에는 @를 하나만 포함해야 합니다.'],
    ['a@b.c', '이메일은 7자 이상이어야 합니다.'],
    [`${'a'.repeat(95)}@b.com`, '이메일은 100자 이하여야 합니다.'],
    ['나눔이@example.com', '이메일에는 영문, 숫자와 일부 기호만 사용할 수 있습니다.'],
    ['nanumi@example', '올바른 이메일 형식이 아닙니다.'],
  ])('%s 이면 "%s" 를 알려 준다', (email, message) => {
    expect(errorsOf({ ...validSignup, email }).email).toBe(message);
  });
});

// 백엔드 PasswordValidator 와 같은 순서·같은 문구여야 함
describe('비밀번호 검증 순서', () => {
  it.each([
    ['', '비밀번호를 입력해 주세요.'],
    ['Ab3! efgh', '비밀번호에는 공백을 포함할 수 없습니다.'],
    ['Ab3!비밀번호', '비밀번호에는 영문, 숫자, 특수문자만 사용할 수 있습니다.'],
    ['Ab3!efg', '비밀번호는 8~20자여야 합니다.'],
    ['abcdefgh!', '비밀번호는 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.'],
  ])('%s 이면 "%s" 를 알려 준다', (password, message) => {
    expect(errorsOf({ ...validSignup, password, passwordConfirm: password }).password).toBe(
      message,
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
