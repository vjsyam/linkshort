import { useState, useEffect, useCallback, createContext, useContext } from 'react';
import { login as loginApi, register as registerApi } from '../api/api';

const AuthContext = createContext(null);

// Listeners that should be called when auth state changes
let authChangeListeners = [];

export function onAuthChange(fn) {
  authChangeListeners.push(fn);
  return () => { authChangeListeners = authChangeListeners.filter(l => l !== fn); };
}

function notifyAuthChange(user) {
  authChangeListeners.forEach(fn => fn(user));
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('ls_token');
    const email = localStorage.getItem('ls_email');
    const displayName = localStorage.getItem('ls_name');
    if (token && email) {
      setUser({ token, email, displayName });
    }
    setLoading(false);
  }, []);

  const login = useCallback(async (email, password) => {
    const { data } = await loginApi(email, password);
    localStorage.setItem('ls_token', data.token);
    localStorage.setItem('ls_email', data.email);
    localStorage.setItem('ls_name', data.displayName);
    setUser(data);
    notifyAuthChange(data);
    return data;
  }, []);

  const register = useCallback(async (email, password, displayName) => {
    const { data } = await registerApi(email, password, displayName);
    localStorage.setItem('ls_token', data.token);
    localStorage.setItem('ls_email', data.email);
    localStorage.setItem('ls_name', data.displayName);
    setUser(data);
    notifyAuthChange(data);
    return data;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('ls_token');
    localStorage.removeItem('ls_email');
    localStorage.removeItem('ls_name');
    setUser(null);
    notifyAuthChange(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be inside AuthProvider');
  return ctx;
}
