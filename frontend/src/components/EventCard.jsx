import { Link } from 'react-router-dom';
import Poster from './Poster.jsx';
import RatingHex from './RatingHex.jsx';

export default function EventCard({ event }) {
  return (
    <Link to={`/events/${event.id}`} className="ecard">
      <div className="ecard__media">
        <Poster event={event} />
        {event.type === 'PLAY' && <span className="ecard__tag">Live play</span>}
        <RatingHex value={event.rating} />
      </div>
      <h3 className="ecard__title">{event.title}</h3>
      <p className="ecard__meta">{event.genres.map((g) => g.name).join(', ') || event.language}</p>
    </Link>
  );
}
