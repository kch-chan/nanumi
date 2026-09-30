import type { TermsBlock, TermsDocument } from '../../../constants/terms';

interface TermsDocumentViewProps {
  terms: TermsDocument;
}

// 표는 모달 폭보다 넓어질 수 있어서 가로 스크롤 컨테이너로 감쌈
function BlockView({ block }: { block: TermsBlock }) {
  if (block.type === 'paragraph') {
    return (
      <p className="whitespace-pre-line leading-relaxed text-stone-600">
        {block.text}
      </p>
    );
  }

  if (block.type === 'list') {
    return (
      <ul className="flex flex-col gap-1.5 pl-1">
        {block.items.map((item, index) => (
          <li key={index} className="flex gap-2 leading-relaxed text-stone-600">
            <span aria-hidden className="text-stone-300">
              ·
            </span>
            <span>{item}</span>
          </li>
        ))}
      </ul>
    );
  }

  return (
    <div className="-mx-1 overflow-x-auto px-1">
      <table className="w-full min-w-[28rem] border-collapse text-left">
        <thead>
          <tr>
            {block.headers.map((header) => (
              <th
                key={header}
                scope="col"
                className="border border-stone-200 bg-stone-50 px-2.5 py-2 font-medium text-stone-700"
              >
                {header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {block.rows.map((row, rowIndex) => (
            <tr key={rowIndex}>
              {row.map((cell, cellIndex) => (
                <td
                  key={cellIndex}
                  className="border border-stone-200 px-2.5 py-2 align-top text-stone-600"
                >
                  {cell}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// 약관 상수(constants/terms.ts)의 구조를 그대로 그려 주는 컴포넌트임
// 본문 자체에는 스크롤을 두지 않고, 감싸는 쪽(모달)에서 높이를 정하도록 둠
function TermsDocumentView({ terms }: TermsDocumentViewProps) {
  return (
    <article className="flex flex-col gap-5 text-xs">
      <header className="flex flex-col gap-0.5">
        <h3 className="text-sm font-semibold text-stone-900">{terms.title}</h3>
        <p className="text-stone-400">{terms.effectiveDate}</p>
      </header>

      {terms.sections.map((section, sectionIndex) => (
        <section key={sectionIndex} className="flex flex-col gap-2">
          {section.heading && (
            <h4 className="font-semibold text-stone-800">{section.heading}</h4>
          )}
          {section.blocks.map((block, blockIndex) => (
            <BlockView key={blockIndex} block={block} />
          ))}
        </section>
      ))}
    </article>
  );
}

export default TermsDocumentView;
