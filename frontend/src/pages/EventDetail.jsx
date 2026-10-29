import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, qs } from '../api.js';
import { useCity } from '../context.jsx';
import Poster from '../components/Poster.jsx';
import RatingHex from '../components/RatingHex.jsx';
import EventExtras from '../components/EventExtras.jsx';
import { dayKey, fmtDate, fmtDuration, fmtTime, money } from '../utils.js';

export default function EventDetail() {
  const { id } = useParams();
  const { city } = useCity();
  const [event, setEvent] = useState(null);
  const [shows, setShows] = useState(null);
  const [error, setError] = useState('');
  const [day, setDay] = useState('');

  useEffect(() => {
    setEvent(null);
    setError('');
    api.get(`/events/${id}`).then(setEvent).catch((e) => setError(e.message));
  }, [id]);

  useEffect(() => {
    setShows(null);
    api.get(`/events/${id}/shows${qs({ city })}`).then((list) => {
      setShows(list);
      setDay((d) => (list.some((s) => dayKey(s.showTime) === d) ? d : list[0] ? dayKey(list[0].showTime) : ''));
    }).catch(() => setShows([]));
  }, [id, city]);

  const days = useMemo(() => [...new Set((shows || []).map((s) => dayKey(s.showTime)))], [shows]);
  const venues = useMemo(() => {
    const map = new Map();
    (shows || []).filter((s) => dayKey(s.showTime) === day).forEach((s) => {
      if (!map.has(s.venue.id)) map.set(s.venue.id, { venue: s.venue, shows: [] });
      map.get(s.venue.id).shows.push(s);
    });
    return [...map.values()];
  }, [shows, day]);

  if (error) return <main className="page narrow"><p className="alert alert--err" role="alert">{error}</p><Link to="/" className="btn">Back to browse</Link></main>;
  if (!event) return <main className="page"><p className="muted">Loading…</p></main>;

  return (
    <main>
      <section className="detail">
        <div className="detail__inner">
          <Poster event={event} className="detail__poster" />
          <div className="detail__info">
            <h1>{event.title}</h1>
            <div className="detail__rating">
              <RatingHex value={event.rating} />
              <span>{event.type === 'PLAY' ? 'Live play' : 'Movie'}</span>
            </div>
            <p className="detail__meta">
              {fmtDuration(event.durationMinutes)}, {event.language}
              {event.certification ? `, rated ${event.certification}` : ''}
              {event.releaseDate ? `, released ${fmtDate(event.releaseDate, { day: 'numeric', month: 'short', year: 'numeric' })}` : ''}
            </p>
            <div className="chips">{event.genres.map((g) => <span key={g.id} className="chip chip--static">{g.name}</span>)}</div>
            <p className="detail__desc">{event.description}</p>
            {event.director && (
              <p className="detail__director" style={{ marginTop: '0.9rem', fontSize: '0.95rem', color: 'var(--comb)' }}>
                <strong style={{ color: 'var(--honey)' }}>Director:</strong> {event.director}
              </p>
            )}
            {event.cast && (
              <p className="detail__cast" style={{ marginTop: '0.4rem', fontSize: '0.95rem', color: 'var(--comb)' }}>
                <strong style={{ color: 'var(--honey)' }}>Cast:</strong> {event.cast}
              </p>
            )}
          </div>
        </div>
      </section>

      <section className="page" aria-labelledby="shows-h">
        <h2 id="shows-h">Choose a show in {city}</h2>
        {shows === null && <p className="muted">Loading showtimes…</p>}
        {shows && shows.length === 0 && (
          <div className="empty">
            <p>No upcoming shows in {city}. Pick another city from the top bar to see where this is playing.</p>
          </div>
        )}
        {shows && shows.length > 0 && (
          <>
            <div className="days" role="tablist" aria-label="Date">
              {days.map((d) => (
                <button key={d} role="tab" aria-selected={d === day} className={`day ${d === day ? 'is-on' : ''}`} onClick={() => setDay(d)}>
                  {fmtDate(`${d}T00:00:00`)}
                </button>
              ))}
            </div>
            <ul className="venues">
              {venues.map(({ venue, shows: list }) => (
                <li key={venue.id} className="venue">
                  <div className="venue__info">
                    <h3>{venue.name}</h3>
                    <p className="muted">{venue.address}</p>
                  </div>
                  <div className="slots">
                    {list.map((s) => {
                      const soldOut = s.availableSeats === 0;
                      const low = s.availableSeats / s.totalSeats < 0.2;
                      const body = (
                        <>
                          <strong>{fmtTime(s.showTime)}</strong>
                          <small>{soldOut ? 'Sold out' : low ? `${s.availableSeats} left` : money(s.price)}</small>
                        </>
                      );
                      return soldOut
                        ? <span key={s.id} className="slot is-off" aria-disabled="true">{body}</span>
                        : <Link key={s.id} to={`/shows/${s.id}/seats`} className={`slot ${low ? 'is-low' : ''}`}>{body}</Link>;
                    })}
                  </div>
                </li>
              ))}
            </ul>
          </>
        )}
      </section>
          <EventExtras eventId={id} />
    </main>
  );
}
