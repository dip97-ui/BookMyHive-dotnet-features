const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });

export const money = (n) => inr.format(Number(n || 0));

export const fmtDate = (iso, opts = { weekday: 'short', day: 'numeric', month: 'short' }) =>
  new Date(iso).toLocaleDateString('en-IN', opts);

export const fmtTime = (iso) =>
  new Date(iso).toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit', hour12: true });

export const fmtDateTime = (iso) => `${fmtDate(iso, { day: 'numeric', month: 'short', year: 'numeric' })}, ${fmtTime(iso)}`;

export const fmtDuration = (min) => `${Math.floor(min / 60)}h ${String(min % 60).padStart(2, '0')}m`;

/** yyyy-mm-dd part of an ISO local date-time, used to group shows by day. */
export const dayKey = (iso) => iso.slice(0, 10);

export const rowLabel = (index) => String.fromCharCode(65 + index);
