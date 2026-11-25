import { useEffect, useState } from 'react';
import FormField from '../components/FormField';
import Icon from '../components/Icon';
import { staffAccountService } from '../services/authService';
import { validateAccount } from '../services/validation';

const EMPTY = { fullName: '', email: '', mobileNumber: '', password: '' };

/** Owner-only: add staff accounts and activate/deactivate them (SRS FR-061). */
export default function StaffAccountsPage() {
  const [staff, setStaff] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [notice, setNotice] = useState('');

  const load = () => staffAccountService.list().then(setStaff).catch((e) => setMessage(e.message));
  useEffect(() => { load(); }, []);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setMessage(''); setNotice('');
    const errs = validateAccount(form, { confirm: false });
    setErrors(errs);
    if (Object.keys(errs).length) return;
    try {
      const created = await staffAccountService.create(form);
      setNotice(`Staff account created for ${created.email}.`);
      setForm(EMPTY);
      load();
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setMessage(err.message);
    }
  };

  const toggle = async (s) => {
    setMessage(''); setNotice('');
    try {
      await staffAccountService.setActive(s.id, !s.active);
      load();
    } catch (err) { setMessage(err.message); }
  };

  return (
    <>
      <div className="page-head"><h1>Staff Accounts</h1><p className="meta">Add front-desk staff and control who can sign in.</p></div>
      {message && <p className="alert">{message}</p>}
      {notice && <p className="success">{notice}</p>}
      <div className="grid">
        <form className="card" onSubmit={onSubmit} noValidate>
          <h2 className="card-title"><span className="title-icon"><Icon name="user" /></span>Add staff account</h2>
          <FormField label="Full name" name="fullName" value={form.fullName} onChange={onChange} error={errors.fullName} />
          <FormField label="Email" name="email" type="email" value={form.email} onChange={onChange} error={errors.email} />
          <FormField label="Mobile number" name="mobileNumber" value={form.mobileNumber} onChange={onChange} error={errors.mobileNumber} hint="11 digits, starting with 09." maxLength={11} />
          <FormField label="Temporary password" name="password" type="password" value={form.password} onChange={onChange} error={errors.password} autoComplete="new-password" />
          <button type="submit">Add staff</button>
        </form>
        <section className="card">
          <h2 className="card-title"><span className="title-icon"><Icon name="users" /></span>Staff list<span className="count">{staff.length}</span></h2>
          {staff.length === 0 ? <p>No staff accounts yet.</p> : (
            <div className="table-wrap"><table>
              <thead><tr><th>Name</th><th>Email</th><th>Mobile</th><th>Status</th><th></th></tr></thead>
              <tbody>
                {staff.map((s) => (
                  <tr key={s.id}>
                    <td>{s.fullName}</td><td>{s.email}</td><td>{s.mobileNumber}</td>
                    <td><span className={s.active ? 'chip chip-green' : 'chip chip-gray'}>{s.active ? 'Active' : 'Deactivated'}</span></td>
                    <td><button className={s.active ? 'danger' : 'outline'} onClick={() => toggle(s)}>{s.active ? 'Deactivate' : 'Reactivate'}</button></td>
                  </tr>
                ))}
              </tbody>
            </table></div>
          )}
        </section>
      </div>
    </>
  );
}
