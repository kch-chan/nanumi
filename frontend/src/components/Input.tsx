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
    return(
        <></>
    );
}

export default Input;
