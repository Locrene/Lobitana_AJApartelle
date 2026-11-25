import { useEffect, useRef } from 'react';

/** After a rejected submit, moves focus to the first field with an error. Put the returned ref on the form. */
export default function useFocusFirstError(errors) {
  const formRef = useRef(null);

  useEffect(() => {
    if (Object.keys(errors).length) formRef.current?.querySelector('[aria-invalid="true"]')?.focus();
  }, [errors]);

  return formRef;
}
