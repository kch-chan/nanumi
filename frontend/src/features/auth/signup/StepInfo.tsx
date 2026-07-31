import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import Button from '../../../components/Button';
import Input from '../../../components/Input';
import Modal from '../../../components/Modal';
import PasswordInput from '../../../components/PasswordInput';
import { useSignup } from '../../../hooks/useSignup';
import { signupSchema } from '../../../schemas/authSchema';
import type { SignupFormValues } from '../../../schemas/authSchema';
import { getErrorMessage } from '../../../utils/errorMessage';

interface StepInfoProps {
  onPrev: () => void;
  onComplete: (nickname: string) => void;
}

function StepInfo({ onPrev, onComplete }: StepInfoProps) {
  const signup = useSignup();
  const [isAddressOpen, setIsAddressOpen] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<SignupFormValues>({
    resolver: zodResolver(signupSchema),
    defaultValues: {
      email: '',
      password: '',
      passwordConfirm: '',
      nickname: '',
      aptName: '',
      dong: '',
      ho: '',
    },
  });

  const onSubmit = (values: SignupFormValues) => {
    signup.mutate(
      {
        email: values.email,
        password: values.password,
        nickname: values.nickname,
        aptName: values.aptName,
        dong: values.dong,
        ho: values.ho,
      },
      { onSuccess: () => onComplete(values.nickname) },
    );
  };

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="text-xl font-semibold text-stone-900">정보 입력</h2>
        <p className="mt-1 text-sm text-stone-500">
          회원 정보를 입력해 주세요.
        </p>
      </div>

      <form
        onSubmit={handleSubmit(onSubmit)}
        className="flex flex-col gap-4"
        noValidate
      >
        <Input
          label="이메일"
          type="email"
          placeholder="example@apt.com"
          error={errors.email?.message}
          {...register('email')}
        />

        <PasswordInput
          label="비밀번호"
          placeholder="8~20자, 영문·숫자·특수문자 포함"
          error={errors.password?.message}
          {...register('password')}
        />

        <PasswordInput
          label="비밀번호 확인"
          placeholder="비밀번호를 다시 입력하세요"
          error={errors.passwordConfirm?.message}
          {...register('passwordConfirm')}
        />

        <Input
          label="닉네임"
          placeholder="한글/영문/숫자 2~10자"
          error={errors.nickname?.message}
          {...register('nickname')}
        />

        {/* 주소: 추후 주소 검색 API(예: 카카오/다음 우편번호) 연동 예정 — 현재는 형태만 */}
        <div className="flex flex-col gap-1.5">
          <span className="text-sm font-medium text-stone-700">아파트</span>
          <div className="flex items-start gap-2">
            <div className="flex-1">
              <Input
                placeholder="아파트명을 검색 또는 입력"
                error={errors.aptName?.message}
                {...register('aptName')}
              />
            </div>
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsAddressOpen(true)}
            >
              주소 검색
            </Button>
          </div>
        </div>

        <div className="flex gap-2">
          <div className="flex-1">
            <Input
              label="동"
              placeholder="예: 101"
              error={errors.dong?.message}
              {...register('dong')}
            />
          </div>
          <div className="flex-1">
            <Input
              label="호"
              placeholder="예: 1203"
              error={errors.ho?.message}
              {...register('ho')}
            />
          </div>
        </div>

        {signup.isError && (
          <p className="text-sm text-red-500">
            {getErrorMessage(signup.error)}
          </p>
        )}

        <div className="mt-2 flex gap-2">
          <Button type="button" variant="outline" fullWidth onClick={onPrev}>
            이전
          </Button>
          <Button type="submit" fullWidth isLoading={signup.isPending}>
            가입하기
          </Button>
        </div>
      </form>

      <Modal
        isOpen={isAddressOpen}
        onClose={() => setIsAddressOpen(false)}
        title="주소 검색"
      >
        <p className="text-sm text-stone-500">
          주소 검색 API는 추후 연동 예정입니다. 지금은 아파트명을 직접 입력해
          주세요.
        </p>
      </Modal>
    </div>
  );
}

export default StepInfo;
