import { useCallback, useEffect, useState } from 'react';

/**
 * Config-driven CRUD table + modal form used by the admin console.
 * Field types: text, textarea, number, date, datetime, select, checkbox, checks.
 */
export default function ResourceManager({
  title, singular, load, create, update, remove, columns, fields, defaults, toForm, toPayload, onChanged, hint,
}) {
  const [rows, setRows] = useState(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [editing, setEditing] = useState(null); // { id?, form }
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  const reload = useCallback(() => {
    load().then(setRows).catch((e) => setError(e.message));
  }, [load]);

  useEffect(reload, [reload]);

  useEffect(() => {
    if (!editing) return undefined;
    const onKey = (e) => e.key === 'Escape' && setEditing(null);
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [editing]);

  const openForm = (row) => {
    setFormError('');
    setFieldErrors({});
    setEditing(row ? { id: row.id, form: toForm ? toForm(row) : { ...row } } : { form: { ...defaults } });
  };

  const setValue = (name, value) => setEditing((s) => ({ ...s, form: { ...s.form, [name]: value } }));

  const afterChange = (message) => {
    setNotice(message);
    setError('');
    reload();
    onChanged?.();
  };

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');
    setFieldErrors({});
    try {
      const payload = toPayload ? toPayload(editing.form) : editing.form;
      if (editing.id) await update(editing.id, payload);
      else await create(payload);
      afterChange(`${singular} ${editing.id ? 'updated' : 'created'}.`);
      setEditing(null);
    } catch (err) {
      setFormError(err.message);
      setFieldErrors(err.fieldErrors || {});
    } finally {
      setSaving(false);
    }
  };

  const del = async (row) => {
    if (!window.confirm(`Delete this ${singular.toLowerCase()}? This cannot be undone.`)) return;
    try {
      await remove(row.id);
      afterChange(`${singular} deleted.`);
    } catch (err) {
      setNotice('');
      setError(err.message);
    }
  };

  return (
    <section aria-label={title}>
      <div className="admin__head">
        <div>
          <h2>{title}</h2>
          {hint && <p className="muted">{hint}</p>}
        </div>
        <button className="btn" onClick={() => openForm(null)}>Add {singular.toLowerCase()}</button>
      </div>

      {notice && <p className="alert alert--ok" role="status">{notice}</p>}
      {error && <p className="alert alert--err" role="alert">{error}</p>}

      {rows === null ? <p className="muted">Loading…</p> : rows.length === 0 ? (
        <p className="empty">No {title.toLowerCase()} yet. Use “Add {singular.toLowerCase()}” to create the first one.</p>
      ) : (
        <div className="tablewrap">
          <table className="table">
            <thead>
              <tr>{columns.map((c) => <th key={c.label}>{c.label}</th>)}<th><span className="sr-only">Actions</span></th></tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  {columns.map((c) => <td key={c.label}>{c.render ? c.render(row) : row[c.key]}</td>)}
                  <td className="table__actions">
                    <button className="btn btn--ghost btn--sm" onClick={() => openForm(row)}>Edit</button>
                    <button className="btn btn--danger btn--sm" onClick={() => del(row)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {editing && (
        <div className="modal" role="presentation" onMouseDown={(e) => e.target === e.currentTarget && setEditing(null)}>
          <form className="modal__panel" onSubmit={submit} role="dialog" aria-modal="true" aria-label={`${editing.id ? 'Edit' : 'Add'} ${singular.toLowerCase()}`}>
            <h2>{editing.id ? `Edit ${singular.toLowerCase()}` : `Add ${singular.toLowerCase()}`}</h2>
            {formError && <p className="alert alert--err" role="alert">{formError}</p>}
            <div className="formgrid">
              {fields.map((f) => (
                <Field key={f.name} field={f} value={editing.form[f.name]} error={fieldErrors[f.name]} onChange={(v) => setValue(f.name, v)} />
              ))}
            </div>
            <div className="modal__actions">
              <button type="button" className="btn btn--ghost" onClick={() => setEditing(null)}>Cancel</button>
              <button className="btn" disabled={saving}>{saving ? 'Saving…' : `Save ${singular.toLowerCase()}`}</button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}

function Field({ field, value, error, onChange }) {
  const { name, label, type = 'text', options = [], required, step, wide } = field;
  const id = `f-${name}`;
  const common = { id, name, required, 'aria-invalid': !!error };
  let input;

  if (type === 'textarea') {
    input = <textarea {...common} rows={4} value={value ?? ''} onChange={(e) => onChange(e.target.value)} />;
  } else if (type === 'select') {
    input = (
      <select {...common} value={value ?? ''} onChange={(e) => onChange(e.target.value)}>
        <option value="">Select…</option>
        {options.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
    );
  } else if (type === 'checkbox') {
    return (
      <label className="check formgrid__wide">
        <input type="checkbox" checked={!!value} onChange={(e) => onChange(e.target.checked)} /> {label}
      </label>
    );
  } else if (type === 'checks') {
    const selected = value || [];
    input = (
      <div className="checks" id={id}>
        {options.map((o) => (
          <label key={o.value} className="check">
            <input
              type="checkbox"
              checked={selected.includes(o.value)}
              onChange={(e) => onChange(e.target.checked ? [...selected, o.value] : selected.filter((v) => v !== o.value))}
            />{' '}{o.label}
          </label>
        ))}
      </div>
    );
  } else {
    const htmlType = type === 'datetime' ? 'datetime-local' : type;
    input = <input {...common} type={htmlType} step={step} value={value ?? ''} onChange={(e) => onChange(e.target.value)} />;
  }

  return (
    <div className={`field ${wide || type === 'textarea' || type === 'checks' ? 'formgrid__wide' : ''}`}>
      <label htmlFor={id}>{label}</label>
      {input}
      {error && <span className="field__error">{error}</span>}
    </div>
  );
}
