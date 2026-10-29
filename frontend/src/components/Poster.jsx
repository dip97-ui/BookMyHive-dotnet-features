import { useState } from 'react';

/** Shows the poster image, or a honey-toned generated cover when there is none (or it fails to load). */
export default function Poster({ event, className = '' }) {
  const [failed, setFailed] = useState(false);

  if (event.posterUrl && !failed) {
    return <img className={`poster ${className}`} src={event.posterUrl} alt={`${event.title} poster`} onError={() => setFailed(true)} loading="lazy" />;
  }
  const hue = 24 + ((event.id * 17) % 30);
  return (
    <div
      className={`poster poster--generated ${className}`}
      style={{ background: `linear-gradient(165deg, hsl(${hue} 78% 42%), hsl(${hue - 6} 62% 14%))` }}
      role="img"
      aria-label={`${event.title} cover`}
    >
      <span className="poster__title">{event.title}</span>
    </div>
  );
}
