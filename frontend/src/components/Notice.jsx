import { useEffect, useRef } from 'react';

const ICON = {
  error: <><circle cx="12" cy="12" r="9" /><path d="M12 7.5v5.5M12 16.5h.01" /></>,
  success: <><circle cx="12" cy="12" r="9" /><path d="m8 12.5 2.8 2.8L16.5 9.5" /></>,
};

/**
 * An error or success message. It brings itself into view, and takes focus unless the person is in a field,
 * because the submit button loses focus while the request runs.
 */
export default function Notice({ tone, title, children }) {
  const ref = useRef(null);

  useEffect(() => {
    if (!document.activeElement?.matches('input')) ref.current.focus({ preventScroll: true });
    ref.current.scrollIntoView({ block: 'nearest' });
  }, []);

  return (
    <div ref={ref} tabIndex={-1} className={tone === 'error' ? 'alert notice' : 'success notice'} role={tone === 'error' ? 'alert' : 'status'}>
      <svg className="notice-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
        strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{ICON[tone]}</svg>
      <div>
        <strong>{title}</strong>
        {children}
      </div>
    </div>
  );
}
