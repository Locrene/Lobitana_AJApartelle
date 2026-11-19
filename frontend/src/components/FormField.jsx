export default function FormField({ label, name, type = 'text', value, onChange, error, ...rest }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input name={name} type={type} value={value} onChange={onChange} aria-invalid={!!error} {...rest} />
      {error && <small className="error">{error}</small>}
    </label>
  );
}
