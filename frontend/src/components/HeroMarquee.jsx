import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { extrasApi } from '../api.js';

const ROTATE_MS = 5000; // auto-advance between the 3 visible slides
const WINDOW = 3; // how many titles the hero shows at a time
const REFRESH_MS = 6 * 60 * 60 * 1000; // re-pick the 3 featured titles every 6 hours
const TICK_MS = 30000; // how often the open page re-checks the rotation window
const FALLBACK_POOL = 10; // catalogue pool size used when no banners are managed

/**
 * PVR-style hero: a full-width auto-sliding banner carousel.
 * Slide pool comes from the .NET extras service (admin-managed banners);
 * when it has none, falls back to top-rated events that have a backdrop image.
 * The hero shows WINDOW slides at a time and rotates which pool items are
 * featured every REFRESH_MS, so the selection refreshes on its own over time.
 */
export default function HeroMarquee({ fallbackEvents = [] }) {
  const [banners, setBanners] = useState(null);
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const [broken, setBroken] = useState(() => new Set());
  // Ticking clock so the featured window rotates even while the page stays open.
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), TICK_MS);
    return () => clearInterval(t);
  }, []);

  useEffect(() => {
    let on = true;
    extrasApi.get('/hero-banners').then((b) => on && setBanners(b)).catch(() => on && setBanners([]));
    return () => { on = false; };
  }, []);

  const fromApi = (banners || []).map((b) => ({
    key: `b${b.id}`, title: b.title, subtitle: b.subtitle, image: b.imageUrl,
    to: b.ctaUrl || (b.eventId ? `/events/${b.eventId}` : '/'),
  }));
  const pool = fromApi.length > 0
    ? fromApi
    : fallbackEvents
        .filter((e) => e.backdropUrl)
        .sort((a, b) => (b.rating || 0) - (a.rating || 0))
        .slice(0, FALLBACK_POOL)
        .map((e) => ({ key: `e${e.id}`, title: e.title, subtitle: e.language, image: e.backdropUrl, to: `/events/${e.id}` }));

  // Time-based rotation: the featured window shifts every REFRESH_MS, so the
  // hero cycles through the whole pool instead of showing the same titles.
  const start = pool.length > WINDOW ? Math.floor(now / REFRESH_MS) % pool.length : 0;
  const slides = pool.length <= WINDOW
    ? pool
    : Array.from({ length: WINDOW }, (_, i) => pool[(start + i) % pool.length]);

  useEffect(() => {
    if (paused || slides.length < 2) return undefined;
    const t = setInterval(() => setIndex((i) => (i + 1) % slides.length), ROTATE_MS);
    return () => clearInterval(t);
  }, [paused, slides.length]);

  if (slides.length === 0) return null;
  const go = (n) => setIndex((i) => (i + n + slides.length) % slides.length);
  const current = index % slides.length;

  return (
    <section
      className="marquee"
      aria-roledescription="carousel"
      aria-label="Featured titles"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
    >
      <div className="marquee__track" style={{ transform: `translateX(-${current * 100}%)` }}>
        {slides.map((s) => (
          <div className={`marquee__slide${broken.has(s.key) ? ' marquee__slide--noimg' : ''}`} key={s.key}>
            {!broken.has(s.key) && (
              <img
                src={s.image}
                alt=""
                className="marquee__bg"
                loading="lazy"
                onError={() => setBroken((prev) => new Set(prev).add(s.key))}
              />
            )}
            <div className="marquee__scrim" />
            <div className="marquee__content">
              <h2>{s.title}</h2>
              {s.subtitle && <p>{s.subtitle}</p>}
              <Link className="btn btn--lg" to={s.to}>Book now</Link>
            </div>
          </div>
        ))}
      </div>
      {slides.length > 1 && (
        <>
          <button type="button" className="marquee__nav marquee__nav--prev" onClick={() => go(-1)} aria-label="Previous slide">&lsaquo;</button>
          <button type="button" className="marquee__nav marquee__nav--next" onClick={() => go(1)} aria-label="Next slide">&rsaquo;</button>
          <div className="marquee__dots">
            {slides.map((s, i) => (
              <button
                type="button"
                key={s.key}
                className={`marquee__dot ${i === current ? 'is-on' : ''}`}
                aria-label={`Go to slide ${i + 1}`}
                aria-pressed={i === current}
                onClick={() => setIndex(i)}
              />
            ))}
          </div>
        </>
      )}
    </section>
  );
}
