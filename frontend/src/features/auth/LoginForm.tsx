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
        error={errors.email?.message}
        {...register('email')}
      />

      <PasswordInput
        label="비밀번호"
        placeholder="비밀번호를 입력하세요"
        error={errors.password?.message}
        {...register('password')}
      />

      {login.isError && (
        <p className="text-sm text-red-500">{getErrorMessage(login.error)}</p>
      )}

      <Button type="submit" fullWidth isLoading={login.isPending}>
        로그인
      </Button>
    </form>
  );
}

export default LoginForm;
