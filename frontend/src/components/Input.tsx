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

        {rightIcon && (
          <span className="absolute right-3 text-stone-400">{rightIcon}</span>
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
