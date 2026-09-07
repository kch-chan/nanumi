import { useState } from 'react';
import Button from '../../../components/Button';
import Checkbox from '../../../components/Checkbox';
import Modal from '../../../components/Modal';
import type { TermsKey } from '../../../constants/terms';
import { TERMS_DOCUMENTS, findTermsDocument } from '../../../constants/terms';
import TermsDocumentView from './TermsDocumentView';

interface StepTermsProps {
  onNext: () => void;
}

function StepTerms({ onNext }: StepTermsProps) {
  const [checked, setChecked] = useState<Record<string, boolean>>({});
  const [openTerm, setOpenTerm] = useState<TermsKey | null>(null);

  const allChecked = TERMS_DOCUMENTS.every((term) => checked[term.key]);
  const requiredChecked = TERMS_DOCUMENTS.filter((term) => term.required).every(
    (term) => checked[term.key],
  );

  // 전문 모달에 그릴 약관임. 닫혀 있으면 null 임
  const openDocument = findTermsDocument(openTerm);

  const toggleAll = () => {
    const next = !allChecked;
    setChecked(
      Object.fromEntries(TERMS_DOCUMENTS.map((term) => [term.key, next])),
    );
  };

  const toggleOne = (key: TermsKey) =>
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
          {TERMS_DOCUMENTS.map((term) => (
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
        isOpen={openDocument !== null}
        onClose={() => setOpenTerm(null)}
        title={openDocument?.title ?? '약관 내용'}
        size="lg"
      >
        {openDocument && (
          <div className="max-h-[60vh] overflow-y-auto pr-1">
            <TermsDocumentView terms={openDocument} />
          </div>
        )}
      </Modal>
    </div>
  );
}

export default StepTerms;
