// 버튼 모양을 만드는 클래스 묶음임
//
// 왜 Button.tsx 밖으로 뺐는가: 링크를 버튼처럼 보이게 해야 할 자리가 있음.
// 전에는 <Link><Button>...</Button></Link> 로 감싸 두었는데, 이건 <a> 안에 <button> 이
// 들어간 모양이라 HTML 명세가 금지하는 중첩임(a 안에 상호작용 요소를 둘 수 없음).
// 스크린 리더가 "링크, 버튼" 으로 두 번 읽고 Tab 이 두 번 멈출 수 있음
//
// 컴포넌트 파일에서 함수를 같이 내보내면 react-refresh 규칙(only-export-components)에
// 걸리므로 별도 파일로 둠

export type ButtonVariant =
  | 'primary'
  | 'secondary'
  | 'outline'
  | 'ghost'
  | 'danger';
export type ButtonSize = 'sm' | 'md' | 'lg';

// Record 로 둬서 variant 를 추가하면 스타일을 빠뜨릴 수 없게 함
export const VARIANT_STYLES: Record<ButtonVariant, string> = {
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

export const SIZE_STYLES: Record<ButtonSize, string> = {
  sm: 'h-9 px-3 text-sm gap-1.5',
  md: 'h-11 px-4 text-sm gap-2',
  lg: 'h-12 px-6 text-base gap-2',
};

const BASE_STYLES = [
  'inline-flex items-center justify-center rounded-lg font-medium',
  'transition-colors duration-150',
  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2',
  'disabled:cursor-not-allowed',
].join(' ');

interface ButtonStyleOptions {
  variant?: ButtonVariant;
  size?: ButtonSize;
  fullWidth?: boolean;
  className?: string;
}

export function buttonClassName({
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  className = '',
}: ButtonStyleOptions = {}): string {
  return [
    BASE_STYLES,
    VARIANT_STYLES[variant],
    SIZE_STYLES[size],
    fullWidth ? 'w-full' : '',
    className,
  ]
    .filter(Boolean)
    .join(' ');
}
