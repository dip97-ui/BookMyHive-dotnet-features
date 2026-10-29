import { useState } from 'react';
import { Link, NavLink, useNavigate, useSearchParams } from 'react-router-dom';
import Logo from './Logo.jsx';
import { CITIES, useAuth, useCity } from '../context.jsx';

export default function Navbar() {
  const { user, isAdmin, logout } = useAuth();
  const { city, setCity } = useCity();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [q, setQ] = useState(params.get('q') || '');
  const type = params.get('type') || '';

  const search = (e) => {
    e.preventDefault();
    navigate(q.trim() ? `/?q=${encodeURIComponent(q.trim())}` : '/');
  };

  const typeLink = (value, label) => (
    <Link to={value ? `/?type=${value}` : '/'} className={`subnav__link ${type === value ? 'is-active' : ''}`}>
      {label}
    </Link>
  );

  return (
    <header className="nav">
      <div className="nav__bar">
        <Link to="/" className="brand" aria-label="BookMyHive home">
          <Logo />
          <span>BookMyHive</span>
        </Link>

        <form className="nav__search" onSubmit={search} role="search">
          <input
            type="search"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Search movies and plays"
            aria-label="Search movies and plays"
          />
        </form>

        <label className="nav__city">
          <span className="sr-only">City</span>
          <select value={city} onChange={(e) => setCity(e.target.value)}>
            {CITIES.map((c) => <option key={c}>{c}</option>)}
          </select>
        </label>

        <div className="nav__user">
          {user ? (
            <>
              <NavLink to="/bookings" className="nav__link">My bookings</NavLink>
              {isAdmin && <NavLink to="/admin" className="nav__link">Admin</NavLink>}
              <span className="nav__name" title={user.email}>Hi, {user.name.split(' ')[0]}</span>
              <button className="btn btn--ghost btn--sm" onClick={() => { logout(); navigate('/'); }}>Log out</button>
            </>
          ) : (
            <>
              <Link to="/register" className="nav__link">Sign up</Link>
              <Link to="/login" className="btn btn--sm">Log in</Link>
            </>
          )}
        </div>
      </div>
      <nav className="subnav" aria-label="Browse">
        {typeLink('', 'All')}
        {typeLink('MOVIE', 'Movies')}
        {typeLink('PLAY', 'Plays')}
      </nav>
    </header>
  );
}
