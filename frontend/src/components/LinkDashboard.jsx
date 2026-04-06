import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';

function LinkDashboard({ urls, onRefresh, onViewAnalytics, onToggle, onDelete, isLoggedIn }) {
  const [search, setSearch] = useState('');

  // Only fetch from server for logged-in users
  useEffect(() => {
    if (isLoggedIn && onRefresh) {
      onRefresh();
    }
  }, [isLoggedIn]);

  const filtered = urls.filter(u =>
    (u.shortCode?.toLowerCase().includes(search.toLowerCase())) ||
    (u.originalUrl?.toLowerCase().includes(search.toLowerCase())) ||
    (u.title?.toLowerCase().includes(search.toLowerCase()))
  );

  const handleToggle = async (code) => {
    try {
      const updated = await onToggle(code);
      toast.success(updated.isActive ? 'Link activated' : 'Link deactivated');
    } catch (err) { toast.error(err.message); }
  };

  const handleDelete = async (code) => {
    if (!confirm('Delete this link permanently?')) return;
    try {
      await onDelete(code);
      toast.success('Link deleted');
    } catch (err) { toast.error(err.message); }
  };

  const handleAnalytics = async (code) => {
    try {
      await onViewAnalytics(code);
    } catch (err) {
      toast.error('Cannot view analytics for this link');
    }
  };

  const fmt = (d) => d ? new Date(d).toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : '';

  // Don't show section at all if anonymous and no urls
  if (!isLoggedIn && urls.length === 0) return null;

  return (
    <section className="dashboard-section anim-fade-up" style={{ animationDelay: '300ms' }} id="link-dashboard">
      <div className="dash-header">
        <div className="dash-title-row">
          <h2 className="dash-title">
            {isLoggedIn ? 'Your Links' : 'Session Links'}
          </h2>
          <span className="dash-count">{urls.length}</span>
          {!isLoggedIn && urls.length > 0 && (
            <span className="session-hint">Auto-clears in 5 min</span>
          )}
        </div>
        <div className="dash-controls">
          {urls.length > 1 && (
            <div className="search-box">
              <span className="search-icon">⌕</span>
              <input
                type="text"
                placeholder="Search links..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="search-input"
                id="search-input"
              />
            </div>
          )}
          {isLoggedIn && onRefresh && (
            <button className="btn-ghost btn-sm" onClick={onRefresh} id="refresh-btn">↻ Refresh</button>
          )}
        </div>
      </div>

      {filtered.length === 0 ? (
        <div className="neo-card empty-card">
          <div className="empty-icon">🔗</div>
          <p className="empty-text">
            {search ? 'No links match your search' : 'No links yet — create your first one above!'}
          </p>
        </div>
      ) : (
        <div className="link-grid stagger">
          {filtered.map((link, i) => (
            <div
              key={link.shortCode || i}
              className={`neo-card link-card anim-fade-up ${!link.isActive ? 'inactive' : ''}`}
            >
              <div className="lc-top">
                <div className="lc-status">
                  <span className={`status-dot ${link.isActive ? 'live' : 'off'}`} />
                  <span className="lc-code">{link.shortCode}</span>
                </div>
                <div className="lc-clicks">
                  <span className="lc-clicks-val">{link.clickCount || 0}</span>
                  <span className="lc-clicks-label">clicks</span>
                </div>
              </div>

              {link.title && <p className="lc-title">{link.title}</p>}

              <p className="lc-url" title={link.originalUrl}>{link.originalUrl}</p>

              <div className="lc-meta">
                <span>{fmt(link.createdAt)}</span>
                {link.hasPassword && <span className="tag tag-lock">🔒</span>}
              </div>

              <div className="lc-actions">
                {/* Analytics only for logged-in users who own the link */}
                {isLoggedIn && (
                  <button className="btn-ghost btn-xs" onClick={() => handleAnalytics(link.shortCode)}>
                    📊 Analytics
                  </button>
                )}
                {isLoggedIn && (
                  <>
                    <button
                      className={`btn-ghost btn-xs ${link.isActive ? '' : 'btn-warn'}`}
                      onClick={() => handleToggle(link.shortCode)}
                    >
                      {link.isActive ? '⏸ Disable' : '▶ Enable'}
                    </button>
                    <button className="btn-ghost btn-xs btn-danger" onClick={() => handleDelete(link.shortCode)}>
                      🗑
                    </button>
                  </>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

export default LinkDashboard;
