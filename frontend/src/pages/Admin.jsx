import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import ResourceManager from '../components/ResourceManager.jsx';
import { fmtDateTime, money } from '../utils.js';

// Stable function references so ResourceManager does not reload in a loop.
const loadEvents = () => api.get('/admin/events');
const loadShows = () => api.get('/admin/shows');
const loadVenues = () => api.get('/venues');
const loadGenres = () => api.get('/genres');

const TABS = ['Events', 'Shows', 'Venues', 'Genres', 'Bookings'];

function Stats() {
  const [s, setS] = useState(null);
  useEffect(() => { api.get('/admin/stats').then(setS).catch(() => {}); }, []);
  if (!s) return null;
  const items = [['Events', s.events], ['Venues', s.venues], ['Shows', s.shows], ['Bookings', s.bookings], ['Users', s.users], ['Revenue', money(s.revenue)]];
  return (
    <dl className="stats">
      {items.map(([label, value]) => (<div key={label}><dd>{value}</dd><dt>{label}</dt></div>))}
    </dl>
  );
}

function Bookings() {
  const [rows, setRows] = useState(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const load = useCallback(() => api.get('/admin/bookings').then(setRows).catch((e) => setError(e.message)), []);
  useEffect(() => { load(); }, [load]);

  const cancel = async (b) => {
    if (!window.confirm(`Cancel booking ${b.reference}?`)) return;
    try {
      await api.patch(`/bookings/${b.id}/cancel`);
      setNotice(`Booking ${b.reference} cancelled.`);
      setError('');
      load();
    } catch (e) {
      setNotice('');
      setError(e.message);
    }
  };

  return (
    <section aria-label="Bookings">
      <div className="admin__head"><div><h2>Bookings</h2><p className="muted">All customer bookings. Admins can cancel any upcoming booking.</p></div></div>
      {notice && <p className="alert alert--ok" role="status">{notice}</p>}
      {error && <p className="alert alert--err" role="alert">{error}</p>}
      {rows === null ? <p className="muted">Loading…</p> : rows.length === 0 ? <p className="empty">No bookings yet.</p> : (
        <div className="tablewrap">
          <table className="table">
            <thead><tr><th>Reference</th><th>Customer</th><th>Event</th><th>Show time</th><th>Seats</th><th>Total</th><th>Status</th><th><span className="sr-only">Actions</span></th></tr></thead>
            <tbody>
              {rows.map((b) => (
                <tr key={b.id}>
                  <td><code>{b.reference}</code></td>
                  <td>{b.customerName}<br /><small className="muted">{b.customerEmail}</small></td>
                  <td>{b.event.title}<br /><small className="muted">{b.venueName}</small></td>
                  <td>{fmtDateTime(b.showTime)}</td>
                  <td>{b.seats.join(', ')}</td>
                  <td>{money(b.totalAmount)}</td>
                  <td><span className={`status status--${b.status.toLowerCase()}`}>{b.status === 'CONFIRMED' ? '✓ Confirmed' : '✕ Cancelled'}</span></td>
                  <td>{b.status === 'CONFIRMED' && new Date(b.showTime) > new Date() && <button className="btn btn--danger btn--sm" onClick={() => cancel(b)}>Cancel</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export default function Admin() {
  const [tab, setTab] = useState('Events');
  const [lookups, setLookups] = useState({ genres: [], venues: [], events: [] });
  const [statsKey, setStatsKey] = useState(0);
  const [syncing, setSyncing] = useState(false);
  const [syncMsg, setSyncMsg] = useState('');

  const refresh = useCallback(() => {
    Promise.all([loadGenres(), loadVenues(), loadEvents()])
      .then(([genres, venues, events]) => setLookups({ genres, venues, events }))
      .catch(() => {});
    setStatsKey((k) => k + 1);
  }, []);
  useEffect(refresh, [refresh]);

  const opt = (list, label) => list.map((x) => ({ value: x.id, label: label(x) }));

  return (
    <main className="page">
      <h1>Admin console</h1>
      <Stats key={statsKey} />

      <div className="tabs" role="tablist" aria-label="Manage">
        {TABS.map((t) => (
          <button key={t} role="tab" aria-selected={tab === t} className={`tab ${tab === t ? 'is-on' : ''}`} onClick={() => setTab(t)}>{t}</button>
        ))}
      </div>

      {tab === 'Events' && (
        <>
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '1rem', gap: '0.75rem', alignItems: 'center' }}>
            {syncMsg && <span style={{ color: 'var(--amber)', fontSize: '0.9rem' }}>{syncMsg}</span>}
            <button
              className="btn btn--sm"
              disabled={syncing}
              onClick={async () => {
                setSyncing(true);
                setSyncMsg('Syncing TMDB movies…');
                try {
                  const res = await api.post('/tmdb/sync');
                  setSyncMsg(`✓ ${res.message || 'Synced movies from TMDB!'}`);
                  refresh();
                } catch (e) {
                  setSyncMsg(`Sync failed: ${e.message}`);
                } finally {
                  setSyncing(false);
                }
              }}
            >
              {syncing ? 'Syncing…' : '↻ Sync Upcoming Movies from TMDB'}
            </button>
          </div>
          <ResourceManager
            title="Events" singular="Event" hint="Movies and plays. Inactive events are hidden from the public catalogue."
            load={loadEvents} create={(d) => api.post('/events', d)} update={(id, d) => api.put(`/events/${id}`, d)} remove={(id) => api.del(`/events/${id}`)}
            onChanged={refresh}
            columns={[
              { label: 'Title', key: 'title' },
              { label: 'Type', render: (r) => (r.type === 'PLAY' ? 'Play' : 'Movie') },
              { label: 'Language', key: 'language' },
              { label: 'Genres', render: (r) => r.genres.map((g) => g.name).join(', ') },
              { label: 'Rating', render: (r) => r.rating ?? '–' },
              { label: 'Status', render: (r) => (r.active ? 'Active' : 'Hidden') },
            ]}
            defaults={{ title: '', type: 'MOVIE', language: '', durationMinutes: 120, certification: 'UA', rating: '', releaseDate: '', posterUrl: '', description: '', genreIds: [], active: true }}
            toForm={(r) => ({ ...r, certification: r.certification || '', rating: r.rating ?? '', releaseDate: r.releaseDate || '', posterUrl: r.posterUrl || '', genreIds: r.genres.map((g) => g.id) })}
            toPayload={(f) => ({ ...f, durationMinutes: Number(f.durationMinutes), rating: f.rating === '' ? null : Number(f.rating), releaseDate: f.releaseDate || null })}
            fields={[
              { name: 'title', label: 'Title', required: true, wide: true },
              { name: 'type', label: 'Type', type: 'select', required: true, options: [{ value: 'MOVIE', label: 'Movie' }, { value: 'PLAY', label: 'Play' }] },
              { name: 'language', label: 'Language', required: true },
              { name: 'durationMinutes', label: 'Duration (minutes)', type: 'number', required: true },
              { name: 'certification', label: 'Certification', type: 'select', options: ['U', 'UA', 'A'].map((c) => ({ value: c, label: c })) },
              { name: 'rating', label: 'Rating (0–10)', type: 'number', step: '0.1' },
              { name: 'releaseDate', label: 'Release date', type: 'date' },
              { name: 'posterUrl', label: 'Poster image URL (optional)', wide: true },
              { name: 'description', label: 'Description', type: 'textarea', required: true },
              { name: 'genreIds', label: 'Genres', type: 'checks', options: opt(lookups.genres, (g) => g.name) },
              { name: 'active', label: 'Visible in the public catalogue', type: 'checkbox' },
            ]}
          />
        </>
      )}

      {tab === 'Shows' && (
        <ResourceManager
          title="Shows" singular="Show" hint="A show is one screening or performance. Seat layout is locked once someone has booked."
          load={loadShows} create={(d) => api.post('/shows', d)} update={(id, d) => api.put(`/shows/${id}`, d)} remove={(id) => api.del(`/shows/${id}`)}
          onChanged={refresh}
          columns={[
            { label: 'Event', render: (r) => r.event.title },
            { label: 'Venue', render: (r) => `${r.venue.name}, ${r.venue.city}` },
            { label: 'Starts', render: (r) => fmtDateTime(r.showTime) },
            { label: 'Price', render: (r) => money(r.price) },
            { label: 'Seats free', render: (r) => `${r.availableSeats} / ${r.totalSeats}` },
          ]}
          defaults={{ eventId: '', venueId: '', showTime: '', price: 250, seatRows: 8, seatsPerRow: 12 }}
          toForm={(r) => ({ eventId: r.event.id, venueId: r.venue.id, showTime: r.showTime.slice(0, 16), price: r.price, seatRows: r.seatRows, seatsPerRow: r.seatsPerRow })}
          toPayload={(f) => ({ eventId: Number(f.eventId), venueId: Number(f.venueId), showTime: f.showTime, price: Number(f.price), seatRows: Number(f.seatRows), seatsPerRow: Number(f.seatsPerRow) })}
          fields={[
            { name: 'eventId', label: 'Event', type: 'select', required: true, options: opt(lookups.events, (e) => e.title) },
            { name: 'venueId', label: 'Venue', type: 'select', required: true, options: opt(lookups.venues, (v) => `${v.name} (${v.city})`) },
            { name: 'showTime', label: 'Date and time', type: 'datetime', required: true },
            { name: 'price', label: 'Price per seat (₹)', type: 'number', required: true },
            { name: 'seatRows', label: 'Rows (A, B, C…)', type: 'number', required: true },
            { name: 'seatsPerRow', label: 'Seats per row', type: 'number', required: true },
          ]}
        />
      )}

      {tab === 'Venues' && (
        <ResourceManager
          title="Venues" singular="Venue" hint="Theatres and halls. A venue with shows cannot be deleted."
          load={loadVenues} create={(d) => api.post('/venues', d)} update={(id, d) => api.put(`/venues/${id}`, d)} remove={(id) => api.del(`/venues/${id}`)}
          onChanged={refresh}
          columns={[{ label: 'Name', key: 'name' }, { label: 'City', key: 'city' }, { label: 'Address', key: 'address' }]}
          defaults={{ name: '', city: '', address: '' }}
          fields={[
            { name: 'name', label: 'Venue name', required: true, wide: true },
            { name: 'city', label: 'City', required: true },
            { name: 'address', label: 'Address', required: true },
          ]}
        />
      )}

      {tab === 'Genres' && (
        <ResourceManager
          title="Genres" singular="Genre" hint="A genre used by an event cannot be deleted."
          load={loadGenres} create={(d) => api.post('/genres', d)} update={(id, d) => api.put(`/genres/${id}`, d)} remove={(id) => api.del(`/genres/${id}`)}
          onChanged={refresh}
          columns={[{ label: 'Name', key: 'name' }]}
          defaults={{ name: '' }}
          fields={[{ name: 'name', label: 'Genre name', required: true, wide: true }]}
        />
      )}

      {tab === 'Bookings' && <Bookings />}
    </main>
  );
}
