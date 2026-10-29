import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { extrasApi } from '../api.js';
import { useAuth } from '../context.jsx';

function StarRow({ value }) {
  return (
    <span className="stars" aria-label={`${value} out of 5 stars`}>
      {[1, 2, 3, 4, 5].map((n) => <span key={n} className={`star ${n <= Math.round(value) ? 'star--on' : ''}`}>&#9733;</span>)}
    </span>
  );
}

function StarPicker({ value, onChange }) {
  return (
    <div className="stars stars--picker" role="radiogroup" aria-label="Your rating">
      {[1, 2, 3, 4, 5].map((n) => (
        <button type="button" key={n} className={`star star--btn ${n <= value ? 'star--on' : ''}`}
          role="radio" aria-checked={n === value} aria-label={`${n} star${n > 1 ? 's' : ''}`}
          onClick={() => onChange(n)}>&#9733;</button>
      ))}
    </div>
  );
}

/** Reviews + comments for one event, served by the .NET extras service. */
export default function EventExtras({ eventId }) {
  const { user } = useAuth();
  const [reviews, setReviews] = useState(null);
  const [summary, setSummary] = useState(null);
  const [comments, setComments] = useState(null);
  const [offline, setOffline] = useState(false);

  const [rating, setRating] = useState(0);
  const [reviewTitle, setReviewTitle] = useState('');
  const [reviewBody, setReviewBody] = useState('');
  const [commentBody, setCommentBody] = useState('');
  const [formError, setFormError] = useState('');
  const [commentError, setCommentError] = useState('');

  const load = useCallback(() => {
    setOffline(false);
    Promise.all([
      extrasApi.get(`/events/${eventId}/reviews`),
      extrasApi.get(`/events/${eventId}/reviews/summary`),
      extrasApi.get(`/events/${eventId}/comments`),
    ]).then(([r, s, c]) => { setReviews(r); setSummary(s); setComments(c); })
      .catch(() => setOffline(true));
  }, [eventId]);

  useEffect(() => { load(); }, [load]);

  const submitReview = async (e) => {
    e.preventDefault();
    setFormError('');
    if (rating < 1) { setFormError('Pick a star rating first.'); return; }
    try {
      await extrasApi.post(`/events/${eventId}/reviews`, {
        authorName: user?.name, rating, title: reviewTitle || undefined, body: reviewBody,
      });
      setRating(0); setReviewTitle(''); setReviewBody('');
      load();
    } catch (err2) { setFormError(err2.message); }
  };

  const submitComment = async (e) => {
    e.preventDefault();
    setCommentError('');
    try {
      await extrasApi.post(`/events/${eventId}/comments`, { authorName: user?.name, body: commentBody });
      setCommentBody('');
      load();
    } catch (err2) { setCommentError(err2.message); }
  };

  if (offline) {
    return (
      <section className="page extras">
        <h2>Reviews &amp; comments</h2>
        <p className="muted">Reviews are unavailable right now (extras service is offline).</p>
      </section>
    );
  }

  const mine = user && reviews?.some((r) => r.authorName === user.name);

  return (
    <section className="page extras">
      <h2>Reviews &amp; comments</h2>

      <div className="extras__summary">
        {summary && summary.count > 0 ? (
          <><StarRow value={summary.averageRating} /> <strong>{summary.averageRating.toFixed(1)}</strong>
            <span className="muted"> from {summary.count} review{summary.count > 1 ? 's' : ''}</span></>
        ) : <span className="muted">No reviews yet - be the first.</span>}
      </div>

      {user ? (
        <form className="extras__form" onSubmit={submitReview}>
          <h3>{mine ? 'Your review is posted' : 'Write a review'}</h3>
          {!mine && (
            <>
              <StarPicker value={rating} onChange={setRating} />
              <input type="text" placeholder="Headline (optional)" value={reviewTitle}
                maxLength={120} onChange={(e) => setReviewTitle(e.target.value)} />
              <textarea placeholder="How was it?" rows={3} value={reviewBody} required
                maxLength={2000} onChange={(e) => setReviewBody(e.target.value)} />
              <button className="btn" type="submit">Post review</button>
            </>
          )}
          {formError && <p className="alert alert--err" role="alert">{formError}</p>}
        </form>
      ) : (
        <p className="muted"><Link to="/login">Log in</Link> to write a review or comment.</p>
      )}

      <ul className="extras__list">
        {(reviews || []).map((r) => (
          <li key={r.id} className="review">
            <div className="review__head">
              <strong>{r.authorName}</strong>
              <StarRow value={r.rating} />
              <span className="muted">{new Date(r.createdAt).toLocaleDateString()}</span>
            </div>
            {r.title && <p className="review__title">{r.title}</p>}
            <p>{r.body}</p>
          </li>
        ))}
      </ul>

      <h3>Comments</h3>
      {user && (
        <form className="extras__form" onSubmit={submitComment}>
          <textarea placeholder="Ask or share something about this event" rows={2} value={commentBody}
            required maxLength={1000} onChange={(e) => setCommentBody(e.target.value)} />
          <button className="btn btn--ghost" type="submit">Post comment</button>
          {commentError && <p className="alert alert--err" role="alert">{commentError}</p>}
        </form>
      )}
      <ul className="extras__list">
        {(comments || []).map((c) => (
          <li key={c.id} className="comment">
            <div className="review__head">
              <strong>{c.authorName}</strong>
              <span className="muted">{new Date(c.createdAt).toLocaleString()}</span>
            </div>
            <p>{c.body}</p>
          </li>
        ))}
      </ul>
    </section>
  );
}
