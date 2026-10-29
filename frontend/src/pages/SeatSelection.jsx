import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { useAuth } from '../context.jsx';
import { fmtDateTime, money, rowLabel } from '../utils.js';

const MAX_SEATS = 10;
const PAYMENT_METHODS = ['UPI', 'Card', 'Net banking'];

export default function SeatSelection() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [show, setShow] = useState(null);
  const [selected, setSelected] = useState([]);
  const [method, setMethod] = useState(PAYMENT_METHODS[0]);
  const [error, setError] = useState('');
  const [loadError, setLoadError] = useState('');
  const [paying, setPaying] = useState(false);

  const loadShow = useCallback(() => api.get(`/shows/${id}`).then((s) => {
    setShow(s);
    setSelected((sel) => sel.filter((seat) => !s.bookedSeats.includes(seat)));
    return s;
  }), [id]);

  useEffect(() => { loadShow().catch((e) => setLoadError(e.message)); }, [loadShow]);

  const booked = useMemo(() => new Set(show?.bookedSeats || []), [show]);

  if (loadError) return <main className="page narrow"><p className="alert alert--err" role="alert">{loadError}</p><Link className="btn" to="/">Back to browse</Link></main>;
  if (!show) return <main className="page"><p className="muted">Loading seat map…</p></main>;

  const isPlay = show.event.type === 'PLAY';
  const total = selected.length * Number(show.price);

  const toggle = (seat) => {
    setError('');
    setSelected((sel) => {
      if (sel.includes(seat)) return sel.filter((s) => s !== seat);
      if (sel.length >= MAX_SEATS) { setError(`You can book up to ${MAX_SEATS} seats at a time.`); return sel; }
      return [...sel, seat];
    });
  };

  const pay = async () => {
    if (!user) return navigate('/login', { state: { from: location.pathname } });
    setPaying(true);
    setError('');
    try {
      const booking = await api.post('/bookings', { showId: show.id, seats: selected, paymentMethod: method });
      navigate('/bookings', { state: { justBooked: booking.reference } });
    } catch (e) {
      setError(e.message);
      if (e.status === 409) await loadShow().catch(() => {}); // someone else took a seat: refresh the map
    } finally {
      setPaying(false);
    }
  };

  const rows = Array.from({ length: show.seatRows }, (_, r) => rowLabel(r));

  return (
    <main className="page seatpage">
      <div className="seatpage__map">
        <p className="muted"><Link to={`/events/${show.event.id}`}>← {show.event.title}</Link></p>
        <h1>{show.venue.name}</h1>
        <p className="muted">{fmtDateTime(show.showTime)}, {money(show.price)} per seat, {show.availableSeats} of {show.totalSeats} seats free</p>

        <div className="screen" aria-hidden="true">{isPlay ? 'Stage' : 'Screen'}</div>

        <div className="seatmap" role="group" aria-label="Seat map">
          {rows.map((row, r) => (
            <div key={row} className={`seatrow ${r % 2 ? 'is-offset' : ''}`}>
              <span className="seatrow__label" aria-hidden="true">{row}</span>
              {Array.from({ length: show.seatsPerRow }, (_, n) => {
                const label = `${row}${n + 1}`;
                const isBooked = booked.has(label);
                const isOn = selected.includes(label);
                return (
                  <button
                    key={label}
                    type="button"
                    className={`seat ${isBooked ? 'is-booked' : ''} ${isOn ? 'is-on' : ''}`}
                    disabled={isBooked}
                    aria-pressed={isOn}
                    aria-label={`Seat ${label}, ${isBooked ? 'taken' : isOn ? 'selected' : 'available'}`}
                    onClick={() => toggle(label)}
                  >
                    {isBooked ? '×' : isOn ? '✓' : n + 1}
                  </button>
                );
              })}
            </div>
          ))}
        </div>

        <ul className="legend">
          <li><span className="seat seat--legend" /> Available</li>
          <li><span className="seat seat--legend is-on" /> Selected</li>
          <li><span className="seat seat--legend is-booked" /> Taken</li>
        </ul>
      </div>

      <aside className="summary" aria-label="Booking summary">
        <h2>{show.event.title}</h2>
        <p className="muted">{show.venue.name}, {show.venue.city}</p>
        <dl className="summary__list">
          <dt>Seats</dt>
          <dd>{selected.length ? [...selected].sort().join(', ') : 'None selected yet'}</dd>
          <dt>Price</dt>
          <dd>{selected.length} × {money(show.price)}</dd>
        </dl>
        <p className="summary__total"><span>Total</span><strong>{money(total)}</strong></p>

        <fieldset className="paymethods">
          <legend>Pay with</legend>
          {PAYMENT_METHODS.map((m) => (
            <label key={m} className="check"><input type="radio" name="pay" checked={method === m} onChange={() => setMethod(m)} /> {m}</label>
          ))}
        </fieldset>

        {error && <p className="alert alert--err" role="alert">{error}</p>}
        <button className="btn btn--lg btn--block" disabled={!selected.length || paying} onClick={pay}>
          {paying ? 'Booking…' : !user ? 'Log in to pay' : `Pay ${money(total)}`}
        </button>
        <p className="muted small">This is a demo. No money is charged.</p>
      </aside>
    </main>
  );
}
