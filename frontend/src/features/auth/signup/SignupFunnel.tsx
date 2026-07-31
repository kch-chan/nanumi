import { useState } from 'react';
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

      {step === 'terms' && <StepTerms onNext={() => setStep('info')} />}
      {step === 'info' && (
        <StepInfo
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
