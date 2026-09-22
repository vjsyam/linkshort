import { useState } from 'react';
import toast from 'react-hot-toast';
import { getQrCodeUrl } from '../api/api';

function ResultCard({ result }) {
  const [copied, setCopied] = useState(false);
  const [showQr, setShowQr] = useState(false);
  const [qrTarget, setQrTarget] = useState('direct'); // Default to direct site URL as requested

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(result.shortUrl);
    } catch {
      const ta = document.createElement('textarea');
      ta.value = result.shortUrl;
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
    }
    setCopied(true);
    toast.success('Copied!');
    setTimeout(() => setCopied(false), 2000);
  };

  const fmt = (d) => d ? new Date(d).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—';

  const currentQrUrl = qrTarget === 'direct' ? result.originalUrl : result.shortUrl;
  const qrImageUrl = getQrCodeUrl(result.shortCode, qrTarget, qrTarget === 'direct' ? result.originalUrl : null);

  const handleDownloadQr = async () => {
    try {
      const response = await fetch(qrImageUrl);
      const blob = await response.blob();
      const blobUrl = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = blobUrl;
      a.download = `qr-${result.shortCode}-${qrTarget}.png`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(blobUrl);
      toast.success('QR Code downloaded!');
    } catch (e) {
      toast.error('Failed to download QR code');
    }
  };

  return (
    <div className="result-wrap anim-fade-up">
      <div className="neo-card result-card" id="result-card">
        <div className="result-status">
          <span className="status-dot live" />
          <span className="status-text">Link Active</span>
          {result.hasPassword && <span className="tag tag-lock">Protected</span>}
        </div>

        <div className="result-url-row">
          <a href={result.shortUrl} target="_blank" rel="noopener noreferrer" className="result-link" id="result-short-url">
            {result.shortUrl}
          </a>
          <div className="result-actions">
            <button className={`btn-ghost btn-sm ${copied ? 'copied' : ''}`} onClick={handleCopy} id="copy-btn">
              {copied ? 'Copied' : 'Copy'}
            </button>
            <button className="btn-ghost btn-sm" onClick={() => setShowQr(!showQr)} id="qr-toggle-btn">
              {showQr ? 'Hide QR' : 'QR Code'}
            </button>
          </div>
        </div>

        <div className="result-orig">
          <span className="result-orig-label">TARGET</span>
          <span className="result-orig-url">{result.originalUrl}</span>
        </div>

        <div className="result-meta-row">
          {result.title && <span className="meta-chip">Title: {result.title}</span>}
          <span className="meta-chip">Created: {fmt(result.createdAt)}</span>
          {result.expiryDate && <span className="meta-chip">Expires: {fmt(result.expiryDate)}</span>}
        </div>

        {showQr && (
          <div className="qr-panel anim-fade-in" style={{ marginTop: '1.25rem', paddingTop: '1rem', borderTop: '1px solid rgba(255,255,255,0.08)' }}>
            <div style={{ display: 'flex', justifyContent: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
              <button
                type="button"
                className={`btn-ghost btn-xs ${qrTarget === 'direct' ? 'active' : ''}`}
                style={{
                  background: qrTarget === 'direct' ? 'rgba(0, 240, 255, 0.2)' : 'transparent',
                  borderColor: qrTarget === 'direct' ? '#00f0ff' : 'rgba(255,255,255,0.15)',
                  color: qrTarget === 'direct' ? '#00f0ff' : '#aaa'
                }}
                onClick={() => setQrTarget('direct')}
              >
                Destination Site QR
              </button>
              <button
                type="button"
                className={`btn-ghost btn-xs ${qrTarget === 'short' ? 'active' : ''}`}
                style={{
                  background: qrTarget === 'short' ? 'rgba(0, 240, 255, 0.2)' : 'transparent',
                  borderColor: qrTarget === 'short' ? '#00f0ff' : 'rgba(255,255,255,0.15)',
                  color: qrTarget === 'short' ? '#00f0ff' : '#aaa'
                }}
                onClick={() => setQrTarget('short')}
              >
                Short Link QR (Track Clicks)
              </button>
            </div>

            <div className="qr-frame" style={{ margin: '0 auto', display: 'flex', justifyContent: 'center' }}>
              <img
                src={qrImageUrl}
                alt="QR Code"
                width="180"
                height="180"
                id="qr-code-img"
                style={{ borderRadius: '8px', background: '#fff', padding: '6px' }}
              />
            </div>

            <p className="qr-hint" style={{ marginTop: '0.75rem', fontSize: '0.8rem', color: '#999', textAlign: 'center' }}>
              Maps to: <strong style={{ color: '#00f0ff', wordBreak: 'break-all' }}>{currentQrUrl}</strong>
            </p>

            <div style={{ display: 'flex', justifyContent: 'center', marginTop: '0.5rem' }}>
              <button type="button" className="btn-ghost btn-xs" onClick={handleDownloadQr}>
                Download QR Code
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default ResultCard;
