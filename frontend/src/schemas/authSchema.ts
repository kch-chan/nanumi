import { z } from 'zod';

// 백엔드 검증 규칙과 동일하게 맞춘다.
// (PasswordValidator / NicknameValidator / @Email 참고)
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&]).{8,20}$/;
const NICKNAME_REGEX = /^[가-힣a-zA-Z0-9]{2,10}$/;

export const loginSchema = z.object({
  email: z
    .string()
    .min(1, '이메일을 입력해 주세요.')
    .regex(EMAIL_REGEX, '올바른 이메일 형식이 아닙니다.'),
  password: z.string().min(1, '비밀번호를 입력해 주세요.'),
});

export const signupSchema = z
  .object({
    email: z
      .string()
      .min(1, '이메일을 입력해 주세요.')
      .regex(EMAIL_REGEX, '올바른 이메일 형식이 아닙니다.'),
    password: z
      .string()
      .regex(
        PASSWORD_REGEX,
        '비밀번호는 8~20자의 영문, 숫자, 특수문자를 포함해야 합니다.',
      ),
    passwordConfirm: z.string().min(1, '비밀번호를 한 번 더 입력해 주세요.'),
    nickname: z
      .string()
      .regex(NICKNAME_REGEX, '닉네임은 한글/영문/숫자 2~10자여야 합니다.'),
    aptName: z
      .string()
      .min(1, '아파트명을 입력해 주세요.')
      .max(100, '아파트명은 100자 이하여야 합니다.'),
    dong: z.string().max(20, '동은 20자 이하여야 합니다.').optional(),
    ho: z.string().max(20, '호는 20자 이하여야 합니다.').optional(),
  })
  .refine((data) => data.password === data.passwordConfirm, {
    message: '비밀번호가 일치하지 않습니다.',
    path: ['passwordConfirm'],
  });

export type LoginFormValues = z.infer<typeof loginSchema>;
export type SignupFormValues = z.infer<typeof signupSchema>;
