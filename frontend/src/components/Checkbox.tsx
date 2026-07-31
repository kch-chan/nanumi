import type { ComponentPropsWithRef, ReactNode } from 'react';
import { useId } from 'react';

interface CheckboxProps extends Omit<ComponentPropsWithRef<'input'>, 'type'> {
  label?: ReactNode;
}

function Checkbox({ ref, label, id, className = '', ...props }: CheckboxProps) {
  const defaultId = useId();
  const checkboxId = id ?? defaultId;

  return (
    <label
      htmlFor={checkboxId}
      className="flex cursor-pointer select-none items-center gap-2"
    >
      <input
        ref={ref}
        id={checkboxId}
        type="checkbox"
        className={`
          h-4 w-4 rounded border-stone-300 text-emerald-600
          focus:ring-2 focus:ring-emerald-500 focus:ring-offset-0
          ${className}
        `.trim()}
        {...props}
      />
      {label && <span className="text-sm text-stone-700">{label}</span>}
    </label>
  );
}

export default Checkbox;
