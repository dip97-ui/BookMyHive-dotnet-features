import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { api } from '../api.js';
import Poster from '../components/Poster.jsx';
import { fmtDateTime, money } from '../utils.js';

function Ticket({ b, onCancel }) {
  const upcoming = b.status === 'CONFIRMED' && new Date(b.showTime) > new Date();
  return (
    <li className={`ticket ${b.status === 'CANCELLED' ? 'is-cancelled' : ''}`}>
      <Poster event={b.event} className="ticket__poster" />
      <div className="ticket__body">
        <h3>{b.event.title}</h3>
        <p>{b.venueName}, {b.city}</p>
        <p>{fmtDateTime(b.showTime)}</p>
        <p>Seats <strong>{b.seats.join(', ')}</strong></p>
      </div>
      <div className="ticket__stub">
        <span className={`status status--${b.status.toLowerCase()}`}>{b.status === 'CONFIRMED' ? '✓ Confirmed' : '✕ Cancelled'}</span>
        <code>{b.reference}</code>
        <strong>{money(b.totalAmount)}</strong>
        <small>{b.paymentMethod}, {b.paymentStatus?.toLowerCase()}</small>
        {upcoming && <button className="btn btn--danger btn--sm" onClick={() => onCancel(b)}>Cancel booking</button>}
      </div>
    </li>
  );
}

export default function MyBookings() {
  const { state } = useLocation();
  const [bookings, setBookings] = useState(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState(state?.justBooked ? `Booked! Your reference is ${state.justBooked}.` : '');

  const load = useCallback(() => api.get('/bookings/my').then(setBookings).catch((e) => setError(e.message)), []);
  useEffect(() => { load(); }, [load]);

  const cancel = async (b) => {
    if (!window.confirm(`Cancel ${b.event.title} (${b.seats.join(', ')})? Your seats will be released.`)) return;
    try {
      await api.patch(`/bookings/${b.id}/cancel`);
      setNotice(`Booking ${b.reference} cancelled and refunded.`);
      setError('');
      load();
    } catch (e) {
      setNotice('');
      setError(e.message);
    }
  };

  const now = new Date();
  const upcoming = (bookings || []).filter((b) => b.status === 'CONFIRMED' && new Date(b.showTime) > now);
  const rest = (bookings || []).filter((b) => !upcoming.includes(b));

  return (
    <main className="page">
      <h1>My bookings</h1>
      {notice && <p className="alert alert--ok" role="status">{notice}</p>}
      {error && <p className="alert alert--err" role="alert">{error}</p>}
      {bookings === null && !error && <p className="muted">Loading…</p>}
      {bookings && bookings.length === 0 && (
        <div className="empty"><p>No tickets yet. Your bookings will show up here.</p><Link className="btn" to="/">Browse shows</Link></div>
      )}
      {upcoming.length > 0 && (<><h2>Upcoming</h2><ul className="tickets">{upcoming.map((b) => <Ticket key={b.id} b={b} onCancel={cancel} />)}</ul></>)}
      {rest.length > 0 && (<><h2>Past and cancelled</h2><ul className="tickets">{rest.map((b) => <Ticket key={b.id} b={b} onCancel={cancel} />)}</ul></>)}
    </main>
  );
}
