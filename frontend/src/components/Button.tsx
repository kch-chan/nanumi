import type { ComponentPropsWithRef, ReactNode } from 'react';

type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
type ButtonSize = 'sm' | 'md' | 'lg';
interface ButtonProps extends ComponentPropsWithRef<'button'> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  isLoading?: boolean;
  fullWidth?: boolean;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
}

// 나중에 전역 css로 빼기
const VARIANT_STYLES: Record<ButtonVariant, string> = {
  primary:
    'bg-emerald-600 text-white hover:bg-emerald-700 active:bg-emerald-800 disabled:bg-emerald-300',
  secondary:
    'bg-stone-100 text-stone-800 hover:bg-stone-200 active:bg-stone-300 disabled:bg-stone-50 disabled:text-stone-400',
  outline:
    'border border-stone-300 text-stone-700 bg-transparent hover:bg-stone-50 active:bg-stone-100 disabled:text-stone-300 disabled:border-stone-200',
  ghost:
    'bg-transparent text-stone-600 hover:bg-stone-100 active:bg-stone-200 disabled:text-stone-300',
  danger:
    'bg-red-600 text-white hover:bg-red-700 active:bg-red-800 disabled:bg-red-300',
};

// 버튼 전용 style이지만 공통 스타일이 될 수 있으면 빼기
const SIZE_STYLES: Record<ButtonSize, string> = {
  sm: 'h-9 px-3 text-sm gap-1.5',
  md: 'h-11 px-4 text-sm gap-2',
  lg: 'h-12 px-6 text-base gap-2',
};

function Button({
  ref,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  fullWidth = false,
  leftIcon,
  rightIcon,
  disabled,
  className = '',
  children,
  ...props
}: ButtonProps) {
  return (
    <></>
  );
}

export default Button;
