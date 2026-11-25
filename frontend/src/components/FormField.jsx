import { useId } from 'react';

export default function FormField({ label, name, type = 'text', value, onChange, error, hint, ...rest }) {
  const id = useId();
  const note = error || hint;
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input id={id} name={name} type={type} value={value} onChange={onChange} aria-invalid={!!error}
        aria-describedby={note ? `${id}-note` : undefined} {...rest} />
      {note && <small id={`${id}-note`} className={error ? 'error' : 'hint'}>{note}</small>}
    </div>
  );
}
