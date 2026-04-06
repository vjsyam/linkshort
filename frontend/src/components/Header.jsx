import { useAuth } from '../hooks/useAuth.jsx';

function Header({ onOpenAuth }) {
  const { user, logout } = useAuth();

  return (
    <header className="header">
      <div className="container header-inner">
        <a href="/" className="logo" id="logo">
          <div className="logo-mark">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="url(#logoGrad)" strokeWidth="2.5" strokeLinecap="round">
              <defs><linearGradient id="logoGrad" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stopColor="#00f0ff"/><stop offset="100%" stopColor="#bf5af2"/></linearGradient></defs>
              <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
              <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
            </svg>
          </div>
          <span className="logo-text">Link<span className="logo-accent">Short</span></span>
        </a>

        <nav className="header-nav">
          {user ? (
            <div className="user-section">
              <div className="user-avatar" title={user.email}>
                {(user.displayName || user.email)[0].toUpperCase()}
              </div>
              <span className="user-name">{user.displayName || user.email.split('@')[0]}</span>
              <button className="btn-ghost btn-sm" onClick={logout} id="logout-btn">
                Sign Out
              </button>
            </div>
          ) : (
            <div className="auth-buttons">
              <button className="btn-ghost btn-sm" onClick={() => onOpenAuth('login')} id="login-btn">
                Sign In
              </button>
              <button className="btn-neon btn-sm" onClick={() => onOpenAuth('register')} id="register-btn">
                Get Started
              </button>
            </div>
          )}
        </nav>
      </div>
    </header>
  );
}

export default Header;
