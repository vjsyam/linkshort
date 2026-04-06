import { useState } from 'react';
import toast from 'react-hot-toast';

function ShortenForm({ onShorten, onBulkShorten, loading, error, setError }) {
  const [url, setUrl] = useState('');
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [customAlias, setCustomAlias] = useState('');
  const [expiryMinutes, setExpiryMinutes] = useState('');
  const [title, setTitle] = useState('');
  const [password, setPassword] = useState('');
  const [bulkMode, setBulkMode] = useState(false);
  const [bulkUrls, setBulkUrls] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (bulkMode) {
      const lines = bulkUrls.split('\n').map(l => l.trim()).filter(l => l.startsWith('http'));
      if (lines.length === 0) { toast.error('Enter at least one valid URL'); return; }
      if (lines.length > 20) { toast.error('Maximum 20 URLs per batch'); return; }
      try {
        await onBulkShorten(lines);
        toast.success(`${lines.length} URLs shortened!`);
        setBulkUrls('');
      } catch (err) { toast.error(err.message); }
      return;
    }

    if (!url.trim()) { toast.error('Paste a URL first'); return; }
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      toast.error('URL must start with http:// or https://');
      return;
    }

    try {
      await onShorten(url.trim(), customAlias || null, expiryMinutes || null, title || null, password || null);
      toast.success('Link created!');
      setUrl(''); setCustomAlias(''); setExpiryMinutes(''); setTitle(''); setPassword('');
    } catch (err) { toast.error(err.message); }
  };

  return (
    <div className="form-wrap anim-fade-up" style={{ animationDelay: '150ms' }}>
      <form onSubmit={handleSubmit} className="neo-card shorten-form" id="shorten-form">
        {/* Mode selector */}
        <div className="mode-tabs">
          <button type="button" className={`mode-tab ${!bulkMode ? 'active' : ''}`} onClick={() => setBulkMode(false)}>
            Single URL
          </button>
          <button type="button" className={`mode-tab ${bulkMode ? 'active' : ''}`} onClick={() => setBulkMode(true)}>
            Bulk Mode
          </button>
        </div>

        {bulkMode ? (
          <textarea
            className="bulk-input"
            placeholder={"Paste URLs (one per line):\nhttps://example.com\nhttps://another-site.com"}
            value={bulkUrls}
            onChange={(e) => setBulkUrls(e.target.value)}
            rows={5}
            disabled={loading}
            id="bulk-input"
          />
        ) : (
          <div className="input-row">
            <input
              type="text"
              className="url-input"
              placeholder="Paste any long URL here..."
              value={url}
              onChange={(e) => { setUrl(e.target.value); setError(null); }}
              disabled={loading}
              id="url-input"
              autoComplete="off"
            />
            <button type="submit" className="btn-neon submit-btn" disabled={loading || (!bulkMode && !url.trim())} id="shorten-btn">
              {loading ? (
                <span className="spinner" />
              ) : (
                <>Shorten</>
              )}
            </button>
          </div>
        )}

        {bulkMode && (
          <button type="submit" className="btn-neon submit-btn full-width" disabled={loading || !bulkUrls.trim()} style={{ marginTop: '0.75rem' }}>
            {loading ? <span className="spinner" /> : `Shorten All`}
          </button>
        )}

        {/* Advanced toggle */}
        {!bulkMode && (
          <>
            <button
              type="button"
              className="adv-toggle"
              onClick={() => setShowAdvanced(!showAdvanced)}
              id="advanced-toggle"
            >
              <span className={`adv-chevron ${showAdvanced ? 'open' : ''}`}>›</span>
              Advanced Options
            </button>

            {showAdvanced && (
              <div className="adv-grid anim-fade-in">
                <div className="adv-field">
                  <label>Custom Alias</label>
                  <input type="text" placeholder="my-brand" value={customAlias} onChange={(e) => setCustomAlias(e.target.value)} maxLength={20} disabled={loading} id="custom-alias" />
                </div>
                <div className="adv-field">
                  <label>Expires in (min)</label>
                  <input type="number" placeholder="1440 = 24h" value={expiryMinutes} onChange={(e) => setExpiryMinutes(e.target.value)} min={1} disabled={loading} id="expiry-minutes" />
                </div>
                <div className="adv-field">
                  <label>Label / Title</label>
                  <input type="text" placeholder="My campaign link" value={title} onChange={(e) => setTitle(e.target.value)} maxLength={100} disabled={loading} id="link-title" />
                </div>
                <div className="adv-field">
                  <label>Password Protect</label>
                  <input type="text" placeholder="Optional password" value={password} onChange={(e) => setPassword(e.target.value)} maxLength={100} disabled={loading} id="link-password" />
                </div>
              </div>
            )}
          </>
        )}
      </form>
    </div>
  );
}

export default ShortenForm;
