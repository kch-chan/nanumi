import { useState } from 'react';
import type { TermsKey } from '../../../constants/terms';
import StepComplete from './StepComplete';
import StepInfo from './StepInfo';
import StepTerms from './StepTerms';

type Step = 'terms' | 'info' | 'complete';

const STEPS: { key: Step; label: string }[] = [
  { key: 'terms', label: '약관 동의' },
  { key: 'info', label: '정보 입력' },
  { key: 'complete', label: '가입 완료' },
];

function SignupFunnel() {
  const [step, setStep] = useState<Step>('terms');
  const [nickname, setNickname] = useState('');

  // 약관 체크 상태를 퍼널이 들고 있음
  //
  // 전에는 StepTerms 안에 있었는데, 단계를 바꾸면 그 컴포넌트가 언마운트되어 상태가 사라졌음.
  // "다음" 뒤에 "이전" 을 누르면 체크가 전부 풀려서 다시 체크해야 했음
  //
  // 가입 요청에 동의 기록을 담아 보내야 하므로 StepInfo 도 이 값을 알아야 함
  const [checkedTerms, setCheckedTerms] = useState<
    Partial<Record<TermsKey, boolean>>
  >({});

  const currentIndex = STEPS.findIndex((s) => s.key === step);

  return (
    <div className="flex flex-col gap-6">
      <ol className="flex items-center justify-between">
        {STEPS.map((s, index) => {
          const active = index <= currentIndex;
          return (
            <li key={s.key} className="flex flex-1 flex-col items-center gap-1">
              <span
                className={`
                  flex h-7 w-7 items-center justify-center rounded-full text-xs font-semibold
                  ${active ? 'bg-emerald-600 text-white' : 'bg-stone-200 text-stone-500'}
                `.trim()}
              >
                {index + 1}
              </span>
              <span
                className={`text-xs ${active ? 'text-emerald-700' : 'text-stone-400'}`}
              >
                {s.label}
              </span>
            </li>
          );
        })}
      </ol>

      {step === 'terms' && (
        <StepTerms
          checked={checkedTerms}
          onCheckedChange={setCheckedTerms}
          onNext={() => setStep('info')}
        />
      )}
      {step === 'info' && (
        <StepInfo
          checkedTerms={checkedTerms}
          onPrev={() => setStep('terms')}
          onComplete={(name) => {
            setNickname(name);
            setStep('complete');
          }}
        />
      )}
      {step === 'complete' && <StepComplete nickname={nickname} />}
    </div>
  );
}

export default SignupFunnel;
