import { z } from 'zod';

// 백엔드 검증 규칙과 같은 순서·같은 문구로 맞춰 둠
// (EmailValidator / PasswordValidator / SafeTextValidator / NicknameValidator 참고)
// 화면에서 미리 걸러 주는 것뿐이고, 진짜 검사는 서버가 다시 함

const WHITESPACE = /\s/;
const WHITESPACE_GLOBAL = /\s/g;

// 공백과 제어문자를 뺀 출력 가능한 ASCII 임
const PRINTABLE_ASCII = /^[\x21-\x7E]+$/;

const EMAIL_SHAPE =
  /^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$/;
const NICKNAME_REGEX = /^[가-힣a-zA-Z0-9]{2,10}$/;

const LETTER = /[A-Za-z]/;
const DIGIT = /[0-9]/;
const SPECIAL = /[^A-Za-z0-9]/;

// &lt; &#60; &#x3c; 같은 문자 참조임
const CHARACTER_REFERENCE =
  /&(#[0-9]+|#[xX][0-9a-fA-F]+|[A-Za-z][A-Za-z0-9]{1,31});/;

// 눈에 보이지 않아서 사람이 못 알아채는 문자들임 (제어문자, 제로 폭, 방향 뒤집기, BOM)
const INVISIBLE_SOURCE =
  '[\\u0000-\\u001F\\u007F-\\u009F\\u00AD\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u2064\\uFEFF]';
const INVISIBLE = new RegExp(INVISIBLE_SOURCE);
const INVISIBLE_GLOBAL = new RegExp(INVISIBLE_SOURCE, 'g');

const SCRIPT_SCHEMES = [
  'javascript:',
  'vbscript:',
  'data:',
  'file:',
  'blob:',
] as const;

// 어느 규칙에 걸렸는지 하나씩만 알려 주려고, 처음 걸린 메시지에서 멈춤
const emailMessage = (value: string): string | null => {
  if (value.trim() === '') return '이메일을 입력해 주세요.';
  if (WHITESPACE.test(value)) return '이메일에는 공백을 포함할 수 없습니다.';
  if ((value.match(/@/g) ?? []).length !== 1)
    return '이메일에는 @를 하나만 포함해야 합니다.';
  if (value.length < 7) return '이메일은 7자 이상이어야 합니다.';
  if (value.length > 100) return '이메일은 100자 이하여야 합니다.';
  if (!PRINTABLE_ASCII.test(value))
    return '이메일에는 영문, 숫자와 일부 기호만 사용할 수 있습니다.';
  if (!EMAIL_SHAPE.test(value)) return '올바른 이메일 형식이 아닙니다.';
  return null;
};

const passwordMessage = (value: string): string | null => {
  if (value === '') return '비밀번호를 입력해 주세요.';
  if (WHITESPACE.test(value)) return '비밀번호에는 공백을 포함할 수 없습니다.';
  if (!PRINTABLE_ASCII.test(value))
    return '비밀번호에는 영문, 숫자, 특수문자만 사용할 수 있습니다.';
  if (value.length < 8 || value.length > 20)
    return '비밀번호는 8~20자여야 합니다.';
  if (!LETTER.test(value) || !DIGIT.test(value) || !SPECIAL.test(value))
    return '비밀번호는 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.';
  return null;
};

// 공백과 보이지 않는 문자를 걷어 낸 뒤에 봐야 "java script:" 같은 걸 잡을 수 있음
const hasScriptScheme = (value: string): boolean => {
  const squeezed = value
    .replace(WHITESPACE_GLOBAL, '')
    .replace(INVISIBLE_GLOBAL, '')
    .toLowerCase();
  return SCRIPT_SCHEMES.some((scheme) => squeezed.includes(scheme));
};

const safeTextMessage = (value: string): string | null => {
  if (/[<>]/.test(value)) return 'HTML 태그는 사용할 수 없습니다.';
  if (CHARACTER_REFERENCE.test(value))
    return 'HTML 문자 참조는 사용할 수 없습니다.';
  if (hasScriptScheme(value)) return '스크립트 주소는 사용할 수 없습니다.';
  if (INVISIBLE.test(value)) return '보이지 않는 문자는 사용할 수 없습니다.';
  return null;
};

// 처음 걸린 메시지 하나만 이슈로 올림
const checkWith =
  (rule: (value: string) => string | null) =>
  (value: string, ctx: z.RefinementCtx) => {
    const message = rule(value);
    if (message) ctx.addIssue({ code: 'custom', message });
  };

// 길이 검사를 먼저 걸고 마지막에 내용 검사를 붙임
const safeText = (schema: z.ZodString) =>
  schema.superRefine(checkWith(safeTextMessage));

export const loginSchema = z.object({
  // 로그인은 형식을 자세히 따지지 않음. 서버도 비어 있는지만 봄
  email: z.string().min(1, '이메일을 입력해 주세요.'),
  password: z.string().min(1, '비밀번호를 입력해 주세요.'),
});

export const signupSchema = z
  .object({
    email: z.string().superRefine(checkWith(emailMessage)),
    password: z.string().superRefine(checkWith(passwordMessage)),
    passwordConfirm: z.string().min(1, '비밀번호를 한 번 더 입력해 주세요.'),
    nickname: safeText(
      z
        .string()
        .regex(NICKNAME_REGEX, '닉네임은 한글/영문/숫자 2~10자여야 합니다.'),
    ),
    aptName: safeText(
      z
        .string()
        .min(1, '아파트명을 입력해 주세요.')
        .max(100, '아파트명은 100자 이하여야 합니다.'),
    ),
    dong: safeText(z.string().max(20, '동은 20자 이하여야 합니다.')).optional(),
    ho: safeText(z.string().max(20, '호는 20자 이하여야 합니다.')).optional(),
  })
  .refine((data) => data.password === data.passwordConfirm, {
    message: '비밀번호가 일치하지 않습니다.',
    path: ['passwordConfirm'],
  });

export type LoginFormValues = z.infer<typeof loginSchema>;
export type SignupFormValues = z.infer<typeof signupSchema>;
