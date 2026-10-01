import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import Button from '../../components/Button';
import Input from '../../components/Input';
import PasswordInput from '../../components/PasswordInput';
import { useLogin } from '../../hooks/useLogin';
import { loginSchema } from '../../schemas/authSchema';
import type { LoginFormValues } from '../../schemas/authSchema';
import { getErrorMessage } from '../../utils/errorMessage';

function LoginForm() {
  const navigate = useNavigate();
  const login = useLogin();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  const onSubmit = (values: LoginFormValues) => {
    login.mutate(values, { onSuccess: () => navigate('/') });
  };

  return (
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
        placeholder="비밀번호를 입력하세요"
        autoComplete="current-password"
        error={errors.password?.message}
        {...register('password')}
      />

      {/* 서버는 어느 칸이 틀렸는지 알려 주지 않음(계정 존재 여부가 드러나므로).
          그래서 칸 아래가 아니라 폼 하단에 둠. role="alert" 로 뜨는 즉시 읽히게 함 */}
      {login.isError && (
        <p role="alert" className="text-sm text-red-500">
          {getErrorMessage(login.error)}
        </p>
      )}

      <Button type="submit" fullWidth isLoading={login.isPending}>
        로그인
      </Button>
    </form>
  );
}

export default LoginForm;
