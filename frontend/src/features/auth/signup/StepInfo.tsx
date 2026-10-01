import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import Button from '../../../components/Button';
import Input from '../../../components/Input';
import Modal from '../../../components/Modal';
import PasswordInput from '../../../components/PasswordInput';
import type { TermsKey } from '../../../constants/terms';
import { toAgreementPayload } from '../../../constants/terms';
import { useSignup } from '../../../hooks/useSignup';
import { signupSchema } from '../../../schemas/authSchema';
import type { SignupFormValues } from '../../../schemas/authSchema';
import { getErrorMessage } from '../../../utils/errorMessage';

interface StepInfoProps {
  // 앞 단계에서 받은 약관 동의 상태임. 가입 요청에 함께 담아 보냄
  checkedTerms: Partial<Record<TermsKey, boolean>>;
  onPrev: () => void;
  onComplete: (nickname: string) => void;
}

function StepInfo({ checkedTerms, onPrev, onComplete }: StepInfoProps) {
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
        // 비워 두면 빈 문자열이라 아예 안 보냄 (서버도 빈 값은 null 로 바꾸지만 여기서도 걸러 둠)
        dong: values.dong || undefined,
        ho: values.ho || undefined,
        // 동의 기록을 함께 보냄. 선택 약관의 거부도 담김
        // 전에는 체크만 받고 아무것도 보내지 않아서 동의 사실이 어디에도 남지 않았음
        agreements: toAgreementPayload(checkedTerms),
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
          autoComplete="email"
          error={errors.email?.message}
          {...register('email')}
        />

        <PasswordInput
          label="비밀번호"
          placeholder="8~20자, 영문·숫자·특수문자 포함"
          autoComplete="new-password"
          error={errors.password?.message}
          {...register('password')}
        />

        <PasswordInput
          label="비밀번호 확인"
          placeholder="비밀번호를 다시 입력하세요"
          autoComplete="new-password"
          error={errors.passwordConfirm?.message}
          {...register('passwordConfirm')}
        />

        <Input
          label="닉네임"
          placeholder="한글/영문/숫자 2~10자"
          autoComplete="nickname"
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

        {/* role="alert" 가 있으면 문구가 나타나는 순간 스크린 리더가 읽어 줌
            없으면 이용자가 그 자리로 옮겨 가야 알 수 있음 */}
        {signup.isError && (
          <p role="alert" className="text-sm text-red-500">
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
