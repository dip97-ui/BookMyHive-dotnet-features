import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context.jsx';
import Logo from '../components/Logo.jsx';

export default function AuthPage({ mode }) {
  const isLogin = mode === 'login';
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const { state } = useLocation();
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [busy, setBusy] = useState(false);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    setFieldErrors({});
    try {
      if (isLogin) await login(form.email, form.password);
      else await register(form.name, form.email, form.password);
      navigate(state?.from || '/', { replace: true });
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors || {});
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="page narrow">
      <form className="authcard" onSubmit={submit} noValidate>
        <Logo size={44} />
        <h1>{isLogin ? 'Welcome back' : 'Create your account'}</h1>
        {state?.from && <p className="muted">Log in to continue your booking.</p>}
        {error && <p className="alert alert--err" role="alert">{error}</p>}

        {!isLogin && (
          <div className="field">
            <label htmlFor="name">Full name</label>
            <input id="name" value={form.name} onChange={set('name')} autoComplete="name" required />
            {fieldErrors.name && <span className="field__error">{fieldErrors.name}</span>}
          </div>
        )}
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" type="email" value={form.email} onChange={set('email')} autoComplete="email" required />
          {fieldErrors.email && <span className="field__error">{fieldErrors.email}</span>}
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <input id="password" type="password" value={form.password} onChange={set('password')} autoComplete={isLogin ? 'current-password' : 'new-password'} required />
          {fieldErrors.password && <span className="field__error">{fieldErrors.password}</span>}
        </div>

        <button className="btn btn--lg btn--block" disabled={busy}>{busy ? 'Please wait…' : isLogin ? 'Log in' : 'Sign up'}</button>
        <p className="muted">
          {isLogin ? <>New here? <Link to="/register" state={state}>Create an account</Link></> : <>Already registered? <Link to="/login" state={state}>Log in</Link></>}
        </p>
        {isLogin && <p className="demo">Demo logins: <code>user@bookmyhive.com</code> / <code>User@123</code> or admin <code>admin@bookmyhive.com</code> / <code>Admin@123</code></p>}
      </form>
    </main>
  );
}
