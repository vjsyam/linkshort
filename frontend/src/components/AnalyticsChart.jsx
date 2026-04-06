import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

function AnalyticsChart({ analytics, onClose }) {
  const { totalClicks, clicksOverTime, shortCode, originalUrl, browsers, devices, referrers, uniqueVisitors } = analytics;

  const dailyAvg = clicksOverTime?.length ? (totalClicks / clicksOverTime.length).toFixed(1) : 0;
  const peak = clicksOverTime?.length
    ? clicksOverTime.reduce((mx, d) => d.clicks > mx.clicks ? d : mx, clicksOverTime[0])
    : { date: '—', clicks: 0 };

  const fmtX = (d) => { const dt = new Date(d); return dt.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }); };

  const CustomTooltip = ({ active, payload, label }) => {
    if (!active || !payload?.length) return null;
    return (
      <div style={{ background: '#111118', border: '1px solid rgba(0,240,255,0.15)', borderRadius: 8, padding: '10px 14px', fontSize: '0.8125rem' }}>
        <p style={{ color: '#8e8e93', marginBottom: 2 }}>{fmtX(label)}</p>
        <p style={{ color: '#00f0ff', fontWeight: 600 }}>{payload[0].value} clicks</p>
      </div>
    );
  };

  const BreakdownBar = ({ items, colorVar }) => {
    if (!items || items.length === 0) return <p className="bd-empty">No data yet</p>;
    return (
      <div className="bd-list">
        {items.map((item, i) => (
          <div key={i} className="bd-item">
            <div className="bd-item-header">
              <span className="bd-name">{item.name}</span>
              <span className="bd-count">{item.count} <span className="bd-pct">({item.percentage}%)</span></span>
            </div>
            <div className="bd-bar-wrap">
              <div className="bd-bar" style={{ width: `${Math.max(item.percentage, 2)}%`, background: colorVar }} />
            </div>
          </div>
        ))}
      </div>
    );
  };

  return (
    <section className="analytics-section anim-fade-up" id="analytics-section">
      <div className="neo-card analytics-card">
        <div className="an-header">
          <div>
            <h2 className="an-title">
              Analytics <span className="an-code">/{shortCode}</span>
            </h2>
            <p className="an-target">{originalUrl}</p>
          </div>
          <button className="btn-ghost btn-sm" onClick={onClose} id="close-analytics-btn">✕ Close</button>
        </div>

        {/* Stats row */}
        <div className="an-stats stagger">
          <div className="an-stat anim-fade-up">
            <span className="an-stat-val">{totalClicks || 0}</span>
            <span className="an-stat-label">Total Clicks</span>
          </div>
          <div className="an-stat anim-fade-up">
            <span className="an-stat-val">{uniqueVisitors || 0}</span>
            <span className="an-stat-label">Unique Visitors</span>
          </div>
          <div className="an-stat anim-fade-up">
            <span className="an-stat-val">{dailyAvg}</span>
            <span className="an-stat-label">Daily Avg</span>
          </div>
          <div className="an-stat anim-fade-up">
            <span className="an-stat-val">{peak?.clicks || 0}</span>
            <span className="an-stat-label">Peak Day</span>
          </div>
        </div>

        {/* Chart */}
        <div className="chart-wrap">
          <ResponsiveContainer width="100%" height={240}>
            <AreaChart data={clicksOverTime} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
              <defs>
                <linearGradient id="cyanGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#00f0ff" stopOpacity={0.3} />
                  <stop offset="100%" stopColor="#00f0ff" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" vertical={false} />
              <XAxis dataKey="date" tickFormatter={fmtX} stroke="rgba(255,255,255,0.15)" tick={{ fill: '#48484a', fontSize: 10 }} interval="preserveStartEnd" />
              <YAxis stroke="rgba(255,255,255,0.15)" tick={{ fill: '#48484a', fontSize: 10 }} allowDecimals={false} />
              <Tooltip content={<CustomTooltip />} />
              <Area type="monotone" dataKey="clicks" stroke="#00f0ff" strokeWidth={2} fill="url(#cyanGrad)" animationDuration={800} />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Breakdowns */}
        <div className="bd-grid">
          <div className="bd-section">
            <h3 className="bd-title">🌐 Browsers</h3>
            <BreakdownBar items={browsers} colorVar="var(--neon-cyan)" />
          </div>
          <div className="bd-section">
            <h3 className="bd-title">📱 Devices</h3>
            <BreakdownBar items={devices} colorVar="var(--neon-violet)" />
          </div>
          <div className="bd-section">
            <h3 className="bd-title">🔗 Referrers</h3>
            <BreakdownBar items={referrers} colorVar="var(--neon-lime)" />
          </div>
        </div>
      </div>
    </section>
  );
}

export default AnalyticsChart;
