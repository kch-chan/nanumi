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
      {/* accent-color 로 색을 바꿈
          전에는 text-emerald-600 / rounded / border-stone-300 을 적어 두었는데,
          브라우저가 직접 그리는 기본 체크박스에는 그 속성들이 먹지 않음.
          @tailwindcss/forms 를 넣으면 먹지만, 그 플러그인은 입력칸 전체의 기본 모양도 함께 바꿈.
          색 하나만 필요하므로 표준 속성인 accent-color 를 씀 */}
      <input
        ref={ref}
        id={checkboxId}
        type="checkbox"
        className={`
          h-4 w-4 accent-emerald-600
          focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500
          ${className}
        `.trim()}
        {...props}
      />
      {label && <span className="text-sm text-stone-700">{label}</span>}
    </label>
  );
}

export default Checkbox;
