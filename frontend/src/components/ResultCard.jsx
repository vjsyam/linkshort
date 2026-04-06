import { useState } from 'react';
import toast from 'react-hot-toast';
import { getQrCodeUrl } from '../api/api';

function ResultCard({ result }) {
  const [copied, setCopied] = useState(false);
  const [showQr, setShowQr] = useState(false);

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

  return (
    <div className="result-wrap anim-fade-up">
      <div className="neo-card result-card" id="result-card">
        <div className="result-status">
          <span className="status-dot live" />
          <span className="status-text">Link Active</span>
          {result.hasPassword && <span className="tag tag-lock">🔒 Protected</span>}
        </div>

        <div className="result-url-row">
          <a href={result.shortUrl} target="_blank" rel="noopener noreferrer" className="result-link" id="result-short-url">
            {result.shortUrl}
          </a>
          <div className="result-actions">
            <button className={`btn-ghost btn-sm ${copied ? 'copied' : ''}`} onClick={handleCopy} id="copy-btn">
              {copied ? '✓ Copied' : 'Copy'}
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
          {result.title && <span className="meta-chip">📌 {result.title}</span>}
          <span className="meta-chip">📅 {fmt(result.createdAt)}</span>
          {result.expiryDate && <span className="meta-chip">⏰ Expires {fmt(result.expiryDate)}</span>}
        </div>

        {showQr && (
          <div className="qr-panel anim-fade-in">
            <div className="qr-frame">
              <img src={getQrCodeUrl(result.shortCode)} alt="QR Code" width="180" height="180" id="qr-code-img" />
            </div>
            <p className="qr-hint">Scan from any device on the same network</p>
          </div>
        )}
      </div>
    </div>
  );
}

export default ResultCard;
