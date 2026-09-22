import { useState, useCallback, useEffect, useRef } from 'react';
import {
  shortenUrl as shortenApi, getMyUrls, getAnalytics,
  toggleUrl as toggleApi, deleteUrl as deleteApi, bulkShorten as bulkApi,
  claimUrls,
} from '../api/api';
import { onAuthChange } from './useAuth.jsx';

function getStoredAnonUrls() {
  try {
    const raw = localStorage.getItem('ls_anon_urls');
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function saveStoredAnonUrls(urls) {
  try {
    localStorage.setItem('ls_anon_urls', JSON.stringify(urls));
  } catch {}
}

export function useShorten() {
  const [urls, setUrls] = useState(() => {
    return localStorage.getItem('ls_token') ? [] : getStoredAnonUrls();
  });
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Listen for auth changes
  useEffect(() => {
    const unsub = onAuthChange(async (user) => {
      setAnalytics(null);
      setError(null);

      if (user) {
        // Logged in — claim any anonymous links created before logging in
        const anonUrls = getStoredAnonUrls();
        if (anonUrls.length > 0) {
          try {
            const codes = anonUrls.map(u => u.shortCode).filter(Boolean);
            if (codes.length > 0) {
              await claimUrls(codes);
            }
          } catch {}
          localStorage.removeItem('ls_anon_urls');
        }

        // Fetch user's links from server
        try {
          const r = await getMyUrls();
          setUrls(Array.isArray(r.data) ? r.data : []);
        } catch { setUrls([]); }
      } else {
        // Logged out — restore any anonymous session links
        setUrls(getStoredAnonUrls());
      }
    });
    return () => { unsub(); };
  }, []);

  const shortenUrl = useCallback(async (originalUrl, customAlias, expiryMinutes, title, password) => {
    setLoading(true);
    setError(null);
    try {
      const { data } = await shortenApi(originalUrl, customAlias, expiryMinutes, title, password);
      setUrls(prev => {
        const next = [data, ...prev];
        if (!localStorage.getItem('ls_token')) {
          saveStoredAnonUrls(next);
        }
        return next;
      });
      return data;
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to shorten URL';
      setError(msg);
      throw new Error(msg);
    } finally {
      setLoading(false);
    }
  }, []);

  const bulkShorten = useCallback(async (urlList) => {
    setLoading(true);
    setError(null);
    try {
      const { data } = await bulkApi(urlList);
      if (Array.isArray(data)) {
        setUrls(prev => {
          const next = [...data, ...prev];
          if (!localStorage.getItem('ls_token')) {
            saveStoredAnonUrls(next);
          }
          return next;
        });
      }
      return data;
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Bulk shorten failed';
      setError(msg);
      throw new Error(msg);
    } finally {
      setLoading(false);
    }
  }, []);

  // Only logged-in users fetch from server
  const fetchMyUrls = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await getMyUrls();
      setUrls(Array.isArray(data) ? data : []);
    } catch { setError('Failed to load your URLs'); }
    finally { setLoading(false); }
  }, []);

  const fetchAnalytics = useCallback(async (shortCode) => {
    setLoading(true);
    try {
      const { data } = await getAnalytics(shortCode);
      setAnalytics(data);
      return data;
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to load analytics';
      setError(msg);
      throw new Error(msg);
    }
    finally { setLoading(false); }
  }, []);

  const toggleUrl = useCallback(async (shortCode) => {
    try {
      const { data } = await toggleApi(shortCode);
      setUrls(prev => prev.map(u => u.shortCode === shortCode ? data : u));
      return data;
    } catch (err) {
      throw new Error(err.response?.data?.message || 'Toggle failed');
    }
  }, []);

  const deleteUrl = useCallback(async (shortCode) => {
    try {
      await deleteApi(shortCode);
      setUrls(prev => prev.filter(u => u.shortCode !== shortCode));
    } catch (err) {
      throw new Error(err.response?.data?.message || 'Delete failed');
    }
  }, []);

  const clearUrls = useCallback(() => {
    setUrls([]);
    setAnalytics(null);
  }, []);

  return {
    urls, analytics, loading, error,
    shortenUrl, bulkShorten, fetchMyUrls,
    fetchAnalytics, toggleUrl, deleteUrl, setError, clearUrls,
  };
}
