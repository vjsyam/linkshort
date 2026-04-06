import { useState, useCallback, useEffect, useRef } from 'react';
import {
  shortenUrl as shortenApi, getMyUrls, getAnalytics,
  toggleUrl as toggleApi, deleteUrl as deleteApi, bulkShorten as bulkApi,
} from '../api/api';
import { onAuthChange } from './useAuth.jsx';

const ANON_TTL_MS = 5 * 60 * 1000; // 5 minutes

export function useShorten() {
  const [urls, setUrls] = useState([]);
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const anonTimerRef = useRef(null);

  // Schedule auto-clear of anonymous session URLs after 5 min
  const scheduleAnonClear = useCallback(() => {
    if (anonTimerRef.current) clearTimeout(anonTimerRef.current);
    anonTimerRef.current = setTimeout(() => {
      if (!localStorage.getItem('ls_token')) {
        setUrls([]);
      }
    }, ANON_TTL_MS);
  }, []);

  // Listen for auth changes
  useEffect(() => {
    const unsub = onAuthChange(async (user) => {
      setAnalytics(null);
      setError(null);

      if (user) {
        // Logged in — fetch only their links
        if (anonTimerRef.current) clearTimeout(anonTimerRef.current);
        try {
          const r = await getMyUrls();
          setUrls(Array.isArray(r.data) ? r.data : []);
        } catch { setUrls([]); }
      } else {
        // Logged out — clear everything
        setUrls([]);
        if (anonTimerRef.current) clearTimeout(anonTimerRef.current);
      }
    });
    return () => { unsub(); if (anonTimerRef.current) clearTimeout(anonTimerRef.current); };
  }, []);

  const shortenUrl = useCallback(async (originalUrl, customAlias, expiryMinutes, title, password) => {
    setLoading(true);
    setError(null);
    try {
      const { data } = await shortenApi(originalUrl, customAlias, expiryMinutes, title, password);
      setUrls(prev => [data, ...prev]);

      // For anonymous users, auto-clear after 5 min
      if (!localStorage.getItem('ls_token')) {
        scheduleAnonClear();
      }

      return data;
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to shorten URL';
      setError(msg);
      throw new Error(msg);
    } finally {
      setLoading(false);
    }
  }, [scheduleAnonClear]);

  const bulkShorten = useCallback(async (urlList) => {
    setLoading(true);
    setError(null);
    try {
      const { data } = await bulkApi(urlList);
      if (Array.isArray(data)) {
        setUrls(prev => [...data, ...prev]);

        if (!localStorage.getItem('ls_token')) {
          scheduleAnonClear();
        }
      }
      return data;
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Bulk shorten failed';
      setError(msg);
      throw new Error(msg);
    } finally {
      setLoading(false);
    }
  }, [scheduleAnonClear]);

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
