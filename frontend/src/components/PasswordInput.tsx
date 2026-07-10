import type { ComponentPropsWithRef } from 'react';
import { useState } from 'react';
import Input from './Input';

interface PasswordInputProps extends Omit<ComponentPropsWithRef<'input'>, 'type'> {
  label?: string;
  error?: string;
  helperText?: string;
}

function PasswordInput({
  ref,
  label,
  error,
  helperText,
  ...props
}: PasswordInputProps) {
  const [isVisible, setIsVisible] = useState(false);

  return (
    <Input
      ref={ref}
      type={isVisible ? 'text' : 'password'}
      label={label}
      error={error}
      helperText={helperText}
      rightIcon={
        <button
          type="button"
          onClick={() => setIsVisible((prev) => !prev)}
          tabIndex={-1}
          className="translate-y-1 pointer-events-auto text-stone-400 hover:text-stone-600 focus-visible:outline-none"
        >
          {isVisible ? <EyeOffIcon /> : <EyeIcon />}
        </button>
      }
      {...props}
    />
  );
}

function EyeIcon() {
  return (
    <svg
      className="h-5 w-5"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"
      />
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
      />
    </svg>
  );
}

function EyeOffIcon() {
  return (
    <svg
      className="h-5 w-5"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M3.98 8.223A10.477 10.477 0 001.934 12c1.292 4.338 5.31 7.5 9.966 7.5.993 0 1.953-.138 2.863-.395M6.228 6.228A10.451 10.451 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.522 10.522 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88"
      />
    </svg>
  );
}

export default PasswordInput;
