import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api, qs } from '../api.js';
import EventCard from '../components/EventCard.jsx';
import HeroMarquee from '../components/HeroMarquee.jsx';
import Poster from '../components/Poster.jsx';

const LANGUAGES = ['Hindi', 'English', 'Marathi', 'Tamil'];

export default function Home() {
  const [params, setParams] = useSearchParams();
  const type = params.get('type') || '';
  const q = params.get('q') || '';
  const genre = params.get('genre') || '';
  const language = params.get('language') || '';

  const [events, setEvents] = useState(null);
  const [featured, setFeatured] = useState([]);
  const [catalogue, setCatalogue] = useState([]);
  const [genres, setGenres] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/genres').then(setGenres).catch(() => {});
    api.get('/events').then((all) => { setCatalogue(all); setFeatured([...all].sort((a, b) => b.rating - a.rating).slice(0, 3)); }).catch(() => {});
  }, []);

  useEffect(() => {
    setEvents(null);
    setError('');
    api.get(`/events${qs({ type, q, genre, language })}`).then(setEvents).catch((e) => setError(e.message));
  }, [type, q, genre, language]);

  const setFilter = (key, value) => {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value); else next.delete(key);
    setParams(next);
  };

  const heading = q ? `Results for “${q}”` : type === 'PLAY' ? 'Plays on stage' : type === 'MOVIE' ? 'Movies in cinemas' : 'Now showing';
  const filtered = !!(type || q || genre || language);

  return (
    <main>
      {!filtered && <HeroMarquee fallbackEvents={catalogue} />}
      {!filtered && (
        <section className="hero">
          <div className="hero__text">
            <h1>Every seat in the house, one cell at a time.</h1>
            <p>Browse new releases and live theatre, see exactly which seats are free, and book in under a minute.</p>
            <div className="hero__cta">
              <Link className="btn btn--lg" to="/?type=MOVIE">Find a movie</Link>
              <Link className="btn btn--lg btn--ghost-light" to="/?type=PLAY">Find a play</Link>
            </div>
          </div>
          <div className="hero__comb" aria-label="Top rated right now">
            {featured.map((e, i) => (
              <Link key={e.id} to={`/events/${e.id}`} className={`hexcell hexcell--${i}`} aria-label={`${e.title}, rated ${e.rating}`}>
                <Poster event={e} />
              </Link>
            ))}
          </div>
        </section>
      )}

      <div className="page">
        <div className="filters">
          <h2>{heading}</h2>
          <div className="filters__row">
            <div className="chips" role="group" aria-label="Genre">
              <button className={`chip ${!genre ? 'is-on' : ''}`} aria-pressed={!genre} onClick={() => setFilter('genre', '')}>All genres</button>
              {genres.map((g) => (
                <button key={g.id} className={`chip ${genre === g.name ? 'is-on' : ''}`} aria-pressed={genre === g.name} onClick={() => setFilter('genre', g.name)}>
                  {g.name}
                </button>
              ))}
            </div>
            <label className="langpick">
              Language
              <select value={language} onChange={(e) => setFilter('language', e.target.value)}>
                <option value="">Any</option>
                {LANGUAGES.map((l) => <option key={l}>{l}</option>)}
              </select>
            </label>
          </div>
        </div>

        {error && <p className="alert alert--err" role="alert">{error}</p>}
        {events === null && !error && <p className="muted">Loading titles…</p>}
        {events && events.length === 0 && (
          <div className="empty">
            <p>Nothing matches those filters yet.</p>
            <Link className="btn btn--ghost" to="/">Clear filters</Link>
          </div>
        )}
        {events && events.length > 0 && (
          <div className="grid">{events.map((e) => <EventCard key={e.id} event={e} />)}</div>
        )}
      </div>
    </main>
  );
}
