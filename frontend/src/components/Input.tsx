import type { ComponentPropsWithRef, ReactNode } from 'react';
import { useId } from 'react';

interface InputProps extends ComponentPropsWithRef<'input'> {
  label?: string;
  error?: string;
  helperText?: string;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
}

function Input({
  ref,
  label,
  error,
  helperText,
  leftIcon,
  rightIcon,
  id,
  className = '',
  ...props
}: InputProps) {
  const defaultId = useId();
  const inputId = id ?? defaultId;

  // 아래에서 오류·도움말 문단에 붙이는 id 와 짝임
  // 이걸 연결하지 않으면 화면을 못 보는 이용자는 테두리가 빨갛다는 것도,
  // 왜 틀렸는지도 알 수 없음. 입력칸과 설명이 따로 떠 있는 상태가 됨
  const describedBy = error
    ? `${inputId}-error`
    : helperText
      ? `${inputId}-helper`
      : undefined;

  return (
    <div className="flex flex-col gap-1.5">
      {label && (
        <label htmlFor={inputId} className="text-sm font-medium text-stone-700">
          {label}
        </label>
      )}

      <div className="relative flex items-center">
        {leftIcon && (
          <span className="pointer-events-none absolute left-3 text-stone-400">
            {leftIcon}
          </span>
        )}

        <input
          ref={ref}
          id={inputId}
          // false 를 넣으면 aria-invalid="false" 가 그대로 남으므로 없을 때는 속성 자체를 뺌
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
          className={`
            h-11 w-full rounded-lg border bg-white text-sm text-stone-900
            placeholder:text-stone-400
            transition-colors duration-150
            focus:outline-none focus:ring-2 focus:ring-offset-0
            disabled:bg-stone-50 disabled:text-stone-400 disabled:cursor-not-allowed
            ${leftIcon ? 'pl-9' : 'pl-3'}
            ${rightIcon ? 'pr-9' : 'pr-3'}
            ${
              error
                ? 'border-red-400 focus:border-red-500 focus:ring-red-200'
                : 'border-stone-300 focus:border-emerald-500 focus:ring-emerald-200'
            }
            ${className}
          `.trim()}
          {...props}
        />

        {/* flex items-center 가 있어야 안에 든 것이 입력칸 수직 중앙에 옴.
            이게 없어서 PasswordInput 쪽에서 translate-y-1 로 눈대중 보정을 하고 있었음 */}
        {rightIcon && (
          <span className="absolute right-3 flex items-center text-stone-400">
            {rightIcon}
          </span>
        )}
      </div>

      {error ? (
        <p id={`${inputId}-error`} className="text-sm text-red-500">
          {error}
        </p>
      ) : helperText ? (
        <p id={`${inputId}-helper`} className="text-sm text-stone-500">
          {helperText}
        </p>
      ) : null}
    </div>
  );
}

export default Input;
