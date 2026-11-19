import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/** Loads a dashboard endpoint; a 401 (expired/revoked session) sends the user back to login. */
export default function useDashboard(loader) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const { refresh } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    loader()
      .then(setData)
      .catch(async (err) => {
        if (err.status === 401) { await refresh(); navigate('/login', { replace: true }); }
        else if (err.status === 403) navigate('/unauthorized', { replace: true });
        else setError(err.message);
      });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return { data, error };
}
