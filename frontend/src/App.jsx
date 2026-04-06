import { useState } from 'react';
import './App.css';
import Header from './components/Header';
import HeroSection from './components/HeroSection';
import ShortenForm from './components/ShortenForm';
import ResultCard from './components/ResultCard';
import LinkDashboard from './components/LinkDashboard';
import AnalyticsChart from './components/AnalyticsChart';
import AuthModal from './components/AuthModal';
import Footer from './components/Footer';
import { useShorten } from './hooks/useShorten';
import { useAuth } from './hooks/useAuth.jsx';
import { Toaster } from 'react-hot-toast';

function App() {
  const { user } = useAuth();
  const {
    urls, analytics, loading, error,
    shortenUrl, bulkShorten, fetchMyUrls,
    fetchAnalytics, toggleUrl, deleteUrl, setError,
  } = useShorten();

  // results can be a single item or an array (bulk mode)
  const [results, setResults] = useState([]);
  const [selectedCode, setSelectedCode] = useState(null);
  const [showAuth, setShowAuth] = useState(false);
  const [authMode, setAuthMode] = useState('login');

  const handleShorten = async (url, alias, expiry, title, password) => {
    try {
      const data = await shortenUrl(url, alias, expiry, title, password);
      setResults([data]);
    } catch {}
  };

  const handleBulk = async (urlList) => {
    try {
      const data = await bulkShorten(urlList);
      // data is an array of ShortenResponse objects
      if (Array.isArray(data)) {
        setResults(data);
      }
    } catch {}
  };

  const openAuth = (mode) => {
    setAuthMode(mode);
    setShowAuth(true);
  };

  return (
    <div className="app">
      <Toaster
        position="top-right"
        toastOptions={{
          style: {
            background: '#111118',
            color: '#f2f2f7',
            border: '1px solid rgba(0,240,255,0.15)',
            borderRadius: '10px',
            fontFamily: 'Outfit, sans-serif',
            fontSize: '0.875rem',
          },
          success: { iconTheme: { primary: '#32d74b', secondary: '#111118' } },
          error: { iconTheme: { primary: '#ff375f', secondary: '#111118' } },
        }}
      />

      <Header onOpenAuth={openAuth} />

      <main className="main">
        <div className="container">
          <HeroSection />

          <ShortenForm
            onShorten={handleShorten}
            onBulkShorten={handleBulk}
            loading={loading}
            error={error}
            setError={setError}
          />

          {/* Show all results — works for both single and bulk */}
          {results.length > 0 && (
            <div className="results-list">
              {results.length > 1 && (
                <div className="bulk-results-header">
                  <span className="bulk-badge">✨ {results.length} links created</span>
                  <button className="btn-ghost btn-sm" onClick={() => setResults([])}>Clear</button>
                </div>
              )}
              {results.map((r, i) => (
                <ResultCard key={r.shortCode || i} result={r} />
              ))}
            </div>
          )}

          <LinkDashboard
            urls={urls}
            onRefresh={user ? fetchMyUrls : null}
            onViewAnalytics={(code) => { setSelectedCode(code); fetchAnalytics(code).catch(() => {}); }}
            onToggle={toggleUrl}
            onDelete={deleteUrl}
            isLoggedIn={!!user}
          />

          {selectedCode && analytics && (
            <AnalyticsChart
              analytics={analytics}
              onClose={() => setSelectedCode(null)}
            />
          )}
        </div>
      </main>

      <Footer />

      {showAuth && (
        <AuthModal
          mode={authMode}
          onSwitchMode={(m) => setAuthMode(m)}
          onClose={() => setShowAuth(false)}
        />
      )}
    </div>
  );
}

export default App;
