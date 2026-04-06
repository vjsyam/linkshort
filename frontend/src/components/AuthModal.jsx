import { useState } from 'react';
import { useAuth } from '../hooks/useAuth.jsx';
import toast from 'react-hot-toast';

function AuthModal({ mode, onSwitchMode, onClose }) {
  const { login, register } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);

  const isLogin = mode === 'login';

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      if (isLogin) {
        await login(email, password);
        toast.success('Welcome back!');
      } else {
        if (!name.trim()) { toast.error('Name is required'); setLoading(false); return; }
        await register(email, password, name.trim());
        toast.success('Account created!');
      }
      onClose();
    } catch (err) {
      toast.error(err.response?.data?.message || err.message || 'Auth failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay anim-fade-in" onClick={onClose}>
      <div className="neo-card modal-card anim-fade-up" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>✕</button>

        <h2 className="modal-title">
          {isLogin ? 'Welcome Back' : 'Create Account'}
        </h2>
        <p className="modal-desc">
          {isLogin
            ? 'Sign in to manage your links and view analytics'
            : 'Join to unlock link management, history, and more'}
        </p>

        <form onSubmit={handleSubmit} className="auth-form">
          {!isLogin && (
            <div className="auth-field">
              <label>Display Name</label>
              <input
                type="text"
                placeholder="John Doe"
                value={name}
                onChange={(e) => setName(e.target.value)}
                disabled={loading}
                id="auth-name"
              />
            </div>
          )}
          <div className="auth-field">
            <label>Email</label>
            <input
              type="email"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              disabled={loading}
              id="auth-email"
              autoComplete="email"
            />
          </div>
          <div className="auth-field">
            <label>Password</label>
            <input
              type="password"
              placeholder={isLogin ? '••••••' : 'Min 6 characters'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={6}
              disabled={loading}
              id="auth-password"
              autoComplete={isLogin ? 'current-password' : 'new-password'}
            />
          </div>

          <button type="submit" className="btn-neon full-width" disabled={loading} id="auth-submit">
            {loading ? 'Please wait...' : (isLogin ? 'Sign In' : 'Create Account')}
          </button>
        </form>

        <p className="auth-switch">
          {isLogin ? "Don't have an account? " : 'Already have an account? '}
          <button
            type="button"
            className="switch-link"
            onClick={() => onSwitchMode(isLogin ? 'register' : 'login')}
          >
            {isLogin ? 'Sign Up' : 'Sign In'}
          </button>
        </p>
      </div>
    </div>
  );
}

export default AuthModal;
