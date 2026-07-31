import axios from 'axios';
import { useState } from 'react';
import Button from '../../components/Button';
import Input from '../../components/Input';

interface FieldConfig {
  name: string;
  label: string;
  placeholder?: string;
  type?: string;
}

interface ApiTestPanelProps {
  title: string;
  method: string;
  path: string;
  fields: FieldConfig[];
  request: (payload: Record<string, string>) => Promise<unknown>;
}

type Status = 'idle' | 'loading' | 'success' | 'error';

// 기능이 정상 동작하는지 확인하는 패널.
// 입력값을 payload 로 만들어 실제 API 를 호출하고 응답 JSON 을 그대로 보여준다.
function ApiTestPanel({
  title,
  method,
  path,
  fields,
  request,
}: ApiTestPanelProps) {
  const [values, setValues] = useState<Record<string, string>>({});
  const [result, setResult] = useState('');
  const [status, setStatus] = useState<Status>('idle');

  const handleChange = (name: string, value: string) =>
    setValues((prev) => ({ ...prev, [name]: value }));

  const handleSubmit = async () => {
    setStatus('loading');
    setResult('');
    try {
      const data = await request(values);
      setStatus('success');
      setResult(JSON.stringify(data, null, 2));
    } catch (error) {
      setStatus('error');
      setResult(formatError(error));
    }
  };

  return (
    <div className="flex flex-col gap-4 rounded-xl border border-stone-200 p-5">
      <div className="flex items-center gap-2">
        <span className="rounded bg-stone-800 px-2 py-0.5 text-xs font-semibold text-white">
          {method}
        </span>
        <span className="font-mono text-sm text-stone-600">{path}</span>
      </div>

      <h3 className="text-base font-semibold text-stone-800">{title}</h3>

      <div className="flex flex-col gap-3">
        {fields.map((field) => (
          <Input
            key={field.name}
            label={field.label}
            type={field.type}
            placeholder={field.placeholder}
            value={values[field.name] ?? ''}
            onChange={(e) => handleChange(field.name, e.target.value)}
          />
        ))}
      </div>

      <Button onClick={handleSubmit} isLoading={status === 'loading'}>
        확인 (요청 보내기)
      </Button>

      {(status === 'success' || status === 'error') && (
        <div className="flex flex-col gap-1">
          <span
            className={`text-xs font-semibold ${
              status === 'success' ? 'text-emerald-600' : 'text-red-500'
            }`}
          >
            {status === 'success' ? '성공 응답' : '에러 응답'}
          </span>
          <pre className="max-h-64 overflow-auto rounded-lg bg-stone-900 p-3 text-xs text-stone-100">
            {result}
          </pre>
        </div>
      )}
    </div>
  );
}

function formatError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    return JSON.stringify(
      {
        status: error.response?.status ?? null,
        body: error.response?.data ?? error.message,
      },
      null,
      2,
    );
  }
  return String(error);
}

export default ApiTestPanel;
