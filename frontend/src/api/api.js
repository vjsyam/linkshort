import axios from 'axios';

// In production: VITE_API_URL = "https://linkshort-api.onrender.com/api"
// In dev: falls back to "/api" (proxied by Vite to localhost:8080)
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
});

// Force-logout callback — registered by AuthProvider at mount time.
// This avoids a circular dependency (api.js ↔ useAuth.jsx).
let _forceLogoutFn = null;
export function registerForceLogout(fn) {
  _forceLogoutFn = fn;
}

// JWT interceptor — attach token to every request if available
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('ls_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor — auto-clear expired/invalid tokens
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid — force full logout to sync React state
      const hadToken = !!localStorage.getItem('ls_token');
      if (hadToken) {
        if (_forceLogoutFn) {
          _forceLogoutFn(); // clears localStorage + updates React auth state
        } else {
          // Fallback if AuthProvider hasn't mounted yet
          localStorage.removeItem('ls_token');
          localStorage.removeItem('ls_email');
          localStorage.removeItem('ls_name');
        }
      }
    }
    return Promise.reject(error);
  }
);

// Auth
export const register = (email, password, displayName) =>
  api.post('/auth/register', { email, password, displayName });

export const login = (email, password) =>
  api.post('/auth/login', { email, password });

// Shorten
export const shortenUrl = (originalUrl, customAlias, expiryMinutes, title, password) => {
  const payload = { originalUrl };
  if (customAlias) payload.customAlias = customAlias;
  if (expiryMinutes) payload.expiryMinutes = parseInt(expiryMinutes);
  if (title) payload.title = title;
  if (password) payload.password = password;
  return api.post('/shorten', payload);
};

export const bulkShorten = (urls) => api.post('/shorten/bulk', { urls });

// URLs
export const getUrls = () => api.get('/urls');
export const getMyUrls = () => api.get('/urls/my');
export const toggleUrl = (shortCode) => api.patch(`/urls/${shortCode}/toggle`);
export const deleteUrl = (shortCode) => api.delete(`/urls/${shortCode}`);
export const claimUrls = (shortCodes) => api.post('/urls/claim', { shortCodes });

// Analytics
export const getAnalytics = (shortCode) => api.get(`/analytics/${shortCode}`);

// QR
const apiBase = import.meta.env.VITE_API_URL || '/api';
export const getQrCodeUrl = (shortCode) => `${apiBase}/qr/${shortCode}`;

export default api;
