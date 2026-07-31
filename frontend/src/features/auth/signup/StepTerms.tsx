import { useState } from 'react';
import Button from '../../../components/Button';
import Checkbox from '../../../components/Checkbox';
import Modal from '../../../components/Modal';

interface StepTermsProps {
  onNext: () => void;
}

const TERMS = [
  { key: 'service', label: '(필수) 서비스 이용약관 동의', required: true },
  { key: 'privacy', label: '(필수) 개인정보 수집·이용 동의', required: true },
  { key: 'marketing', label: '(선택) 마케팅 정보 수신 동의', required: false },
] as const;

function StepTerms({ onNext }: StepTermsProps) {
  const [checked, setChecked] = useState<Record<string, boolean>>({});
  const [openTerm, setOpenTerm] = useState<string | null>(null);

  const allChecked = TERMS.every((term) => checked[term.key]);
  const requiredChecked = TERMS.filter((term) => term.required).every(
    (term) => checked[term.key],
  );

  const toggleAll = () => {
    const next = !allChecked;
    setChecked(Object.fromEntries(TERMS.map((term) => [term.key, next])));
  };

  const toggleOne = (key: string) =>
    setChecked((prev) => ({ ...prev, [key]: !prev[key] }));

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="text-xl font-semibold text-stone-900">약관 동의</h2>
        <p className="mt-1 text-sm text-stone-500">
          서비스 이용을 위해 약관에 동의해 주세요.
        </p>
      </div>

      <div className="flex flex-col gap-3">
        <div className="rounded-lg border border-stone-200 bg-stone-50 px-4 py-3">
          <Checkbox
            label={
              <span className="font-medium text-stone-800">전체 동의</span>
            }
            checked={allChecked}
            onChange={toggleAll}
          />
        </div>

        <div className="flex flex-col gap-2.5 px-1">
          {TERMS.map((term) => (
            <div key={term.key} className="flex items-center justify-between">
              <Checkbox
                label={term.label}
                checked={!!checked[term.key]}
                onChange={() => toggleOne(term.key)}
              />
              <button
                type="button"
                onClick={() => setOpenTerm(term.key)}
                className="text-xs text-stone-400 underline hover:text-stone-600"
              >
                보기
              </button>
            </div>
          ))}
        </div>
      </div>

      <Button fullWidth disabled={!requiredChecked} onClick={onNext}>
        다음
      </Button>

      <Modal
        isOpen={openTerm !== null}
        onClose={() => setOpenTerm(null)}
        title="약관 내용"
      >
        <p className="text-sm text-stone-500">
          약관 내용이 아직 입력되지 않았습니다.
        </p>
      </Modal>
    </div>
  );
}

export default StepTerms;
