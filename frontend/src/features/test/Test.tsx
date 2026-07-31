import { useState } from 'react';
import Button from '../../components/Button';
import Input from '../../components/Input';
import Modal from '../../components/Modal';
import PasswordInput from '../../components/PasswordInput';
import ApiTestPanel from './ApiTestPanel';
import { login, signup } from '../../api/auth';
import type { LoginRequest, SignupRequest } from '../../types/auth';

const VARIANTS = [
  'primary',
  'secondary',
  'outline',
  'ghost',
  'danger',
] as const;
const SIZES = ['sm', 'md', 'lg'] as const;

function Test() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const [email, setEmail] = useState('');
  const [emailError, setEmailError] = useState('');
  const [password, setPassword] = useState('');
  const [passwordError, setPasswordError] = useState('');

  const handleValidate = () => {
    const nextEmailError = /^\S+@\S+\.\S+$/.test(email)
      ? ''
      : '올바른 이메일 형식이 아닙니다.';
    const nextPasswordError =
      password.length >= 8 ? '' : '비밀번호는 8자 이상이어야 합니다.';

    setEmailError(nextEmailError);
    setPasswordError(nextPasswordError);
  };

  const handleLoadingClick = () => {
    setIsLoading(true);
    setTimeout(() => setIsLoading(false), 1500);
  };

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-10 p-8">
      <h1 className="text-2xl font-bold text-stone-900">테스트 페이지</h1>

      <section className="flex flex-col gap-4">
        <h2 className="text-lg font-semibold text-stone-800">
          API 동작 테스트
        </h2>
        <p className="text-sm text-stone-500">
          입력값을 넣고 <strong>확인</strong>을 누르면 실제 요청을 보내고 응답
          JSON을 그대로 보여줍니다. (백엔드가 실행 중이어야 합니다.)
        </p>

        <ApiTestPanel
          title="회원가입"
          method="POST"
          path="/api/auth/signup"
          fields={[
            {
              name: 'email',
              label: '이메일',
              placeholder: 'example@apt.com',
              type: 'email',
            },
            {
              name: 'password',
              label: '비밀번호',
              placeholder: 'Test1234!',
              type: 'password',
            },
            { name: 'nickname', label: '닉네임', placeholder: '나눔이' },
            { name: 'aptName', label: '아파트명', placeholder: '행복아파트' },
            { name: 'dong', label: '동', placeholder: '101' },
            { name: 'ho', label: '호', placeholder: '1203' },
          ]}
          request={(payload) => signup(payload as unknown as SignupRequest)}
        />

        <ApiTestPanel
          title="로그인"
          method="POST"
          path="/api/auth/login"
          fields={[
            {
              name: 'email',
              label: '이메일',
              placeholder: 'example@apt.com',
              type: 'email',
            },
            {
              name: 'password',
              label: '비밀번호',
              placeholder: 'Test1234!',
              type: 'password',
            },
          ]}
          request={(payload) => login(payload as unknown as LoginRequest)}
        />
      </section>

      <hr className="border-stone-200" />

      <section className="flex flex-col gap-4">
        <h2 className="text-lg font-semibold text-stone-800">Button</h2>

        {SIZES.map((size) => (
          <div key={size} className="flex flex-col gap-2">
            <span className="text-sm text-stone-500">size: {size}</span>
            <div className="flex flex-wrap items-center gap-3">
              {VARIANTS.map((variant) => (
                <Button key={variant} variant={variant} size={size}>
                  {variant}
                </Button>
              ))}
            </div>
          </div>
        ))}

        <div className="flex flex-col gap-2">
          <span className="text-sm text-stone-500">기타 상태</span>
          <div className="flex flex-wrap items-center gap-3">
            <Button disabled>disabled</Button>
            <Button isLoading={isLoading} onClick={handleLoadingClick}>
              {isLoading ? '로딩 중' : '로딩 테스트 (클릭)'}
            </Button>
            <Button fullWidth variant="outline">
              fullWidth
            </Button>
          </div>
        </div>
      </section>

      <hr className="border-stone-200" />

      <section className="flex flex-col gap-4">
        <h2 className="text-lg font-semibold text-stone-800">Input</h2>

        <div className="flex flex-col gap-4 max-w-sm">
          <Input
            label="이메일"
            type="email"
            placeholder="example@apt.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={emailError}
            helperText={
              !emailError ? '아파트 인증 이메일을 입력하세요.' : undefined
            }
          />

          <PasswordInput
            label="비밀번호"
            placeholder="8자 이상 입력"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={passwordError}
            helperText={
              !passwordError ? '영문, 숫자 조합 8자 이상.' : undefined
            }
          />

          <Button onClick={handleValidate}>검증 실행</Button>
        </div>
      </section>

      <hr className="border-stone-200" />

      <section className="flex flex-col gap-4">
        <h2 className="text-lg font-semibold text-stone-800">Modal</h2>

        <div className="flex gap-3">
          <Button onClick={() => setIsModalOpen(true)}>모달 열기</Button>
        </div>

        <Modal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          title="나눔 물품 등록"
          footer={
            <>
              <Button variant="ghost" onClick={() => setIsModalOpen(false)}>
                취소
              </Button>
              <Button onClick={() => setIsModalOpen(false)}>등록하기</Button>
            </>
          }
        >
          <p className="text-sm text-stone-600">테스트</p>
        </Modal>
      </section>
    </div>
  );
}

export default Test;
